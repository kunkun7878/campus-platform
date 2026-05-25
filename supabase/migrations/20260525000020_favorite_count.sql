-- ═══════════════════════════════════════════════════════════
-- Migration: 20260525000020_favorite_count
-- 描述: market_listings 添加 favorite_count 字段 +
--       user_favorites INSERT/DELETE trigger 自动维护收藏计数
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- 1. 添加 favorite_count 字段
ALTER TABLE public.market_listings
    ADD COLUMN IF NOT EXISTS favorite_count integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN public.market_listings.favorite_count IS
    '收藏数，由 user_favorites 表的 INSERT/DELETE trigger 自动维护';

-- 2. 将已有收藏数初始化（确保数据一致）
UPDATE public.market_listings ml
SET favorite_count = sub.cnt
FROM (
    SELECT target_id, COUNT(*) AS cnt
    FROM public.user_favorites
    WHERE target_type = 'market_listing'
    GROUP BY target_id
) sub
WHERE ml.id = sub.target_id
  AND ml.favorite_count IS DISTINCT FROM sub.cnt;

-- 3. trigger 函数：收藏时 +1
CREATE OR REPLACE FUNCTION public.trg_user_favorites_insert_count()
RETURNS trigger AS $$
BEGIN
    IF NEW.target_type = 'market_listing' THEN
        UPDATE public.market_listings
        SET favorite_count = favorite_count + 1
        WHERE id = NEW.target_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 4. trigger 函数：取消收藏时 -1
CREATE OR REPLACE FUNCTION public.trg_user_favorites_delete_count()
RETURNS trigger AS $$
BEGIN
    IF OLD.target_type = 'market_listing' THEN
        UPDATE public.market_listings
        SET favorite_count = GREATEST(favorite_count - 1, 0)
        WHERE id = OLD.target_id;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, pg_temp;

-- 5. 应用 INSERT trigger
DROP TRIGGER IF EXISTS trg_user_favorites_insert_count ON public.user_favorites;
CREATE TRIGGER trg_user_favorites_insert_count
    AFTER INSERT ON public.user_favorites
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_user_favorites_insert_count();

-- 6. 应用 DELETE trigger
DROP TRIGGER IF EXISTS trg_user_favorites_delete_count ON public.user_favorites;
CREATE TRIGGER trg_user_favorites_delete_count
    AFTER DELETE ON public.user_favorites
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_user_favorites_delete_count();
