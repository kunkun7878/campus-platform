-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000013_wallet_system
-- 回滚: 删除 user_coupons / coupons / announcements
--       / wallet_transactions / wallets
--       表及其 RLS、trigger、索引
-- ═══════════════════════════════════════════════════════════

-- 5. user_coupons (depends on coupons, users)
DROP POLICY IF EXISTS user_coupons_select_policy ON public.user_coupons;
DROP POLICY IF EXISTS user_coupons_agent_select_policy ON public.user_coupons;
DROP POLICY IF EXISTS user_coupons_insert_policy ON public.user_coupons;
DROP POLICY IF EXISTS user_coupons_update_policy ON public.user_coupons;
DROP POLICY IF EXISTS user_coupons_delete_policy ON public.user_coupons;
ALTER TABLE IF EXISTS public.user_coupons DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_user_coupons_updated_at ON public.user_coupons;
DROP TABLE IF EXISTS public.user_coupons CASCADE;

-- 4. coupons
DROP POLICY IF EXISTS coupons_select_policy ON public.coupons;
DROP POLICY IF EXISTS coupons_agent_select_policy ON public.coupons;
DROP POLICY IF EXISTS coupons_insert_policy ON public.coupons;
DROP POLICY IF EXISTS coupons_update_policy ON public.coupons;
DROP POLICY IF EXISTS coupons_delete_policy ON public.coupons;
ALTER TABLE IF EXISTS public.coupons DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_coupons_updated_at ON public.coupons;
DROP TABLE IF EXISTS public.coupons CASCADE;

-- 3. announcements
DROP POLICY IF EXISTS announcements_select_policy ON public.announcements;
DROP POLICY IF EXISTS announcements_agent_select_policy ON public.announcements;
DROP POLICY IF EXISTS announcements_insert_policy ON public.announcements;
DROP POLICY IF EXISTS announcements_update_policy ON public.announcements;
DROP POLICY IF EXISTS announcements_delete_policy ON public.announcements;
ALTER TABLE IF EXISTS public.announcements DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_announcements_updated_at ON public.announcements;
DROP TABLE IF EXISTS public.announcements CASCADE;

-- 2. wallet_transactions (depends on wallets)
DROP POLICY IF EXISTS wallet_transactions_select_policy ON public.wallet_transactions;
DROP POLICY IF EXISTS wallet_transactions_agent_select_policy ON public.wallet_transactions;
ALTER TABLE IF EXISTS public.wallet_transactions DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.wallet_transactions CASCADE;

-- 1. wallets
DROP POLICY IF EXISTS wallets_select_policy ON public.wallets;
DROP POLICY IF EXISTS wallets_agent_select_policy ON public.wallets;
DROP POLICY IF EXISTS wallets_insert_policy ON public.wallets;
DROP POLICY IF EXISTS wallets_update_policy ON public.wallets;
DROP POLICY IF EXISTS wallets_delete_policy ON public.wallets;
ALTER TABLE IF EXISTS public.wallets DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_wallets_updated_at ON public.wallets;
DROP TABLE IF EXISTS public.wallets CASCADE;
