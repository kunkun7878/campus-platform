-- ═══════════════════════════════════════════════════════════
-- Migration: 20260523000017_phase6_schema
-- 描述: Phase 6 数据库扩展 —
--       1. community_posts.section CHECK 枚举扩展（2→6 值）
--       2. community_posts + community_comments 加 review_reason 字段
--       3. 3 个计数 trigger（like_count / comment_count / member_count）
--       4. fcm_tokens 表 — FCM 设备推送令牌
--       5. moderation_logs 表 — 内容审核日志
--       6. 数据一致性修复（补算现有计数字段）
--       7. RLS 策略调整（review_reason 保护 + fcm_tokens 隔离）
--       8. notifications.type CHECK 扩展（5→8 值 + priority/push_sent 字段）
--       9. conversations 加 source_type/source_id/last_message_sender_id
--      10. lost_found_items 加 returned_at
--      11. messages UPDATE RLS（修复 markAsRead 静默失败 P0）
--      12. community_posts/comments status CHECK 加 pending_review
--      13. community_posts/comments SELECT RLS（pending_review 仅作者可见）
--      14. conversations last_message trigger（消息插入时自动更新）
--      15. notification push trigger 存根
--      16. lost_found_claims partial unique index + claim→item 状态联动
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
--
-- 说明: 所有计数 trigger 豁免 service_role，
--       Edge Function 批量操作时由 service_role 写入。
--       review_reason 仅 Agent / service_role 可写，
--       普通用户不可自改此字段。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. community_posts.section CHECK 枚举扩展
--    原值: campus_wall, discussion
--    新值: campus_wall, discussion, lost_found, second_hand, help, announcement
--    （与 Android 端 ChannelTabBar.kt / CommunityViewModel 保持同步）
--    使用 DO 块动态获取约束名后 DROP + ADD
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
      AND pg_get_constraintdef(con.oid) ILIKE '%section%';

    IF v_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.community_posts DROP CONSTRAINT IF EXISTS %I', v_constraint_name);
    END IF;

    -- 添加新 CHECK，6 个枚举值
    ALTER TABLE public.community_posts
        ADD CONSTRAINT community_posts_section_check
        CHECK (section IN ('campus_wall', 'discussion', 'lost_found', 'second_hand', 'help', 'announcement'));
END
$$;

-- 为新增的 section 值建索引（扩展已有复合索引 coverage）
-- campus_wall / discussion 已由 idx_community_posts_school_section 覆盖，
-- 其他值需要新索引以保持查询性能
CREATE INDEX IF NOT EXISTS idx_community_posts_section_lost_found
    ON public.community_posts(school_id, created_at DESC)
    WHERE section = 'lost_found';
CREATE INDEX IF NOT EXISTS idx_community_posts_section_second_hand
    ON public.community_posts(school_id, created_at DESC)
    WHERE section = 'second_hand';
CREATE INDEX IF NOT EXISTS idx_community_posts_section_help
    ON public.community_posts(school_id, created_at DESC)
    WHERE section = 'help';
CREATE INDEX IF NOT EXISTS idx_community_posts_section_announcement
    ON public.community_posts(school_id, created_at DESC)
    WHERE section = 'announcement';


-- ═══════════════════════════════════════════════════════════
-- 2. community_posts ADD review_reason — 审核原因/备注
--    Agent 审核帖子时填写，记录为何通过/拒绝/隐藏
--    普通用户不可写（由 trigger 保护）
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.community_posts
    ADD COLUMN IF NOT EXISTS review_reason text;
-- NULL = 未经过审核（首次发布默认值）
-- 有值 = Agent 审核后填写的原因说明


-- ═══════════════════════════════════════════════════════════
-- 3. community_comments ADD review_reason — 审核原因/备注
--    评论和帖子走同一个审核流程
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.community_comments
    ADD COLUMN IF NOT EXISTS review_reason text;
