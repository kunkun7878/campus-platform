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
 * Check whether the caller is an agent by looking up their profile.
 * We query profiles directly (instead of calling is_agent() RPC) because
 * the service_role client has no real auth.uid() binding — is_agent()
 * would resolve auth.uid() to the caller incorrectly.
 */
async function requireAgent(callerId: string): Promise<void> {
  const { data: profile, error } = await supabaseAdmin
    .from("profiles")
    .select("is_agent")
    .eq("id", callerId)
    .single();

  if (error || !profile) {
    throw { status: 403, message: "Profile not found — only agents can perform this action" };
  }

  if (!profile.is_agent) {
    throw { status: 403, message: "Only agents can perform this action" };
  }
}

// ── Action handlers ──────────────────────────────────────────────

/** create — 买家或跑腿员发起售后申请 */
async function handleCreate(
  callerId: string,
  orderId: string,
  type: string,
  reason: string,
  description?: string
): Promise<Response> {
  // 1. Fetch order
  const { data: order, error: orderError } = await supabaseAdmin
    .from("runner_orders")
    .select("*")
    .eq("id", orderId)
    .single();

  if (orderError || !order) {
    return err(404, "Order not found");
  }

  // 2. Only buyer or runner can create after-sale
  if (order.buyer_id !== callerId && order.runner_id !== callerId) {
    return err(403, "Only the buyer or runner can initiate an after-sale request");
  }

  // 3. Order must be in an active post-acceptance state
  const allowedOrderStatuses = ["delivering", "delivered", "completed"];
  if (!allowedOrderStatuses.includes(order.status as string)) {
    return err(
      422,
      `Cannot create after-sale for order in '${order.status}' status. ` +
        `Order must be one of: ${allowedOrderStatuses.join(", ")}`
    );
  }

  // 4. Validate after_sale type
  const allowedTypes = ["refund", "return", "complaint"];
  if (!allowedTypes.includes(type)) {
    return err(400, `Invalid after-sale type '${type}'. Must be one of: ${allowedTypes.join(", ")}`);
  }

  // 5. Insert after_sales record
  const { data: afterSales, error: insertError } = await supabaseAdmin
    .from("after_sales")
    .insert({
      order_id: orderId,
      requester_id: callerId,
      type,
      reason,
      status: "pending",
      school_id: order.school_id,
    })
    .select("*");

  if (insertError || !afterSales || afterSales.length === 0) {
    return err(500, "Failed to create after-sale record");
  }

  const afterSale = afterSales[0];

  // 6. Atomically update order status → after_sale (conditional on current status)
  const { data: updatedOrders, error: updateError } = await supabaseAdmin
    .from("runner_orders")
    .update({ status: "after_sale", updated_at: new Date().toISOString() })
    .eq("id", orderId)
    .in("status", allowedOrderStatuses)
    .select("*");

  if (updateError || !updatedOrders || updatedOrders.length === 0) {
    // Rollback after_sale creation — order status changed concurrently
    await supabaseAdmin.from("after_sales").delete().eq("id", afterSale.id);
    return err(422, "Order status has changed — cannot create after-sale at this time");
  }

  // 7. Insert timeline
  const { error: timelineError } = await supabaseAdmin
    .from("after_sale_timeline")
    .insert({
      after_sale_id: afterSale.id,
      event: "created",
      description: description || `After-sale '${type}' created: ${reason}`,
      operator_id: callerId,
      school_id: order.school_id,
    });

  if (timelineError) {
    console.error("Failed to insert after-sale timeline:", timelineError.message);
    // Non-fatal — after_sale record is already committed
  }

  return json({
    success: true,
    after_sale_id: afterSale.id,
    order_id: orderId,
    status: "pending",
  });
}

/** approve — Agent 同意售后 */
async function handleApprove(
  callerId: string,
  afterSaleId: string,
  resultComment?: string
): Promise<Response> {
  await requireAgent(callerId);

  // 1. Fetch after_sale
  const { data: afterSale, error: fetchError } = await supabaseAdmin
    .from("after_sales")
    .select("*")
    .eq("id", afterSaleId)
    .single();

  if (fetchError || !afterSale) {
    return err(404, "After-sale record not found");
  }

  // 2. Validate status transition
  const allowedFrom = ["pending", "processing"];
  if (!allowedFrom.includes(afterSale.status as string)) {
    return err(
      422,
      `Cannot approve after-sale in '${afterSale.status}' status. ` +
        `Must be one of: ${allowedFrom.join(", ")}`
    );
  }

  // 3. Update status
  const { data: updated, error: updateError } = await supabaseAdmin
    .from("after_sales")
    .update({
      status: "approved",
      result_comment: resultComment || null,
      updated_at: new Date().toISOString(),
    })
    .eq("id", afterSaleId)
    .in("status", allowedFrom)
    .select("*");

  if (updateError || !updated || updated.length === 0) {
    return err(422, "After-sale status has changed concurrently — cannot approve");
  }

  // 4. Timeline
  const { error: timelineError } = await supabaseAdmin
    .from("after_sale_timeline")
    .insert({
      after_sale_id: afterSaleId,
      event: "approved",
      description: resultComment || "After-sale approved by agent",
      operator_id: callerId,
      school_id: afterSale.school_id,
    });

  if (timelineError) {
    console.error("Failed to insert after-sale timeline:", timelineError.message);
  }

  return json({ success: true, after_sale_id: afterSaleId, status: "approved" });
}

