import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// ── CORS ────────────────────────────────────────────────────────
const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

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

// ── Helpers ─────────────────────────────────────────────────────
function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function err(status: number, error: string, detail?: string): Response {
  const body: Record<string, string> = { error };
  if (detail) body.detail = detail;
  return json(body, status);
}

/** Extract the caller's user id from the Authorization Bearer JWT. */
async function extractCallerId(req: Request): Promise<string> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    throw { status: 401, message: "Missing or malformed Authorization header" };
  }
  const token = authHeader.replace("Bearer ", "");
  const {
    data: { user },
    error: authError,
  } = await supabaseAdmin.auth.getUser(token);
  if (authError || !user) {
    throw { status: 401, message: "Invalid or expired token" };
  }
  return user.id;
}

/**
 * Atomic update on lost_found_items, conditional on current status.
 */
async function atomicUpdateItem(
  itemId: string,
  expectedStatuses: string[],
  updates: Record<string, unknown>
): Promise<Record<string, unknown>> {
  const { data, error } = await supabaseAdmin
    .from("lost_found_items")
    .update({ ...updates, updated_at: new Date().toISOString() })
    .eq("id", itemId)
    .in("status", expectedStatuses)
    .select("*");

  if (error) {
    console.error("Database error during atomic update:", error.message);
    throw { status: 500, message: "服务器内部错误，请稍后重试" };
  }

  if (!data || data.length === 0) {
    throw {
      status: 422,
      message: "物品状态已变更，请刷新后重试",
    };
  }

  return data[0];
}

/**
 * Atomic update on lost_found_claims, conditional on current status.
 */
async function atomicUpdateClaim(
  claimId: string,
  expectedStatuses: string[],
  updates: Record<string, unknown>
): Promise<Record<string, unknown>> {
  const { data, error } = await supabaseAdmin
    .from("lost_found_claims")
    .update({ ...updates, updated_at: new Date().toISOString() })
    .eq("id", claimId)
    .in("status", expectedStatuses)
    .select("*");

  if (error) {
    console.error("Database error during atomic claim update:", error.message);
    throw { status: 500, message: "服务器内部错误，请稍后重试" };
  }

  if (!data || data.length === 0) {
    throw {
      status: 422,
      message: "认领状态已变更，请刷新后重试",
    };
  }

  return data[0];
}

// ── Constants ─────────────────────────────────────────────────────
const FEN_MULTIPLIER = 100; // wallet balance uses fen (分), reward input is yuan (元)

// ── Action handlers ──────────────────────────────────────────────

/**
 * publish_item — 发布失物/招领启事。
 * 同时冻结悬赏金额（如果 reward > 0）。
 */
async function handlePublishItem(
  callerId: string,
  body: Record<string, unknown>
): Promise<Response> {
  const {
    title, type, description, images, location, lostDate,
    category, reward, contact, school_id,
  } = body;

  // ── Validate required fields ──
  if (!title || !type || !school_id) {
    return err(400, "缺少必填字段：title, type, school_id");
  }
  if (type !== "lost" && type !== "found") {
    return err(400, "type 必须为 lost 或 found");
  }
  if (typeof title !== "string" || title.trim().length === 0) {
    return err(400, "标题不能为空");
  }

  const now = new Date().toISOString();
  const itemReward = typeof reward === "number" ? Math.max(0, Math.floor(reward)) : 0;
  // Wallet balance is in fen (分); convert reward from yuan to fen for wallet operations
  const itemRewardInFen = itemReward * FEN_MULTIPLIER;

  // ── If there's a reward, freeze it from publisher's wallet ──
  if (itemReward > 0) {
    try {
      const { data: wallet, error: walletError } = await supabaseAdmin
        .from("wallets")
        .select("balance")
        .eq("user_id", callerId)
        .single();

      if (walletError || !wallet) {
        return err(400, "钱包信息获取失败，无法冻结悬赏");
      }

      const currentBalance = wallet.balance as number;
      if (currentBalance < itemRewardInFen) {
        return err(400, "余额不足，无法设置该悬赏金额");
      }

      // Deduct from balance (freeze) with optimistic concurrency guard
      const { data: updated, error: deductError } = await supabaseAdmin
        .from("wallets")
        .update({ balance: currentBalance - itemRewardInFen })
        .eq("user_id", callerId)
        .gte("balance", itemRewardInFen) // optimistic concurrency guard
        .select("balance")
        .single();

      if (deductError || !updated) {
        console.error("Failed to freeze reward:", deductError?.message ?? "concurrent modification");
        return err(500, "冻结悬赏失败，请稍后重试");
      }
    } catch (e) {
      console.error("Wallet freeze error:", e);
      // Wallet may not exist yet; treat as non-fatal for MVP, but include warning
      return err(400, "钱包操作异常，无法冻结悬赏金额，请先充值");
    }
  }

  // ── Insert item ──
  const { data: items, error: insertError } = await supabaseAdmin
    .from("lost_found_items")
    .insert({
      publisher_id: callerId,
      type,
      title: (title as string).trim(),
      description: description || null,
      images: images || "[]",
      location: location || null,
      lost_date: lostDate || null,
      category: category || "other",
      status: "active",
      school_id,
      reward: itemReward,
      contact: contact || "站内私信联系",
      created_at: now,
      updated_at: now,
    })
    .select("*");

  if (insertError || !items || items.length === 0) {
    // Rollback wallet freeze
    if (itemReward > 0) {
      try {
        const { data: currentWallet } = await supabaseAdmin
          .from("wallets")
          .select("balance")
          .eq("user_id", callerId)
          .single();
        if (currentWallet) {
          await supabaseAdmin
            .from("wallets")
            .update({ balance: (currentWallet.balance as number) + itemRewardInFen })
            .eq("user_id", callerId);
        }
      } catch (rbErr) {
        console.error("Rollback freeze failed:", rbErr);
      }
    }
    console.error("Failed to insert item:", insertError?.message);
    return err(500, "发布失败，请稍后重试");
  }

  const item = items[0];

  return json({
    success: true,
    item_id: item.id,
    status: "active",
  });
}

/**
 * approve_claim — 发布者批准认领。
 * 将 claim → approved，item → claimed。
 */
async function handleApproveClaim(
  callerId: string,
  claimId: string
): Promise<Response> {
  // 1. Fetch claim
  const { data: claim, error: claimError } = await supabaseAdmin
    .from("lost_found_claims")
    .select("*")
    .eq("id", claimId)
    .single();

  if (claimError || !claim) {
    return err(404, "认领记录不存在");
  }

  if (claim.status !== "pending") {
    return err(422, "该认领已处理，请刷新后重试");
  }

  // 2. Fetch item & verify caller is publisher
  const { data: item, error: itemError } = await supabaseAdmin
    .from("lost_found_items")
    .select("*")
    .eq("id", claim.item_id as string)
    .single();

  if (itemError || !item) {
    return err(404, "物品不存在");
  }

  if (item.publisher_id !== callerId) {
    return err(403, "只有发布者可以批准认领");
  }

  if (item.status !== "active" && item.status !== "claimed") {
    return err(422, "物品当前状态不允许批准认领");
  }

  // 3. Atomic: update claim → approved (must be pending)
  const updatedClaim = await atomicUpdateClaim(claimId, ["pending"], {
    status: "approved",
    resolved_at: new Date().toISOString(),
  });

  // 4. Update item → claimed (only if still active/claimed);
  //    rollback claim if item update fails
  try {
    await atomicUpdateItem(item.id as string, ["active", "claimed"], {
      status: "claimed",
    });
  } catch (itemErr) {
    // Rollback step 3: revert claim back to pending
    console.error("Item status update failed, rolling back claim:", itemErr);
    try {
      await supabaseAdmin
        .from("lost_found_claims")
        .update({ status: "pending", updated_at: new Date().toISOString() })
        .eq("id", claimId)
        .eq("status", "approved");
    } catch (rbErr) {
      console.error("Rollback claim failed; data may be inconsistent:", rbErr);
    }
    throw itemErr; // propagate to caller
  }

  // 4a. Notify claimant that their claim was approved
  try {
    await supabaseAdmin.from("notifications").insert({
      user_id: claim.claimant_id,
      type: "lost_found",
      title: "认领已通过",
      body: `您对"${item.title}"的认领已被发布者通过`,
      ref_type: "lost_found_item",
      ref_id: item.id,
    });
  } catch (notifErr) {
    console.error("Failed to insert approve_claim notification:", notifErr);
    // Non-fatal — do not block the approve flow
  }

  return json({
    success: true,
    claim_id: claimId,
    item_id: item.id,
    status: "approved",
  });
}

