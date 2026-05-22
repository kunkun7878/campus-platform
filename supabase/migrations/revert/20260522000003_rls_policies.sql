-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000003_rls_policies
-- 回滚: 删除所有 RLS 策略 + Helper 函数 + 禁用 RLS
-- ═══════════════════════════════════════════════════════════

-- 1. 删除 profiles 表的所有策略并禁用 RLS
DROP POLICY IF EXISTS profiles_select_policy ON public.profiles;
DROP POLICY IF EXISTS profiles_update_policy ON public.profiles;
DROP POLICY IF EXISTS profiles_delete_policy ON public.profiles;
DROP POLICY IF EXISTS profiles_agent_policy ON public.profiles;
DROP POLICY IF EXISTS profiles_agent_update_policy ON public.profiles;
DROP POLICY IF EXISTS profiles_agent_delete_policy ON public.profiles;
ALTER TABLE IF EXISTS public.profiles DISABLE ROW LEVEL SECURITY;

-- 2. 删除 schools 表的所有策略并禁用 RLS
DROP POLICY IF EXISTS schools_select_policy ON public.schools;
DROP POLICY IF EXISTS schools_modify_policy ON public.schools;
DROP POLICY IF EXISTS schools_update_policy ON public.schools;
DROP POLICY IF EXISTS schools_delete_policy ON public.schools;
ALTER TABLE IF EXISTS public.schools DISABLE ROW LEVEL SECURITY;

-- 3. 删除 campuses 表的所有策略并禁用 RLS
DROP POLICY IF EXISTS campuses_select_policy ON public.campuses;
DROP POLICY IF EXISTS campuses_modify_policy ON public.campuses;
DROP POLICY IF EXISTS campuses_update_policy ON public.campuses;
DROP POLICY IF EXISTS campuses_delete_policy ON public.campuses;
ALTER TABLE IF EXISTS public.campuses DISABLE ROW LEVEL SECURITY;

-- 4. 删除 Helper 函数
DROP FUNCTION IF EXISTS public.get_user_school_id();
DROP FUNCTION IF EXISTS public.get_user_campus_id();
DROP FUNCTION IF EXISTS public.is_agent();
DROP FUNCTION IF EXISTS public.has_selected_school();
