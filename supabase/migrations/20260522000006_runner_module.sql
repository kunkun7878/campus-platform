-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000006_runner_module
-- 描述: 创建跑腿模块 4 张表 — runner_tasks / runner_orders
--       / runner_reviews / user_addresses
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. runner_tasks — 跑腿任务
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.runner_tasks (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    publisher_id    uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    runner_id       uuid REFERENCES auth.users(id) ON DELETE SET NULL,
    type            text NOT NULL CHECK (type IN ('pickup', 'delivery', 'purchase', 'universal')),
    -- 值与Android端保持一致: pickup=帮取, delivery=帮送, purchase=帮买, universal=万能帮
    title           text NOT NULL,
    description     text,
    pickup_addr     text,
    delivery_addr   text,
    price           integer NOT NULL DEFAULT 0 CHECK (price >= 0),
    tip             integer DEFAULT 0 CHECK (tip >= 0),
    status          text NOT NULL DEFAULT 'published'
                    CHECK (status IN ('published', 'assigned', 'in_progress', 'completed', 'cancelled')),
    -- published=已发布, assigned=已接单, in_progress=进行中, completed=已完成, cancelled=已取消
    deadline        timestamptz,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    images          jsonb DEFAULT '[]'::jsonb,
    gender_restriction text CHECK (gender_restriction IN ('any','female_only','male_only')) DEFAULT 'any',
    auto_cancel_minutes integer DEFAULT 20,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_runner_tasks_updated_at ON public.runner_tasks;
CREATE TRIGGER trg_runner_tasks_updated_at
    BEFORE UPDATE ON public.runner_tasks
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_runner_tasks_school_id ON public.runner_tasks(school_id);
CREATE INDEX IF NOT EXISTS idx_runner_tasks_publisher_id ON public.runner_tasks(publisher_id);
CREATE INDEX IF NOT EXISTS idx_runner_tasks_runner_id ON public.runner_tasks(runner_id);
CREATE INDEX IF NOT EXISTS idx_runner_tasks_status ON public.runner_tasks(status);
CREATE INDEX IF NOT EXISTS idx_runner_tasks_type ON public.runner_tasks(type);
CREATE INDEX IF NOT EXISTS idx_runner_tasks_school_status ON public.runner_tasks(school_id, status);
CREATE INDEX IF NOT EXISTS idx_runner_tasks_created_at ON public.runner_tasks(created_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 2. runner_orders — 跑腿订单
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.runner_orders (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id         uuid NOT NULL REFERENCES public.runner_tasks(id) ON DELETE CASCADE,
    buyer_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    runner_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status          text NOT NULL DEFAULT 'accepted'
                    CHECK (status IN ('accepted', 'delivering', 'delivered', 'completed', 'cancelled', 'after_sale')),
    -- accepted=已接单, delivering=配送中, delivered=已送达, completed=已完成, cancelled=已取消, after_sale=售后中
    cancel_reason   text,
    completed_at    timestamptz,
    expected_at     timestamptz,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_runner_orders_updated_at ON public.runner_orders;
CREATE TRIGGER trg_runner_orders_updated_at
    BEFORE UPDATE ON public.runner_orders
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_runner_orders_school_id ON public.runner_orders(school_id);
CREATE INDEX IF NOT EXISTS idx_runner_orders_task_id ON public.runner_orders(task_id);
CREATE INDEX IF NOT EXISTS idx_runner_orders_buyer_id ON public.runner_orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_runner_orders_runner_id ON public.runner_orders(runner_id);
CREATE INDEX IF NOT EXISTS idx_runner_orders_status ON public.runner_orders(status);
CREATE INDEX IF NOT EXISTS idx_runner_orders_school_status ON public.runner_orders(school_id, status);

-- ═══════════════════════════════════════════════════════════
-- 3. runner_reviews — 跑腿评价
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.runner_reviews (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        uuid NOT NULL REFERENCES public.runner_orders(id) ON DELETE CASCADE,
    reviewer_id     uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    reviewee_id     uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    rating          smallint NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         text,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_runner_reviews_updated_at ON public.runner_reviews;
CREATE TRIGGER trg_runner_reviews_updated_at
    BEFORE UPDATE ON public.runner_reviews
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_runner_reviews_school_id ON public.runner_reviews(school_id);
CREATE INDEX IF NOT EXISTS idx_runner_reviews_order_id ON public.runner_reviews(order_id);
CREATE INDEX IF NOT EXISTS idx_runner_reviews_reviewer_id ON public.runner_reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_runner_reviews_reviewee_id ON public.runner_reviews(reviewee_id);

-- 每人每单只能评价一次
CREATE UNIQUE INDEX IF NOT EXISTS uq_runner_reviews_order_reviewer
    ON public.runner_reviews(order_id, reviewer_id);

-- ═══════════════════════════════════════════════════════════
-- 4. user_addresses — 用户地址簿
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.user_addresses (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    label           text NOT NULL,          -- 标签，如"宿舍"/"教学楼"/"图书馆"
    contact_name    text NOT NULL,
    contact_phone   text NOT NULL,
    address         text NOT NULL,
    is_default      boolean DEFAULT false,
    school_id       uuid REFERENCES public.schools(id) ON DELETE SET NULL,
    -- school_id 为统一设计预留，地址属于个人数据，不按学校隔离
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_user_addresses_updated_at ON public.user_addresses;
CREATE TRIGGER trg_user_addresses_updated_at
    BEFORE UPDATE ON public.user_addresses
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON public.user_addresses(user_id);

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── runner_tasks ──────────────────────────────────────────

ALTER TABLE public.runner_tasks ENABLE ROW LEVEL SECURITY;

-- SELECT: 同校用户可见（学校隔离）
DROP POLICY IF EXISTS runner_tasks_select_policy ON public.runner_tasks;
CREATE POLICY runner_tasks_select_policy ON public.runner_tasks
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

-- Agent 可查看所有学校的任务
DROP POLICY IF EXISTS runner_tasks_agent_select_policy ON public.runner_tasks;
CREATE POLICY runner_tasks_agent_select_policy ON public.runner_tasks
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: 发布者必须是自己，且 school_id 必须与自己的学校匹配
DROP POLICY IF EXISTS runner_tasks_insert_policy ON public.runner_tasks;
CREATE POLICY runner_tasks_insert_policy ON public.runner_tasks
    FOR INSERT
    TO authenticated
    WITH CHECK (
        publisher_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

-- UPDATE: 发布者、接单跑腿者可更新；Agent 可更新所有
DROP POLICY IF EXISTS runner_tasks_update_policy ON public.runner_tasks;
CREATE POLICY runner_tasks_update_policy ON public.runner_tasks
    FOR UPDATE
    TO authenticated
    USING (
        (publisher_id = auth.uid() OR runner_id = auth.uid())
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        (publisher_id = auth.uid() OR runner_id = auth.uid())
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS runner_tasks_agent_update_policy ON public.runner_tasks;
CREATE POLICY runner_tasks_agent_update_policy ON public.runner_tasks
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

-- DELETE: 仅 Agent 可硬删除；普通用户通过 UPDATE status='cancelled' 取消
DROP POLICY IF EXISTS runner_tasks_delete_policy ON public.runner_tasks;
CREATE POLICY runner_tasks_delete_policy ON public.runner_tasks
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── runner_orders ─────────────────────────────────────────

ALTER TABLE public.runner_orders ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS runner_orders_select_policy ON public.runner_orders;
CREATE POLICY runner_orders_select_policy ON public.runner_orders
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS runner_orders_agent_select_policy ON public.runner_orders;
CREATE POLICY runner_orders_agent_select_policy ON public.runner_orders
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- INSERT: buyer 创建订单时指定自己为 buyer；runner 接单时创建订单并指定自己为 runner
DROP POLICY IF EXISTS runner_orders_insert_policy ON public.runner_orders;
CREATE POLICY runner_orders_insert_policy ON public.runner_orders
    FOR INSERT
    TO authenticated
    WITH CHECK (
        (buyer_id = auth.uid() OR runner_id = auth.uid())
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS runner_orders_update_policy ON public.runner_orders;
CREATE POLICY runner_orders_update_policy ON public.runner_orders
    FOR UPDATE
    TO authenticated
    USING (
        (buyer_id = auth.uid() OR runner_id = auth.uid())
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        (buyer_id = auth.uid() OR runner_id = auth.uid())
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS runner_orders_agent_update_policy ON public.runner_orders;
CREATE POLICY runner_orders_agent_update_policy ON public.runner_orders
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS runner_orders_delete_policy ON public.runner_orders;
CREATE POLICY runner_orders_delete_policy ON public.runner_orders
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── runner_reviews ────────────────────────────────────────

ALTER TABLE public.runner_reviews ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS runner_reviews_select_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_select_policy ON public.runner_reviews
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS runner_reviews_agent_select_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_agent_select_policy ON public.runner_reviews
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

DROP POLICY IF EXISTS runner_reviews_insert_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_insert_policy ON public.runner_reviews
    FOR INSERT
    TO authenticated
    WITH CHECK (
        reviewer_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS runner_reviews_update_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_update_policy ON public.runner_reviews
    FOR UPDATE
    TO authenticated
    USING (
        reviewer_id = auth.uid()
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        reviewer_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS runner_reviews_agent_update_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_agent_update_policy ON public.runner_reviews
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS runner_reviews_delete_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_delete_policy ON public.runner_reviews
    FOR DELETE
    TO authenticated
    USING (reviewer_id = auth.uid());

DROP POLICY IF EXISTS runner_reviews_agent_delete_policy ON public.runner_reviews;
CREATE POLICY runner_reviews_agent_delete_policy ON public.runner_reviews
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── user_addresses ────────────────────────────────────────
-- 地址为个人数据，仅用户本人可见/可操作

ALTER TABLE public.user_addresses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_addresses_select_policy ON public.user_addresses;
CREATE POLICY user_addresses_select_policy ON public.user_addresses
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS user_addresses_insert_policy ON public.user_addresses;
CREATE POLICY user_addresses_insert_policy ON public.user_addresses
    FOR INSERT
    TO authenticated
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS user_addresses_update_policy ON public.user_addresses;
CREATE POLICY user_addresses_update_policy ON public.user_addresses
    FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS user_addresses_delete_policy ON public.user_addresses;
CREATE POLICY user_addresses_delete_policy ON public.user_addresses
    FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());
