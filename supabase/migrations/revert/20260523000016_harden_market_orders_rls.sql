-- ═══════════════════════════════════════════════════════════
-- Revert: 20260523000016_harden_market_orders_rls
-- 描述: 还原 market_orders_insert_policy 至 Migration 07 原版
--       原版仅校验 buyer_id = auth.uid() 和 school_id，
--       移除 listing 有效性、seller 一致性、买家≠卖家三项加固校验。
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

DROP POLICY IF EXISTS market_orders_insert_policy ON public.market_orders;
CREATE POLICY market_orders_insert_policy ON public.market_orders
    FOR INSERT
    TO authenticated
    WITH CHECK (
        buyer_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );
