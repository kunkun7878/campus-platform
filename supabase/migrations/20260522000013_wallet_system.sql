-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000013_wallet_system
-- 描述: 创建钱包系统 + 公告 + 优惠券 5 张表 —
--       wallets / wallet_transactions / announcements
--       / coupons / user_coupons
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
--
-- 说明: wallet_transactions 为 append-only，无 updated_at。
--       announcements.school_id = NULL 表示全平台公告。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. wallets — 用户钱包
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.wallets (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    balance         integer NOT NULL DEFAULT 0 CHECK (balance >= 0),
    -- 可用余额，单位：分
    frozen_balance  integer NOT NULL DEFAULT 0 CHECK (frozen_balance >= 0),
    -- 冻结余额（交易中），单位：分
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now(),
    UNIQUE(user_id)
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_wallets_updated_at ON public.wallets;
CREATE TRIGGER trg_wallets_updated_at
    BEFORE UPDATE ON public.wallets
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON public.wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_balance ON public.wallets(user_id, balance);

-- ═══════════════════════════════════════════════════════════
-- 2. wallet_transactions — 交易流水（append-only）
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.wallet_transactions (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    wallet_id       uuid NOT NULL REFERENCES public.wallets(id) ON DELETE CASCADE,
    amount          integer NOT NULL,
    -- 交易金额（正数为收入，负数为支出），单位：分
    type            text NOT NULL
                    CHECK (type IN ('income', 'expense', 'freeze', 'unfreeze', 'refund')),
    -- income=收入, expense=支出, freeze=冻结, unfreeze=解冻, refund=退款
    balance_before  integer NOT NULL,
    -- 交易前余额，单位：分
    balance_after   integer NOT NULL,
    -- 交易后余额，单位：分
    ref_type        text,
    -- 关联业务类型，如 'runner_order', 'market_order', 'after_sale' 等
    ref_id          uuid,
    -- 关联业务 ID
    description     text,
    created_at      timestamptz DEFAULT now()
);
-- 注意：wallet_transactions 为 append-only，无 updated_at 字段。
--       交易流水一旦生成不可修改，保证对账准确性。

-- indexes
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_id ON public.wallet_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_id ON public.wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_type ON public.wallet_transactions(type);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_ref ON public.wallet_transactions(ref_type, ref_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_created_at ON public.wallet_transactions(user_id, created_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 3. announcements — 公告
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.announcements (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title           text NOT NULL,
    content         text,
    school_id       uuid REFERENCES public.schools(id) ON DELETE RESTRICT,
    -- NULL = 全平台公告，所有用户可见
    published_by    uuid NOT NULL REFERENCES auth.users(id) ON DELETE SET NULL,
    is_pinned       boolean DEFAULT false,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_announcements_updated_at ON public.announcements;
CREATE TRIGGER trg_announcements_updated_at
    BEFORE UPDATE ON public.announcements
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_announcements_school_id ON public.announcements(school_id);
CREATE INDEX IF NOT EXISTS idx_announcements_pinned ON public.announcements(is_pinned DESC, created_at DESC)
    WHERE is_pinned = true;
CREATE INDEX IF NOT EXISTS idx_announcements_created_at ON public.announcements(created_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 4. coupons — 优惠券模板
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.coupons (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title           text NOT NULL,
    type            text NOT NULL CHECK (type IN ('fixed', 'percentage')),
    -- fixed=固定金额减免, percentage=按比例折扣
    value           integer NOT NULL CHECK (value > 0),
    -- fixed 时为减免金额（分），percentage 时为折扣百分数（如 10 表示 10% off）
    min_amount      integer NOT NULL DEFAULT 0 CHECK (min_amount >= 0),
    -- 最低消费金额（分），0 = 无门槛
    total_count     integer NOT NULL DEFAULT 0 CHECK (total_count >= 0),
    -- 总发放数量，0 = 不限量
    used_count      integer NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    -- 已使用数量
    start_at        timestamptz,
    -- 有效期开始
    end_at          timestamptz,
    -- 有效期结束
    school_id       uuid REFERENCES public.schools(id) ON DELETE RESTRICT,
    -- NULL = 全平台通用
    is_active       boolean DEFAULT true,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_coupons_updated_at ON public.coupons;
CREATE TRIGGER trg_coupons_updated_at
    BEFORE UPDATE ON public.coupons
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_coupons_school_id ON public.coupons(school_id);
CREATE INDEX IF NOT EXISTS idx_coupons_type ON public.coupons(type);
CREATE INDEX IF NOT EXISTS idx_coupons_active ON public.coupons(is_active, end_at)
    WHERE is_active = true;

-- ═══════════════════════════════════════════════════════════
-- 5. user_coupons — 用户领券
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.user_coupons (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    coupon_id       uuid NOT NULL REFERENCES public.coupons(id) ON DELETE CASCADE,
    status          text NOT NULL DEFAULT 'unused'
                    CHECK (status IN ('unused', 'used', 'expired')),
    -- unused=未使用, used=已使用, expired=已过期
    used_at         timestamptz,
    order_id        uuid,
    -- 使用该券的订单 ID（通用引用，不限定单表）
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_user_coupons_updated_at ON public.user_coupons;
CREATE TRIGGER trg_user_coupons_updated_at
    BEFORE UPDATE ON public.user_coupons
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_user_coupons_user_id ON public.user_coupons(user_id);
CREATE INDEX IF NOT EXISTS idx_user_coupons_coupon_id ON public.user_coupons(coupon_id);
CREATE INDEX IF NOT EXISTS idx_user_coupons_status ON public.user_coupons(user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_coupons_user_coupon ON public.user_coupons(user_id, coupon_id);

-- 每人每个优惠券模板只能领取一次
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_coupons_user_coupon
    ON public.user_coupons(user_id, coupon_id);

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── wallets ────────────────────────────────────────────────
-- 钱包是用户私产，仅本人可见

ALTER TABLE public.wallets ENABLE ROW LEVEL SECURITY;

-- SELECT: 用户只能看自己的钱包
DROP POLICY IF EXISTS wallets_select_policy ON public.wallets;
CREATE POLICY wallets_select_policy ON public.wallets
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- Agent 可查看所有钱包
DROP POLICY IF EXISTS wallets_agent_select_policy ON public.wallets;
CREATE POLICY wallets_agent_select_policy ON public.wallets
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 用户或 Agent 可创建钱包（通常由 trigger/Edge Function 自动创建）
DROP POLICY IF EXISTS wallets_insert_policy ON public.wallets;
CREATE POLICY wallets_insert_policy ON public.wallets
    FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        OR public.is_agent()
    );

-- UPDATE: 仅 Agent 可操作余额变更（用户不可直接修改余额）
DROP POLICY IF EXISTS wallets_update_policy ON public.wallets;
CREATE POLICY wallets_update_policy ON public.wallets
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 禁止删除钱包
DROP POLICY IF EXISTS wallets_delete_policy ON public.wallets;
CREATE POLICY wallets_delete_policy ON public.wallets
    FOR DELETE
    TO authenticated
    USING (false);

-- ── wallet_transactions ────────────────────────────────────
-- 交易流水：用户可查自己的，append-only（客户端无 INSERT/UPDATE/DELETE policy）

ALTER TABLE public.wallet_transactions ENABLE ROW LEVEL SECURITY;

-- SELECT: 用户只能看自己的交易流水
DROP POLICY IF EXISTS wallet_transactions_select_policy ON public.wallet_transactions;
CREATE POLICY wallet_transactions_select_policy ON public.wallet_transactions
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- Agent 可查看所有交易流水
DROP POLICY IF EXISTS wallet_transactions_agent_select_policy ON public.wallet_transactions;
CREATE POLICY wallet_transactions_agent_select_policy ON public.wallet_transactions
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- 注意：wallet_transactions 为 append-only 表，INSERT 由 Edge Function
--       使用 service_role 写入以保证对账一致性，客户端无 INSERT/UPDATE/DELETE policy。

-- ── announcements ──────────────────────────────────────────
-- school_id = NULL 表示全平台公告；否则仅对应学校可见

ALTER TABLE public.announcements ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校公告 OR 全平台公告 (school_id IS NULL)
DROP POLICY IF EXISTS announcements_select_policy ON public.announcements;
CREATE POLICY announcements_select_policy ON public.announcements
    FOR SELECT
    TO authenticated
    USING (
        school_id IS NULL
        OR school_id = public.get_user_school_id()
    );

-- Agent 可查看所有公告
DROP POLICY IF EXISTS announcements_agent_select_policy ON public.announcements;
CREATE POLICY announcements_agent_select_policy ON public.announcements
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 仅 Agent 可发布公告
DROP POLICY IF EXISTS announcements_insert_policy ON public.announcements;
CREATE POLICY announcements_insert_policy ON public.announcements
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

-- UPDATE: 仅 Agent 可修改公告
DROP POLICY IF EXISTS announcements_update_policy ON public.announcements;
CREATE POLICY announcements_update_policy ON public.announcements
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除公告
DROP POLICY IF EXISTS announcements_delete_policy ON public.announcements;
CREATE POLICY announcements_delete_policy ON public.announcements
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── coupons ────────────────────────────────────────────────
-- 所有用户可见可用优惠券；仅 Agent 可管理模板

ALTER TABLE public.coupons ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校或全平台通用（school_id IS NULL）
DROP POLICY IF EXISTS coupons_select_policy ON public.coupons;
CREATE POLICY coupons_select_policy ON public.coupons
    FOR SELECT
    TO authenticated
    USING (
        school_id IS NULL
        OR school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS coupons_agent_select_policy ON public.coupons;
CREATE POLICY coupons_agent_select_policy ON public.coupons
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 仅 Agent 可创建优惠券模板
DROP POLICY IF EXISTS coupons_insert_policy ON public.coupons;
CREATE POLICY coupons_insert_policy ON public.coupons
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

-- UPDATE: 仅 Agent 可修改模板
DROP POLICY IF EXISTS coupons_update_policy ON public.coupons;
CREATE POLICY coupons_update_policy ON public.coupons
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除模板
DROP POLICY IF EXISTS coupons_delete_policy ON public.coupons;
CREATE POLICY coupons_delete_policy ON public.coupons
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── user_coupons ───────────────────────────────────────────

ALTER TABLE public.user_coupons ENABLE ROW LEVEL SECURITY;

-- SELECT: 用户只能看自己的券
DROP POLICY IF EXISTS user_coupons_select_policy ON public.user_coupons;
CREATE POLICY user_coupons_select_policy ON public.user_coupons
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS user_coupons_agent_select_policy ON public.user_coupons;
CREATE POLICY user_coupons_agent_select_policy ON public.user_coupons
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 用户可领券；Agent 可发放
DROP POLICY IF EXISTS user_coupons_insert_policy ON public.user_coupons;
CREATE POLICY user_coupons_insert_policy ON public.user_coupons
    FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        OR public.is_agent()
    );

-- UPDATE: 仅 Agent 可更新（如标记已使用）
DROP POLICY IF EXISTS user_coupons_update_policy ON public.user_coupons;
CREATE POLICY user_coupons_update_policy ON public.user_coupons
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS user_coupons_delete_policy ON public.user_coupons;
CREATE POLICY user_coupons_delete_policy ON public.user_coupons
    FOR DELETE
    TO authenticated
    USING (public.is_agent());
