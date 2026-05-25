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

/** Check if the caller is an Agent. */
async function isAgent(userId: string): Promise<boolean> {
  const { data } = await supabaseAdmin
    .from("profiles")
    .select("role")
    .eq("id", userId)
    .single();
  return data?.role === "agent";
}

// ── Constants ────────────────────────────────────────────────────
const FEN_MULTIPLIER = 100; // wallet balance uses fen (分)
const CODE_LENGTH = 8;

/** Generate a random alphanumeric invite code. */
function generateCode(): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let code = "";
  for (let i = 0; i < CODE_LENGTH; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

// ── Action handlers ──────────────────────────────────────────────

/**
 * generate — Agent 或管理员生成邀请码。
 *
 * body params:
 *   - max_uses (optional): 最大使用次数，默认 100
 *   - expires_at (optional): ISO 过期时间，NULL = 永不过期
 *   - reward_amount (optional): 奖励金额（元），默认 0，写入 invite_codes 表
 *     verify 时从表读取，不接受调用方传入。
 */
async function handleGenerate(
  callerId: string,
  body: Record<string, unknown>
): Promise<Response> {
  // Auth check: only Agent can generate invite codes
  const agent = await isAgent(callerId);
  if (!agent) {
    return err(403, "仅管理员可生成邀请码");
  }

  const maxUses = typeof body.max_uses === "number" ? Math.max(1, Math.floor(body.max_uses as number)) : 100;
  const expiresAt = typeof body.expires_at === "string" ? body.expires_at : null;
  const rewardAmount = typeof body.reward_amount === "number" ? Math.max(0, Math.floor(body.reward_amount as number)) : 0;

  // Generate a unique code (retry on collision)
  let code = generateCode();
  let attempts = 0;
  while (attempts < 5) {
    const { data: existing } = await supabaseAdmin
      .from("invite_codes")
      .select("id")
      .eq("code", code)
      .maybeSingle();
    if (!existing) break;
    code = generateCode();
    attempts++;
  }

  const { data: insertData, error: insertError } = await supabaseAdmin
    .from("invite_codes")
    .insert({
      user_id: callerId,
      code,
      max_uses: maxUses,
      expires_at: expiresAt,
      is_active: true,
      reward_amount: rewardAmount,
    })
    .select("*")
    .single();

  if (insertError || !insertData) {
    console.error("Failed to insert invite code:", insertError?.message);
    // Check for unique violation on user_id (one code per user)
    if (insertError?.code === "23505") {
      return err(409, "该用户已有邀请码，请使用已有邀请码或联系管理员");
    }
    return err(500, "生成邀请码失败，请稍后重试");
  }

  return json({
    success: true,
    code: insertData.code,
    max_uses: insertData.max_uses,
    expires_at: insertData.expires_at,
    reward_amount: rewardAmount,
  });
}

/**
 * verify — 验证邀请码有效性并建立邀请关系。
 *
 * 由注册流程调用（service_role 或已验证用户）。
 * 成功后：INSERT invite_records → UPDATE usage_count
 *         + 更新 invitee profile.referrer_id
 *         + 发放奖励（如有）到邀请人钱包。
 *
 * body params:
 *   - code: 邀请码（必填）
 *   - invitee_id: 被邀请人（新注册用户）ID（必填）
 *
 * reward_amount 从 invite_codes 表读取，不接受调用方传入。
 */
async function handleVerify(
  body: Record<string, unknown>
): Promise<Response> {
  const code = body.code as string | undefined;
  const inviteeId = body.invitee_id as string | undefined;

  if (!code) return err(400, "缺少必填字段：code");
  if (!inviteeId) return err(400, "缺少必填字段：invitee_id");

  // 1. Look up the invite code
  const { data: inviteCode, error: codeError } = await supabaseAdmin
    .from("invite_codes")
    .select("*")
    .eq("code", code)
    .single();

  if (codeError || !inviteCode) {
    return err(404, "邀请码不存在");
  }

  // 2. Validate code status
  if (!inviteCode.is_active) {
    return err(422, "邀请码已失效");
  }

  if (inviteCode.expires_at && new Date(inviteCode.expires_at) < new Date()) {
    return err(422, "邀请码已过期");
  }

  if (inviteCode.usage_count >= inviteCode.max_uses) {
    return err(422, "邀请码使用次数已达上限");
  }

  const inviterId = inviteCode.user_id as string;

  // B5 fix: 禁止自己邀请自己
  if (inviteeId === inviterId) {
    return err(400, "不能使用自己的邀请码");
  }

  // 3. Check invitee hasn't been invited before (one invite per user)
  const { data: existingRecord } = await supabaseAdmin
    .from("invite_records")
    .select("id")
    .eq("invitee_id", inviteeId)
    .maybeSingle();

  if (existingRecord) {
    return err(409, "该用户已使用过邀请码");
  }

  // B2 fix: reward_amount 从 invite_codes 表读取，不接受调用方传入
  const rewardAmount = typeof inviteCode.reward_amount === "number"
    ? Math.max(0, Math.floor(inviteCode.reward_amount as number))
    : 0;
  const rewardInFen = rewardAmount * FEN_MULTIPLIER;

  // B3 fix: 先 INSERT invite_records，成功后再 UPDATE usage_count
  // 4. Insert invite record BEFORE incrementing usage_count
  const { data: inviteRecord, error: recordError } = await supabaseAdmin
    .from("invite_records")
    .insert({
      code,
      inviter_id: inviterId,
      invitee_id: inviteeId,
      registered_at: new Date().toISOString(),
    })
    .select("id")
    .single();

  if (recordError) {
    console.error("Failed to insert invite record:", recordError.message);
    return err(500, "记录邀请关系失败，请稍后重试");
  }

  // 5. Increment usage_count with optimistic lock (usage_count < max_uses)
  const { data: updatedRows, error: updateError } = await supabaseAdmin
    .from("invite_codes")
    .update({ usage_count: inviteCode.usage_count + 1 })
    .eq("id", inviteCode.id)
    .lt("usage_count", inviteCode.max_uses)
    .select("id");

  if (updateError) {
    console.error("Failed to increment invite code usage:", updateError.message);
    // Clean up the invite record we just created
    await supabaseAdmin.from("invite_records").delete().eq("id", inviteRecord.id);
    return err(500, "验证邀请码失败，请稍后重试");
  }

  if (!updatedRows || updatedRows.length === 0) {
    // Optimistic lock conflict — another request consumed the last use
    await supabaseAdmin.from("invite_records").delete().eq("id", inviteRecord.id);
    return err(409, "请重试");
  }

  // 6. Update invitee profile.referrer_id
  try {
    await supabaseAdmin
      .from("profiles")
      .update({ referrer_id: inviterId })
      .eq("id", inviteeId);
  } catch (profileErr) {
    console.error("Failed to update profile referrer_id:", profileErr);
    // Non-fatal — invitation record already created
  }

  // 7. Grant reward to inviter if reward_amount > 0
  if (rewardInFen > 0) {
    try {
      const { data: inviterWallet } = await supabaseAdmin
        .from("wallets")
        .select("id, balance")
        .eq("user_id", inviterId)
        .single();

      if (inviterWallet) {
        const balanceBefore = inviterWallet.balance as number;
        const balanceAfter = balanceBefore + rewardInFen;
        await supabaseAdmin
          .from("wallets")
          .update({ balance: balanceAfter })
          .eq("user_id", inviterId);

        await supabaseAdmin.from("wallet_transactions").insert({
          user_id: inviterId,
          wallet_id: inviterWallet.id,
          amount: rewardInFen,
          type: "income",
          balance_before: balanceBefore,
          balance_after: balanceAfter,
          ref_type: "invite",
          ref_id: inviteRecord.id,
          description: `邀请奖励到账 — 用户 ${inviteeId.slice(0, 8)} 使用您的邀请码注册`,
        });
      } else {
        // Create wallet for inviter if not exists
        const { data: newWallet } = await supabaseAdmin
          .from("wallets")
          .insert({ user_id: inviterId, balance: rewardInFen })
          .select("id, balance")
          .single();

        if (newWallet) {
          await supabaseAdmin.from("wallet_transactions").insert({
            user_id: inviterId,
            wallet_id: newWallet.id,
            amount: rewardInFen,
            type: "income",
            balance_before: 0,
            balance_after: rewardInFen,
            ref_type: "invite",
            ref_id: inviteRecord.id,
            description: `邀请奖励到账 — 用户 ${inviteeId.slice(0, 8)} 使用您的邀请码注册`,
          });
        }
      }
    } catch (rewardErr) {
      console.error("Failed to grant invite reward:", rewardErr);
      // Non-fatal — invitation record already created
    }
  }

  return json({
    success: true,
    inviter_id: inviterId,
    invitee_id: inviteeId,
    code,
    reward_amount: rewardAmount,
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
    const body: Record<string, unknown> = await req.json();
    const action = body.action as string | undefined;

    if (!action) {
      return err(400, "Missing required field: action");
    }

    switch (action) {
      case "generate": {
        const callerId = await extractCallerId(req);
        return await handleGenerate(callerId, body);
      }

      case "verify": {
        // verify is called by registration flow (service_role),
        // but we still accept authenticated users
        try {
          await extractCallerId(req);
        } catch {
          // If no auth header, allow — the caller may use service_role key
        }
        return await handleVerify(body);
      }

      default:
        return err(400, "不支持的操作类型");
    }
  } catch (e) {
    if (e && typeof e === "object" && "status" in e && "message" in e) {
      const { status, message, detail } = e as Record<string, unknown>;
      return err(status as number, message as string, detail as string | undefined);
    }
    console.error("Unexpected error:", e);
    return err(500, "Internal server error");
  }
});