-- NULL = 未经过审核
-- 有值 = Agent 审核后填写的原因说明


-- ═══════════════════════════════════════════════════════════
-- 4. 计数 trigger: like_count
--    post_likes INSERT → community_posts.like_count +1
--    post_likes DELETE → community_posts.like_count -1
--    豁免 service_role（Edge Function 批量操作）
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.update_post_like_count()
RETURNS trigger AS $$
BEGIN
    -- service_role 不触发计数变更（由调用方自行维护）
    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN COALESCE(NEW, OLD);
    END IF;

    IF TG_OP = 'INSERT' THEN
        UPDATE public.community_posts
        SET like_count = like_count + 1
        WHERE id = NEW.post_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.community_posts
        SET like_count = GREATEST(like_count - 1, 0)
        WHERE id = OLD.post_id;
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 应用 trigger
DROP TRIGGER IF EXISTS trg_post_likes_count ON public.post_likes;
CREATE TRIGGER trg_post_likes_count
    AFTER INSERT OR DELETE ON public.post_likes
    FOR EACH ROW
    EXECUTE FUNCTION public.update_post_like_count();


-- ═══════════════════════════════════════════════════════════
-- 5. 计数 trigger: comment_count
--    community_comments INSERT → community_posts.comment_count +1
--    community_comments DELETE → community_posts.comment_count -1
--    仅统计 published 状态的评论；hidden/deleted 不计数
--    豁免 service_role（Edge Function 批量操作）
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.update_post_comment_count()
RETURNS trigger AS $$
BEGIN
    -- service_role 不触发计数变更（由调用方自行维护）
    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN COALESCE(NEW, OLD);
    END IF;

    IF TG_OP = 'INSERT' AND NEW.status = 'published' THEN
        UPDATE public.community_posts
        SET comment_count = comment_count + 1
        WHERE id = NEW.post_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' AND OLD.status = 'published' THEN
        UPDATE public.community_posts
        SET comment_count = GREATEST(comment_count - 1, 0)
        WHERE id = OLD.post_id;
        RETURN OLD;
    -- UPDATE: 状态从 published → hidden/deleted 时 -1
    --        状态从 hidden/deleted → published 时 +1
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.status = 'published' AND NEW.status != 'published' THEN
            UPDATE public.community_posts
            SET comment_count = GREATEST(comment_count - 1, 0)
            WHERE id = NEW.post_id;
        ELSIF OLD.status != 'published' AND NEW.status = 'published' THEN
            UPDATE public.community_posts
            SET comment_count = comment_count + 1
            WHERE id = NEW.post_id;
        END IF;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 应用 trigger
DROP TRIGGER IF EXISTS trg_community_comments_count ON public.community_comments;
CREATE TRIGGER trg_community_comments_count
    AFTER INSERT OR DELETE OR UPDATE OF status ON public.community_comments
    FOR EACH ROW
    EXECUTE FUNCTION public.update_post_comment_count();


-- ═══════════════════════════════════════════════════════════
-- 6. 计数 trigger: member_count
--    group_members INSERT → official_groups.member_count +1
--    group_members DELETE → official_groups.member_count -1
--    豁免 service_role（Edge Function 批量操作）
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.update_group_member_count()
RETURNS trigger AS $$
BEGIN
    -- service_role 不触发计数变更（由调用方自行维护）
    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN COALESCE(NEW, OLD);
    END IF;

    IF TG_OP = 'INSERT' THEN
        UPDATE public.official_groups
        SET member_count = member_count + 1
        WHERE id = NEW.group_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.official_groups
        SET member_count = GREATEST(member_count - 1, 0)
        WHERE id = OLD.group_id;
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 应用 trigger
DROP TRIGGER IF EXISTS trg_group_members_count ON public.group_members;
CREATE TRIGGER trg_group_members_count
    AFTER INSERT OR DELETE ON public.group_members
    FOR EACH ROW
    EXECUTE FUNCTION public.update_group_member_count();


