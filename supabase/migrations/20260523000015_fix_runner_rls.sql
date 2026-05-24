-- ═══════════════════════════════════════════════════════════
-- Migration: 20260523000015_fix_runner_rls
-- 描述: 修复跑腿模块 4 处 RLS 策略漏洞 —
--       1. runner_reviews INSERT 添加订单参与者校验
--       2. after_sales INSERT 添加订单参与者校验
--       3. order_timeline SELECT 限制为订单参与者
--       4. after_sale_timeline SELECT 限制为售后订单参与者
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 修复 1: runner_reviews INSERT — 增加订单参与者校验
-- 原策略仅校验 reviewer_id + school_id，同校任意用户可写评价
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS runner_reviews_insert_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_insert_policy ON public.runner_reviews
    FOR INSERT
    TO authenticated
    WITH CHECK (
        reviewer_id = auth.uid()
        AND school_id = public.get_user_school_id()
        AND EXISTS (
            SELECT 1 FROM public.runner_orders
            WHERE runner_orders.id = order_id
            AND (runner_orders.buyer_id = auth.uid() OR runner_orders.runner_id = auth.uid())
        )
    );

-- ═══════════════════════════════════════════════════════════
-- 修复 2: after_sales INSERT — 增加订单参与者校验
-- 原策略仅校验 requester_id + school_id
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS after_sales_insert_policy ON public.after_sales;
CREATE POLICY after_sales_insert_policy ON public.after_sales
    FOR INSERT
    TO authenticated
    WITH CHECK (
        requester_id = auth.uid()
        AND school_id = public.get_user_school_id()
        AND EXISTS (
            SELECT 1 FROM public.runner_orders
            WHERE runner_orders.id = order_id
            AND (runner_orders.buyer_id = auth.uid() OR runner_orders.runner_id = auth.uid())
        )
    );

-- ═══════════════════════════════════════════════════════════
-- 修复 3: order_timeline SELECT — 限制为订单参与者
-- 原策略仅校验 school_id，同校所有用户可见所有订单时间线
-- Agent 策略 (order_timeline_agent_select_policy) 保留不变
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS order_timeline_select_policy ON public.order_timeline;
CREATE POLICY order_timeline_select_policy ON public.order_timeline
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
        AND EXISTS (
            SELECT 1 FROM public.runner_orders
            WHERE runner_orders.id = order_timeline.order_id
            AND (runner_orders.buyer_id = auth.uid() OR runner_orders.runner_id = auth.uid())
        )
    );

-- ═══════════════════════════════════════════════════════════
-- 修复 4: after_sale_timeline SELECT — 限制为售后订单参与者
-- 原策略仅校验 school_id，同校所有用户可见所有售后时间线
-- Agent 策略 (after_sale_timeline_agent_select_policy) 保留不变
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS after_sale_timeline_select_policy ON public.after_sale_timeline;
CREATE POLICY after_sale_timeline_select_policy ON public.after_sale_timeline
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
        AND EXISTS (
            SELECT 1 FROM public.after_sales
            JOIN public.runner_orders ON runner_orders.id = after_sales.order_id
            WHERE after_sales.id = after_sale_timeline.after_sale_id
            AND (runner_orders.buyer_id = auth.uid() OR runner_orders.runner_id = auth.uid())
        )
    );
