// ═══════════════════════════════════════════════════════════════
// community-moderation Edge Function
//
// 职责: 接管社区帖子/评论的发布流程, 在入库前执行敏感词审核。
//
// Actions:
//   publish_post    — 发布帖子 (审核→入库)
//   update_post     — 编辑帖子 (审核→更新)
//   publish_comment — 发布评论 (审核→入库)
//
// 三级风险判定:
//   block  — 命中硬黑名单 → 不存库, 直接返回拒绝原因 + 命中词
//   review — 命中联系方式/灰名单 → 存库 status='pending_review'
//            + 写 moderation_logs (仅作者本人可见)
//   pass   — 无命中 → 存库 status='published' + 写 moderation_logs
//
// 安全:
//   - service_role 客户端 (绕过 RLS)
//   - JWT 提取 caller ID (防伪造作者)
//   - e.message 不返回客户端
// ═══════════════════════════════════════════════════════════════

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { moderate, type ModerationResult } from "../_shared/sensitive-words.ts";

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

// ── Moderation helpers ──────────────────────────────────────────

interface ModerationLogEntry {
  ref_type: "community_post" | "community_comment";
  ref_id: string;
  action: "approve" | "hide" | "delete" | "block" | "restore";
  reason: string;
  operator_id: string | null;
  school_id: string;
}

/** Write a moderation_log entry. Non-fatal — logs error but does not throw. */
async function writeModerationLog(entry: ModerationLogEntry): Promise<void> {
  const { error } = await supabaseAdmin.from("moderation_logs").insert(entry);
  if (error) {
    console.error("[moderation_logs] insert failed:", error.message);
    // Non-fatal — content is already persisted; log is best-effort
  }
}

// ── publish_post ────────────────────────────────────────────────

interface PublishPostInput {
  section: string;
  title: string;
  content: string;
  school_id: string;
  images?: unknown;
}

const VALID_SECTIONS = [
  "campus_wall",
  "discussion",
  "lost_found",
  "second_hand",
  "help",
  "announcement",
] as const;

function validatePublishPost(body: Record<string, unknown>): PublishPostInput {
  const section = body.section as string | undefined;
  const title = body.title as string | undefined;
  const content = body.content as string | undefined;
  const school_id = body.school_id as string | undefined;

  if (!section) throw { status: 400, message: "缺少必要参数: section" };
  if (!VALID_SECTIONS.includes(section as typeof VALID_SECTIONS[number])) {
    throw { status: 400, message: "无效的版块: " + section };
  }
  if (!title || typeof title !== "string" || title.trim().length === 0) {
    throw { status: 400, message: "缺少必要参数: title" };
  }
  if (title.trim().length > 200) {
    throw { status: 400, message: "标题长度不能超过 200 字" };
  }
  if (!content || typeof content !== "string" || content.trim().length === 0) {
    throw { status: 400, message: "缺少必要参数: content" };
  }
  if (content.trim().length > 10000) {
    throw { status: 400, message: "内容长度不能超过 10000 字" };
  }
  if (!school_id) {
    throw { status: 400, message: "缺少必要参数: school_id" };
  }

  return {
    section: section.trim(),
    title: title.trim(),
    content: content.trim(),
    school_id,
    images: body.images ?? [],
  };
}