-- ═══════════════════════════════════════════════════════════
-- 7. 数据一致性修复：从现有数据重新计算计数字段
--    确保 like_count / comment_count / member_count 与实际行数一致
-- ═══════════════════════════════════════════════════════════

-- 7.1 校准 community_posts.like_count
UPDATE public.community_posts cp
SET like_count = (
    SELECT COUNT(*) FROM public.post_likes pl
    WHERE pl.post_id = cp.id
)
WHERE like_count != (
    SELECT COUNT(*) FROM public.post_likes pl
    WHERE pl.post_id = cp.id
);

-- 7.2 校准 community_posts.comment_count（仅计数 published 状态评论）
UPDATE public.community_posts cp
SET comment_count = (
    SELECT COUNT(*) FROM public.community_comments cc
    WHERE cc.post_id = cp.id AND cc.status = 'published'
)
WHERE comment_count != (
    SELECT COUNT(*) FROM public.community_comments cc
    WHERE cc.post_id = cp.id AND cc.status = 'published'
);

-- 7.3 校准 official_groups.member_count
UPDATE public.official_groups og
SET member_count = (
    SELECT COUNT(*) FROM public.group_members gm
    WHERE gm.group_id = og.id
)
WHERE member_count != (
    SELECT COUNT(*) FROM public.group_members gm
    WHERE gm.group_id = og.id
);


-- ═══════════════════════════════════════════════════════════
-- 8. fcm_tokens — FCM 设备推送令牌
--    支持单用户多设备（手机 + 平板等）
--    token 唯一，设备更换时 INSERT 新 token + DELETE 旧 token
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.fcm_tokens (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token           text NOT NULL,
    -- FCM 注册令牌（由客户端获取后上报）
    platform        text NOT NULL DEFAULT 'android'
                    CHECK (platform IN ('android', 'ios', 'web')),
    -- 设备平台
    device_name     text,
    -- 设备名称（如 "Xiaomi 14"），用于多设备管理
    is_active       boolean DEFAULT true,
    -- 令牌是否有效，注销或更换设备时设为 false
    last_used_at    timestamptz DEFAULT now(),
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now(),
    UNIQUE(token)
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_fcm_tokens_updated_at ON public.fcm_tokens;
CREATE TRIGGER trg_fcm_tokens_updated_at
    BEFORE UPDATE ON public.fcm_tokens
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_id ON public.fcm_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_token ON public.fcm_tokens(token);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_active ON public.fcm_tokens(user_id, is_active)
    WHERE is_active = true;


-- ═══════════════════════════════════════════════════════════
-- 9. moderation_logs — 内容审核日志
--    记录 Agent / 系统对帖子或评论的每一次审核操作
--    用于审核追溯和投诉处理
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.moderation_logs (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_type        text NOT NULL
                    CHECK (ref_type IN ('community_post', 'community_comment')),
    -- 被审核的内容类型
    ref_id          uuid NOT NULL,
    -- 被审核的内容 ID（post_id 或 comment_id）
    action          text NOT NULL
                    CHECK (action IN ('approve', 'hide', 'delete', 'block', 'restore')),
    -- approve=通过, hide=隐藏, delete=删除, block=封禁, restore=恢复
    reason          text,
    -- 审核原因/备注
    operator_id     uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    -- 审核人 ID，NULL = 系统自动审核
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    -- 所属学校（用于学校级管理）
    created_at      timestamptz DEFAULT now()
);
-- 注：审核日志为 append-only，不可修改/删除，无 updated_at。

-- indexes
CREATE INDEX IF NOT EXISTS idx_moderation_logs_school_id ON public.moderation_logs(school_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_ref ON public.moderation_logs(ref_type, ref_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_operator_id ON public.moderation_logs(operator_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_created_at ON public.moderation_logs(school_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_action ON public.moderation_logs(school_id, action, created_at DESC);


-- ═══════════════════════════════════════════════════════════
-- 10. review_reason 写入保护 trigger
--     普通用户不可修改自己帖子的 review_reason
--     仅 Agent 或 service_role 可写入
-- ═══════════════════════════════════════════════════════════

-- 10.1 community_posts: 保护 review_reason
CREATE OR REPLACE FUNCTION public.check_posts_review_reason()
RETURNS trigger AS $$
BEGIN
    -- service_role 可修改任意字段，不拦截
    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN NEW;
    END IF;

    -- Agent 可修改任意字段，不拦截
    IF public.is_agent() THEN
        RETURN NEW;
    END IF;

    -- 普通用户：不允许修改 review_reason
    IF NEW.review_reason IS DISTINCT FROM OLD.review_reason THEN
        RAISE EXCEPTION 'permission denied: only agents can modify review_reason';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

DROP TRIGGER IF EXISTS trg_community_posts_review_reason ON public.community_posts;
CREATE TRIGGER trg_community_posts_review_reason
    BEFORE UPDATE ON public.community_posts
    FOR EACH ROW
    EXECUTE FUNCTION public.check_posts_review_reason();

-- 10.2 community_comments: 保护 review_reason
CREATE OR REPLACE FUNCTION public.check_comments_review_reason()
RETURNS trigger AS $$
BEGIN
    -- service_role 可修改任意字段，不拦截
    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN NEW;
    END IF;

    -- Agent 可修改任意字段，不拦截
    IF public.is_agent() THEN
        RETURN NEW;
    END IF;

    -- 普通用户：不允许修改 review_reason
    IF NEW.review_reason IS DISTINCT FROM OLD.review_reason THEN
        RAISE EXCEPTION 'permission denied: only agents can modify review_reason';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

DROP TRIGGER IF EXISTS trg_community_comments_review_reason ON public.community_comments;
CREATE TRIGGER trg_community_comments_review_reason
    BEFORE UPDATE ON public.community_comments
    FOR EACH ROW
    EXECUTE FUNCTION public.check_comments_review_reason();


-- ═══════════════════════════════════════════════════════════
-- 11. RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── fcm_tokens ────────────────────────────────────────────

ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

-- SELECT: 用户只能看自己的令牌
DROP POLICY IF EXISTS fcm_tokens_select_policy ON public.fcm_tokens;
CREATE POLICY fcm_tokens_select_policy ON public.fcm_tokens
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- Agent 可查看所有令牌
DROP POLICY IF EXISTS fcm_tokens_agent_select_policy ON public.fcm_tokens;
CREATE POLICY fcm_tokens_agent_select_policy ON public.fcm_tokens
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 用户注册自己的设备令牌
DROP POLICY IF EXISTS fcm_tokens_insert_policy ON public.fcm_tokens;
CREATE POLICY fcm_tokens_insert_policy ON public.fcm_tokens
    FOR INSERT
    TO authenticated
    WITH CHECK (user_id = auth.uid());

-- UPDATE: 用户更新自己的令牌状态（如登出时停用）；Agent 可管理所有
DROP POLICY IF EXISTS fcm_tokens_update_policy ON public.fcm_tokens;
CREATE POLICY fcm_tokens_update_policy ON public.fcm_tokens
    FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS fcm_tokens_agent_update_policy ON public.fcm_tokens;
CREATE POLICY fcm_tokens_agent_update_policy ON public.fcm_tokens
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 用户可删除自己的令牌（设备登出）
DROP POLICY IF EXISTS fcm_tokens_delete_policy ON public.fcm_tokens;
CREATE POLICY fcm_tokens_delete_policy ON public.fcm_tokens
    FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS fcm_tokens_agent_delete_policy ON public.fcm_tokens;
CREATE POLICY fcm_tokens_agent_delete_policy ON public.fcm_tokens
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── moderation_logs ───────────────────────────────────────

ALTER TABLE public.moderation_logs ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校 Agent 可见本校审核日志；普通用户不可见任何日志
DROP POLICY IF EXISTS moderation_logs_select_policy ON public.moderation_logs;
CREATE POLICY moderation_logs_select_policy ON public.moderation_logs
    FOR SELECT
    TO authenticated
    USING (
        public.is_agent()
        AND school_id = public.get_user_school_id()
    );

-- Agent 可跨校查看（上级管理）
DROP POLICY IF EXISTS moderation_logs_agent_select_policy ON public.moderation_logs;
CREATE POLICY moderation_logs_agent_select_policy ON public.moderation_logs
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 仅 Agent / service_role 可写入审核日志
DROP POLICY IF EXISTS moderation_logs_insert_policy ON public.moderation_logs;
CREATE POLICY moderation_logs_insert_policy ON public.moderation_logs
    FOR INSERT
    TO authenticated
    WITH CHECK (
        public.is_agent()
        AND school_id = public.get_user_school_id()
    );

-- UPDATE/DELETE: 审核日志为 append-only，不允许修改或删除
-- （不创建 UPDATE/DELETE policy = append-only）


-- ═══════════════════════════════════════════════════════════
-- 12. community_posts UPDATE RLS 补充：Agent 审核写入保护
--     确保非 Agent 用户即使绕过了 review_reason trigger，
--     RLS 层面也无法写入 review_reason（第二道防线）
--     （原有 UPDATE policy 仅限制 author_id + school_id，
--       未阻止用户修改 status/review_reason 绕过审核）
-- ═══════════════════════════════════════════════════════════

-- 替换用户自更新策略，加 status 变更限制：
-- 普通用户只能修改 title/content/images（内容字段），
-- 不可修改 status/pinned/review_reason（审核字段）
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
        -- 以下字段不可由作者自改：
        AND status = (
            SELECT p.status FROM public.community_posts p
            WHERE p.id = community_posts.id
        )
        AND is_pinned = (
            SELECT p.is_pinned FROM public.community_posts p
            WHERE p.id = community_posts.id
        )
        AND review_reason IS NOT DISTINCT FROM (
            SELECT p.review_reason FROM public.community_posts p
            WHERE p.id = community_posts.id
        )
    );

-- ── community_comments UPDATE RLS 同理 ────────────────────

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
        -- 以下字段不可由作者自改：
        AND status = (
            SELECT c.status FROM public.community_comments c
            WHERE c.id = community_comments.id
        )
        AND review_reason IS NOT DISTINCT FROM (
            SELECT c.review_reason FROM public.community_comments c
            WHERE c.id = community_comments.id
        )
    );


-- ═══════════════════════════════════════════════════════════
-- 13. notifications.type CHECK 枚举扩展（5 → 8 值）
--     原值: order_status, review, system, chat, after_sale
--     新增: lost_found, community, group_chat
--     DO 块动态获取约束名后 DROP + ADD
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
        CHECK (type IN ('order_status', 'review', 'system', 'chat', 'after_sale',
                        'lost_found', 'community', 'group_chat'));
END
$$;


-- ═══════════════════════════════════════════════════════════
-- 14. notifications 加字段 — 推送相关
--     priority: 通知优先级（用于客户端排序 + 推送策略）
--     push_sent: FCM 推送是否已发送
--     push_sent_at: 推送发送时间
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS priority text NOT NULL DEFAULT 'normal'
        CHECK (priority IN ('low', 'normal', 'high', 'urgent'));

ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS push_sent boolean DEFAULT false;

ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS push_sent_at timestamptz;

-- 为新字段建索引（高频查询：按优先级排列未读通知）
CREATE INDEX IF NOT EXISTS idx_notifications_user_priority
    ON public.notifications(user_id, priority, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_push_pending
    ON public.notifications(push_sent, created_at DESC)
    WHERE push_sent = false;


-- ═══════════════════════════════════════════════════════════
-- 15. conversations 加字段 — 来源追踪 + 最后发言者
--     source_type / source_id: 会话发起来源
--       （如 'lost_found'→item_id, 'community_post'→post_id）
--     last_message_sender_id: 最后一条消息的发送者
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.conversations
    ADD COLUMN IF NOT EXISTS source_type text;

ALTER TABLE public.conversations
    ADD COLUMN IF NOT EXISTS source_id uuid;

ALTER TABLE public.conversations
    ADD COLUMN IF NOT EXISTS last_message_sender_id uuid
        REFERENCES auth.users(id) ON DELETE SET NULL;

-- 索引：按来源类型+ID 查询会话
CREATE INDEX IF NOT EXISTS idx_conversations_source
    ON public.conversations(source_type, source_id);


-- ═══════════════════════════════════════════════════════════
-- 16. lost_found_items 加字段 — returned_at
--     物品归还/领取时间，与 status='claimed' 配套使用
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.lost_found_items
    ADD COLUMN IF NOT EXISTS returned_at timestamptz;


-- ═══════════════════════════════════════════════════════════
-- 17. messages UPDATE RLS — 修复 markAsRead 静默失败 (P0)
--     当前无 UPDATE policy，任何 messages UPDATE 都失败。
--     本次新增：会话参与者可标记本会话消息的 is_read 字段。
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS messages_update_policy ON public.messages;
CREATE POLICY messages_update_policy ON public.messages
    FOR UPDATE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversations
            WHERE conversations.id = messages.conversation_id
              AND (conversations.user1_id = auth.uid()
                   OR conversations.user2_id = auth.uid())
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.conversations
            WHERE conversations.id = messages.conversation_id
              AND (conversations.user1_id = auth.uid()
                   OR conversations.user2_id = auth.uid())
        )
        -- 仅允许修改 is_read 字段（其他字段不可变更）
        AND sender_id IS NOT DISTINCT FROM (
            SELECT m.sender_id FROM public.messages m
            WHERE m.id = messages.id
        )
        AND conversation_id IS NOT DISTINCT FROM (
            SELECT m.conversation_id FROM public.messages m
            WHERE m.id = messages.id
        )
        AND content IS NOT DISTINCT FROM (
            SELECT m.content FROM public.messages m
            WHERE m.id = messages.id
        )
        AND created_at IS NOT DISTINCT FROM (
            SELECT m.created_at FROM public.messages m
            WHERE m.id = messages.id
        )
    );

-- Agent 可更新所有消息
DROP POLICY IF EXISTS messages_agent_update_policy ON public.messages;
CREATE POLICY messages_agent_update_policy ON public.messages
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());


-- ═══════════════════════════════════════════════════════════
-- 18. community_posts + community_comments status CHECK 扩展
--     原值: published, hidden, deleted
--     新增: pending_review（Agent 审核前的新建草稿状态）
--     DO 块动态获取约束名后 DROP + ADD
-- ═══════════════════════════════════════════════════════════

-- 18.1 community_posts.status CHECK 扩展
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
        CHECK (status IN ('published', 'hidden', 'deleted', 'pending_review'));
END
$$;

-- 18.2 community_comments.status CHECK 扩展
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
        CHECK (status IN ('published', 'hidden', 'deleted', 'pending_review'));
END
$$;


-- ═══════════════════════════════════════════════════════════
-- 19. community_posts + community_comments SELECT RLS 调整
--     pending_review 状态的帖子/评论仅作者本人可见
--     （Agent 通过 agent_select_policy 不受影响）
-- ═══════════════════════════════════════════════════════════

-- 19.1 community_posts SELECT RLS：pending_review 仅 author 可见
DROP POLICY IF EXISTS community_posts_select_policy ON public.community_posts;
CREATE POLICY community_posts_select_policy ON public.community_posts
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
        AND status != 'deleted'
        AND (status != 'pending_review' OR author_id = auth.uid())
    );

