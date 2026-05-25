-- 回滚 Migration 20260525000020_favorite_count
-- 描述: 删除 favorite_count 列、trigger 函数及 trigger

-- 1. 删除 trigger
DROP TRIGGER IF EXISTS trg_user_favorites_insert_count ON public.user_favorites;
DROP TRIGGER IF EXISTS trg_user_favorites_delete_count ON public.user_favorites;

-- 2. 删除 trigger 函数
DROP FUNCTION IF EXISTS public.trg_user_favorites_insert_count();
DROP FUNCTION IF EXISTS public.trg_user_favorites_delete_count();

-- 3. 删除列
ALTER TABLE public.market_listings
    DROP COLUMN IF EXISTS favorite_count;
