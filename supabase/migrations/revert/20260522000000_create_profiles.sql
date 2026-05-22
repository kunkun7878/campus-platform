-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000000_create_profiles
-- 回滚: 删除 profiles 表及其关联的 trigger 和函数
-- ═══════════════════════════════════════════════════════════

-- 1. 删除 auth.users 上的 trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

-- 2. 删除 profiles 上的 trigger
DROP TRIGGER IF EXISTS set_profiles_updated_at ON public.profiles;

-- 3. 删除 trigger 函数
DROP FUNCTION IF EXISTS public.handle_new_user();
DROP FUNCTION IF EXISTS public.update_updated_at_column();

-- 4. 删除 profiles 表
DROP TABLE IF EXISTS public.profiles CASCADE;
