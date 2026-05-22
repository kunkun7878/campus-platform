-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000012_messaging_social
-- 描述: 创建社交 + 消息模块 5 张表 —
--       post_likes / conversations / messages
--       / group_messages / group_members
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
--
-- 说明: group_messages 无 school_id，通过 JOIN
--       official_groups.school_id 实现学校隔离。
--       group_members 按 user_id 隔离。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. post_likes — 帖子点赞
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.post_likes (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         uuid NOT NULL REFERENCES public.community_posts(id) ON DELETE CASCADE,
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at      timestamptz DEFAULT now()
);

-- 每个用户对每个帖子只能点赞一次（通过 INSERT/DELETE 实现点赞/取消）
CREATE UNIQUE INDEX IF NOT EXISTS uq_post_likes_post_user
    ON public.post_likes(post_id, user_id);

-- indexes
CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON public.post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_likes_user_id ON public.post_likes(user_id);

-- ═══════════════════════════════════════════════════════════
-- 2. conversations — 私信会话
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.conversations (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    user2_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    last_message    text,
    last_message_at timestamptz,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now(),
    UNIQUE(user1_id, user2_id)
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_conversations_updated_at ON public.conversations;
CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON public.conversations
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_conversations_user1_id ON public.conversations(user1_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user2_id ON public.conversations(user2_id);
CREATE INDEX IF NOT EXISTS idx_conversations_last_message_at ON public.conversations(last_message_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 3. messages — 私信消息
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.messages (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id uuid NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content         text NOT NULL,
    is_read         boolean DEFAULT false,
    created_at      timestamptz DEFAULT now()
);

-- indexes
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON public.messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON public.messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON public.messages(conversation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_unread ON public.messages(conversation_id, is_read)
    WHERE is_read = false;

-- ═══════════════════════════════════════════════════════════
-- 4. group_messages — 群聊消息
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.group_messages (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        uuid NOT NULL REFERENCES public.official_groups(id) ON DELETE CASCADE,
    sender_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content         text NOT NULL,
    type            text NOT NULL DEFAULT 'text'
                    CHECK (type IN ('text', 'image', 'system')),
    -- text=文字, image=图片, system=系统消息（如入群/退群通知）
    created_at      timestamptz DEFAULT now()
);

-- indexes
CREATE INDEX IF NOT EXISTS idx_group_messages_group_id ON public.group_messages(group_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_sender_id ON public.group_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_created_at ON public.group_messages(group_id, created_at ASC);

-- ═══════════════════════════════════════════════════════════
-- 5. group_members — 群成员
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.group_members (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        uuid NOT NULL REFERENCES public.official_groups(id) ON DELETE CASCADE,
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role            text NOT NULL DEFAULT 'member'
                    CHECK (role IN ('member', 'admin', 'owner')),
    -- member=成员, admin=管理员, owner=群主
    joined_at       timestamptz DEFAULT now(),
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now(),
    UNIQUE(group_id, user_id)
);

-- trigger: updated_at（角色变更时更新）
DROP TRIGGER IF EXISTS trg_group_members_updated_at ON public.group_members;
CREATE TRIGGER trg_group_members_updated_at
    BEFORE UPDATE ON public.group_members
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON public.group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user_id ON public.group_members(user_id);

-- trigger 函数：阻止非 Agent/非 service_role 用户修改 group_members.role 字段
CREATE OR REPLACE FUNCTION public.check_group_members_role_change()
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

    -- 非 Agent 用户：不允许修改 role
    IF NEW.role IS DISTINCT FROM OLD.role THEN
        RAISE EXCEPTION 'permission denied: only agents can modify group role';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 应用 trigger
DROP TRIGGER IF EXISTS trg_group_members_check_role ON public.group_members;
CREATE TRIGGER trg_group_members_check_role
    BEFORE UPDATE ON public.group_members
    FOR EACH ROW
    EXECUTE FUNCTION public.check_group_members_role_change();

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── post_likes ─────────────────────────────────────────────

ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校用户可看（通过 JOIN community_posts 隔离）
DROP POLICY IF EXISTS post_likes_select_policy ON public.post_likes;
CREATE POLICY post_likes_select_policy ON public.post_likes
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.community_posts
            WHERE community_posts.id = post_likes.post_id
            AND community_posts.school_id = public.get_user_school_id()
        )
    );

DROP POLICY IF EXISTS post_likes_agent_select_policy ON public.post_likes;
CREATE POLICY post_likes_agent_select_policy ON public.post_likes
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 只能点赞同校且已发布（非删除）的帖子
DROP POLICY IF EXISTS post_likes_insert_policy ON public.post_likes;
CREATE POLICY post_likes_insert_policy ON public.post_likes
    FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM public.community_posts
            WHERE community_posts.id = post_likes.post_id
            AND community_posts.school_id = public.get_user_school_id()
            AND community_posts.status = 'published'
        )
    );

-- DELETE: 只能取消自己的点赞
DROP POLICY IF EXISTS post_likes_delete_policy ON public.post_likes;
CREATE POLICY post_likes_delete_policy ON public.post_likes
    FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS post_likes_agent_delete_policy ON public.post_likes;
CREATE POLICY post_likes_agent_delete_policy ON public.post_likes
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── conversations ──────────────────────────────────────────

ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;

-- SELECT: 只能看自己参与的会话
DROP POLICY IF EXISTS conversations_select_policy ON public.conversations;
CREATE POLICY conversations_select_policy ON public.conversations
    FOR SELECT
    TO authenticated
    USING (
        user1_id = auth.uid() OR user2_id = auth.uid()
    );

DROP POLICY IF EXISTS conversations_agent_select_policy ON public.conversations;
CREATE POLICY conversations_agent_select_policy ON public.conversations
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 会话参与者之一是自己；不允许自聊；两方必须同校
DROP POLICY IF EXISTS conversations_insert_policy ON public.conversations;
CREATE POLICY conversations_insert_policy ON public.conversations
    FOR INSERT
    TO authenticated
    WITH CHECK (
        (user1_id = auth.uid() OR user2_id = auth.uid())
        AND user1_id != user2_id
        AND EXISTS (
            SELECT 1 FROM public.profiles p1
            JOIN public.profiles p2 ON p1.school_id = p2.school_id
            WHERE p1.id = user1_id AND p2.id = user2_id
        )
    );

-- UPDATE: 只能更新自己参与的会话
DROP POLICY IF EXISTS conversations_update_policy ON public.conversations;
CREATE POLICY conversations_update_policy ON public.conversations
    FOR UPDATE
    TO authenticated
    USING (
        user1_id = auth.uid() OR user2_id = auth.uid()
    )
    WITH CHECK (
        user1_id = auth.uid() OR user2_id = auth.uid()
    );

-- DELETE: 仅 Agent 可硬删除
DROP POLICY IF EXISTS conversations_delete_policy ON public.conversations;
CREATE POLICY conversations_delete_policy ON public.conversations
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── messages ───────────────────────────────────────────────

ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

-- SELECT: 只能看自己参与会话的消息
DROP POLICY IF EXISTS messages_select_policy ON public.messages;
CREATE POLICY messages_select_policy ON public.messages
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversations
            WHERE conversations.id = messages.conversation_id
            AND (conversations.user1_id = auth.uid()
                 OR conversations.user2_id = auth.uid())
        )
    );

