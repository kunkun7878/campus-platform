-- ═══════════════════════════════════════════════════════════
-- Revert: 20260523000017_phase6_schema
-- 回滚: 撤销 Phase 6 数据库扩展 —
--       1. 社区帖子/评论 UPDATE RLS 还原至 Migration 09 原版
--       2. 移除 moderation_logs + fcm_tokens 表及 RLS 策略
--       3. 移除 review_reason 保护 trigger + 字段
--       4. 移除 3 个计数 trigger + 函数
--       5. 社区帖子 section CHECK 还原为 2 值
--       6. claim→item 联动 trigger 移除 + partial unique index 还原
--       7. notification push trigger 移除
--       8. conversations last_message trigger 移除
--       9. 社区帖子/评论 SELECT RLS 还原
--      10. 社区帖子/评论 status CHECK 还原（移除 pending_review）
--      11. messages UPDATE RLS 移除
--      12. 新增字段移除（lost_found_items / conversations / notifications）
--      13. notifications.type CHECK 还原为 5 值
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
--
-- 警告: community_posts.section 回滚至 2 值 (campus_wall, discussion)
--       时，如已有数据包含新枚举值 (lost_found/second_hand/
--       help/announcement)，需先手工将相关行 section 改回 'discussion'，
--       否则 DDL 执行失败。
--       同理，status 含 pending_review 的行需先改状态。
-- ═══════════════════════════════════════════════════════════


-- ═══════════════════════════════════════════════════════════
-- 1. community_posts + community_comments UPDATE RLS 还原
--    恢复 Migration 09 原有策略（仅 author_id + school_id 校验）
--    顺序：先还原 comments → 再还原 posts
-- ═══════════════════════════════════════════════════════════

-- 1.1 还原 community_comments 用户 UPDATE 策略
DROP POLICY IF EXISTS community_comments_update_policy ON public.community_comments;
CREATE POLICY community_comments_update_policy ON public.community_comments
    FOR UPDATE
    TO authenticated
    USING (
        author_id = auth.uid()
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        author_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

-- 1.2 还原 community_posts 用户 UPDATE 策略
DROP POLICY IF EXISTS community_posts_update_policy ON public.community_posts;
CREATE POLICY community_posts_update_policy ON public.community_posts
    FOR UPDATE
    TO authenticated
    USING (
        author_id = auth.uid()
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        author_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );


-- ═══════════════════════════════════════════════════════════
-- 2. moderation_logs 表移除
--    DROP TABLE 自动清理关联的 RLS 策略、索引、约束
-- ═══════════════════════════════════════════════════════════

DROP TABLE IF EXISTS public.moderation_logs CASCADE;


-- ═══════════════════════════════════════════════════════════
-- 3. fcm_tokens 表移除
--    DROP TABLE 自动清理关联的 RLS 策略、trigger、索引
-- ═══════════════════════════════════════════════════════════

DROP TABLE IF EXISTS public.fcm_tokens CASCADE;


-- ═══════════════════════════════════════════════════════════
-- 4. review_reason 保护 trigger 移除
--    注意: 必须先删 trigger 再删字段，否则 trigger 引用不存在的列
-- ═══════════════════════════════════════════════════════════

-- 4.1 移除 community_posts review_reason 保护 trigger
DROP TRIGGER IF EXISTS trg_community_posts_review_reason ON public.community_posts;
DROP FUNCTION IF EXISTS public.check_posts_review_reason();

-- 4.2 移除 community_comments review_reason 保护 trigger
DROP TRIGGER IF EXISTS trg_community_comments_review_reason ON public.community_comments;
DROP FUNCTION IF EXISTS public.check_comments_review_reason();


-- ═══════════════════════════════════════════════════════════
-- 5. 计数 trigger 移除
-- ═══════════════════════════════════════════════════════════

-- 5.1 移除 member_count trigger
DROP TRIGGER IF EXISTS trg_group_members_count ON public.group_members;
DROP FUNCTION IF EXISTS public.update_group_member_count();

-- 5.2 移除 comment_count trigger
DROP TRIGGER IF EXISTS trg_community_comments_count ON public.community_comments;
DROP FUNCTION IF EXISTS public.update_post_comment_count();

-- 5.3 移除 like_count trigger
DROP TRIGGER IF EXISTS trg_post_likes_count ON public.post_likes;
DROP FUNCTION IF EXISTS public.update_post_like_count();


-- ═══════════════════════════════════════════════════════════
-- 6. community_comments + community_posts 移除 review_reason 字段
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.community_comments
    DROP COLUMN IF EXISTS review_reason;

ALTER TABLE public.community_posts
    DROP COLUMN IF EXISTS review_reason;


-- ═══════════════════════════════════════════════════════════
-- 7. community_posts.section CHECK 还原为 2 值
--    (campus_wall, discussion)
--    移除 Migration 17 新增的 4 个 section 索引
-- ═══════════════════════════════════════════════════════════

-- 7.1 移除新增的部分索引
DROP INDEX IF EXISTS idx_community_posts_section_lost_found;
DROP INDEX IF EXISTS idx_community_posts_section_second_hand;
DROP INDEX IF EXISTS idx_community_posts_section_help;
DROP INDEX IF EXISTS idx_community_posts_section_announcement;

-- 7.2 使用 DO 块还原 CHECK 约束为原值
DO $$
DECLARE
    v_constraint_name text;
BEGIN
    -- 找到包含 section 的 CHECK 约束
    SELECT con.conname INTO v_constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'community_posts'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) ILIKE '%section%';

    IF v_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.community_posts DROP CONSTRAINT IF EXISTS %I', v_constraint_name);
    END IF;

    -- 还原为原始 2 值 CHECK
    ALTER TABLE public.community_posts
        ADD CONSTRAINT community_posts_section_check
        CHECK (section IN ('campus_wall', 'discussion'));
