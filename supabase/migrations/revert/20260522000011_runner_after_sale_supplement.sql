-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000011_runner_after_sale_supplement
-- 回滚: 删除 after_sale_timeline / after_sales
--       / order_timeline / runner_applications
--       表及其 RLS、trigger、索引
-- ═══════════════════════════════════════════════════════════

-- 4. after_sale_timeline（最先删除：依赖 after_sales）
DROP POLICY IF EXISTS after_sale_timeline_select_policy ON public.after_sale_timeline;
DROP POLICY IF EXISTS after_sale_timeline_agent_select_policy ON public.after_sale_timeline;
DROP POLICY IF EXISTS after_sale_timeline_insert_policy ON public.after_sale_timeline;
DROP POLICY IF EXISTS after_sale_timeline_update_policy ON public.after_sale_timeline;
DROP POLICY IF EXISTS after_sale_timeline_delete_policy ON public.after_sale_timeline;
ALTER TABLE IF EXISTS public.after_sale_timeline DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_after_sale_timeline_updated_at ON public.after_sale_timeline;
DROP TABLE IF EXISTS public.after_sale_timeline CASCADE;

-- 3. after_sales
DROP POLICY IF EXISTS after_sales_select_policy ON public.after_sales;
DROP POLICY IF EXISTS after_sales_agent_select_policy ON public.after_sales;
DROP POLICY IF EXISTS after_sales_insert_policy ON public.after_sales;
DROP POLICY IF EXISTS after_sales_update_policy ON public.after_sales;
DROP POLICY IF EXISTS after_sales_delete_policy ON public.after_sales;
ALTER TABLE IF EXISTS public.after_sales DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_after_sales_updated_at ON public.after_sales;
DROP TABLE IF EXISTS public.after_sales CASCADE;

-- 2. order_timeline
DROP POLICY IF EXISTS order_timeline_select_policy ON public.order_timeline;
DROP POLICY IF EXISTS order_timeline_agent_select_policy ON public.order_timeline;
DROP POLICY IF EXISTS order_timeline_insert_policy ON public.order_timeline;
DROP POLICY IF EXISTS order_timeline_update_policy ON public.order_timeline;
DROP POLICY IF EXISTS order_timeline_delete_policy ON public.order_timeline;
ALTER TABLE IF EXISTS public.order_timeline DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_order_timeline_updated_at ON public.order_timeline;
DROP TABLE IF EXISTS public.order_timeline CASCADE;

-- 1. runner_applications
DROP POLICY IF EXISTS runner_applications_select_policy ON public.runner_applications;
DROP POLICY IF EXISTS runner_applications_agent_select_policy ON public.runner_applications;
DROP POLICY IF EXISTS runner_applications_insert_policy ON public.runner_applications;
DROP POLICY IF EXISTS runner_applications_update_policy ON public.runner_applications;
DROP POLICY IF EXISTS runner_applications_delete_policy ON public.runner_applications;
ALTER TABLE IF EXISTS public.runner_applications DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_runner_applications_updated_at ON public.runner_applications;
DROP TABLE IF EXISTS public.runner_applications CASCADE;
