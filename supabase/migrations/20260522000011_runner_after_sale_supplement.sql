-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000011_runner_after_sale_supplement
-- 描述: 创建跑腿补充 + 售后模块 4 张表 —
--       runner_applications / order_timeline / after_sales
--       / after_sale_timeline
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. runner_applications — 跑腿员申请
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.runner_applications (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    real_name       text NOT NULL,
    student_id      text NOT NULL,
    phone           text NOT NULL,
    reason          text,
    id_card_front   text,       -- 身份证正面图片 URL
    id_card_back    text,       -- 身份证反面图片 URL
    status          text NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'approved', 'rejected')),
    -- pending=待审核, approved=已通过, rejected=已拒绝
    review_comment  text,       -- 审核备注
    reviewed_by     uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    reviewed_at     timestamptz,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_runner_applications_updated_at ON public.runner_applications;
CREATE TRIGGER trg_runner_applications_updated_at
    BEFORE UPDATE ON public.runner_applications
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_runner_applications_school_id ON public.runner_applications(school_id);
CREATE INDEX IF NOT EXISTS idx_runner_applications_user_id ON public.runner_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_runner_applications_status ON public.runner_applications(status);

-- 每人每校只能有一条申请
CREATE UNIQUE INDEX IF NOT EXISTS uq_runner_applications_user_school
    ON public.runner_applications(user_id, school_id);

-- ═══════════════════════════════════════════════════════════
-- 2. order_timeline — 订单时间线
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.order_timeline (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        uuid NOT NULL REFERENCES public.runner_orders(id) ON DELETE CASCADE,
    event           text NOT NULL,
    -- 事件类型，如 'created', 'accepted', 'delivering', 'delivered',
    --              'completed', 'cancelled', 'after_sale'
    description     text,
    operator_id     uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_order_timeline_updated_at ON public.order_timeline;
CREATE TRIGGER trg_order_timeline_updated_at
    BEFORE UPDATE ON public.order_timeline
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_order_timeline_school_id ON public.order_timeline(school_id);
CREATE INDEX IF NOT EXISTS idx_order_timeline_order_id ON public.order_timeline(order_id);
CREATE INDEX IF NOT EXISTS idx_order_timeline_created_at ON public.order_timeline(order_id, created_at ASC);

-- ═══════════════════════════════════════════════════════════
-- 3. after_sales — 售后申请
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.after_sales (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        uuid NOT NULL REFERENCES public.runner_orders(id) ON DELETE CASCADE,
    requester_id    uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type            text NOT NULL CHECK (type IN ('refund', 'return', 'complaint')),
    -- refund=退款, return=退货, complaint=投诉
    reason          text NOT NULL,
    images          jsonb DEFAULT '[]'::jsonb,
    status          text NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'processing', 'approved', 'rejected', 'completed')),
    -- pending=待处理, processing=处理中, approved=已同意,
    -- rejected=已拒绝, completed=已完成
    result_comment  text,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_after_sales_updated_at ON public.after_sales;
CREATE TRIGGER trg_after_sales_updated_at
    BEFORE UPDATE ON public.after_sales
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_after_sales_school_id ON public.after_sales(school_id);
CREATE INDEX IF NOT EXISTS idx_after_sales_order_id ON public.after_sales(order_id);
CREATE INDEX IF NOT EXISTS idx_after_sales_requester_id ON public.after_sales(requester_id);
CREATE INDEX IF NOT EXISTS idx_after_sales_type ON public.after_sales(type);
CREATE INDEX IF NOT EXISTS idx_after_sales_status ON public.after_sales(status);
CREATE INDEX IF NOT EXISTS idx_after_sales_school_status ON public.after_sales(school_id, status);

-- ═══════════════════════════════════════════════════════════
-- 4. after_sale_timeline — 售后进度时间线
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.after_sale_timeline (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    after_sale_id   uuid NOT NULL REFERENCES public.after_sales(id) ON DELETE CASCADE,
    event           text NOT NULL,
    -- 事件类型，如 'created', 'processing', 'approved', 'rejected', 'completed'
    description     text,
    operator_id     uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_after_sale_timeline_updated_at ON public.after_sale_timeline;
CREATE TRIGGER trg_after_sale_timeline_updated_at
    BEFORE UPDATE ON public.after_sale_timeline
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_after_sale_timeline_school_id ON public.after_sale_timeline(school_id);
CREATE INDEX IF NOT EXISTS idx_after_sale_timeline_after_sale_id ON public.after_sale_timeline(after_sale_id);
CREATE INDEX IF NOT EXISTS idx_after_sale_timeline_created_at ON public.after_sale_timeline(after_sale_id, created_at ASC);

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── runner_applications ────────────────────────────────────

ALTER TABLE public.runner_applications ENABLE ROW LEVEL SECURITY;

-- SELECT: 申请人可查看自己的申请；Agent 可查看所有
DROP POLICY IF EXISTS runner_applications_select_policy ON public.runner_applications;
CREATE POLICY runner_applications_select_policy ON public.runner_applications
    FOR SELECT
    TO authenticated
    USING (
        user_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS runner_applications_agent_select_policy ON public.runner_applications;
CREATE POLICY runner_applications_agent_select_policy ON public.runner_applications
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 用户提交自己的申请，school_id 必须与用户一致
DROP POLICY IF EXISTS runner_applications_insert_policy ON public.runner_applications;
CREATE POLICY runner_applications_insert_policy ON public.runner_applications
    FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

-- UPDATE: Agent 可审核；用户不可修改申请
DROP POLICY IF EXISTS runner_applications_update_policy ON public.runner_applications;
CREATE POLICY runner_applications_update_policy ON public.runner_applications
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS runner_applications_delete_policy ON public.runner_applications;
CREATE POLICY runner_applications_delete_policy ON public.runner_applications
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── order_timeline ─────────────────────────────────────────

ALTER TABLE public.order_timeline ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校用户可见（学校隔离），通过 JOIN runner_orders 可进一步限制
--        为本人相关订单；此处先做学校级别隔离
DROP POLICY IF EXISTS order_timeline_select_policy ON public.order_timeline;
CREATE POLICY order_timeline_select_policy ON public.order_timeline
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS order_timeline_agent_select_policy ON public.order_timeline;
CREATE POLICY order_timeline_agent_select_policy ON public.order_timeline
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 仅 Agent/系统可写入时间线（由 Edge Function 以 service_role 写入为主）
DROP POLICY IF EXISTS order_timeline_insert_policy ON public.order_timeline;
CREATE POLICY order_timeline_insert_policy ON public.order_timeline
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

-- UPDATE: 仅 Agent 可修改
DROP POLICY IF EXISTS order_timeline_update_policy ON public.order_timeline;
CREATE POLICY order_timeline_update_policy ON public.order_timeline
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS order_timeline_delete_policy ON public.order_timeline;
CREATE POLICY order_timeline_delete_policy ON public.order_timeline
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── after_sales ────────────────────────────────────────────

ALTER TABLE public.after_sales ENABLE ROW LEVEL SECURITY;

-- SELECT: 申请人或订单跑腿者可见
DROP POLICY IF EXISTS after_sales_select_policy ON public.after_sales;
CREATE POLICY after_sales_select_policy ON public.after_sales
    FOR SELECT
    TO authenticated
    USING (
        (requester_id = auth.uid() AND school_id = public.get_user_school_id())
        OR EXISTS (
            SELECT 1 FROM public.runner_orders
            WHERE runner_orders.id = after_sales.order_id
            AND runner_orders.runner_id = auth.uid()
            AND runner_orders.school_id = public.get_user_school_id()
        )
    );

DROP POLICY IF EXISTS after_sales_agent_select_policy ON public.after_sales;
CREATE POLICY after_sales_agent_select_policy ON public.after_sales
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 申请人发起售后
DROP POLICY IF EXISTS after_sales_insert_policy ON public.after_sales;
CREATE POLICY after_sales_insert_policy ON public.after_sales
    FOR INSERT
    TO authenticated
    WITH CHECK (
        requester_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

-- UPDATE: Agent 可更新售后状态
DROP POLICY IF EXISTS after_sales_update_policy ON public.after_sales;
CREATE POLICY after_sales_update_policy ON public.after_sales
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS after_sales_delete_policy ON public.after_sales;
CREATE POLICY after_sales_delete_policy ON public.after_sales
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── after_sale_timeline ────────────────────────────────────

ALTER TABLE public.after_sale_timeline ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校用户可查看（学校隔离）
DROP POLICY IF EXISTS after_sale_timeline_select_policy ON public.after_sale_timeline;
CREATE POLICY after_sale_timeline_select_policy ON public.after_sale_timeline
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS after_sale_timeline_agent_select_policy ON public.after_sale_timeline;
CREATE POLICY after_sale_timeline_agent_select_policy ON public.after_sale_timeline
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 仅 Agent/Edge Function 可写入
DROP POLICY IF EXISTS after_sale_timeline_insert_policy ON public.after_sale_timeline;
CREATE POLICY after_sale_timeline_insert_policy ON public.after_sale_timeline
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_agent());

-- UPDATE: 仅 Agent 可修改
DROP POLICY IF EXISTS after_sale_timeline_update_policy ON public.after_sale_timeline;
CREATE POLICY after_sale_timeline_update_policy ON public.after_sale_timeline
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可删除
DROP POLICY IF EXISTS after_sale_timeline_delete_policy ON public.after_sale_timeline;
CREATE POLICY after_sale_timeline_delete_policy ON public.after_sale_timeline
    FOR DELETE
    TO authenticated
    USING (public.is_agent());
