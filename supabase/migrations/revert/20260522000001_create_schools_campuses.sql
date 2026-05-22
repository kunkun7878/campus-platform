-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000001_create_schools_campuses
-- 回滚: 删除 profiles 外键约束、campuses 表和 schools 表
-- ═══════════════════════════════════════════════════════════

-- 1. 删除 profiles 上的外键约束（先解除依赖）
ALTER TABLE IF EXISTS public.profiles
    DROP CONSTRAINT IF EXISTS profiles_school_id_fkey;
ALTER TABLE IF EXISTS public.profiles
    DROP CONSTRAINT IF EXISTS profiles_campus_id_fkey;

-- 2. 删除 campuses 表
DROP TABLE IF EXISTS public.campuses CASCADE;

-- 3. 删除 schools 表
DROP TABLE IF EXISTS public.schools CASCADE;
