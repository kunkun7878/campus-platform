-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000009_community_module
-- 描述: 创建社区模块 3 张表 — community_posts / community_comments
--       / official_groups
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. community_posts — 社区帖子（校园墙 + 讨论区）
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.community_posts (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    section         text NOT NULL DEFAULT 'campus_wall' CHECK (section IN ('campus_wall', 'discussion')),
    -- campus_wall=校园墙, discussion=讨论区
    title           text NOT NULL,
    content         text NOT NULL,
    images          jsonb DEFAULT '[]'::jsonb,
    like_count      integer DEFAULT 0 CHECK (like_count >= 0),
    comment_count   integer DEFAULT 0 CHECK (comment_count >= 0),
    is_pinned       boolean DEFAULT false,
    status          text NOT NULL DEFAULT 'published'
                    CHECK (status IN ('published', 'hidden', 'deleted')),
    -- published=已发布, hidden=已隐藏, deleted=已删除
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    view_count      integer DEFAULT 0 CHECK (view_count >= 0),
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_community_posts_updated_at ON public.community_posts;
CREATE TRIGGER trg_community_posts_updated_at
    BEFORE UPDATE ON public.community_posts
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_community_posts_school_id ON public.community_posts(school_id);
CREATE INDEX IF NOT EXISTS idx_community_posts_author_id ON public.community_posts(author_id);
CREATE INDEX IF NOT EXISTS idx_community_posts_section ON public.community_posts(section);
CREATE INDEX IF NOT EXISTS idx_community_posts_status ON public.community_posts(status);
CREATE INDEX IF NOT EXISTS idx_community_posts_school_section ON public.community_posts(school_id, section);
CREATE INDEX IF NOT EXISTS idx_community_posts_pinned ON public.community_posts(school_id, is_pinned DESC, created_at DESC)
    WHERE is_pinned = true;
CREATE INDEX IF NOT EXISTS idx_community_posts_created_at ON public.community_posts(created_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 2. community_comments — 帖子评论
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.community_comments (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         uuid NOT NULL REFERENCES public.community_posts(id) ON DELETE CASCADE,
    author_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    parent_id       uuid REFERENCES public.community_comments(id) ON DELETE SET NULL,
    -- NULL=顶级评论, 非NULL=回复某评论
    content         text NOT NULL,
    like_count      integer DEFAULT 0 CHECK (like_count >= 0),
    status          text NOT NULL DEFAULT 'published'
                    CHECK (status IN ('published', 'hidden', 'deleted')),
    -- published=已发布, hidden=已隐藏, deleted=已删除
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_community_comments_updated_at ON public.community_comments;
CREATE TRIGGER trg_community_comments_updated_at
    BEFORE UPDATE ON public.community_comments
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_community_comments_school_id ON public.community_comments(school_id);
CREATE INDEX IF NOT EXISTS idx_community_comments_post_id ON public.community_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_community_comments_author_id ON public.community_comments(author_id);
CREATE INDEX IF NOT EXISTS idx_community_comments_parent_id ON public.community_comments(parent_id);
CREATE INDEX IF NOT EXISTS idx_community_comments_created_at ON public.community_comments(post_id, created_at ASC);

-- ═══════════════════════════════════════════════════════════
-- 3. official_groups — 官方群
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.official_groups (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name            text NOT NULL,
    description     text,
    direction       text NOT NULL CHECK (direction IN ('chat', 'dating', 'part_time')),
    -- chat=聊天, dating=交友, part_time=兼职
    avatar_url      text,
    member_count    integer DEFAULT 0 CHECK (member_count >= 0),
    is_pinned       boolean DEFAULT true,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_by      uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_official_groups_updated_at ON public.official_groups;
CREATE TRIGGER trg_official_groups_updated_at
    BEFORE UPDATE ON public.official_groups
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_official_groups_school_id ON public.official_groups(school_id);
CREATE INDEX IF NOT EXISTS idx_official_groups_direction ON public.official_groups(direction);
CREATE INDEX IF NOT EXISTS idx_official_groups_pinned ON public.official_groups(school_id, is_pinned DESC);

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── community_posts ───────────────────────────────────────

ALTER TABLE public.community_posts ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校用户可见非删除的帖子；Agent 可见所有
DROP POLICY IF EXISTS community_posts_select_policy ON public.community_posts;
CREATE POLICY community_posts_select_policy ON public.community_posts
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
        AND status != 'deleted'
    );

DROP POLICY IF EXISTS community_posts_agent_select_policy ON public.community_posts;
CREATE POLICY community_posts_agent_select_policy ON public.community_posts
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

DROP POLICY IF EXISTS community_posts_insert_policy ON public.community_posts;
CREATE POLICY community_posts_insert_policy ON public.community_posts
    FOR INSERT
    TO authenticated
    WITH CHECK (
        author_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

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

DROP POLICY IF EXISTS community_posts_agent_update_policy ON public.community_posts;
CREATE POLICY community_posts_agent_update_policy ON public.community_posts
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 作者可删除自己的帖子（软删除由客户端通过 UPDATE status 实现，
--        此策略仅允许 Agent 硬删除）
DROP POLICY IF EXISTS community_posts_delete_policy ON public.community_posts;
CREATE POLICY community_posts_delete_policy ON public.community_posts
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── community_comments ────────────────────────────────────
-- 通过 school_id 直接实现学校隔离

ALTER TABLE public.community_comments ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校 + 非删除帖子下的非删除评论；Agent 可见所有
DROP POLICY IF EXISTS community_comments_select_policy ON public.community_comments;
CREATE POLICY community_comments_select_policy ON public.community_comments
    FOR SELECT
    TO authenticated
    USING (
        school_id = public.get_user_school_id()
        AND status != 'deleted'
    );

DROP POLICY IF EXISTS community_comments_agent_select_policy ON public.community_comments;
CREATE POLICY community_comments_agent_select_policy ON public.community_comments
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 只能评论同校的帖子
DROP POLICY IF EXISTS community_comments_insert_policy ON public.community_comments;
CREATE POLICY community_comments_insert_policy ON public.community_comments
    FOR INSERT
    TO authenticated
    WITH CHECK (
        author_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

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

DROP POLICY IF EXISTS community_comments_agent_update_policy ON public.community_comments;
CREATE POLICY community_comments_agent_update_policy ON public.community_comments
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS community_comments_delete_policy ON public.community_comments;
CREATE POLICY community_comments_delete_policy ON public.community_comments
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── official_groups ───────────────────────────────────────
-- 仅 Agent 可管理；所有用户可读同校的群

ALTER TABLE public.official_groups ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS official_groups_select_policy ON public.official_groups;
CREATE POLICY official_groups_select_policy ON public.official_groups
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS official_groups_agent_select_policy ON public.official_groups;
CREATE POLICY official_groups_agent_select_policy ON public.official_groups
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

DROP POLICY IF EXISTS official_groups_insert_policy ON public.official_groups;
CREATE POLICY official_groups_insert_policy ON public.official_groups
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS official_groups_update_policy ON public.official_groups;
CREATE POLICY official_groups_update_policy ON public.official_groups
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS official_groups_delete_policy ON public.official_groups;
CREATE POLICY official_groups_delete_policy ON public.official_groups
    FOR DELETE
    TO authenticated
    USING (public.is_agent());
