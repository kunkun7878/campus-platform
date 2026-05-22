-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000006_runner_module
-- 回滚: 删除 runner_tasks / runner_orders / runner_reviews
--       / user_addresses 表及其 RLS、trigger、索引
-- ═══════════════════════════════════════════════════════════

-- 1. runner_tasks — 删除策略、禁用 RLS、删除 trigger、删除表
DROP POLICY IF EXISTS runner_tasks_select_policy ON public.runner_tasks;
DROP POLICY IF EXISTS runner_tasks_agent_select_policy ON public.runner_tasks;
DROP POLICY IF EXISTS runner_tasks_insert_policy ON public.runner_tasks;
DROP POLICY IF EXISTS runner_tasks_update_policy ON public.runner_tasks;
DROP POLICY IF EXISTS runner_tasks_agent_update_policy ON public.runner_tasks;
DROP POLICY IF EXISTS runner_tasks_delete_policy ON public.runner_tasks;
ALTER TABLE IF EXISTS public.runner_tasks DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_runner_tasks_updated_at ON public.runner_tasks;
DROP TABLE IF EXISTS public.runner_tasks CASCADE;

-- 2. runner_orders
DROP POLICY IF EXISTS runner_orders_select_policy ON public.runner_orders;
DROP POLICY IF EXISTS runner_orders_agent_select_policy ON public.runner_orders;
DROP POLICY IF EXISTS runner_orders_insert_policy ON public.runner_orders;
DROP POLICY IF EXISTS runner_orders_update_policy ON public.runner_orders;
DROP POLICY IF EXISTS runner_orders_agent_update_policy ON public.runner_orders;
DROP POLICY IF EXISTS runner_orders_delete_policy ON public.runner_orders;
ALTER TABLE IF EXISTS public.runner_orders DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_runner_orders_updated_at ON public.runner_orders;
DROP TABLE IF EXISTS public.runner_orders CASCADE;

-- 3. runner_reviews
DROP POLICY IF EXISTS runner_reviews_select_policy ON public.runner_reviews;
DROP POLICY IF EXISTS runner_reviews_agent_select_policy ON public.runner_reviews;
DROP POLICY IF EXISTS runner_reviews_insert_policy ON public.runner_reviews;
DROP POLICY IF EXISTS runner_reviews_update_policy ON public.runner_reviews;
DROP POLICY IF EXISTS runner_reviews_agent_update_policy ON public.runner_reviews;
DROP POLICY IF EXISTS runner_reviews_delete_policy ON public.runner_reviews;
DROP POLICY IF EXISTS runner_reviews_agent_delete_policy ON public.runner_reviews;
ALTER TABLE IF EXISTS public.runner_reviews DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_runner_reviews_updated_at ON public.runner_reviews;
DROP TABLE IF EXISTS public.runner_reviews CASCADE;

-- 4. user_addresses
DROP POLICY IF EXISTS user_addresses_select_policy ON public.user_addresses;
DROP POLICY IF EXISTS user_addresses_insert_policy ON public.user_addresses;
DROP POLICY IF EXISTS user_addresses_update_policy ON public.user_addresses;
DROP POLICY IF EXISTS user_addresses_delete_policy ON public.user_addresses;
ALTER TABLE IF EXISTS public.user_addresses DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_user_addresses_updated_at ON public.user_addresses;
DROP TABLE IF EXISTS public.user_addresses CASCADE;