-- 19.2 community_comments SELECT RLS：pending_review 仅 author 可见
DROP POLICY IF EXISTS community_comments_select_policy ON public.community_comments;
CREATE POLICY community_comments_select_policy ON public.community_comments
    FOR SELECT
    TO authenticated
    USING (
        school_id = public.get_user_school_id()
        AND status != 'deleted'
        AND (status != 'pending_review' OR author_id = auth.uid())
    );


-- ═══════════════════════════════════════════════════════════
-- 20. conversations last_message trigger
--     messages INSERT 后自动更新对应 conversation 的
--     last_message / last_message_at / last_message_sender_id
--     service_role 豁免（Edge Function 批量操作由调用方维护）
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.update_conversation_last_message()
RETURNS trigger AS $$
BEGIN
    -- service_role 不触发更新（由调用方自行维护）
    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN NEW;
    END IF;

    UPDATE public.conversations
    SET last_message            = NEW.content,
        last_message_at         = NEW.created_at,
        last_message_sender_id  = NEW.sender_id
    WHERE id = NEW.conversation_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

DROP TRIGGER IF EXISTS trg_messages_update_conversation ON public.messages;
CREATE TRIGGER trg_messages_update_conversation
    AFTER INSERT ON public.messages
    FOR EACH ROW
    EXECUTE FUNCTION public.update_conversation_last_message();


-- ═══════════════════════════════════════════════════════════
-- 21. notification push trigger 存根
--     notifications INSERT 时初始化 push_sent = false
--     Edge Function / 定时任务根据 push_sent=false 扫描待推送通知
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.init_notification_push()
RETURNS trigger AS $$
BEGIN
    NEW.push_sent = COALESCE(NEW.push_sent, false);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

DROP TRIGGER IF EXISTS trg_notifications_push_init ON public.notifications;
CREATE TRIGGER trg_notifications_push_init
    BEFORE INSERT ON public.notifications
    FOR EACH ROW
    EXECUTE FUNCTION public.init_notification_push();


-- ═══════════════════════════════════════════════════════════
-- 22. lost_found_claims partial unique index
--     每人每物仅允许一个活跃认领（rejected 状态的旧认领不冲突）
--     Migration 08 已有全量 unique index (item_id, claimant_id)，
--     本次替换为 partial unique index，允许被拒后重新认领。
-- ═══════════════════════════════════════════════════════════

-- 先删除旧的 full unique index
DROP INDEX IF EXISTS uq_lost_found_claims_item_claimant;

-- 再创建 partial unique index：仅非 rejected 状态受唯一约束
CREATE UNIQUE INDEX IF NOT EXISTS uq_lost_found_claims_item_claimant
    ON public.lost_found_claims(item_id, claimant_id)
    WHERE status != 'rejected';

