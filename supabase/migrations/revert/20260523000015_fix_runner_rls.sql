-- ═══════════════════════════════════════════════════════════
-- Revert: 20260523000015_fix_runner_rls
-- 回滚: 还原跑腿模块 4 处 RLS 策略至修复前状态
-- ═══════════════════════════════════════════════════════════

-- 还原 4: after_sale_timeline SELECT（移除订单参与者限制）
DROP POLICY IF EXISTS after_sale_timeline_select_policy ON public.after_sale_timeline;
CREATE POLICY after_sale_timeline_select_policy ON public.after_sale_timeline
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

-- 还原 3: order_timeline SELECT（移除订单参与者限制）
DROP POLICY IF EXISTS order_timeline_select_policy ON public.order_timeline;
CREATE POLICY order_timeline_select_policy ON public.order_timeline
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

-- 还原 2: after_sales INSERT（移除订单参与者校验）
DROP POLICY IF EXISTS after_sales_insert_policy ON public.after_sales;
CREATE POLICY after_sales_insert_policy ON public.after_sales
    FOR INSERT
    TO authenticated
    WITH CHECK (
        requester_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

-- 还原 1: runner_reviews INSERT（移除订单参与者校验）
DROP POLICY IF EXISTS runner_reviews_insert_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_insert_policy ON public.runner_reviews
    FOR INSERT
    TO authenticated
    WITH CHECK (
        reviewer_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );
