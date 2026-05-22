-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000003_rls_policies
-- 描述: RLS helper 函数 + 全部表的安全策略
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ── Helper 函数 ──────────────────────────────────────────

-- 获取当前用户的 school_id
CREATE OR REPLACE FUNCTION public.get_user_school_id()
RETURNS uuid AS $$
    SELECT school_id FROM public.profiles WHERE id = auth.uid()
    LIMIT 1;
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- 获取当前用户的 campus_id
CREATE OR REPLACE FUNCTION public.get_user_campus_id()
RETURNS uuid AS $$
    SELECT campus_id FROM public.profiles WHERE id = auth.uid()
    LIMIT 1;
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- 当前用户是否为代理
CREATE OR REPLACE FUNCTION public.is_agent()
RETURNS boolean AS $$
    SELECT COALESCE((SELECT is_agent FROM public.profiles WHERE id = auth.uid() LIMIT 1), false);
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- 当前用户是否已选校
CREATE OR REPLACE FUNCTION public.has_selected_school()
RETURNS boolean AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND school_id IS NOT NULL AND campus_id IS NOT NULL
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── profiles 表 ──────────────────────────────────────────

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- 查看自己的 profile，或同校用户可互看基本资料（昵称+头像，status != 2）
DROP POLICY IF EXISTS profiles_select_policy ON public.profiles;
CREATE POLICY profiles_select_policy ON public.profiles
    FOR SELECT
    TO authenticated
    USING (
        id = auth.uid()
        OR (
            public.get_user_school_id() = school_id
            AND status != 2
        )
    );

-- 插入由 handle_new_user trigger 处理

-- 更新自己的 profile（选校后不可更改 school_id）
DROP POLICY IF EXISTS profiles_update_policy ON public.profiles;
CREATE POLICY profiles_update_policy ON public.profiles
    FOR UPDATE
    TO authenticated
    USING (id = auth.uid())
    WITH CHECK (
        id = auth.uid()
        AND (
            school_id = public.get_user_school_id()
            OR public.get_user_school_id() IS NULL
        )
    );

-- 禁止删除（仅软删除）
DROP POLICY IF EXISTS profiles_delete_policy ON public.profiles;
CREATE POLICY profiles_delete_policy ON public.profiles
    FOR DELETE
    TO authenticated
    USING (false);

-- Agent 可查看所有 profile
DROP POLICY IF EXISTS profiles_agent_policy ON public.profiles;
CREATE POLICY profiles_agent_policy ON public.profiles
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- ── schools 表 ───────────────────────────────────────────

ALTER TABLE public.schools ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS schools_select_policy ON public.schools;
CREATE POLICY schools_select_policy ON public.schools
    FOR SELECT
    TO authenticated
    USING (true);

DROP POLICY IF EXISTS schools_modify_policy ON public.schools;
CREATE POLICY schools_modify_policy ON public.schools
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS schools_update_policy ON public.schools;
CREATE POLICY schools_update_policy ON public.schools
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS schools_delete_policy ON public.schools;
CREATE POLICY schools_delete_policy ON public.schools
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── campuses 表 ──────────────────────────────────────────

ALTER TABLE public.campuses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS campuses_select_policy ON public.campuses;
CREATE POLICY campuses_select_policy ON public.campuses
    FOR SELECT
    TO authenticated
    USING (true);

DROP POLICY IF EXISTS campuses_modify_policy ON public.campuses;
CREATE POLICY campuses_modify_policy ON public.campuses
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS campuses_update_policy ON public.campuses;
CREATE POLICY campuses_update_policy ON public.campuses
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS campuses_delete_policy ON public.campuses;
CREATE POLICY campuses_delete_policy ON public.campuses
    FOR DELETE
    TO authenticated
    USING (public.is_agent());
