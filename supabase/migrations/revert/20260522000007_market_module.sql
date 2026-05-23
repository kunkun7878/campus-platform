-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000007_market_module
-- 回滚: 删除 market_listings / market_orders / user_favorites
--       表及其 RLS、trigger、索引
-- ═══════════════════════════════════════════════════════════

-- 1. market_listings
DROP POLICY IF EXISTS market_listings_select_policy ON public.market_listings;
DROP POLICY IF EXISTS market_listings_agent_select_policy ON public.market_listings;
DROP POLICY IF EXISTS market_listings_insert_policy ON public.market_listings;
DROP POLICY IF EXISTS market_listings_update_policy ON public.market_listings;
DROP POLICY IF EXISTS market_listings_agent_update_policy ON public.market_listings;
DROP POLICY IF EXISTS market_listings_delete_policy ON public.market_listings;
ALTER TABLE IF EXISTS public.market_listings DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_market_listings_updated_at ON public.market_listings;
DROP TABLE IF EXISTS public.market_listings CASCADE;

-- 2. market_orders
DROP POLICY IF EXISTS market_orders_select_policy ON public.market_orders;
DROP POLICY IF EXISTS market_orders_agent_select_policy ON public.market_orders;
DROP POLICY IF EXISTS market_orders_insert_policy ON public.market_orders;
DROP POLICY IF EXISTS market_orders_update_policy ON public.market_orders;
DROP POLICY IF EXISTS market_orders_agent_update_policy ON public.market_orders;
DROP POLICY IF EXISTS market_orders_delete_policy ON public.market_orders;
ALTER TABLE IF EXISTS public.market_orders DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_market_orders_updated_at ON public.market_orders;
DROP TABLE IF EXISTS public.market_orders CASCADE;

-- 3. user_favorites
DROP POLICY IF EXISTS user_favorites_select_policy ON public.user_favorites;
DROP POLICY IF EXISTS user_favorites_agent_select_policy ON public.user_favorites;
DROP POLICY IF EXISTS user_favorites_insert_policy ON public.user_favorites;
DROP POLICY IF EXISTS user_favorites_delete_policy ON public.user_favorites;
ALTER TABLE IF EXISTS public.user_favorites DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.user_favorites CASCADE;