-- 物品级别唯一约束：同一物品最多一个 approved claim
-- 防止并发竞争下同一物品被多人同时 approve
CREATE UNIQUE INDEX IF NOT EXISTS uq_lost_found_claims_item_approved
    ON public.lost_found_claims(item_id)
    WHERE status = 'approved';


-- ═══════════════════════════════════════════════════════════
-- 23. claim → lost_found_items 状态联动 trigger
--     当 lost_found_claims.status 变为 'approved' 时，
--     自动将对应 lost_found_items.status 设为 'claimed'，
--     并写入 returned_at。
--     当 claim 被拒绝/取消时，如无其他活跃 claim，恢复 item 为 'active'。
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.sync_lost_found_item_on_claim()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.status = 'approved' AND OLD.status != 'approved' THEN
        -- claim 通过：标记物品为已认领
        UPDATE public.lost_found_items
        SET status      = 'claimed',
            returned_at = now()
        WHERE id = NEW.item_id;
    ELSIF TG_OP = 'UPDATE' AND OLD.status = 'approved' AND NEW.status != 'approved' THEN
        -- 已批准的 claim 被撤回/拒绝：检查是否还有其他 approved claim
        IF NOT EXISTS (
            SELECT 1 FROM public.lost_found_claims
            WHERE item_id = NEW.item_id
              AND id != NEW.id
              AND status = 'approved'
        ) THEN
            UPDATE public.lost_found_items
            SET status      = 'active',
                returned_at = NULL
            WHERE id = NEW.item_id;
        END IF;
    ELSIF TG_OP = 'DELETE' AND OLD.status = 'approved' THEN
        -- claim 被删除：检查是否还有其他 approved claim
        IF NOT EXISTS (
            SELECT 1 FROM public.lost_found_claims
            WHERE item_id = OLD.item_id
              AND id != OLD.id
              AND status = 'approved'
        ) THEN
            UPDATE public.lost_found_items
            SET status      = 'active',
                returned_at = NULL
            WHERE id = OLD.item_id;
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

