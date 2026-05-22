-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000004_wechat_identities
-- 回滚: 删除 wechat_identities 表及其 RLS
-- ═══════════════════════════════════════════════════════════

-- 1. 删除策略并禁用 RLS
DROP POLICY IF EXISTS wechat_identities_select_policy ON public.wechat_identities;
ALTER TABLE IF EXISTS public.wechat_identities DISABLE ROW LEVEL SECURITY;

-- 2. 删除表
DROP TABLE IF EXISTS public.wechat_identities CASCADE;
