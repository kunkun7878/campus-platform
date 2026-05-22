-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000001_create_schools_campuses
-- 描述: 创建 schools + campuses 表
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ── schools 表 ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.schools (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name         text NOT NULL,
    abbreviation text,
    city         text,
    province     text,
    created_at   timestamptz DEFAULT now()
);

-- ── campuses 表 ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.campuses (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   uuid NOT NULL REFERENCES public.schools(id),
    name        text NOT NULL,
    address     text,
    created_at  timestamptz DEFAULT now()
);

-- ── campuses 索引：按学校查询 ──────────────────────────────
CREATE INDEX IF NOT EXISTS idx_campuses_school_id
    ON public.campuses(school_id);

-- ── 补充 profiles 的外键约束（如果 schools/campuses 表已存在） ──
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'profiles_school_id_fkey'
    ) THEN
        ALTER TABLE public.profiles
            ADD CONSTRAINT profiles_school_id_fkey
            FOREIGN KEY (school_id) REFERENCES public.schools(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'profiles_campus_id_fkey'
    ) THEN
        ALTER TABLE public.profiles
            ADD CONSTRAINT profiles_campus_id_fkey
            FOREIGN KEY (campus_id) REFERENCES public.campuses(id);
    END IF;
END;
$$ LANGUAGE plpgsql;
