import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// ── Service-role admin client (bypasses RLS) ────────────────────
const supabaseAdmin = createClient(
  Deno.env.get("SUPABASE_URL") ?? "",
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
  {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  }
);

// ── Constants ─────────────────────────────────────────────────────
const FEN_MULTIPLIER = 100;
const EXPIRY_DAYS = 30;
const ADVISORY_LOCK_KEY = 9876543;

// ── Helpers ───────────────────────────────────────────────────────
interface ExpiredItem {
  id: string;
  publisher_id: string;
  reward: number;
  title: string;
}

/**
 * Scan lost_found_items where status='active', reward > 0,
 * and reward_frozen_at is older than EXPIRY_DAYS days.
 */
async function findExpiredItems(): Promise<ExpiredItem[]> {
  const cutoff = new Date();
  cutoff.setDate(cutoff.getDate() - EXPIRY_DAYS);

  const { data, error } = await supabaseAdmin
    .from("lost_found_items")
    .select("id, publisher_id, reward, title")
    .eq("status", "active")
    .gt("reward", 0)
    .lt("reward_frozen_at", cutoff.toISOString());

  if (error) {
    console.error("Failed to scan expired items:", error.message);
    return [];
  }

  return (data as ExpiredItem[]) ?? [];
}

/**
 * Layer 2+3: Atomically close an item (status: active → closed) using
 * optimistic locking. Only the instance that successfully executes the
 * UPDATE wins ownership; other concurrent instances will see zero rows
 * affected and skip.
 *
 * Returns the closed item data if successful, null if already processed.
 */
async function atomicallyCloseItem(item: ExpiredItem): Promise<ExpiredItem | null> {
  const { data, error } = await supabaseAdmin
    .from("lost_found_items")
    .update({ status: "closed", updated_at: new Date().toISOString() })
    .eq("id", item.id)
    .eq("status", "active")
    .select("id, publisher_id, reward, title")
    .single();

  if (error) {
    // PGRST116 = no rows returned — item already closed by another instance
    if (error.code === "PGRST116") {
      console.log(`Item ${item.id} already closed by another instance, skipping`);
      return null;
    }
    console.error(`Failed to atomically close item ${item.id}:`, error.message);
    return null;
  }

  return data as ExpiredItem;
}

/**
 * Process a single expired item: refund reward to publisher, record the
 * transaction, and send a notification.
 *
 * Idempotency guarantee — three-layer protection:
 *   1. Distributed advisory lock (pg_try_advisory_lock) at caller level
 *   2. Atomic close via UPDATE RETURNING — only one instance succeeds
 *   3. Close-before-refund ordering — item is closed before any money moves
 */
