-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000007_market_module
-- 描述: 创建二手交易模块 3 张表 — market_listings / market_orders
--       / user_favorites
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- 1. market_listings — 二手商品
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.market_listings (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title           text NOT NULL,
    description     text,
    price           integer NOT NULL DEFAULT 0 CHECK (price >= 0),
    original_price  integer CHECK (original_price >= 0),
    images          jsonb DEFAULT '[]'::jsonb,
    category        text NOT NULL,
    condition       text NOT NULL CHECK (condition IN ('brand_new', 'like_new', 'good', 'fair', 'poor')),
    -- brand_new=全新, like_new=几乎全新, good=良好, fair=一般, poor=较差
    status          text NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active', 'reserved', 'sold', 'cancelled')),
    -- active=在售, reserved=已预订, sold=已售出, cancelled=已取消
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    is_bargain      boolean DEFAULT true,
    contact         text DEFAULT '站内私信联系',
    meetup_location text,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_market_listings_updated_at ON public.market_listings;
CREATE TRIGGER trg_market_listings_updated_at
    BEFORE UPDATE ON public.market_listings
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_market_listings_school_id ON public.market_listings(school_id);
CREATE INDEX IF NOT EXISTS idx_market_listings_seller_id ON public.market_listings(seller_id);
CREATE INDEX IF NOT EXISTS idx_market_listings_status ON public.market_listings(status);
CREATE INDEX IF NOT EXISTS idx_market_listings_category ON public.market_listings(category);
CREATE INDEX IF NOT EXISTS idx_market_listings_school_status ON public.market_listings(school_id, status);
CREATE INDEX IF NOT EXISTS idx_market_listings_created_at ON public.market_listings(created_at DESC);

-- ═══════════════════════════════════════════════════════════
-- 2. market_orders — 二手交易订单
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.market_orders (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      uuid NOT NULL REFERENCES public.market_listings(id) ON DELETE CASCADE,
    buyer_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    seller_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status          text NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'accepted', 'completed', 'cancelled')),
    -- pending=待确认, accepted=已接受, completed=已完成, cancelled=已取消
    meetup_location text,
    completed_at    timestamptz,
    school_id       uuid NOT NULL REFERENCES public.schools(id) ON DELETE RESTRICT,
    created_at      timestamptz DEFAULT now(),
    updated_at      timestamptz DEFAULT now()
);

-- trigger: updated_at
DROP TRIGGER IF EXISTS trg_market_orders_updated_at ON public.market_orders;
CREATE TRIGGER trg_market_orders_updated_at
    BEFORE UPDATE ON public.market_orders
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- indexes
CREATE INDEX IF NOT EXISTS idx_market_orders_school_id ON public.market_orders(school_id);
CREATE INDEX IF NOT EXISTS idx_market_orders_listing_id ON public.market_orders(listing_id);
CREATE INDEX IF NOT EXISTS idx_market_orders_buyer_id ON public.market_orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_market_orders_seller_id ON public.market_orders(seller_id);
CREATE INDEX IF NOT EXISTS idx_market_orders_status ON public.market_orders(status);

-- ═══════════════════════════════════════════════════════════
-- 3. user_favorites — 用户收藏
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.user_favorites (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    target_type     text NOT NULL CHECK (target_type IN ('runner_task', 'market_listing', 'lost_found', 'community_post')),
    -- runner_task=跑腿任务, market_listing=二手商品, lost_found=失物招领, community_post=社区帖子
    target_id       uuid NOT NULL,
    created_at      timestamptz DEFAULT now(),
    UNIQUE(user_id, target_type, target_id)
);

-- indexes（UNIQUE 约束已自动创建 user_id + target_type + target_id 复合索引）
CREATE INDEX IF NOT EXISTS idx_user_favorites_user_id ON public.user_favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_user_favorites_target ON public.user_favorites(target_type, target_id);

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

-- ── market_listings ───────────────────────────────────────

ALTER TABLE public.market_listings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS market_listings_select_policy ON public.market_listings;
CREATE POLICY market_listings_select_policy ON public.market_listings
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS market_listings_agent_select_policy ON public.market_listings;
CREATE POLICY market_listings_agent_select_policy ON public.market_listings
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

DROP POLICY IF EXISTS market_listings_insert_policy ON public.market_listings;
CREATE POLICY market_listings_insert_policy ON public.market_listings
    FOR INSERT
    TO authenticated
    WITH CHECK (
        seller_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS market_listings_update_policy ON public.market_listings;
CREATE POLICY market_listings_update_policy ON public.market_listings
    FOR UPDATE
    TO authenticated
    USING (
        seller_id = auth.uid()
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        seller_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS market_listings_agent_update_policy ON public.market_listings;
CREATE POLICY market_listings_agent_update_policy ON public.market_listings
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS market_listings_delete_policy ON public.market_listings;
CREATE POLICY market_listings_delete_policy ON public.market_listings
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── market_orders ─────────────────────────────────────────

ALTER TABLE public.market_orders ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS market_orders_select_policy ON public.market_orders;
CREATE POLICY market_orders_select_policy ON public.market_orders
    FOR SELECT
    TO authenticated
    USING (
        public.get_user_school_id() = school_id
    );

DROP POLICY IF EXISTS market_orders_agent_select_policy ON public.market_orders;
CREATE POLICY market_orders_agent_select_policy ON public.market_orders
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

DROP POLICY IF EXISTS market_orders_insert_policy ON public.market_orders;
CREATE POLICY market_orders_insert_policy ON public.market_orders
    FOR INSERT
    TO authenticated
    WITH CHECK (
        buyer_id = auth.uid()
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS market_orders_update_policy ON public.market_orders;
CREATE POLICY market_orders_update_policy ON public.market_orders
    FOR UPDATE
    TO authenticated
    USING (
        (buyer_id = auth.uid() OR seller_id = auth.uid())
        AND school_id = public.get_user_school_id()
    )
    WITH CHECK (
        (buyer_id = auth.uid() OR seller_id = auth.uid())
        AND school_id = public.get_user_school_id()
    );

DROP POLICY IF EXISTS market_orders_agent_update_policy ON public.market_orders;
CREATE POLICY market_orders_agent_update_policy ON public.market_orders
    FOR UPDATE
    TO authenticated
    USING (public.is_agent())
    WITH CHECK (public.is_agent());

DROP POLICY IF EXISTS market_orders_delete_policy ON public.market_orders;
CREATE POLICY market_orders_delete_policy ON public.market_orders
    FOR DELETE
    TO authenticated
    USING (public.is_agent());

-- ── user_favorites ────────────────────────────────────────
-- 收藏为个人数据，仅用户本人可见/可操作

ALTER TABLE public.user_favorites ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_favorites_select_policy ON public.user_favorites;
CREATE POLICY user_favorites_select_policy ON public.user_favorites
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS user_favorites_insert_policy ON public.user_favorites;
CREATE POLICY user_favorites_insert_policy ON public.user_favorites
    FOR INSERT
    TO authenticated
    WITH CHECK (user_id = auth.uid());

-- Agent 可查看所有收藏（客服排查）
DROP POLICY IF EXISTS user_favorites_agent_select_policy ON public.user_favorites;
CREATE POLICY user_favorites_agent_select_policy ON public.user_favorites
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- user_favorites 无 UPDATE policy（收藏记录无需修改，只有新增/删除）

DROP POLICY IF EXISTS user_favorites_delete_policy ON public.user_favorites;
CREATE POLICY user_favorites_delete_policy ON public.user_favorites
    FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());
