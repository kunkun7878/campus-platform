-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000008_lost_found_module
-- 描述: 创建失物招领模块 2 张表 — lost_found_items / lost_found_claims
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. lost_found_items — 失物招领物品
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.lost_found_items (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    publisher_id    uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type            text NOT NULL CHECK (type IN ('lost', 'found')),
    -- lost=寻物, found=招领
    title           text NOT NULL,
    description     text,
    images          jsonb DEFAULT '[]'::jsonb,
    location        text,
    lost_date       date,
    category        text NOT NULL DEFAULT 'other',
    status          text NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active', 'claimed', 'closed')),
    -- active=进行中, claimed=已认领, closed=已关闭
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    reward          integer DEFAULT 0 CHECK (reward >= 0),
    contact         text DEFAULT '站内私信联系',
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_lost_found_items_updated_at ON public.lost_found_items;
CREATE TRIGGER trg_lost_found_items_updated_at
    BEFORE UPDATE ON public.lost_found_items
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_lost_found_items_school_id ON public.lost_found_items(school_id);
CREATE INDEX IF NOT EXISTS idx_lost_found_items_publisher_id ON public.lost_found_items(publisher_id);
CREATE INDEX IF NOT EXISTS idx_lost_found_items_type ON public.lost_found_items(type);
CREATE INDEX IF NOT EXISTS idx_lost_found_items_status ON public.lost_found_items(status);
CREATE INDEX IF NOT EXISTS idx_lost_found_items_category ON public.lost_found_items(category);
CREATE INDEX IF NOT EXISTS idx_lost_found_items_school_type ON public.lost_found_items(school_id, type);
CREATE INDEX IF NOT EXISTS idx_lost_found_items_created_at ON public.lost_found_items(created_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 2. lost_found_claims — 失物认领
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.lost_found_claims (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id           uuid NOT NULL REFERENCES public.lost_found_items(id) ON DELETE CASCADE,
    claimant_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    proof_description text,
    status            text NOT NULL DEFAULT 'pending'
                      CHECK (status IN ('pending', 'approved', 'rejected')),
    -- pending=待确认, approved=已通过, rejected=已拒绝
    school_id         uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    resolved_at       timestamptz,
    created_at        timestamptz DEFAULT now(),
    updated_at        timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_lost_found_claims_updated_at ON public.lost_found_claims;
CREATE TRIGGER trg_lost_found_claims_updated_at
    BEFORE UPDATE ON public.lost_found_claims
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_lost_found_claims_school_id ON public.lost_found_claims(school_id);
CREATE INDEX IF NOT EXISTS idx_lost_found_claims_item_id ON public.lost_found_claims(item_id);
CREATE INDEX IF NOT EXISTS idx_lost_found_claims_claimant_id ON public.lost_found_claims(claimant_id);

-- 每人每物只能认领一次
CREATE UNIQUE INDEX IF NOT EXISTS uq_lost_found_claims_item_claimant
    ON public.lost_found_claims(item_id, claimant_id);

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── lost_found_items ──────────────────────────────────────

ALTER TABLE public.lost_found_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS lost_found_items_select_policy ON public.lost_found_items;
CREATE POLICY lost_found_items_select_policy ON public.lost_found_items
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS lost_found_items_agent_select_policy ON public.lost_found_items;
CREATE POLICY lost_found_items_agent_select_policy ON public.lost_found_items
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

DROP POLICY IF EXISTS lost_found_items_insert_policy ON public.lost_found_items;
CREATE POLICY lost_found_items_insert_policy ON public.lost_found_items
    FOR INSERT
    TO authenticated
    WITH CHECK (
        publisher_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS lost_found_items_update_policy ON public.lost_found_items;
CREATE POLICY lost_found_items_update_policy ON public.lost_found_items
    FOR UPDATE
    TO authenticated
    USING (
        publisher_id = auth.uid()
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        publisher_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS lost_found_items_agent_update_policy ON public.lost_found_items;
CREATE POLICY lost_found_items_agent_update_policy ON public.lost_found_items
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS lost_found_items_delete_policy ON public.lost_found_items;
CREATE POLICY lost_found_items_delete_policy ON public.lost_found_items
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── lost_found_claims ─────────────────────────────────────

ALTER TABLE public.lost_found_claims ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS lost_found_claims_select_policy ON public.lost_found_claims;
CREATE POLICY lost_found_claims_select_policy ON public.lost_found_claims
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS lost_found_claims_agent_select_policy ON public.lost_found_claims;
CREATE POLICY lost_found_claims_agent_select_policy ON public.lost_found_claims
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

DROP POLICY IF EXISTS lost_found_claims_insert_policy ON public.lost_found_claims;
CREATE POLICY lost_found_claims_insert_policy ON public.lost_found_claims
    FOR INSERT
    TO authenticated
    WITH CHECK (
        claimant_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

-- UPDATE: 认领人可更新自己的认领（仅 pending 状态）；
--         物品发布者可批准/拒绝认领；Agent 可更新所有
DROP POLICY IF EXISTS lost_found_claims_update_policy ON public.lost_found_claims;
CREATE POLICY lost_found_claims_update_policy ON public.lost_found_claims
    FOR UPDATE
    TO authenticated
    USING (
        (
            claimant_id = auth.uid()
            OR EXISTS (
                SELECT 1 FROM public.lost_found_items
                WHERE id = lost_found_claims.item_id
                AND publisher_id = auth.uid()
            )
        )
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        (
            claimant_id = auth.uid()
            OR EXISTS (
                SELECT 1 FROM public.lost_found_items
                WHERE id = lost_found_claims.item_id
                AND publisher_id = auth.uid()
            )
        )
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS lost_found_claims_agent_update_policy ON public.lost_found_claims;
CREATE POLICY lost_found_claims_agent_update_policy ON public.lost_found_claims
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS lost_found_claims_delete_policy ON public.lost_found_claims;
CREATE POLICY lost_found_claims_delete_policy ON public.lost_found_claims
    FOR DELETE
    TO authenticated
    USING (public.is_agent());
