-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000009_community_module
-- 回滚: 删除 community_posts / community_comments
--       / official_groups 表及其 RLS、trigger、索引
-- ═══════════════════════════════════════════════════════════

-- 1. community_posts
DROP POLICY IF EXISTS community_posts_select_policy ON public.community_posts;
DROP POLICY IF EXISTS community_posts_agent_select_policy ON public.community_posts;
DROP POLICY IF EXISTS community_posts_insert_policy ON public.community_posts;
DROP POLICY IF EXISTS community_posts_update_policy ON public.community_posts;
DROP POLICY IF EXISTS community_posts_agent_update_policy ON public.community_posts;
DROP POLICY IF EXISTS community_posts_delete_policy ON public.community_posts;
ALTER TABLE IF EXISTS public.community_posts DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_community_posts_updated_at ON public.community_posts;
DROP TABLE IF EXISTS public.community_posts CASCADE;

-- 2. community_comments
DROP POLICY IF EXISTS community_comments_select_policy ON public.community_comments;
DROP POLICY IF EXISTS community_comments_agent_select_policy ON public.community_comments;
DROP POLICY IF EXISTS community_comments_insert_policy ON public.community_comments;
DROP POLICY IF EXISTS community_comments_update_policy ON public.community_comments;
DROP POLICY IF EXISTS community_comments_agent_update_policy ON public.community_comments;
DROP POLICY IF EXISTS community_comments_delete_policy ON public.community_comments;
ALTER TABLE IF EXISTS public.community_comments DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_community_comments_updated_at ON public.community_comments;
DROP TABLE IF EXISTS public.community_comments CASCADE;

-- 3. official_groups
DROP POLICY IF EXISTS official_groups_select_policy ON public.official_groups;
DROP POLICY IF EXISTS official_groups_agent_select_policy ON public.official_groups;
DROP POLICY IF EXISTS official_groups_insert_policy ON public.official_groups;
DROP POLICY IF EXISTS official_groups_update_policy ON public.official_groups;
DROP POLICY IF EXISTS official_groups_delete_policy ON public.official_groups;
ALTER TABLE IF EXISTS public.official_groups DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_official_groups_updated_at ON public.official_groups;
DROP TABLE IF EXISTS public.official_groups CASCADE;