DROP POLICY IF EXISTS messages_agent_select_policy ON public.messages;
CREATE POLICY messages_agent_select_policy ON public.messages
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 只能在自己参与的会话中发消息
DROP POLICY IF EXISTS messages_insert_policy ON public.messages;
CREATE POLICY messages_insert_policy ON public.messages
    FOR INSERT
    TO authenticated
    WITH CHECK (
        sender_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM public.conversations
            WHERE conversations.id = messages.conversation_id
            AND (conversations.user1_id = auth.uid()
                 OR conversations.user2_id = auth.uid())
        )
    );

-- DELETE: 发送者可删除自己的消息；Agent 可删除所有
DROP POLICY IF EXISTS messages_delete_policy ON public.messages;
CREATE POLICY messages_delete_policy ON public.messages
    FOR DELETE
    TO authenticated
    USING (
        sender_id = auth.uid()
        OR public.is_agent()
    );

-- ── group_messages ─────────────────────────────────────────
-- 学校隔离通过 JOIN official_groups.school_id 实现

ALTER TABLE public.group_messages ENABLE ROW LEVEL SECURITY;

-- SELECT: 同群 + 同校用户可见
DROP POLICY IF EXISTS group_messages_select_policy ON public.group_messages;
CREATE POLICY group_messages_select_policy ON public.group_messages
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.official_groups
            WHERE official_groups.id = group_messages.group_id
            AND official_groups.school_id = public.get_user_school_id()
        )
    );