END
$$;


-- ═══════════════════════════════════════════════════════════
-- 8. claim → lost_found_items 状态联动 trigger 移除
--    注意：先删 trigger 再删函数
-- ═══════════════════════════════════════════════════════════

DROP TRIGGER IF EXISTS trg_lost_found_claims_sync ON public.lost_found_claims;
DROP FUNCTION IF EXISTS public.sync_lost_found_item_on_claim();


-- ═══════════════════════════════════════════════════════════
-- 9. lost_found_claims partial unique index 还原
--    删除 partial unique index，恢复 Migration 08 的全量唯一索引
-- ═══════════════════════════════════════════════════════════

DROP INDEX IF EXISTS uq_lost_found_claims_item_approved;
DROP INDEX IF EXISTS uq_lost_found_claims_item_claimant;
CREATE UNIQUE INDEX IF NOT EXISTS uq_lost_found_claims_item_claimant
    ON public.lost_found_claims(item_id, claimant_id);


-- ═══════════════════════════════════════════════════════════
-- 10. notification push trigger 存根移除
-- ═══════════════════════════════════════════════════════════

DROP TRIGGER IF EXISTS trg_notifications_push_init ON public.notifications;
DROP FUNCTION IF EXISTS public.init_notification_push();


-- ═══════════════════════════════════════════════════════════
-- 11. conversations last_message trigger 移除
-- ═══════════════════════════════════════════════════════════

DROP TRIGGER IF EXISTS trg_messages_update_conversation ON public.messages;
DROP FUNCTION IF EXISTS public.update_conversation_last_message();


-- ═══════════════════════════════════════════════════════════
-- 12. community_comments SELECT RLS 还原
--     恢复 Migration 09 原有策略（仅 school_id + status!='deleted' 校验）
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS community_comments_select_policy ON public.community_comments;
CREATE POLICY community_comments_select_policy ON public.community_comments
    FOR SELECT
    TO authenticated
    USING (
        school_id = public.get_user_school_id()
        AND status != 'deleted'
    );


-- ═══════════════════════════════════════════════════════════
-- 13. community_posts SELECT RLS 还原
--     恢复 Migration 09 原有策略（仅 school_id + status!='deleted' 校验）
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS community_posts_select_policy ON public.community_posts;
CREATE POLICY community_posts_select_policy ON public.community_posts
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
        AND status != 'deleted'
    );


-- ═══════════════════════════════════════════════════════════
-- 14. community_comments.status CHECK 还原（移除 pending_review）
--     还原为 Migration 09 的 3 值：published, hidden, deleted
--     警告：如已有 pending_review 状态的评论，需先手工改状态
-- ═══════════════════════════════════════════════════════════

DO $$
DECLARE
    v_constraint_name text;
BEGIN
    SELECT con.conname INTO v_constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'community_comments'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) ILIKE '%status%';

    IF v_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.community_comments DROP CONSTRAINT IF EXISTS %I', v_constraint_name);
    END IF;

    ALTER TABLE public.community_comments
        ADD CONSTRAINT community_comments_status_check
        CHECK (status IN ('published', 'hidden', 'deleted'));
END
$$;


-- ═══════════════════════════════════════════════════════════
-- 15. community_posts.status CHECK 还原（移除 pending_review）
--     还原为 Migration 09 的 3 值：published, hidden, deleted
--     警告：如已有 pending_review 状态的帖子，需先手工改状态
-- ═══════════════════════════════════════════════════════════

DO $$
DECLARE
    v_constraint_name text;
BEGIN
    SELECT con.conname INTO v_constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'community_posts'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) ILIKE '%status%';

    IF v_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.community_posts DROP CONSTRAINT IF EXISTS %I', v_constraint_name);
    END IF;

    ALTER TABLE public.community_posts
        ADD CONSTRAINT community_posts_status_check
        CHECK (status IN ('published', 'hidden', 'deleted'));
END
$$;


-- ═══════════════════════════════════════════════════════════
-- 16. messages UPDATE RLS 移除
--     还原为 Migration 12 的原始状态（无 UPDATE policy）
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS messages_update_policy ON public.messages;
DROP POLICY IF EXISTS messages_agent_update_policy ON public.messages;


-- ═══════════════════════════════════════════════════════════
-- 17. lost_found_items 移除 returned_at 字段
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.lost_found_items
    DROP COLUMN IF EXISTS returned_at;


-- ═══════════════════════════════════════════════════════════
-- 18. conversations 移除新增字段 + 索引
--     last_message_sender_id → source_id → source_type
--     注意：先删 FK 字段避免依赖报错
-- ═══════════════════════════════════════════════════════════

DROP INDEX IF EXISTS idx_conversations_source;

ALTER TABLE public.conversations
    DROP COLUMN IF EXISTS last_message_sender_id CASCADE;

ALTER TABLE public.conversations
    DROP COLUMN IF EXISTS source_id;

ALTER TABLE public.conversations
    DROP COLUMN IF EXISTS source_type;


-- ═══════════════════════════════════════════════════════════
-- 19. notifications 移除新增字段 + 索引
--     push_sent_at → push_sent → priority
-- ═══════════════════════════════════════════════════════════

DROP INDEX IF EXISTS idx_notifications_user_priority;
DROP INDEX IF EXISTS idx_notifications_push_pending;

ALTER TABLE public.notifications
    DROP COLUMN IF EXISTS push_sent_at;

ALTER TABLE public.notifications
    DROP COLUMN IF EXISTS push_sent;

ALTER TABLE public.notifications
    DROP COLUMN IF EXISTS priority;


-- ═══════════════════════════════════════════════════════════
-- 20. notifications.type CHECK 还原为 5 值
--     移除 lost_found, community, group_chat
--     还原为 Migration 10 原值: order_status, review, system, chat, after_sale
--     警告：如已有数据使用了新枚举值，需先手工改 type
-- ═══════════════════════════════════════════════════════════

DO $$
DECLARE
    v_constraint_name text;
BEGIN
    SELECT con.conname INTO v_constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    WHERE rel.relname = 'notifications'
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) ILIKE '%type%';

    IF v_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS %I', v_constraint_name);
    END IF;

    ALTER TABLE public.notifications
        ADD CONSTRAINT notifications_type_check
        CHECK (type IN ('order_status', 'review', 'system', 'chat', 'after_sale'));
END
$$;
