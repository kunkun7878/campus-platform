-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000004_wechat_identities
-- 描述: 创建微信身份绑定表
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
--
-- 注意: 微信登录 Edge Function 代码不在此 migration 中，
--        需单独部署。Android 端暂不集成微信登录功能。
-- ═══════════════════════════════════════════════════════════

-- ── wechat_identities 表 ─────────────────────────────────
CREATE TABLE IF NOT EXISTS public.wechat_identities (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    wechat_unionid  text UNIQUE NOT NULL,
    wechat_openid   text NOT NULL,
    created_at      timestamptz DEFAULT now(),
    UNIQUE(user_id, wechat_unionid)
);

-- ── RLS ──────────────────────────────────────────────────
--
-- 安全设计：客户端只读，写入仅 Edge Function 使用 service_role
-- 客户端仅允许 SELECT 自己的微信绑定记录。
-- INSERT/UPDATE/DELETE 操作由 Edge Function（微信登录回调）以 service_role 执行，
-- 客户端没有任何 INSERT/UPDATE/DELETE policy。
-- 清理旧版 INSERT/DELETE policy（如已创建则移除，确保客户端无法写入）

ALTER TABLE public.wechat_identities ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS wechat_identities_select_policy ON public.wechat_identities;
CREATE POLICY wechat_identities_select_policy ON public.wechat_identities
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS wechat_identities_insert_policy ON public.wechat_identities;
DROP POLICY IF EXISTS wechat_identities_update_policy ON public.wechat_identities;
DROP POLICY IF EXISTS wechat_identities_delete_policy ON public.wechat_identities;
