-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000000_create_profiles
-- 描述: 创建 profiles 表 + 自动创建 trigger
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ── profiles 表 ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.profiles (
    id              uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    phone           text,
    email           text,
    email_verified_at timestamptz,
    nickname        text,
    avatar_url      text,
    school_id       uuid,
    campus_id       uuid,
    is_agent        boolean DEFAULT false,
    status          smallint DEFAULT 0,  -- 0=正常 1=禁用 2=注销
    deleted_at      timestamptz,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- ── 更新 updated_at 的 trigger 函数 ───────────────────────
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- ── profiles updated_at trigger ──────────────────────────
DROP TRIGGER IF EXISTS set_profiles_updated_at ON public.profiles;
CREATE TRIGGER set_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- ── 新用户注册时自动创建 profiles 行 ─────────────────────
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.profiles (id, phone, created_at, updated_at)
    VALUES (NEW.id, NEW.phone, now(), now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- ── on_auth_user_created trigger ─────────────────────────
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();