/**
 * reject_claim — 发布者拒绝认领。
 * 将 claim → rejected。
 */
async function handleRejectClaim(
  callerId: string,
  claimId: string
): Promise<Response> {
  // 1. Fetch claim
  const { data: claim, error: claimError } = await supabaseAdmin
    .from("lost_found_claims")
    .select("*")
    .eq("id", claimId)
    .single();

  if (claimError || !claim) {
    return err(404, "认领记录不存在");
  }

  if (claim.status !== "pending") {
    return err(422, "该认领已处理，请刷新后重试");
  }

  // 2. Fetch item & verify caller is publisher
  const { data: item, error: itemError } = await supabaseAdmin
    .from("lost_found_items")
    .select("*")
    .eq("id", claim.item_id as string)
    .single();

  if (itemError || !item) {
    return err(404, "物品不存在");
  }

  if (item.publisher_id !== callerId) {
    return err(403, "只有发布者可以拒绝认领");
  }

  // 3. Atomic: update claim → rejected (must be pending)
  await atomicUpdateClaim(claimId, ["pending"], {
    status: "rejected",
    resolved_at: new Date().toISOString(),
  });

  // 3a. Notify claimant that their claim was rejected
  try {
    await supabaseAdmin.from("notifications").insert({
      user_id: claim.claimant_id,
      type: "lost_found",
      title: "认领未通过",
      body: `您对"${item.title}"的认领未被通过`,
      ref_type: "lost_found_item",
      ref_id: item.id,
    });
  } catch (notifErr) {
    console.error("Failed to insert reject_claim notification:", notifErr);
    // Non-fatal — do not block the reject flow
  }

  return json({
    success: true,
    claim_id: claimId,
    item_id: item.id,
    status: "rejected",
  });
}

/**
 * resolve_item — 发布者确认物品已归还/找到，将悬赏转账给认领人。
 * item → closed，reward → approved claimant.
 */
async function handleResolveItem(
  callerId: string,
  itemId: string,
  claimId?: string
): Promise<Response> {
  // 1. Fetch item & verify caller is publisher
  const { data: item, error: itemError } = await supabaseAdmin
    .from("lost_found_items")
    .select("*")
    .eq("id", itemId)
    .single();

  if (itemError || !item) {
    return err(404, "物品不存在");
  }

  if (item.publisher_id !== callerId) {
    return err(403, "只有发布者可以确认已解决");
  }

  if (item.status === "closed") {
    return err(422, "物品已关闭");
  }

  const now = new Date().toISOString();
  let targetClaimId: string | null = claimId || null;

  // 2. If no claimId specified, find the first approved claim
  if (!targetClaimId) {
    const { data: approvedClaims } = await supabaseAdmin
      .from("lost_found_claims")
      .select("id, claimant_id")
      .eq("item_id", itemId)
      .eq("status", "approved")
      .order("resolved_at", { ascending: true })
      .limit(1);

    if (approvedClaims && approvedClaims.length > 0) {
      targetClaimId = approvedClaims[0].id as string;
    }
  }

  // 3. Atomic: update item → closed
  await atomicUpdateItem(itemId, ["active", "claimed"], {
    status: "closed",
  });

  // 4. Transfer reward to claimant (if reward > 0 and claimant found)
  const itemReward = item.reward as number;
  const itemRewardInFen = itemReward * FEN_MULTIPLIER;
  if (itemReward > 0 && targetClaimId) {
    try {
      // Find the approved claimant
      const { data: targetClaim } = await supabaseAdmin
        .from("lost_found_claims")
        .select("claimant_id")
        .eq("id", targetClaimId)
        .single();

      if (targetClaim) {
        const claimantId = targetClaim.claimant_id as string;

        // Credit claimant wallet
        const { data: claimantWallet } = await supabaseAdmin
          .from("wallets")
          .select("balance")
          .eq("user_id", claimantId)
          .single();

        if (claimantWallet) {
          await supabaseAdmin
            .from("wallets")
            .update({
              balance: (claimantWallet.balance as number) + itemRewardInFen,
            })
            .eq("user_id", claimantId);
        } else {
          // Create wallet for claimant if not exists
          await supabaseAdmin
            .from("wallets")
            .insert({ user_id: claimantId, balance: itemRewardInFen });
        }
      }
    } catch (e) {
      console.error("Reward transfer to claimant failed (non-fatal):", e);
      // Non-fatal — item is already closed
    }
  }

  return json({
    success: true,
    item_id: itemId,
    status: "closed",
    reward_transferred: itemReward,
  });
}

