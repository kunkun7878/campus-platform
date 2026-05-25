-- ═══════════════════════════════════════════════════════════
-- Migration: 20260525000019_reward_expiry
-- 描述: lost_found_items 添加 reward_frozen_at 字段 +
--       悬赏金过期索引，配合 reward-expiry Edge Function
--       自动解冻退还超过 30 天未处理的悬赏金。
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- 1. 添加 reward_frozen_at 字段
ALTER TABLE public.lost_found_items
    ADD COLUMN IF NOT EXISTS reward_frozen_at timestamptz;

COMMENT ON COLUMN public.lost_found_items.reward_frozen_at IS
    '悬赏金冻结时间，用于定时任务扫描超过 30 天未处理的物品自动解冻退款';

-- 2. 索引：加速定时任务扫描过期悬赏
CREATE INDEX IF NOT EXISTS idx_lost_found_items_reward_expiry
    ON public.lost_found_items(reward_frozen_at)
    WHERE status = 'active' AND reward > 0;