async function processExpiredItem(item: ExpiredItem): Promise<boolean> {
  const itemRewardInFen = item.reward * FEN_MULTIPLIER;

  try {
    // ═══ Layer 2 + 3: Atomically close the item first ═══
    // This UPDATE acts as an optimistic lock — only the instance that
    // succeeds in flipping status='active' → 'closed' gets to refund.
    const ownedItem = await atomicallyCloseItem(item);
    if (!ownedItem) {
      // Item was already closed by another instance — safe, not a failure
      return true;
    }

    // ═══ From here on, this instance owns the item exclusively ═══

    // 1. Get publisher wallet
    const { data: wallet, error: walletError } = await supabaseAdmin
      .from("wallets")
      .select("id, balance")
      .eq("user_id", ownedItem.publisher_id)
      .single();

    if (walletError || !wallet) {
      console.error(
        `Item ${ownedItem.id} closed but wallet not found for user ${ownedItem.publisher_id} — no refund issued`
      );
      return true; // item is already closed, not a processing failure
    }

    const balanceBefore = wallet.balance as number;
    const balanceAfter = balanceBefore + itemRewardInFen;

    // 2. Refund to publisher wallet
    const { error: refundError } = await supabaseAdmin
      .from("wallets")
      .update({ balance: balanceAfter })
      .eq("user_id", ownedItem.publisher_id)
      .eq("balance", balanceBefore);

    if (refundError) {
      console.error(`Failed to refund wallet for item ${ownedItem.id}:`, refundError.message);
      return false;
    }

    // 3. Insert wallet_transactions
    try {
      await supabaseAdmin.from("wallet_transactions").insert({
        user_id: ownedItem.publisher_id,
        wallet_id: wallet.id,
        amount: itemRewardInFen,
        type: "refund",
        balance_before: balanceBefore,
        balance_after: balanceAfter,
        ref_type: "lost_found_item",
        ref_id: ownedItem.id,
        description: `悬赏金过期自动退款 — "${ownedItem.title}" 超过 ${EXPIRY_DAYS} 天未处理`,
      });
    } catch (txnErr) {
      console.error(`Failed to insert wallet_transactions for item ${ownedItem.id}:`, txnErr);
    }

    // 4. Notify publisher
    try {
      await supabaseAdmin.from("notifications").insert({
        user_id: ownedItem.publisher_id,
        type: "lost_found",
        title: "悬赏金已自动退回",
        body: `您发布的"${ownedItem.title}"超过${EXPIRY_DAYS}天未处理，悬赏金${ownedItem.reward}元已退回您的钱包`,
        ref_type: "lost_found_item",
        ref_id: ownedItem.id,
      });
    } catch (notifErr) {
      console.error(`Failed to send expiry notification for item ${ownedItem.id}:`, notifErr);
    }

    console.log(`Processed expired item ${ownedItem.id}: refunded ${ownedItem.reward} yuan to ${ownedItem.publisher_id}`);
    return true;
  } catch (e) {
    console.error(`Unexpected error processing item ${item.id}:`, e);
    return false;
  }
}

// ── Main handler ───────────────────────────────────────────────────
Deno.serve(async (_req: Request) => {
  console.log("reward-expiry cron started");

  // ────────────── Layer 1: Distributed advisory lock ────────────────
  // Prevent concurrent cron instances from executing simultaneously.
  // pg_try_advisory_lock is non-blocking — returns false if another
  // instance already holds the lock, letting us bail out cleanly.
  let lockAcquired = false;
  try {
    const { data, error } = await supabaseAdmin.rpc("pg_try_advisory_lock", {
      key: ADVISORY_LOCK_KEY,
    });
    if (error) {
      console.warn("Failed to check advisory lock, proceeding without lock:", error.message);
    } else if (!data) {
      console.log("Another cron instance is already running (advisory lock held), exiting");
      return new Response(
        JSON.stringify({ success: true, message: "Skipped — another instance is running" }),
        { status: 200, headers: { "Content-Type": "application/json" } }
      );
    } else {
      lockAcquired = true;
      console.log("Advisory lock acquired");
    }
  } catch (e) {
    console.warn("Advisory lock check threw, proceeding without lock:", e);
  }

  try {
    const items = await findExpiredItems();
    console.log(`Found ${items.length} expired items to process`);

    let successCount = 0;
    let failCount = 0;

    for (const item of items) {
      const ok = await processExpiredItem(item);
      if (ok) {
        successCount++;
      } else {
        failCount++;
      }
    }

    console.log(`reward-expiry cron finished: ${successCount} succeeded, ${failCount} failed`);

    return new Response(
      JSON.stringify({
        success: true,
        total: items.length,
        succeeded: successCount,
        failed: failCount,
      }),
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }
    );
  } finally {
    // Always release the advisory lock, even on errors
    if (lockAcquired) {
      try {
        await supabaseAdmin.rpc("pg_advisory_unlock", { key: ADVISORY_LOCK_KEY });
        console.log("Advisory lock released");
      } catch (e) {
        console.warn("Failed to release advisory lock (will auto-release on connection close):", e);
      }
    }
  }
});
