-- ═══════════════════════════════════════════════════════════
-- Revert: 20260522000014_misc_alter_profiles
-- 回滚: 删除 attachments / login_codes / invite_records
--       / invite_codes / feedbacks 表及其 RLS、trigger、索引
--       撤销 profiles 新增字段 + 撤销敏感字段 trigger
-- ═══════════════════════════════════════════════════════════

-- ── 5. attachments ─────────────────────────────────────────
DROP POLICY IF EXISTS attachments_select_policy ON public.attachments;
DROP POLICY IF EXISTS attachments_agent_select_policy ON public.attachments;
DROP POLICY IF EXISTS attachments_insert_policy ON public.attachments;
DROP POLICY IF EXISTS attachments_delete_policy ON public.attachments;
ALTER TABLE IF EXISTS public.attachments DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.attachments CASCADE;

-- ── 4. login_codes ─────────────────────────────────────────
-- 无客户端 policy，仅需清理表
ALTER TABLE IF EXISTS public.login_codes DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.login_codes CASCADE;

-- ── 3. invite_records ──────────────────────────────────────
DROP POLICY IF EXISTS invite_records_select_policy ON public.invite_records;
DROP POLICY IF EXISTS invite_records_agent_select_policy ON public.invite_records;
DROP POLICY IF EXISTS invite_records_insert_policy ON public.invite_records;
DROP POLICY IF EXISTS invite_records_delete_policy ON public.invite_records;
ALTER TABLE IF EXISTS public.invite_records DISABLE ROW LEVEL SECURITY;
DROP TABLE IF EXISTS public.invite_records CASCADE;

-- ── 2. invite_codes ────────────────────────────────────────
DROP POLICY IF EXISTS invite_codes_select_policy ON public.invite_codes;
DROP POLICY IF EXISTS invite_codes_agent_select_policy ON public.invite_codes;
DROP POLICY IF EXISTS invite_codes_insert_policy ON public.invite_codes;
DROP POLICY IF EXISTS invite_codes_update_policy ON public.invite_codes;
DROP POLICY IF EXISTS invite_codes_delete_policy ON public.invite_codes;
ALTER TABLE IF EXISTS public.invite_codes DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_invite_codes_updated_at ON public.invite_codes;
DROP TABLE IF EXISTS public.invite_codes CASCADE;

-- ── 1. feedbacks ───────────────────────────────────────────
DROP POLICY IF EXISTS feedbacks_select_policy ON public.feedbacks;
DROP POLICY IF EXISTS feedbacks_agent_select_policy ON public.feedbacks;
DROP POLICY IF EXISTS feedbacks_insert_policy ON public.feedbacks;
DROP POLICY IF EXISTS feedbacks_update_policy ON public.feedbacks;
DROP POLICY IF EXISTS feedbacks_delete_policy ON public.feedbacks;
ALTER TABLE IF EXISTS public.feedbacks DISABLE ROW LEVEL SECURITY;
DROP TRIGGER IF EXISTS trg_feedbacks_updated_at ON public.feedbacks;
DROP TABLE IF EXISTS public.feedbacks CASCADE;

-- ── profiles 回滚：撤销敏感字段 trigger ──────────────────

DROP TRIGGER IF EXISTS trg_profiles_check_sensitive ON public.profiles;
DROP FUNCTION IF EXISTS public.check_profiles_sensitive_fields();

-- ── profiles 回滚：撤销新增字段 ────────────────────────────

ALTER TABLE IF EXISTS public.profiles
    DROP COLUMN IF EXISTS runner_status;

ALTER TABLE IF EXISTS public.profiles
    DROP COLUMN IF EXISTS invite_code;

ALTER TABLE IF EXISTS public.profiles
    DROP COLUMN IF EXISTS referrer_id;
