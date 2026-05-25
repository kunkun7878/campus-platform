-- 回滚 Migration 20260525000019_reward_expiry
-- 描述: 删除 reward_frozen_at 列及相关索引

-- 1. 删除悬赏过期索引
DROP INDEX IF EXISTS public.idx_lost_found_items_reward_expiry;

-- 2. 删除 reward_frozen_at 列
ALTER TABLE public.lost_found_items
    DROP COLUMN IF EXISTS reward_frozen_at;
