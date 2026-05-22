-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000012_messaging_social
-- 回滚: 删除 group_members / group_messages / messages
--       / conversations / post_likes
--       表及其 RLS、trigger、索引
-- ═══════════════════════════════════════════════════════════

-- 5. group_members
DROP POLICY IF EXISTS group_members_select_policy ON public.group_members;
DROP POLICY IF EXISTS group_members_agent_select_policy ON public.group_members;
DROP POLICY IF EXISTS group_members_insert_policy ON public.group_members;
DROP POLICY IF EXISTS group_members_update_policy ON public.group_members;
DROP POLICY IF EXISTS group_members_delete_policy ON public.group_members;
ALTER TABLE IF EXISTS public.group_members DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_group_members_updated_at ON public.group_members;
DROP TRIGGER IF EXISTS trg_group_members_check_role ON public.group_members;
DROP FUNCTION IF EXISTS public.check_group_members_role_change();
DROP TABLE IF EXISTS public.group_members CASCADE;

-- 4. group_messages
DROP POLICY IF EXISTS group_messages_select_policy ON public.group_messages;
DROP POLICY IF EXISTS group_messages_agent_select_policy ON public.group_messages;
DROP POLICY IF EXISTS group_messages_insert_policy ON public.group_messages;
DROP POLICY IF EXISTS group_messages_delete_policy ON public.group_messages;
ALTER TABLE IF EXISTS public.group_messages DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.group_messages CASCADE;

-- 3. messages
DROP POLICY IF EXISTS messages_select_policy ON public.messages;
DROP POLICY IF EXISTS messages_agent_select_policy ON public.messages;
DROP POLICY IF EXISTS messages_insert_policy ON public.messages;
DROP POLICY IF EXISTS messages_delete_policy ON public.messages;
ALTER TABLE IF EXISTS public.messages DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.messages CASCADE;

-- 2. conversations
DROP POLICY IF EXISTS conversations_select_policy ON public.conversations;
DROP POLICY IF EXISTS conversations_agent_select_policy ON public.conversations;
DROP POLICY IF EXISTS conversations_insert_policy ON public.conversations;
DROP POLICY IF EXISTS conversations_update_policy ON public.conversations;
DROP POLICY IF EXISTS conversations_delete_policy ON public.conversations;
ALTER TABLE IF EXISTS public.conversations DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_conversations_updated_at ON public.conversations;
DROP TABLE IF EXISTS public.conversations CASCADE;

-- 1. post_likes
DROP POLICY IF EXISTS post_likes_select_policy ON public.post_likes;
DROP POLICY IF EXISTS post_likes_agent_select_policy ON public.post_likes;
DROP POLICY IF EXISTS post_likes_insert_policy ON public.post_likes;
DROP POLICY IF EXISTS post_likes_delete_policy ON public.post_likes;
DROP POLICY IF EXISTS post_likes_agent_delete_policy ON public.post_likes;
ALTER TABLE IF EXISTS public.post_likes DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.post_likes CASCADE;
