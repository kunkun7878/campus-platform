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

// ── Validation helpers ──────────────────────────────────────────

/**
 * Validate that the order exists and its status is among `allowedStatuses`.
 * Returns the full order row, or throws a Response-compatible error object.
 */
async function fetchOrder(
  orderId: string,
  allowedStatuses: string[]
): Promise<Record<string, unknown>> {
  const { data, error } = await supabaseAdmin
    .from("runner_orders")
    .select("*")
    .eq("id", orderId)
    .single();

  if (error || !data) {
    throw { status: 404, message: "Order not found" };
  }

  if (!allowedStatuses.includes(data.status as string)) {
    throw {
      status: 422,
      message: "当前订单状态不允许此操作",
    };
  }

  return data;
}

/**
 * Atomically update an order's status (conditional on current status
 * being in `expectedStatuses` to prevent races).
 * Returns the updated row or throws.
 */
async function atomicUpdateOrderStatus(
  orderId: string,
  expectedStatuses: string[],
  updates: Record<string, unknown>
): Promise<Record<string, unknown>> {
  const { data, error } = await supabaseAdmin
    .from("runner_orders")
    .update({ ...updates, updated_at: new Date().toISOString() })
    .eq("id", orderId)
    .in("status", expectedStatuses)
    .select("*");

  if (error) {
    console.error("Database error during atomic update:", error.message);
    throw { status: 500, message: "服务器内部错误，请稍后重试" };
  }

  if (!data || data.length === 0) {
    throw {
      status: 422,
      message: "订单状态已变更，请刷新后重试",
    };
  }

  return data[0];
}

async function insertTimeline(entry: {
  order_id: string;
  event: string;
  description?: string;
  operator_id: string;
  school_id: string;
}): Promise<void> {
  const { error } = await supabaseAdmin.from("order_timeline").insert(entry);
  if (error) {
    console.error("Failed to insert timeline:", error.message);
    // Non-fatal — order state has been committed; timeline is best-effort
  }
}

// ── Action handlers ──────────────────────────────────────────────

/** accept — 跑腿员接单 */
async function handleAccept(
  callerId: string,
  taskId: string,
  description?: string
): Promise<Response> {
  // 1. Fetch task
  const { data: task, error: taskError } = await supabaseAdmin
    .from("runner_tasks")
    .select("*")
    .eq("id", taskId)
    .single();

  if (taskError || !task) {
    return err(404, "Task not found");
  }

  // 2. Cannot accept own task
  if (task.publisher_id === callerId) {
    return err(403, "Cannot accept your own task");
  }

  // 3. Verify caller has approved runner status
  const { data: profile, error: profileError } = await supabaseAdmin
    .from("profiles")
    .select("runner_status")
    .eq("id", callerId)
    .single();

  if (profileError || !profile) {
    return err(500, "无法验证用户身份");
  }

  if (profile.runner_status !== "approved") {
    return err(403, "您尚未通过跑腿员认证，请先申请认证");
  }

  // 4. Atomic accept — only if status='published' AND runner_id IS NULL
  //    (step numbers shifted: was 3, now 4 after adding runner_status check)
  const { data: updatedTasks, error: acceptError } = await supabaseAdmin
    .from("runner_tasks")
    .update({
      runner_id: callerId,
      status: "assigned",
      updated_at: new Date().toISOString(),
    })
    .eq("id", taskId)
    .eq("status", "published")
    .is("runner_id", null)
    .select("*");

  if (acceptError) {
    console.error("Database error during accept:", acceptError.message);
    return err(500, "服务器内部错误，请稍后重试");
  }

  if (!updatedTasks || updatedTasks.length === 0) {
    // Could be already taken, cancelled, etc.
    const { data: current } = await supabaseAdmin
      .from("runner_tasks")
      .select("status, runner_id")
      .eq("id", taskId)
      .single();

    if (current) {
      if (current.runner_id !== null) {
        return err(422, "该任务已被其他跑腿员接单");
      }
      return err(422, "该任务已无法接单，请刷新后重试");
    }
    return err(422, "该任务已无法接单");
  }

  const acceptedTask = updatedTasks[0];

  // 5. Create order
  const { data: orders, error: orderError } = await supabaseAdmin
    .from("runner_orders")
    .insert({
      task_id: taskId,
      buyer_id: task.publisher_id,
      runner_id: callerId,
      status: "accepted",
      school_id: task.school_id,
    })
    .select("*");

  if (orderError || !orders || orders.length === 0) {
    // Rollback task
    console.error("Failed to create order:", orderError?.message);
    await supabaseAdmin
      .from("runner_tasks")
      .update({
        runner_id: null,
        status: "published",
        updated_at: new Date().toISOString(),
      })
      .eq("id", taskId);
    return err(500, "接单失败，请稍后重试");
  }

  const order = orders[0];

  // 6. Timeline
  await insertTimeline({
    order_id: order.id,
    event: "accepted",
    description: description || "Runner accepted the task",
    operator_id: callerId,
    school_id: task.school_id,
  });

  return json({
    success: true,
    order_id: order.id,
    task_id: taskId,
    status: "accepted",
  });
}

/** start_delivery — 跑腿员确认取件，开始配送 */
async function handleStartDelivery(
  callerId: string,
  orderId: string,
  description?: string
): Promise<Response> {
  // 1. Fetch order & validate status
  const order = await fetchOrder(orderId, ["accepted"]);

  // 2. Only the runner of this order can start delivery
  if (order.runner_id !== callerId) {
    return err(403, "Only the assigned runner can start delivery");
  }

  // 3. Atomic update order → delivering
  await atomicUpdateOrderStatus(orderId, ["accepted"], {
    status: "delivering",
  });

  // 4. Update task → in_progress
  const { error: taskError } = await supabaseAdmin
    .from("runner_tasks")
    .update({ status: "in_progress", updated_at: new Date().toISOString() })
    .eq("id", order.task_id as string)
    .in("status", ["assigned"]);

  if (taskError) {
    console.error("Failed to update task to in_progress:", taskError.message);
    // Non-fatal — order is already marked as delivering
  }

  // 5. Timeline
  await insertTimeline({
    order_id: orderId,
    event: "delivering",
    description: description || "Runner picked up and started delivery",
    operator_id: callerId,
    school_id: order.school_id as string,
  });

  return json({ success: true, order_id: orderId, status: "delivering" });
}

/** confirm_delivery — 跑腿员确认送达 */
async function handleConfirmDelivery(
  callerId: string,
  orderId: string,
  description?: string
): Promise<Response> {
  const order = await fetchOrder(orderId, ["delivering"]);

  if (order.runner_id !== callerId) {
    return err(403, "Only the assigned runner can confirm delivery");
  }

  await atomicUpdateOrderStatus(orderId, ["delivering"], {
    status: "delivered",
  });

  await insertTimeline({
    order_id: orderId,
    event: "delivered",
    description: description || "Runner confirmed delivery",
    operator_id: callerId,
    school_id: order.school_id as string,
  });

  return json({ success: true, order_id: orderId, status: "delivered" });
}

/** confirm_receipt — 买家确认收货 */
async function handleConfirmReceipt(
  callerId: string,
  orderId: string,
  description?: string
): Promise<Response> {
  const order = await fetchOrder(orderId, ["delivered"]);

  if (order.buyer_id !== callerId) {
    return err(403, "Only the buyer can confirm receipt");
  }

  // 1. Update order → completed
  await atomicUpdateOrderStatus(orderId, ["delivered"], {
    status: "completed",
    completed_at: new Date().toISOString(),
  });

  // 2. Update task → completed
  const { error: taskError } = await supabaseAdmin
    .from("runner_tasks")
    .update({ status: "completed", updated_at: new Date().toISOString() })
    .eq("id", order.task_id as string)
    .in("status", ["in_progress"]);

  if (taskError) {
    console.error("Failed to update task to completed:", taskError.message);
  }

  // 3. Timeline
  await insertTimeline({
    order_id: orderId,
    event: "completed",
    description: description || "Buyer confirmed receipt — order completed",
    operator_id: callerId,
    school_id: order.school_id as string,
  });

  return json({ success: true, order_id: orderId, status: "completed" });
}

/** cancel — 买家或跑腿员取消订单 */
async function handleCancel(
  callerId: string,
  orderId: string,
  reason?: string,
  description?: string
): Promise<Response> {
  const order = await fetchOrder(orderId, ["accepted", "delivering"]);

  if (order.buyer_id !== callerId && order.runner_id !== callerId) {
    return err(403, "Only the buyer or runner can cancel this order");
  }

  // 1. Update order → cancelled (atomic conditional on current status)
  await atomicUpdateOrderStatus(orderId, ["accepted", "delivering"], {
    status: "cancelled",
    cancel_reason: reason || null,
  });

  // 2. Check if task has other active orders
  const { data: activeOrders } = await supabaseAdmin
    .from("runner_orders")
    .select("id")
    .eq("task_id", order.task_id as string)
    .neq("id", orderId)
    .in("status", ["accepted", "delivering", "delivered", "after_sale"]);

  const hasOtherActive =
    activeOrders !== null && activeOrders.length > 0;

  let taskReset = false;

  // 3. If no other active orders, reset task to published
  if (!hasOtherActive) {
    const { error: resetError } = await supabaseAdmin
      .from("runner_tasks")
      .update({
        status: "published",
        runner_id: null,
        updated_at: new Date().toISOString(),
      })
      .eq("id", order.task_id as string);

    if (!resetError) {
      taskReset = true;
    } else {
      console.error("Failed to reset task after cancel:", resetError.message);
    }
  }

  // 4. Timeline
  const eventDescription =
    description ||
    reason ||
    `Order cancelled by ${callerId === order.buyer_id ? "buyer" : "runner"}`;

  await insertTimeline({
    order_id: orderId,
    event: "cancelled",
    description: eventDescription,
    operator_id: callerId,
    school_id: order.school_id as string,
  });

  return json({
    success: true,
    order_id: orderId,
    status: "cancelled",
    task_reset: taskReset,
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
      case "accept": {
        const taskId = body.task_id as string | undefined;
        if (!taskId) {
          return err(400, "Missing required field: task_id");
        }
        return await handleAccept(
          callerId,
          taskId,
          body.description as string | undefined
        );
      }

      case "start_delivery": {
        const orderId = body.order_id as string | undefined;
        if (!orderId) return err(400, "Missing required field: order_id");
        return await handleStartDelivery(
          callerId,
          orderId,
          body.description as string | undefined
        );
      }

      case "confirm_delivery": {
        const orderId = body.order_id as string | undefined;
        if (!orderId) return err(400, "Missing required field: order_id");
        return await handleConfirmDelivery(
          callerId,
          orderId,
          body.description as string | undefined
        );
      }

      case "confirm_receipt": {
        const orderId = body.order_id as string | undefined;
        if (!orderId) return err(400, "Missing required field: order_id");
        return await handleConfirmReceipt(
          callerId,
          orderId,
          body.description as string | undefined
        );
      }

      case "cancel": {
        const orderId = body.order_id as string | undefined;
        if (!orderId) return err(400, "Missing required field: order_id");
        return await handleCancel(
          callerId,
          orderId,
          (body.reason as string) || (body.cancel_reason as string) || undefined,
          body.description as string | undefined
        );
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