/** reject — Agent 拒绝售后 */
async function handleReject(
  callerId: string,
  afterSaleId: string,
  resultComment?: string
): Promise<Response> {
  await requireAgent(callerId);

  const { data: afterSale, error: fetchError } = await supabaseAdmin
    .from("after_sales")
    .select("*")
    .eq("id", afterSaleId)
    .single();

  if (fetchError || !afterSale) {
    return err(404, "After-sale record not found");
  }

  const allowedFrom = ["pending", "processing"];
  if (!allowedFrom.includes(afterSale.status as string)) {
    return err(
      422,
      `Cannot reject after-sale in '${afterSale.status}' status. ` +
        `Must be one of: ${allowedFrom.join(", ")}`
    );
  }

  const { data: updated, error: updateError } = await supabaseAdmin
    .from("after_sales")
    .update({
      status: "rejected",
      result_comment: resultComment || null,
      updated_at: new Date().toISOString(),
    })
    .eq("id", afterSaleId)
    .in("status", allowedFrom)
    .select("*");

  if (updateError || !updated || updated.length === 0) {
    return err(422, "After-sale status has changed concurrently — cannot reject");
  }

  const { error: timelineError } = await supabaseAdmin
    .from("after_sale_timeline")
    .insert({
      after_sale_id: afterSaleId,
      event: "rejected",
      description: resultComment || "After-sale rejected by agent",
      operator_id: callerId,
      school_id: afterSale.school_id,
    });

  if (timelineError) {
    console.error("Failed to insert after-sale timeline:", timelineError.message);
  }

  return json({ success: true, after_sale_id: afterSaleId, status: "rejected" });
}

/** complete — Agent 完成售后 */
async function handleComplete(
  callerId: string,
  afterSaleId: string,
  resultComment?: string
): Promise<Response> {
  await requireAgent(callerId);

  const { data: afterSale, error: fetchError } = await supabaseAdmin
    .from("after_sales")
    .select("*")
    .eq("id", afterSaleId)
    .single();

  if (fetchError || !afterSale) {
    return err(404, "After-sale record not found");
  }

  // Complete is only valid from 'approved' status
  const allowedFrom = ["approved"];
  if (!allowedFrom.includes(afterSale.status as string)) {
    return err(
      422,
      `Cannot complete after-sale in '${afterSale.status}' status. ` +
        "After-sale must be 'approved' first"
    );
  }

  const { data: updated, error: updateError } = await supabaseAdmin
    .from("after_sales")
    .update({
      status: "completed",
      result_comment: resultComment || null,
      updated_at: new Date().toISOString(),
    })
    .eq("id", afterSaleId)
    .in("status", allowedFrom)
    .select("*");

  if (updateError || !updated || updated.length === 0) {
    return err(422, "After-sale status has changed concurrently — cannot complete");
  }

  const { error: timelineError } = await supabaseAdmin
    .from("after_sale_timeline")
    .insert({
      after_sale_id: afterSaleId,
      event: "completed",
      description: resultComment || "After-sale completed by agent",
      operator_id: callerId,
      school_id: afterSale.school_id,
    });

  if (timelineError) {
    console.error("Failed to insert after-sale timeline:", timelineError.message);
  }

  return json({ success: true, after_sale_id: afterSaleId, status: "completed" });
}

// ── Main handler ─────────────────────────────────────────────────
Deno.serve(async (req: Request) => {
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
      case "create": {
        const orderId = body.order_id as string | undefined;
        const type = body.type as string | undefined;
        const reason = body.reason as string | undefined;

        if (!orderId) return err(400, "Missing required field: order_id");
        if (!type) return err(400, "Missing required field: type");
        if (!reason) return err(400, "Missing required field: reason");

        return await handleCreate(
          callerId,
          orderId,
          type,
          reason,
          body.description as string | undefined
        );
      }

      case "approve": {
        const afterSaleId = body.after_sale_id as string | undefined;
        if (!afterSaleId) return err(400, "Missing required field: after_sale_id");
        return await handleApprove(
          callerId,
          afterSaleId,
          body.result_comment as string | undefined
        );
      }

      case "reject": {
        const afterSaleId = body.after_sale_id as string | undefined;
        if (!afterSaleId) return err(400, "Missing required field: after_sale_id");
        return await handleReject(
          callerId,
          afterSaleId,
          body.result_comment as string | undefined
        );
      }

      case "complete": {
        const afterSaleId = body.after_sale_id as string | undefined;
        if (!afterSaleId) return err(400, "Missing required field: after_sale_id");
        return await handleComplete(
          callerId,
          afterSaleId,
          body.result_comment as string | undefined
        );
      }

      default:
        return err(
          400,
          `Unknown action: '${action}'. Valid: create, approve, reject, complete`
        );
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
