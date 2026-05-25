// ═══════════════════════════════════════════════════════════════
// push-notification Edge Function
//
// 职责: 扫描 notifications 表中 push_sent=false 的记录,
//       通过 FCM HTTP v1 API 向目标用户设备推送离线通知。
//
// 运行方式:
//   - Supabase 定时任务 (pg_cron 每分钟执行)
//   - SELECT cron.schedule('push-notification', '* * * * *',
//       $$ SELECT net.http_post('https://<project>.functions.supabase.co/push-notification', ...) $$);
//   - 或通过 Supabase Dashboard → Edge Functions → Schedule
//
// 流程:
//   1. 查询 push_sent=false 的 notification (LIMIT 100)
//   2. 对每条 notification 查用户活跃 FCM tokens
//   3. 逐 token 调用 FCM HTTP v1 API 推送
//   4. 标记 push_sent=true, push_sent_at=now()
//   5. 清理无效 token (FCM 返回 UNREGISTERED 等错误)
//
// 安全:
//   - service_role 客户端 (绕过 RLS 读取所有用户的 tokens)
//   - FCM 服务帐号 OAuth2 认证
//   - Authorization header 校验 — 仅允许持有 service_role_key 的调用方
// ═══════════════════════════════════════════════════════════════

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { JWT } from "npm:google-auth-library@9";

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

// ── FCM ─────────────────────────────────────────────────────────

/** Firebase project ID extracted from service account JSON. */
let fcmProjectId: string | null = null;

/** Lazily-initialized Google JWT client for FCM HTTP v1 API. */
let jwtClient: JWT | null = null;

function getFcmClient(): JWT {
  if (jwtClient) return jwtClient;

  const raw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");
  if (!raw) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT environment variable is not set");
  }

  let sa: { client_email: string; private_key: string; project_id: string };
  try {
    sa = JSON.parse(raw);
  } catch {
    throw new Error("FIREBASE_SERVICE_ACCOUNT is not valid JSON");
  }

  if (!sa.client_email || !sa.private_key) {
    throw new Error(
      "FIREBASE_SERVICE_ACCOUNT missing client_email or private_key"
    );
  }

  fcmProjectId = sa.project_id;

  jwtClient = new JWT({
    email: sa.client_email,
    key: sa.private_key,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });

  return jwtClient;
}

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

// ── Data types ──────────────────────────────────────────────────

interface PendingNotification {
  id: string;
  user_id: string;
  type: string;
  title: string;
  body: string | null;
  ref_type: string | null;
  ref_id: string | null;
  priority: string;
}

interface FcmToken {
  id: string;
  token: string;
  platform: string;
}

/**
 * Map notification type → Android notification channel ID.
 * Must match NotificationChannelSetup.kt channel IDs.
 */
function channelForType(type: string): string {
  switch (type) {
    case "chat":
    case "group_chat":
      return "chat";
    case "order_status":
    case "after_sale":
    case "review":
      return "order";
    case "community":
      return "community";
    case "lost_found":
      return "lost_found";
    default:
      return "system";
  }
}

/**
 * Map notification priority → FCM Android delivery config.
 *
 * NOTE: This returns only delivery hints (priority, TTL).
 * We do NOT include an android.notification block because this is a
 * pure data message.  That ensures onMessageReceived() is called in
 * ALL states (including background) so the app can build a
 * notification with the correct deep-link PendingIntent.
 */
function androidConfig(priority: string): Record<string, unknown> {
  const isUrgent = priority === "urgent" || priority === "high";
  return {
    priority: isUrgent ? "high" : "normal",
    ttl: "86400s", // 24 hours
  };
}

// ── FCM send ────────────────────────────────────────────────────

interface FcmSendResult {
  token: string;
  success: boolean;
  error?: string;
  /** true → this token is permanently invalid and should be deleted */
  invalidToken?: boolean;
}

/**
 * Send a single FCM message via HTTP v1 API.
 * Returns structured result for token cleanup.
 *
 * PAYLOAD DESIGN (pure data message):
 * We do NOT include a top-level `notification` block because on Android
 * that would cause the system tray to handle the notification when the
 * app is in the background — onMessageReceived() would never fire,
 * and our custom deep-link PendingIntent would never be created.
 *
 * Instead we send a data-only message.  CampusMessagingService
 * receives it in every state and builds the notification with the
 * correct Intent extras for deep-link navigation.
 */
async function sendFcmMessage(
  notification: PendingNotification,
  fcmToken: FcmToken,
  accessToken: string
): Promise<FcmSendResult> {
  const channelId = channelForType(notification.type);

  const message: Record<string, unknown> = {
    message: {
      token: fcmToken.token,
      // Data payload — always delivered to onMessageReceived()
      data: {
        type: notification.type,
        title: notification.title,
        body: notification.body ?? "",
        ref_type: notification.ref_type ?? "",
        ref_id: notification.ref_id ?? "",
        notification_id: notification.id,
        channel_id: channelId,
      },
      // Android delivery hints only (no notification block)
      android: androidConfig(notification.priority),
    },
  };

  try {
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(message),
      }
    );

    const result = await response.json();

    if (response.ok) {
      return { token: fcmToken.token, success: true };
    }

    const fcmError = (result as any)?.error;
    const errorCode = fcmError?.details?.[0]?.errorCode as string | undefined;

    // Permanently invalid tokens — delete them
    const unrecoverable = [
      "UNREGISTERED",
      "INVALID_ARGUMENT",
      "SENDER_ID_MISMATCH",
      "NOT_FOUND",
    ];
    if (errorCode && unrecoverable.includes(errorCode)) {
      console.warn(
        `FCM unrecoverable error for token ${fcmToken.token.slice(0, 12)}...: ${errorCode}`
      );
      return {
        token: fcmToken.token,
        success: false,
        error: errorCode,
        invalidToken: true,
      };
    }

    // Transient errors — keep token, will retry next cycle
    return {
      token: fcmToken.token,
      success: false,
      error: fcmError?.message ?? "Unknown FCM error",
    };
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    console.error(`FCM network error for token ${fcmToken.token.slice(0, 12)}...: ${msg}`);
    return {
      token: fcmToken.token,
      success: false,
      error: msg,
    };
  }
}