DROP POLICY IF EXISTS group_messages_agent_select_policy ON public.group_messages;
CREATE POLICY group_messages_agent_select_policy ON public.group_messages
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 同群 + 同校 + 群成员可发送消息
DROP POLICY IF EXISTS group_messages_insert_policy ON public.group_messages;
CREATE POLICY group_messages_insert_policy ON public.group_messages
    FOR INSERT
    TO authenticated
    WITH CHECK (
        sender_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM public.official_groups
            WHERE official_groups.id = group_messages.group_id
            AND official_groups.school_id = public.get_user_school_id()
        )
        AND EXISTS (
            SELECT 1 FROM public.group_members
            WHERE group_members.group_id = group_messages.group_id
            AND group_members.user_id = auth.uid()
        )
    );

-- DELETE: 发送者可删除自己的消息；Agent 可删除所有
DROP POLICY IF EXISTS group_messages_delete_policy ON public.group_messages;
CREATE POLICY group_messages_delete_policy ON public.group_messages
    FOR DELETE
    TO authenticated
    USING (
        sender_id = auth.uid()
        OR public.is_agent()
    );

-- ── group_members ──────────────────────────────────────────
-- 按 user_id 隔离：用户只能看到自己所在的群成员身份

ALTER TABLE public.group_members ENABLE ROW LEVEL SECURITY;

-- SELECT: 同群成员可见（通过 JOIN official_groups 学校隔离）
DROP POLICY IF EXISTS group_members_select_policy ON public.group_members;
CREATE POLICY group_members_select_policy ON public.group_members
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.official_groups
            WHERE official_groups.id = group_members.group_id
            AND official_groups.school_id = public.get_user_school_id()
        )
    );

DROP POLICY IF EXISTS group_members_agent_select_policy ON public.group_members;
CREATE POLICY group_members_agent_select_policy ON public.group_members
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 用户自己加入群（需同校）
DROP POLICY IF EXISTS group_members_insert_policy ON public.group_members;
CREATE POLICY group_members_insert_policy ON public.group_members
    FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM public.official_groups
            WHERE official_groups.id = group_members.group_id
            AND official_groups.school_id = public.get_user_school_id()
        )
    );

-- UPDATE: 用户可退出群（修改自己的记录）；Agent/群主可修改角色
DROP POLICY IF EXISTS group_members_update_policy ON public.group_members;
CREATE POLICY group_members_update_policy ON public.group_members
    FOR UPDATE
    TO authenticated
    USING (
        user_id = auth.uid()
        OR public.is_agent()
    )
    WITH CHECK (
        user_id = auth.uid()
        OR public.is_agent()
    );

-- DELETE: 用户可退群；Agent 可移除任意成员
DROP POLICY IF EXISTS group_members_delete_policy ON public.group_members;
CREATE POLICY group_members_delete_policy ON public.group_members
    FOR DELETE
    TO authenticated
    USING (
        user_id = auth.uid()
        OR public.is_agent()
    );
