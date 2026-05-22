-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000008_lost_found_module
-- 回滚: 删除 lost_found_items / lost_found_claims 表及其
--       RLS、trigger、索引
-- ═══════════════════════════════════════════════════════════

-- 1. lost_found_items
DROP POLICY IF EXISTS lost_found_items_select_policy ON public.lost_found_items;
DROP POLICY IF EXISTS lost_found_items_agent_select_policy ON public.lost_found_items;
DROP POLICY IF EXISTS lost_found_items_insert_policy ON public.lost_found_items;
DROP POLICY IF EXISTS lost_found_items_update_policy ON public.lost_found_items;
DROP POLICY IF EXISTS lost_found_items_agent_update_policy ON public.lost_found_items;
DROP POLICY IF EXISTS lost_found_items_delete_policy ON public.lost_found_items;
ALTER TABLE IF EXISTS public.lost_found_items DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_lost_found_items_updated_at ON public.lost_found_items;
DROP TABLE IF EXISTS public.lost_found_items CASCADE;

-- 2. lost_found_claims
DROP POLICY IF EXISTS lost_found_claims_select_policy ON public.lost_found_claims;
DROP POLICY IF EXISTS lost_found_claims_agent_select_policy ON public.lost_found_claims;
DROP POLICY IF EXISTS lost_found_claims_insert_policy ON public.lost_found_claims;
DROP POLICY IF EXISTS lost_found_claims_update_policy ON public.lost_found_claims;
DROP POLICY IF EXISTS lost_found_claims_agent_update_policy ON public.lost_found_claims;
DROP POLICY IF EXISTS lost_found_claims_delete_policy ON public.lost_found_claims;
ALTER TABLE IF EXISTS public.lost_found_claims DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_lost_found_claims_updated_at ON public.lost_found_claims;
DROP TABLE IF EXISTS public.lost_found_claims CASCADE;