DROP TRIGGER IF EXISTS trg_lost_found_claims_sync ON public.lost_found_claims;
CREATE TRIGGER trg_lost_found_claims_sync
    AFTER INSERT OR UPDATE OF status OR DELETE ON public.lost_found_claims
    FOR EACH ROW
    EXECUTE FUNCTION public.sync_lost_found_item_on_claim();


-- ═══════════════════════════════════════════════════════════
-- 24. community_comments INSERT → notifications trigger
--    当有新评论时，通知帖子作者（自己评论自己不通知）
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.notify_post_author_on_comment()
RETURNS trigger AS $$
DECLARE
    v_author_id uuid;
    v_post_title text;
BEGIN
    -- service_role bypass (Edge Function writes directly)
    IF auth.role() = 'service_role' THEN
        RETURN NEW;
    END IF;

    -- Only notify for published comments (pending_review comments are invisible to author)
    IF NEW.status != 'published' THEN
        RETURN NEW;
    END IF;

    -- Fetch post author and title
    SELECT author_id, title INTO v_author_id, v_post_title
    FROM public.community_posts
    WHERE id = NEW.post_id;

    -- Don't notify if commenting on own post
    IF v_author_id IS NOT NULL AND v_author_id != NEW.author_id THEN
        INSERT INTO public.notifications (
            user_id, type, title, body, ref_type, ref_id
        ) VALUES (
            v_author_id,
            'community',
            '新评论',
            '有人评论了你的帖子"' || left(v_post_title, 50) || '"',
            'community_post',
            NEW.post_id
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

DROP TRIGGER IF EXISTS trg_notify_comment ON public.community_comments;
CREATE TRIGGER trg_notify_comment
    AFTER INSERT ON public.community_comments
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_post_author_on_comment();


-- ═══════════════════════════════════════════════════════════
-- 25. lost_found_claims INSERT → notifications trigger
--    当有人认领物品时，通知物品发布者
-- ═══════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.notify_publisher_on_claim()
RETURNS trigger AS $$
DECLARE
    v_publisher_id uuid;
    v_item_title text;
BEGIN
    -- Fetch item publisher and title
    SELECT publisher_id, title INTO v_publisher_id, v_item_title
    FROM public.lost_found_items
    WHERE id = NEW.item_id;

    -- Notify the publisher (don't notify if claiming own item)
    IF v_publisher_id IS NOT NULL AND v_publisher_id != NEW.claimant_id THEN
        INSERT INTO public.notifications (
            user_id, type, title, body, ref_type, ref_id
        ) VALUES (
            v_publisher_id,
            'lost_found',
            '新的认领',
            '有人对你的物品"' || left(v_item_title, 50) || '"发起了认领',
            'lost_found_item',
            NEW.item_id
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

DROP TRIGGER IF EXISTS trg_notify_claim ON public.lost_found_claims;
CREATE TRIGGER trg_notify_claim
    AFTER INSERT ON public.lost_found_claims
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_publisher_on_claim();
