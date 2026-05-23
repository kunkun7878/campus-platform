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
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 获取当前用户的 campus_id
CREATE OR REPLACE FUNCTION public.get_user_campus_id()
RETURNS uuid AS $$
    SELECT campus_id FROM public.profiles WHERE id = auth.uid()
    LIMIT 1;
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 当前用户是否为代理
CREATE OR REPLACE FUNCTION public.is_agent()
RETURNS boolean AS $$
    SELECT COALESCE((SELECT is_agent FROM public.profiles WHERE id = auth.uid() LIMIT 1), false);
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 当前用户是否已选校
CREATE OR REPLACE FUNCTION public.has_selected_school()
RETURNS boolean AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND school_id IS NOT NULL AND campus_id IS NOT NULL
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── profiles 表 ──────────────────────────────────────────

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- 查看自己的 profile，或同校用户可互看基本资料（昵称+头像，status != 2）
-- 隐私注意：当前 SELECT 返回完整行含 phone 字段，同校用户可互看 phone。
-- 如需限制字段粒度，请启用下方的 public.profiles_public VIEW。
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

-- Agent 可更新所有 profile（含软删除：UPDATE status = 2）
DROP POLICY IF EXISTS profiles_agent_update_policy ON public.profiles;
CREATE POLICY profiles_agent_update_policy ON public.profiles
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- 禁止硬删除 profile（业务上应通过 profiles_agent_update_policy
-- 执行软删除 UPDATE status=2，避免直接硬删除用户数据）
DROP POLICY IF EXISTS profiles_agent_delete_policy ON public.profiles;
CREATE POLICY profiles_agent_delete_policy ON public.profiles
    FOR DELETE
    TO authenticated
    USING (false);

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

-- ── wechat_identities 表 ─────────────────────────────────────
-- 客户端只读：仅允许用户通过 SELECT policy 查看自己的微信绑定记录。
-- INSERT/UPDATE/DELETE 操作仅由 Edge Function 使用 service_role 执行。
-- 客户端无需 INSERT/UPDATE/DELETE policy（无 policy = 客户端无法写入）。

-- ═══════════════════════════════════════════════════════════
-- 列级隐私粒度说明
-- ═══════════════════════════════════════════════════════════
-- PostgreSQL RLS 按行过滤，不按列过滤。profiles_select_policy 的同校互读规则
-- 目前返回完整 profile 行（含 phone 等敏感字段），如需限制同校互看字段粒度，
-- 建议创建 VIEW public_profiles 仅暴露 nickname / avatar / school_id / campus_id，
-- 业务代码从该 VIEW 读取而非直接查询 profiles 表。本 migration 暂不修改。

-- ═══════════════════════════════════════════════════════════
-- 未来可启用的列级过滤方案（profiles_public VIEW）
-- ═══════════════════════════════════════════════════════════
-- 创建不暴露 phone 字段的公共 VIEW，用于同校互看的场景。
-- 启用方式：取消下方注释并执行，业务代码从 profiles_public 读取。
--
-- CREATE VIEW public.profiles_public AS
-- SELECT id, nickname, avatar_url, school_id, campus_id, status
-- FROM public.profiles
-- WHERE status != 2;
--
-- GRANT SELECT ON public.profiles_public TO authenticated;
-- ═══════════════════════════════════════════════════════════
