-- ═══════════════════════════════════════════════════════════
-- Migration: 20260522000010_notifications
-- 描述: 创建通知模块 1 张表 — notifications
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
--
-- 说明: 通知为个人数据，按 user_id 隔离，不包含 school_id。
--       系统通知（如订单状态变更、售后提醒）由 Edge Function
--       或后端服务以 service_role 写入。
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
-- notifications — 用户通知
-- ═══════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS public.notifications (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type            text NOT NULL CHECK (type IN ('order_status','review','system','chat','after_sale')),
    -- 通知类型：order_status=订单状态, review=评价, system=系统, chat=聊天, after_sale=售后
    title           text NOT NULL,
    body            text,
    ref_type        text,
    -- 关联业务类型，如 'runner_order', 'market_order', 'community_post' 等
    ref_id          uuid,
    -- 关联业务 ID
    is_read         boolean DEFAULT false,
    read_at         timestamptz,
    created_at      timestamptz DEFAULT now()
);

-- indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON public.notifications(user_id, is_read)
    WHERE is_read = false;
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_ref ON public.notifications(ref_type, ref_id);

-- ═══════════════════════════════════════════════════════════
-- RLS Policies
-- ═══════════════════════════════════════════════════════════

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- SELECT: 用户只能看自己的通知
DROP POLICY IF EXISTS notifications_select_policy ON public.notifications;
CREATE POLICY notifications_select_policy ON public.notifications
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- INSERT: 用户本人或 Agent 可插入（Agent 用于系统通知推送）
DROP POLICY IF EXISTS notifications_insert_policy ON public.notifications;
CREATE POLICY notifications_insert_policy ON public.notifications
    FOR INSERT
    TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        OR public.is_agent()
    );
-- 注意：跨用户系统通知（如提醒跑腿接单）由 Edge Function 使用 service_role 写入，
--      不依赖此 client-policy。

-- UPDATE: 用户可标记自己的通知为已读
DROP POLICY IF EXISTS notifications_update_policy ON public.notifications;
CREATE POLICY notifications_update_policy ON public.notifications
    FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- DELETE: 用户可删除自己的通知
DROP POLICY IF EXISTS notifications_delete_policy ON public.notifications;
CREATE POLICY notifications_delete_policy ON public.notifications
    FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

-- SELECT: Agent 可查看所有通知
DROP POLICY IF EXISTS notifications_agent_select_policy ON public.notifications;
CREATE POLICY notifications_agent_select_policy ON public.notifications
    FOR SELECT
    TO authenticated
    USING (public.is_agent());

-- DELETE: Agent 可删除所有通知
DROP POLICY IF EXISTS notifications_agent_delete_policy ON public.notifications;
CREATE POLICY notifications_agent_delete_policy ON public.notifications
    FOR DELETE
    TO authenticated
    USING (public.is_agent());
