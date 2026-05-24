-- ═══════════════════════════════════════════════════════════
-- Migration: 20260523000016_harden_market_orders_rls
-- 描述: 加强 market_orders INSERT RLS 策略，增加业务校验 —
--       1. listing_id 对应的商品必须存在且状态为 active
--       2. seller_id 必须与 listing 的实际卖家一致
--       3. 买家不能是卖家自己
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- 原策略仅校验 buyer_id = auth.uid() 和 school_id，
-- 未校验 listing 有效性、seller 一致性、买家≠卖家，
-- 虽然正常流程走 Edge Function，但 RLS 是最后防线。

DROP POLICY IF EXISTS market_orders_insert_policy ON public.market_orders;
CREATE POLICY market_orders_insert_policy ON public.market_orders
    FOR INSERT
    TO authenticated
    WITH CHECK (
        buyer_id = auth.uid()
        AND school_id = public.get_user_school_id()
        AND EXISTS (
            SELECT 1 FROM public.market_listings
            WHERE market_listings.id = listing_id
            AND market_listings.status = 'active'
        )
        AND seller_id = (
            SELECT market_listings.seller_id FROM public.market_listings
            WHERE market_listings.id = listing_id
        )
        AND buyer_id != seller_id
    );