// ── Main handler ─────────────────────────────────────────────────

/**
 * POST /push-notification
 *
 * Headers:
 *   Authorization: Bearer <service_role_key>  (REQUIRED)
 *
 * Body (optional):
 *   { limit?: number }  — max notifications to process (default 100)
 */
Deno.serve(async (req: Request) => {
  // CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return err(405, "Method not allowed — use POST");
  }

  // ── Authorization header validation ────────────────────────────
  const authHeader = req.headers.get("Authorization");
  const expectedKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (
    !expectedKey ||
    !authHeader ||
    authHeader !== `Bearer ${expectedKey}`
  ) {
    return err(401, "Unauthorized");
  }

  try {
    // ── 1. Parse optional limit ──────────────────────────────
    let limit = 100;
    try {
      const body = await req.json();
      if (body.limit && typeof body.limit === "number") {
        limit = Math.min(body.limit, 500); // cap at 500
      }
    } catch {
      // No body or invalid JSON — use default limit
    }

    // ── 2. Fetch pending notifications ──────────────────────
    const { data: notifications, error: notifError } = await supabaseAdmin
      .from("notifications")
      .select("id, user_id, type, title, body, ref_type, ref_id, priority")
      .eq("push_sent", false)
      .order("priority", { ascending: false })
      .order("created_at", { ascending: true })
      .limit(limit);

    if (notifError) {
      console.error("Failed to fetch notifications:", notifError.message);
      return err(500, "Failed to fetch pending notifications");
    }

    if (!notifications || notifications.length === 0) {
      return json({ processed: 0, message: "No pending notifications" });
    }

    const pending = notifications as PendingNotification[];
    console.log(`Processing ${pending.length} pending notifications`);

    // ── 3. Get FCM access token ──────────────────────────────
    const client = getFcmClient();
    let accessToken: string | null | undefined;
    try {
      accessToken = await client.getAccessToken();
    } catch (e) {
      console.error("Failed to get FCM access token:", e);
      return err(500, "FCM authentication failed");
    }

    if (!accessToken) {
      return err(500, "FCM authentication failed — no access token");
    }

    // ── 4. Process each notification ─────────────────────────
    let sentCount = 0;
    let failedCount = 0;
    const invalidTokens: string[] = [];

    for (const notif of pending) {
      // 4a. Get user's active FCM tokens
      const { data: tokens, error: tokenError } = await supabaseAdmin
        .from("fcm_tokens")
        .select("id, token, platform")
        .eq("user_id", notif.user_id)
        .eq("is_active", true);

      if (tokenError) {
        console.error(
          `Failed to fetch tokens for user ${notif.user_id}:`,
          tokenError.message
        );
        failedCount++;
        continue;
      }

      if (!tokens || tokens.length === 0) {
        // No active tokens — mark as sent (nothing to push to)
        await supabaseAdmin
          .from("notifications")
          .update({
            push_sent: true,
            push_sent_at: new Date().toISOString(),
          })
          .eq("id", notif.id);
        continue;
      }

      const fcmTokens = tokens as FcmToken[];

      // 4b. Send to each token
      let anySuccess = false;
      for (const fcmToken of fcmTokens) {
        const result = await sendFcmMessage(notif, fcmToken, accessToken);
        if (result.success) {
          sentCount++;
          anySuccess = true;
        } else {
          failedCount++;
          if (result.invalidToken) {
            invalidTokens.push(result.token);
          }
        }
      }

      // 4c. Mark notification as push_sent if at least one token succeeded
      if (anySuccess) {
        const { error: updateError } = await supabaseAdmin
          .from("notifications")
          .update({
            push_sent: true,
            push_sent_at: new Date().toISOString(),
          })
          .eq("id", notif.id);

        if (updateError) {
          console.error(
            `Failed to mark notification ${notif.id} as push_sent:`,
            updateError.message
          );
        }
      } else if (fcmTokens.length === 0) {
        // Mark as sent anyway (no target devices)
        await supabaseAdmin
          .from("notifications")
          .update({
            push_sent: true,
            push_sent_at: new Date().toISOString(),
          })
          .eq("id", notif.id);
      }
      // else: all tokens failed — leave push_sent=false for retry
    }

    // ── 5. Clean up invalid tokens ──────────────────────────
    if (invalidTokens.length > 0) {
      const { error: deleteError } = await supabaseAdmin
        .from("fcm_tokens")
        .update({ is_active: false, updated_at: new Date().toISOString() })
        .in("token", invalidTokens);

      if (deleteError) {
        console.error("Failed to deactivate invalid tokens:", deleteError.message);
      } else {
        console.log(`Deactivated ${invalidTokens.length} invalid FCM tokens`);
      }
    }

    // ── 6. Return summary ───────────────────────────────────
    return json({
      processed: pending.length,
      sent: sentCount,
      failed: failedCount,
      invalid_tokens_cleaned: invalidTokens.length,
      message: "OK",
    });
  } catch (e) {
    // Unexpected error — do NOT expose e.message to client
    console.error("Unexpected error:", e);
    return err(500, "Internal server error");
  }
});
