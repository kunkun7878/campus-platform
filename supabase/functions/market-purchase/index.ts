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

// ── Purchase handler ────────────────────────────────────────────

/**
 * 发起二手商品购买请求。
 *
 * 使用 service_role 客户端绕过 RLS，在同一请求中原子性地完成：
 *   1. 检查幂等性（同一买家对同一商品是否已有 pending 订单）
 *   2. 校验商品状态为 active
 *   3. 校验买家不是卖家本人
 *   4. 创建 market_order（status='pending'）
 *   5. 将 listing.status 更新为 reserved（乐观锁：WHERE status='active'）
 *
 * 返回 { order, listing }。
 */
async function handlePurchase(
  callerId: string,
  listingId: string
): Promise<Response> {
  // ── 1. 幂等性检查 ──────────────────────────────────────────
  const { data: existingOrders, error: idemError } = await supabaseAdmin
    .from("market_orders")
    .select("*")
    .eq("listing_id", listingId)
    .eq("buyer_id", callerId)
    .eq("status", "pending")
    .limit(1);

  if (idemError) {
    console.error("Idempotency check failed:", idemError.message);
    return err(500, "服务器内部错误，请稍后重试");
  }

  if (existingOrders && existingOrders.length > 0) {
    // 已有 pending 订单，直接返回（附带当前 listing 信息）
    const { data: listing } = await supabaseAdmin
      .from("market_listings")
      .select("*")
      .eq("id", listingId)
      .single();

    return json({
      order: existingOrders[0],
      listing: listing || null,
    });
  }

  // ── 2. 查询商品 ────────────────────────────────────────────
  const { data: listing, error: listingError } = await supabaseAdmin
    .from("market_listings")
    .select("*")
    .eq("id", listingId)
    .single();

  if (listingError || !listing) {
    return err(404, "商品不存在");
  }

  // ── 3. 校验商品状态 ────────────────────────────────────────
  if (listing.status !== "active") {
    return err(422, "该商品当前不可购买");
  }

  // ── 4. 校验不能购买自己的商品 ──────────────────────────────
  if (listing.seller_id === callerId) {
    return err(403, "不能购买自己发布的商品");
  }

  // ── 5. 创建订单 ────────────────────────────────────────────
  const { data: orders, error: orderError } = await supabaseAdmin
    .from("market_orders")
    .insert({
      listing_id: listingId,
      buyer_id: callerId,
      seller_id: listing.seller_id,
      status: "pending",
      school_id: listing.school_id,
    })
    .select("*");

  if (orderError || !orders || orders.length === 0) {
    console.error("Failed to create market order:", orderError?.message);
    return err(500, "下单失败，请稍后重试");
  }

  const order = orders[0];

  // ── 6. 更新商品状态为 reserved（乐观锁） ───────────────────
  const { data: updatedListings, error: updateError } = await supabaseAdmin
    .from("market_listings")
    .update({ status: "reserved" })
    .eq("id", listingId)
    .eq("status", "active") // 乐观锁：仅当仍为 active 时更新
    .select("*");

  if (updateError) {
    console.error("Failed to update listing status:", updateError.message);
    // 回滚：取消已创建的订单
    await supabaseAdmin
      .from("market_orders")
      .update({ status: "cancelled" })
      .eq("id", order.id);
    return err(500, "下单失败，请稍后重试");
  }

  if (!updatedListings || updatedListings.length === 0) {
    // 并发冲突：商品状态在查询后已被修改
    console.warn(
      `Optimistic lock failed for listing ${listingId}: status no longer active`
    );
    // 回滚：取消已创建的订单
    await supabaseAdmin
      .from("market_orders")
      .update({ status: "cancelled" })
      .eq("id", order.id);
    return err(422, "该商品已被其他用户预订");
  }

  // ── 7. 返回结果 ────────────────────────────────────────────
  return json({
    order,
    listing: updatedListings[0],
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
    const listingId = body.listing_id as string | undefined;

    if (!listingId) {
      return err(400, "缺少必要参数：listing_id");
    }

    return await handlePurchase(callerId, listingId);
  } catch (e) {
    // 处理 extractCallerId 抛出的结构化错误
    if (e && typeof e === "object" && "status" in e && "message" in e) {
      const { status, message, detail } = e as Record<string, unknown>;
      return err(status as number, message as string, detail as string | undefined);
    }
    console.error("Unexpected error:", e);
    return err(500, "服务器内部错误，请稍后重试");
  }
});