async function handlePublishPost(
  callerId: string,
  body: Record<string, unknown>
): Promise<Response> {
  const input = validatePublishPost(body);

  // 1. Verify school_id matches caller's profile
  const { data: profile, error: profileError } = await supabaseAdmin
    .from("profiles")
    .select("school_id")
    .eq("id", callerId)
    .single();

  if (profileError || !profile) {
    console.error("Profile lookup failed:", profileError?.message);
    return err(500, "服务器内部错误，请稍后重试");
  }

  if (profile.school_id !== input.school_id) {
    return err(403, "只能在本校发布内容");
  }

  // 2. Moderate content (title + content both checked)
  const combinedText = input.title + " " + input.content;
  const result: ModerationResult = moderate(combinedText);

  // 3. Block: don't save, just return rejection
  if (result.level === "block") {
    return json(
      {
        success: false,
        action: "block",
        reason: result.reason,
        matched_words: result.matched,
      },
      200 // 200 not 4xx — client handles this as business rejection, not HTTP error
    );
  }

  // 4. Determine status and review_reason
  const status = result.level === "review" ? "pending_review" : "published";
  const reviewReason =
    result.level === "review"
      ? result.reason
      : "系统自动通过";

  // 5. Insert post
  const { data: posts, error: insertError } = await supabaseAdmin
    .from("community_posts")
    .insert({
      author_id: callerId,
      section: input.section,
      title: input.title,
      content: input.content,
      images: input.images,
      school_id: input.school_id,
      status,
      review_reason: reviewReason,
    })
    .select("id, title, status, created_at");

  if (insertError) {
    console.error("Post insert failed:", insertError.message);
    return err(500, "服务器内部错误，请稍后重试");
  }

  const post = posts![0];

  // 6. Notify post author of moderation result
  try {
    await supabaseAdmin.from("notifications").insert({
      user_id: callerId,
      type: "community",
      title: status === "published" ? "帖子发布成功" : "帖子已提交审核",
      body: status === "published"
        ? `您的帖子"${input.title}"已发布`
        : `您的帖子"${input.title}"已提交审核，审核通过后将自动发布`,
      ref_type: "community_post",
      ref_id: post.id,
    });
  } catch (notifErr) {
    console.error("Failed to insert publish_post notification:", notifErr);
    // Non-fatal — do not block the publish flow
  }

  // 7. Write moderation log
  const logEntry: ModerationLogEntry = {
    ref_type: "community_post",
    ref_id: post.id,
    action: "approve", // system auto-approved (for pass) or auto-flagged for review
    reason: result.level === "pass"
      ? `[AUTO] 系统自动通过`
      : `[AUTO-FLAG] ${result.reason}`,
    operator_id: null, // NULL = 系统自动审核
    school_id: input.school_id,
  };
  await writeModerationLog(logEntry);

  // 8. Return success
  return json(
    {
      success: true,
      action: result.level === "pass" ? "pass" : "review",
      post_id: post.id,
      status: post.status,
      reason: result.level === "review" ? result.reason : undefined,
    },
    201
  );
}

// ── publish_comment ─────────────────────────────────────────────

interface PublishCommentInput {
  post_id: string;
  content: string;
  school_id: string;
  parent_id?: string | null;
}

function validatePublishComment(
  body: Record<string, unknown>
): PublishCommentInput {
  const post_id = body.post_id as string | undefined;
  const content = body.content as string | undefined;
  const school_id = body.school_id as string | undefined;

  if (!post_id) throw { status: 400, message: "缺少必要参数: post_id" };
  if (!content || typeof content !== "string" || content.trim().length === 0) {
    throw { status: 400, message: "缺少必要参数: content" };
  }
  if (content.trim().length > 2000) {
    throw { status: 400, message: "评论长度不能超过 2000 字" };
  }
  if (!school_id) {
    throw { status: 400, message: "缺少必要参数: school_id" };
  }

  return {
    post_id,
    content: content.trim(),
    school_id,
    parent_id: (body.parent_id as string) || null,
  };
}

