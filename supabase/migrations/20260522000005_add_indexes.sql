-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000005_add_indexes
-- 描述: 为高频查询字段补充数据库索引
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ── profiles.school_id 索引 ─────────────────────────────────
-- 加速按学校筛选用户、同校用户互看等场景
CREATE INDEX IF NOT EXISTS idx_profiles_school_id ON public.profiles(school_id);
