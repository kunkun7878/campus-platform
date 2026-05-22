-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000014_misc_alter_profiles
-- 描述: 创建杂项表 + 修改 profiles 表 + 细化 UPDATE 策略
--       feedbacks / invite_codes / invite_records
--       / login_codes / attachments
--       + ALTER profiles (balance, runner_status,
--         invite_code, referrer_id)
--       + profiles 细化 UPDATE: 用户不可自改
--         balance/runner_status
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
--
-- 说明: login_codes 客户端无任何 policy（参照 wechat_identities
--       模式），仅 Edge Function 以 service_role 写入。
--       profiles UPDATE 新增 trigger 限制用户自改
--       balance/runner_status，仅 Agent/system 可修改。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. feedbacks — 用户反馈
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.feedbacks (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type            text NOT NULL CHECK (type IN ('bug', 'suggestion', 'complaint', 'other')),
    -- bug=Bug反馈, suggestion=功能建议, complaint=投诉, other=其他
    content         text NOT NULL,
    contact         text,
    -- 联系方式（选填）
    images          jsonb DEFAULT '[]'::jsonb,
    status          text NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'processing', 'resolved', 'closed')),
    -- pending=待处理, processing=处理中, resolved=已解决, closed=已关闭
    reply           text,
    -- 运营回复
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_feedbacks_updated_at ON public.feedbacks;
CREATE TRIGGER trg_feedbacks_updated_at
    BEFORE UPDATE ON public.feedbacks
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_feedbacks_school_id ON public.feedbacks(school_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_user_id ON public.feedbacks(user_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_type ON public.feedbacks(type);
CREATE INDEX IF NOT EXISTS idx_feedbacks_status ON public.feedbacks(status);

-- ═══════════════════════════════════════════════════════════
-- 2. invite_codes — 邀请码
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.invite_codes (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    code            text NOT NULL,
    usage_count     integer NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    -- 已使用次数
    max_uses        integer NOT NULL DEFAULT 100 CHECK (max_uses > 0),
    -- 最大使用次数，默认 100
    expires_at      timestamptz,
    -- 过期时间，NULL = 永不过期
    is_active       boolean DEFAULT true,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now(),
    UNIQUE(user_id),
    UNIQUE(code)
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_invite_codes_updated_at ON public.invite_codes;
CREATE TRIGGER trg_invite_codes_updated_at
    BEFORE UPDATE ON public.invite_codes
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_invite_codes_user_id ON public.invite_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_invite_codes_code ON public.invite_codes(code);
CREATE INDEX IF NOT EXISTS idx_invite_codes_active ON public.invite_codes(is_active)
    WHERE is_active = true;

-- ═══════════════════════════════════════════════════════════
-- 3. invite_records — 邀请记录
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.invite_records (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code            text NOT NULL,
    inviter_id      uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    invitee_id      uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    registered_at   timestamptz DEFAULT now(),
    created_at      timestamptz DEFAULT now()
);

-- indexes
CREATE INDEX IF NOT EXISTS idx_invite_records_inviter_id ON public.invite_records(inviter_id);
CREATE INDEX IF NOT EXISTS idx_invite_records_invitee_id ON public.invite_records(invitee_id);
CREATE INDEX IF NOT EXISTS idx_invite_records_code ON public.invite_records(code);
CREATE INDEX IF NOT EXISTS idx_invite_records_registered_at ON public.invite_records(inviter_id, registered_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 4. login_codes — 短信验证码
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.login_codes (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           text NOT NULL,
    code            text NOT NULL,
    used            boolean DEFAULT false,
    expires_at      timestamptz NOT NULL,
    created_at      timestamptz DEFAULT now()
);
-- 安全设计：login_codes 客户端无任何 policy（参照 wechat_identities 模式）。
-- 验证码的 INSERT（发送）、SELECT（验证）、UPDATE（标记已用）、DELETE（清理）
-- 全部由 Edge Function 使用 service_role 执行。
-- 客户端不可读/写/改/删 login_codes。

-- indexes
CREATE INDEX IF NOT EXISTS idx_login_codes_phone ON public.login_codes(phone);
CREATE INDEX IF NOT EXISTS idx_login_codes_phone_code ON public.login_codes(phone, code);
CREATE INDEX IF NOT EXISTS idx_login_codes_expires_at ON public.login_codes(expires_at)
    WHERE used = false;

-- ═══════════════════════════════════════════════════════════
-- 5. attachments — 统一附件表
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.attachments (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    url             text NOT NULL,
    ref_type        text NOT NULL
                    CHECK (ref_type IN (
                        'runner_task', 'runner_order', 'market_product',
                        'community_post', 'after_sale', 'feedback',
                        'avatar', 'id_card', 'chat_image', 'other'
                    )),
    -- runner_task=跑腿任务附件, runner_order=跑腿订单附件,
    -- market_product=商品图片, community_post=帖子图片,
    -- after_sale=售后凭证, feedback=反馈截图,
    -- avatar=头像, id_card=身份证照片, chat_image=聊天图片, other=其他
    ref_id          uuid,
    -- 关联业务 ID
    filename        text,
    -- 原始文件名
    mime_type       text,
    -- MIME 类型，如 'image/png'
    size            bigint,
    -- 文件大小，字节
    created_at      timestamptz DEFAULT now()
);
-- 注：附件为不可变数据，上传后不修改，无 updated_at。

-- indexes
CREATE INDEX IF NOT EXISTS idx_attachments_user_id ON public.attachments(user_id);
CREATE INDEX IF NOT EXISTS idx_attachments_ref ON public.attachments(ref_type, ref_id);
CREATE INDEX IF NOT EXISTS idx_attachments_ref_type ON public.attachments(ref_type);
CREATE INDEX IF NOT EXISTS idx_attachments_created_at ON public.attachments(user_id, created_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 6. ALTER profiles — 新增字段
-- ═══════════════════════════════════════════════════════════

-- 钱包余额（单位：分），仅 Agent/系统可修改
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS balance integer DEFAULT 0 CHECK (balance >= 0);

-- 跑腿员状态，仅 Agent/系统可修改
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS runner_status text DEFAULT 'none'
    CHECK (runner_status IN ('none', 'pending', 'approved', 'rejected', 'suspended'));
-- none=非跑腿员, pending=申请中, approved=已通过, rejected=已拒绝, suspended=已停用

-- 个人邀请码（UNIQUE），注册时由系统生成，不可自改
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS invite_code text UNIQUE;

-- 推荐人 ID（通过谁的邀请码注册）
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS referrer_id uuid REFERENCES auth.users(id) ON DELETE SET NULL;

-- ── profiles 新字段索引 ───────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_profiles_runner_status ON public.profiles(runner_status);
CREATE INDEX IF NOT EXISTS idx_profiles_invite_code ON public.profiles(invite_code);
CREATE INDEX IF NOT EXISTS idx_profiles_referrer_id ON public.profiles(referrer_id);

-- ═══════════════════════════════════════════════════════════
-- 7. profiles UPDATE 细化策略
--    用户不可自改 balance/runner_status
--    仅 Agent 或 service_role 可修改这两个字段
-- ═══════════════════════════════════════════════════════════

-- trigger 函数：阻止非 Agent 用户修改 balance 或 runner_status
CREATE OR REPLACE FUNCTION public.check_profiles_sensitive_fields()
RETURNS trigger AS $$
BEGIN
    -- service_role 可修改任意字段（Edge Function / 系统调用），不拦截
    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN NEW;
    END IF;

    -- Agent 可修改任意字段，不拦截
    IF public.is_agent() THEN
        RETURN NEW;
    END IF;

    -- 非 Agent 用户：不允许修改 balance
    IF NEW.balance IS DISTINCT FROM OLD.balance THEN
        RAISE EXCEPTION 'permission denied: only agents can modify balance';
    END IF;

    -- 非 Agent 用户：不允许修改 runner_status
    IF NEW.runner_status IS DISTINCT FROM OLD.runner_status THEN
        RAISE EXCEPTION 'permission denied: only agents can modify runner_status';
    END IF;

    -- 非 Agent/非 service_role 用户：不允许修改 invite_code（系统生成后不可改）
    IF NEW.invite_code IS DISTINCT FROM OLD.invite_code THEN
        RAISE EXCEPTION 'permission denied: invite_code cannot be modified';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 应用 trigger：在 UPDATE 之前检查
DROP TRIGGER IF EXISTS trg_profiles_check_sensitive ON public.profiles;
CREATE TRIGGER trg_profiles_check_sensitive
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.check_profiles_sensitive_fields();

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── feedbacks ──────────────────────────────────────────────

ALTER TABLE public.feedbacks ENABLE ROW LEVEL SECURITY;

-- SELECT: 提交者可见（本人数据隔离）
DROP POLICY IF EXISTS feedbacks_select_policy ON public.feedbacks;
CREATE POLICY feedbacks_select_policy ON public.feedbacks
    FOR SELECT
    TO authenticated
    USING (
        user_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS feedbacks_agent_select_policy ON public.feedbacks;
CREATE POLICY feedbacks_agent_select_policy ON public.feedbacks
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 用户提交自己的反馈
DROP POLICY IF EXISTS feedbacks_insert_policy ON public.feedbacks;
CREATE POLICY feedbacks_insert_policy ON public.feedbacks
    FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

-- UPDATE: 仅 Agent 可更新（回复反馈）
DROP POLICY IF EXISTS feedbacks_update_policy ON public.feedbacks;
CREATE POLICY feedbacks_update_policy ON public.feedbacks
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS feedbacks_delete_policy ON public.feedbacks;
CREATE POLICY feedbacks_delete_policy ON public.feedbacks
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── invite_codes ───────────────────────────────────────────

ALTER TABLE public.invite_codes ENABLE ROW LEVEL SECURITY;

-- SELECT: 用户可见自己的邀请码；Agent 可见所有
DROP POLICY IF EXISTS invite_codes_select_policy ON public.invite_codes;
CREATE POLICY invite_codes_select_policy ON public.invite_codes
    FOR SELECT
    TO authenticated
    USING (
        user_id = auth.uid()
    );

DROP POLICY IF EXISTS invite_codes_agent_select_policy ON public.invite_codes;
CREATE POLICY invite_codes_agent_select_policy ON public.invite_codes
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 仅 Agent/系统可创建邀请码（由 trigger 或 Edge Function 执行）
DROP POLICY IF EXISTS invite_codes_insert_policy ON public.invite_codes;
CREATE POLICY invite_codes_insert_policy ON public.invite_codes
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

-- UPDATE: 仅 Agent 可修改
DROP POLICY IF EXISTS invite_codes_update_policy ON public.invite_codes;
CREATE POLICY invite_codes_update_policy ON public.invite_codes
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS invite_codes_delete_policy ON public.invite_codes;
CREATE POLICY invite_codes_delete_policy ON public.invite_codes
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── invite_records ─────────────────────────────────────────

ALTER TABLE public.invite_records ENABLE ROW LEVEL SECURITY;

-- SELECT: 邀请人或被邀请人可见；Agent 可见所有
DROP POLICY IF EXISTS invite_records_select_policy ON public.invite_records;
CREATE POLICY invite_records_select_policy ON public.invite_records
    FOR SELECT
    TO authenticated
    USING (
        inviter_id = auth.uid()
        OR invitee_id = auth.uid()
    );

DROP POLICY IF EXISTS invite_records_agent_select_policy ON public.invite_records;
CREATE POLICY invite_records_agent_select_policy ON public.invite_records
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 仅 Agent/系统可写入（由注册 Edge Function 执行）
DROP POLICY IF EXISTS invite_records_insert_policy ON public.invite_records;
CREATE POLICY invite_records_insert_policy ON public.invite_records
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS invite_records_delete_policy ON public.invite_records;
CREATE POLICY invite_records_delete_policy ON public.invite_records
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── login_codes ────────────────────────────────────────────
-- 安全设计：login_codes 客户端无任何 policy。
-- 验证码的 INSERT/SELECT/UPDATE/DELETE 全部由
-- Edge Function 使用 service_role 执行。
-- 客户端不可读/写/改/删 login_codes。

ALTER TABLE public.login_codes ENABLE ROW LEVEL SECURITY;

-- 清理历史残留 policy（确保客户端无法访问）
DROP POLICY IF EXISTS login_codes_select_policy ON public.login_codes;
DROP POLICY IF EXISTS login_codes_insert_policy ON public.login_codes;
DROP POLICY IF EXISTS login_codes_update_policy ON public.login_codes;
DROP POLICY IF EXISTS login_codes_delete_policy ON public.login_codes;

-- ── attachments ────────────────────────────────────────────
-- 附件按 user_id 隔离：用户只能操作自己的附件

ALTER TABLE public.attachments ENABLE ROW LEVEL SECURITY;

-- SELECT: 用户只能看自己的附件
DROP POLICY IF EXISTS attachments_select_policy ON public.attachments;
CREATE POLICY attachments_select_policy ON public.attachments
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- Agent 可查看所有附件
DROP POLICY IF EXISTS attachments_agent_select_policy ON public.attachments;
CREATE POLICY attachments_agent_select_policy ON public.attachments
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 用户上传自己的附件
DROP POLICY IF EXISTS attachments_insert_policy ON public.attachments;
CREATE POLICY attachments_insert_policy ON public.attachments
    FOR INSERT
    TO authenticated
    WITH CHECK (user_id = auth.uid());

-- DELETE: 用户可删除自己的附件；Agent 可删除所有
DROP POLICY IF EXISTS attachments_delete_policy ON public.attachments;
CREATE POLICY attachments_delete_policy ON public.attachments
    FOR DELETE
    TO authenticated
    USING (
        user_id = auth.uid()
        OR public.is_agent()
    );