async function handlePublishComment(
  callerId: string,
  body: Record<string, unknown>
): Promise<Response> {
  const input = validatePublishComment(body);

  // 1. Verify school_id matches caller's profile
  const { data: profile, error: profileError } = await supabaseAdmin
    .from("profiles")
    .select("school_id")
    .eq("id", callerId)
    .single();

  if (profileError || !profile) {
    console.error("Profile lookup failed:", profileError?.message);
    return err(500, "服务器内部错误，请稍后重试");
  }

  if (profile.school_id !== input.school_id) {
    return err(403, "只能在本校发布评论");
  }

  // 2. Verify post exists and is not deleted
  const { data: post, error: postError } = await supabaseAdmin
    .from("community_posts")
    .select("id, school_id, status")
    .eq("id", input.post_id)
    .single();

  if (postError || !post) {
    return err(404, "帖子不存在或已删除");
  }

  if (post.status === "deleted") {
    return err(404, "帖子不存在或已删除");
  }

  if (post.school_id !== input.school_id) {
    return err(403, "不能评论其他学校的帖子");
  }

  // 3. If parent_id provided, verify parent comment exists and belongs to same post
  if (input.parent_id) {
    const { data: parent, error: parentError } = await supabaseAdmin
      .from("community_comments")
      .select("id, post_id, status")
      .eq("id", input.parent_id)
      .single();

    if (parentError || !parent) {
      return err(404, "父评论不存在或已删除");
    }
    if (parent.post_id !== input.post_id) {
      return err(400, "父评论不属于该帖子");
    }
    if (parent.status === "deleted") {
      return err(400, "不能回复已删除的评论");
    }
  }

  // 4. Moderate content
  const result: ModerationResult = moderate(input.content);

  // 5. Block: don't save, just return rejection
  if (result.level === "block") {
    return json(
      {
        success: false,
        action: "block",
        reason: result.reason,
        matched_words: result.matched,
      },
      200
    );
  }

  // 6. Determine status and review_reason
  const status = result.level === "review" ? "pending_review" : "published";
  const reviewReason =
    result.level === "review"
      ? result.reason
      : "系统自动通过";

  // 7. Insert comment
  const { data: comments, error: insertError } = await supabaseAdmin
    .from("community_comments")
    .insert({
      post_id: input.post_id,
      author_id: callerId,
      parent_id: input.parent_id || null,
      content: input.content,
      school_id: input.school_id,
      status,
      review_reason: reviewReason,
    })
    .select("id, post_id, status, created_at");

  if (insertError) {
    console.error("Comment insert failed:", insertError.message);
    return err(500, "服务器内部错误，请稍后重试");
  }

  const comment = comments![0];

  // 8. Notify comment author of moderation result
  try {
    await supabaseAdmin.from("notifications").insert({
      user_id: callerId,
      type: "community",
      title: status === "published" ? "评论发布成功" : "评论已提交审核",
      body: status === "published"
        ? `您的评论已发布`
        : `您的评论已提交审核，审核通过后将自动发布`,
      ref_type: "community_comment",
      ref_id: comment.id,
    });
  } catch (notifErr) {
    console.error("Failed to insert publish_comment notification:", notifErr);
    // Non-fatal — do not block the comment flow
  }

  // 9. Write moderation log
  const logEntry: ModerationLogEntry = {
    ref_type: "community_comment",
    ref_id: comment.id,
    action: "approve",
    reason: result.level === "pass"
      ? `[AUTO] 系统自动通过`
      : `[AUTO-FLAG] ${result.reason}`,
    operator_id: null,
    school_id: input.school_id,
  };
  await writeModerationLog(logEntry);

  // 10. Return success
  return json(
    {
      success: true,
      action: result.level === "pass" ? "pass" : "review",
      comment_id: comment.id,
      status: comment.status,
      reason: result.level === "review" ? result.reason : undefined,
    },
    201
  );
}

// ── update_post ──────────────────────────────────────────────────

interface UpdatePostInput {
  id: string;
  title: string;
  content: string;
  school_id: string;
  images?: unknown;
}

function validateUpdatePost(
  body: Record<string, unknown>
): UpdatePostInput {
  const id = body.id as string | undefined;
  const title = body.title as string | undefined;
  const content = body.content as string | undefined;
  const school_id = body.school_id as string | undefined;

  if (!id) throw { status: 400, message: "缺少必要参数: id" };
  if (!title || typeof title !== "string" || title.trim().length === 0) {
    throw { status: 400, message: "缺少必要参数: title" };
  }
  if (title.trim().length > 200) {
    throw { status: 400, message: "标题长度不能超过 200 字" };
  }
  if (!content || typeof content !== "string" || content.trim().length === 0) {
    throw { status: 400, message: "缺少必要参数: content" };
  }
  if (content.trim().length > 10000) {
    throw { status: 400, message: "内容长度不能超过 10000 字" };
  }
  if (!school_id) {
    throw { status: 400, message: "缺少必要参数: school_id" };
  }

  return {
    id,
    title: title.trim(),
    content: content.trim(),
    school_id,
    images: body.images ?? [],
  };
}

