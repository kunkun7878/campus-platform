-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000010_notifications
-- 回滚: 删除 notifications 表及其 RLS、索引
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS notifications_select_policy ON public.notifications;
DROP POLICY IF EXISTS notifications_insert_policy ON public.notifications;
DROP POLICY IF EXISTS notifications_update_policy ON public.notifications;
DROP POLICY IF EXISTS notifications_delete_policy ON public.notifications;
DROP POLICY IF EXISTS notifications_agent_select_policy ON public.notifications;
DROP POLICY IF EXISTS notifications_agent_delete_policy ON public.notifications;
ALTER TABLE IF EXISTS public.notifications DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.notifications CASCADE;