/**
 * close_item — 发布者关闭物品，悬赏退回发布者。
 * item → closed，reward refund to publisher.
 */
async function handleCloseItem(
  callerId: string,
  itemId: string
): Promise<Response> {
  // 1. Fetch item & verify caller is publisher
  const { data: item, error: itemError } = await supabaseAdmin
    .from("lost_found_items")
    .select("*")
    .eq("id", itemId)
    .single();

  if (itemError || !item) {
    return err(404, "物品不存在");
  }

  if (item.publisher_id !== callerId) {
    return err(403, "只有发布者可以关闭物品");
  }

  if (item.status === "closed") {
    return err(422, "物品已关闭");
  }

  // 2. Atomic: update item → closed
  await atomicUpdateItem(itemId, ["active", "claimed"], {
    status: "closed",
  });

  // 3. Refund reward to publisher (if reward > 0)
  const itemReward = item.reward as number;
  const itemRewardInFen = itemReward * FEN_MULTIPLIER;
  if (itemReward > 0) {
    try {
      const { data: publisherWallet } = await supabaseAdmin
        .from("wallets")
        .select("balance")
        .eq("user_id", callerId)
        .single();

      if (publisherWallet) {
        await supabaseAdmin
          .from("wallets")
          .update({
            balance: (publisherWallet.balance as number) + itemRewardInFen,
          })
          .eq("user_id", callerId);
      }
    } catch (e) {
      console.error("Reward refund to publisher failed (non-fatal):", e);
    }
  }

  // 4. Reject any pending claims
  try {
    await supabaseAdmin
      .from("lost_found_claims")
      .update({
        status: "rejected",
        resolved_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      })
      .eq("item_id", itemId)
      .eq("status", "pending");
  } catch (e) {
    console.error("Rejecting pending claims failed (non-fatal):", e);
  }

  return json({
    success: true,
    item_id: itemId,
    status: "closed",
    reward_refunded: itemReward,
  });
}

// ── Main handler ─────────────────────────────────────────────────
Deno.serve(async (req: Request) => {
  // CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return err(405, "Method not allowed — use POST");
  }

  try {
    const callerId = await extractCallerId(req);

    const body: Record<string, unknown> = await req.json();
    const action = body.action as string | undefined;

    if (!action) {
      return err(400, "Missing required field: action");
    }

    switch (action) {
      case "publish_item": {
        return await handlePublishItem(callerId, body);
      }

      case "approve_claim": {
        const claimId = body.claim_id as string | undefined;
        if (!claimId) return err(400, "Missing required field: claim_id");
        return await handleApproveClaim(callerId, claimId);
      }

      case "reject_claim": {
        const claimId = body.claim_id as string | undefined;
        if (!claimId) return err(400, "Missing required field: claim_id");
        return await handleRejectClaim(callerId, claimId);
      }

      case "resolve_item": {
        const itemId = body.item_id as string | undefined;
        if (!itemId) return err(400, "Missing required field: item_id");
        return await handleResolveItem(
          callerId,
          itemId,
          body.claim_id as string | undefined
        );
      }

      case "close_item": {
        const itemId = body.item_id as string | undefined;
        if (!itemId) return err(400, "Missing required field: item_id");
        return await handleCloseItem(callerId, itemId);
      }

      default:
        return err(400, "不支持的操作类型");
    }
  } catch (e) {
    // If the error was thrown from our helpers it carries {status, message}
    if (e && typeof e === "object" && "status" in e && "message" in e) {
      const { status, message, detail } = e as Record<string, unknown>;
      return err(status as number, message as string, detail as string | undefined);
    }
    console.error("Unexpected error:", e);
    return err(500, "Internal server error");
  }
});