async function handleUpdatePost(
  callerId: string,
  body: Record<string, unknown>
): Promise<Response> {
  const input = validateUpdatePost(body);

  // 1. Verify post exists and caller is the author
  const { data: post, error: postError } = await supabaseAdmin
    .from("community_posts")
    .select("id, author_id, school_id, status")
    .eq("id", input.id)
    .single();

  if (postError || !post) {
    return err(404, "帖子不存在");
  }

  if (post.author_id !== callerId) {
    return err(403, "只能编辑自己的帖子");
  }

  if (post.status === "deleted") {
    return err(400, "已删除的帖子无法编辑");
  }

  // 2. Verify school_id matches
  if (post.school_id !== input.school_id) {
    return err(403, "只能在本校编辑帖子");
  }

  // 3. Moderate updated content
  const combinedText = input.title + " " + input.content;
  const result: ModerationResult = moderate(combinedText);

  // 4. Block: don't update, just return rejection
  if (result.level === "block") {
    return json(
      {
        success: false,
        action: "block",
        reason: result.reason,
        matched_words: result.matched,
      },
      200
    );
  }

  // 5. Determine new status and review_reason
  const newStatus =
    result.level === "review" ? "pending_review" : "published";
  const reviewReason =
    result.level === "review"
      ? result.reason
      : "系统自动通过";

  // 6. Update post
  const { error: updateError } = await supabaseAdmin
    .from("community_posts")
    .update({
      title: input.title,
      content: input.content,
      images: input.images,
      status: newStatus,
      review_reason: reviewReason,
      updated_at: new Date().toISOString(),
    })
    .eq("id", input.id);

  if (updateError) {
    console.error("Post update failed:", updateError.message);
    return err(500, "服务器内部错误，请稍后重试");
  }

  // 7. Notify post author of moderation result
  try {
    await supabaseAdmin.from("notifications").insert({
      user_id: callerId,
      type: "community",
      title: newStatus === "published" ? "帖子编辑成功" : "帖子编辑已提交审核",
      body: newStatus === "published"
        ? `您的帖子"${input.title}"已更新`
        : `您的帖子"${input.title}"编辑后已提交审核，审核通过后将自动发布`,
      ref_type: "community_post",
      ref_id: input.id,
    });
  } catch (notifErr) {
    console.error("Failed to insert update_post notification:", notifErr);
    // Non-fatal — do not block the update flow
  }

  // 8. Write moderation log
  const logEntry: ModerationLogEntry = {
    ref_type: "community_post",
    ref_id: input.id,
    action: "approve",
    reason:
      result.level === "pass"
        ? `[AUTO] 编辑后系统自动通过`
        : `[AUTO-FLAG] 编辑触发审核: ${result.reason}`,
    operator_id: null,
    school_id: input.school_id,
  };
  await writeModerationLog(logEntry);

  // 9. Return success
  return json(
    {
      success: true,
      action: result.level === "pass" ? "pass" : "review",
      post_id: input.id,
      status: newStatus,
      reason: result.level === "review" ? result.reason : undefined,
    },
    200
  );
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
      return err(400, "缺少必要参数: action");
    }

    switch (action) {
      case "publish_post":
        return await handlePublishPost(callerId, body);

      case "publish_comment":
        return await handlePublishComment(callerId, body);

      case "update_post":
        return await handleUpdatePost(callerId, body);

      default:
        return err(400, "不支持的操作类型: " + action);
    }
  } catch (e) {
    // If the error was thrown from our helpers it carries {status, message}
    if (e && typeof e === "object" && "status" in e && "message" in e) {
      const { status, message, detail } = e as Record<string, unknown>;
      return err(status as number, message as string, detail as string | undefined);
    }
    // Unexpected error — do NOT expose e.message to client
    console.error("Unexpected error:", e);
    return err(500, "Internal server error");
  }
});
