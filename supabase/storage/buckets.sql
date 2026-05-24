-- ═══════════════════════════════════════════════════════════
-- Storage: Buckets + RLS Policies
-- 文件: supabase/storage/buckets.sql
-- 描述: 创建 4 个 storage bucket + 完整 RLS 策略
--
-- Buckets:
--   lost-found-images  — 失物招领物品图片
--   community-images   — 社区帖子配图
--   chat-images        — 即时聊天图片
--   avatars            — 用户头像
--
-- 路径规范: {school_id}/{resource_id}_{random6}.{ext}
--   school_id   — UUID，第一段目录，用于 RLS 学校隔离
--   resource_id — 业务资源 UUID（如 post_id / message_id / user_id）
--   random6     — 6 位随机字符串，防止文件名碰撞
--   ext         — 文件扩展名（jpg / png / webp / gif）
--
-- 安全模型:
--   SELECT   — 同校用户可读本校目录下所有文件（school_id 隔离）
--   INSERT   — 用户只能上传到本校目录（school_id 隔离），owner 自动为 auth.uid()
--   UPDATE   — 仅文件 owner 或 Agent 可更新元数据
--   DELETE   — 仅文件 owner 或 Agent 可删除
--
-- 依赖:
--   public.get_user_school_id() — 已由 migration 00003 创建
--   public.is_agent()           — 已由 migration 00003 创建
--
-- 执行方式: 在 Supabase Dashboard → SQL Editor 中打开本文件，
--           选中全部内容后点击 Run 执行。
-- ═══════════════════════════════════════════════════════════

-- ============================================================
-- 1. 创建 Buckets
-- ============================================================
-- 使用 private bucket（public = false），所有文件访问必须经过
-- RLS 策略鉴权，确保学校隔离不可绕过。
--
-- 文件大小限制:
--   lost-found-images  10 MB  — 物品照片，需清晰可辨
--   community-images   10 MB  — 帖子配图，支持多图
--   chat-images        10 MB  — 聊天图片，即时分享
--   avatars             2 MB  — 头像，小尺寸即可
--
-- MIME 类型白名单: 仅允许常见图片格式，禁止 SVG（防 XSS）
-- ============================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
    (
        'lost-found-images',
        'lost-found-images',
        false,
        10485760,  -- 10 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/gif']
    ),
    (
        'community-images',
        'community-images',
        false,
        10485760,  -- 10 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/gif']
    ),
    (
        'chat-images',
        'chat-images',
        false,
        10485760,  -- 10 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/gif']
    ),
    (
        'avatars',
        'avatars',
        false,
        2097152,  -- 2 MB
        ARRAY['image/jpeg', 'image/png', 'image/webp']
    )
ON CONFLICT (id) DO UPDATE SET
    public             = EXCLUDED.public,
    file_size_limit    = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;


-- ============================================================
-- 2. 启用 storage.objects 行级安全
-- ============================================================
-- storage.objects 记录每个文件的元数据（路径、owner、大小等）。
-- 启用 RLS 后，所有对该表的访问都必须匹配至少一条 policy。
-- ============================================================

ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;


-- ============================================================
-- 3. RLS Policies — 失物招领图片 (lost-found-images)
-- ============================================================
-- 场景: 用户发布失物/招领信息时上传物品照片，同校用户浏览时查看。
-- 隔离粒度: school_id（路径第一段）。
-- ============================================================

-- 3.1.1 SELECT — 同校用户可读取本校失物招领图片
--       用途: 失物招领列表/详情页加载图片，同校互看。
--       校验: 文件路径第一段（school_id）必须等于当前用户的 school_id。
DROP POLICY IF EXISTS "lost_found_select_school" ON storage.objects;
CREATE POLICY "lost_found_select_school" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'lost-found-images'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 3.1.2 INSERT — 用户只能上传到本校失物招领目录
--       用途: 发布失物/招领信息时上传图片。
--       校验: 上传路径第一段必须等于当前用户的 school_id，
--             且路径至少包含一层目录（禁止上传到根目录）。
--       owner 由 Supabase Storage 自动设置为 auth.uid()。
DROP POLICY IF EXISTS "lost_found_insert_school" ON storage.objects;
CREATE POLICY "lost_found_insert_school" ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'lost-found-images'
        AND name LIKE '%/%'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 3.1.3 UPDATE — 仅文件 owner 或 Agent 可更新
--       用途: 极少使用（通常文件不可变），保留给 Agent 审核场景。
--       校验: auth.uid() 等于文件 owner，或当前用户是 Agent。
DROP POLICY IF EXISTS "lost_found_update_owner_agent" ON storage.objects;
CREATE POLICY "lost_found_update_owner_agent" ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'lost-found-images'
        AND (auth.uid() = owner OR public.is_agent())
    )
    WITH CHECK (
        bucket_id = 'lost-found-images'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
        AND (auth.uid() = owner OR public.is_agent())
    );

-- 3.1.4 DELETE — 仅文件 owner 或 Agent 可删除
--       用途: 用户删除自己发布的失物招领图片，或 Agent 清理违规内容。
--       校验: auth.uid() 等于文件 owner，或当前用户是 Agent。
DROP POLICY IF EXISTS "lost_found_delete_owner_agent" ON storage.objects;
CREATE POLICY "lost_found_delete_owner_agent" ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'lost-found-images'
        AND (auth.uid() = owner OR public.is_agent())
    );


-- ============================================================
-- 4. RLS Policies — 社区帖子图片 (community-images)
-- ============================================================
-- 场景: 用户发布社区帖子时上传配图，同校用户浏览帖子时查看。
-- 隔离粒度: school_id（路径第一段）。
-- ============================================================

-- 4.1.1 SELECT — 同校用户可读取本校社区图片
--       用途: 社区帖子列表/详情页加载图片，同校互看。
--       校验: 文件路径第一段（school_id）必须等于当前用户的 school_id。
DROP POLICY IF EXISTS "community_select_school" ON storage.objects;
CREATE POLICY "community_select_school" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'community-images'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 4.1.2 INSERT — 用户只能上传到本校社区目录
--       用途: 发布社区帖子时上传配图。
--       校验: 上传路径第一段必须等于当前用户的 school_id。
DROP POLICY IF EXISTS "community_insert_school" ON storage.objects;
CREATE POLICY "community_insert_school" ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'community-images'
        AND name LIKE '%/%'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 4.1.3 UPDATE — 仅文件 owner 或 Agent 可更新
--       用途: Agent 审核场景（如替换违规图片为占位图）。
DROP POLICY IF EXISTS "community_update_owner_agent" ON storage.objects;
CREATE POLICY "community_update_owner_agent" ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'community-images'
        AND (auth.uid() = owner OR public.is_agent())
    )
    WITH CHECK (
        bucket_id = 'community-images'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
        AND (auth.uid() = owner OR public.is_agent())
    );

-- 4.1.4 DELETE — 仅文件 owner 或 Agent 可删除
--       用途: 用户删除自己的帖子配图，或 Agent 清理违规内容。
DROP POLICY IF EXISTS "community_delete_owner_agent" ON storage.objects;
CREATE POLICY "community_delete_owner_agent" ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'community-images'
        AND (auth.uid() = owner OR public.is_agent())
    );


-- ============================================================
-- 5. RLS Policies — 聊天图片 (chat-images)
-- ============================================================
-- 场景: 即时聊天中发送图片，同校聊天参与者查看。
-- 隔离粒度: school_id（路径第一段）。
-- ============================================================

-- 5.1.1 SELECT — 同校用户可读取本校聊天图片
--       用途: 聊天界面加载历史图片消息。
--       校验: 文件路径第一段（school_id）必须等于当前用户的 school_id。
DROP POLICY IF EXISTS "chat_select_school" ON storage.objects;
CREATE POLICY "chat_select_school" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'chat-images'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 5.1.2 INSERT — 用户只能上传到本校聊天目录
--       用途: 聊天中发送图片消息。
--       校验: 上传路径第一段必须等于当前用户的 school_id。
DROP POLICY IF EXISTS "chat_insert_school" ON storage.objects;
CREATE POLICY "chat_insert_school" ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'chat-images'
        AND name LIKE '%/%'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 5.1.3 UPDATE — 仅文件 owner 或 Agent 可更新
--       用途: 极少使用，保留给 Agent 审核场景。
DROP POLICY IF EXISTS "chat_update_owner_agent" ON storage.objects;
CREATE POLICY "chat_update_owner_agent" ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'chat-images'
        AND (auth.uid() = owner OR public.is_agent())
    )
    WITH CHECK (
        bucket_id = 'chat-images'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
        AND (auth.uid() = owner OR public.is_agent())
    );

-- 5.1.4 DELETE — 仅文件 owner 或 Agent 可删除
--       用途: 用户删除自己发送的聊天图片，或 Agent 清理违规内容。
DROP POLICY IF EXISTS "chat_delete_owner_agent" ON storage.objects;
CREATE POLICY "chat_delete_owner_agent" ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'chat-images'
        AND (auth.uid() = owner OR public.is_agent())
    );


-- ============================================================
-- 6. RLS Policies — 用户头像 (avatars)
-- ============================================================
-- 场景: 用户上传/更换头像，同校用户在各处看到头像。
-- 隔离粒度: school_id（路径第一段）。
-- ============================================================

-- 6.1.1 SELECT — 同校用户可读取本校用户头像
--       用途: 帖子列表、聊天、跑腿订单等场景展示用户头像。
--       校验: 文件路径第一段（school_id）必须等于当前用户的 school_id。
--       注意: 头像路径格式为 {school_id}/{user_id}_{random6}.{ext}，
--             user_id 不用于 RLS 校验，仅 school_id 做隔离。
DROP POLICY IF EXISTS "avatars_select_school" ON storage.objects;
CREATE POLICY "avatars_select_school" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'avatars'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 6.1.2 INSERT — 用户只能上传到本校头像目录
--       用途: 注册后首次设置头像或后续更换头像。
--       校验: 上传路径第一段必须等于当前用户的 school_id。
DROP POLICY IF EXISTS "avatars_insert_school" ON storage.objects;
CREATE POLICY "avatars_insert_school" ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'avatars'
        AND name LIKE '%/%'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
    );

-- 6.1.3 UPDATE — 仅文件 owner 或 Agent 可更新
--       用途: 用户更换自己的头像（覆盖旧文件），或 Agent 替换违规头像。
DROP POLICY IF EXISTS "avatars_update_owner_agent" ON storage.objects;
CREATE POLICY "avatars_update_owner_agent" ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'avatars'
        AND (auth.uid() = owner OR public.is_agent())
    )
    WITH CHECK (
        bucket_id = 'avatars'
        AND split_part(name, '/', 1) = public.get_user_school_id()::text
        AND (auth.uid() = owner OR public.is_agent())
    );

-- 6.1.4 DELETE — 仅文件 owner 或 Agent 可删除
--       用途: 用户删除自己的旧头像，或 Agent 清理违规头像。
DROP POLICY IF EXISTS "avatars_delete_owner_agent" ON storage.objects;
CREATE POLICY "avatars_delete_owner_agent" ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'avatars'
        AND (auth.uid() = owner OR public.is_agent())
    );


-- ============================================================
-- 7. Agent 跨校 SELECT 策略（覆盖全部 4 个 bucket）
-- ============================================================
-- Agent 作为平台管理者，需要能够查看所有学校的文件以执行
-- 内容审核、违规处理等管理操作。此策略为每个 bucket 追加一条
-- Agent 专用的 SELECT 规则，不受 school_id 限制。
-- ============================================================

-- 7.1 Agent 可读取全部失物招领图片
DROP POLICY IF EXISTS "lost_found_select_agent" ON storage.objects;
CREATE POLICY "lost_found_select_agent" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'lost-found-images'
        AND public.is_agent()
    );

-- 7.2 Agent 可读取全部社区图片
DROP POLICY IF EXISTS "community_select_agent" ON storage.objects;
CREATE POLICY "community_select_agent" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'community-images'
        AND public.is_agent()
    );

-- 7.3 Agent 可读取全部聊天图片
DROP POLICY IF EXISTS "chat_select_agent" ON storage.objects;
CREATE POLICY "chat_select_agent" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'chat-images'
        AND public.is_agent()
    );

-- 7.4 Agent 可读取全部头像
DROP POLICY IF EXISTS "avatars_select_agent" ON storage.objects;
CREATE POLICY "avatars_select_agent" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'avatars'
        AND public.is_agent()
    );


-- ═══════════════════════════════════════════════════════════
-- 策略总结
-- ═══════════════════════════════════════════════════════════
--
-- 每 bucket 共 5 条 policy:
--   1. SELECT (school)   — 同校用户读取本校文件
--   2. SELECT (agent)    — Agent 读取所有学校文件
--   3. INSERT (school)   — 用户上传到本校目录
--   4. UPDATE (owner)    — owner 或 Agent 更新
--   5. DELETE (owner)    — owner 或 Agent 删除
--
-- 4 buckets × 5 policies = 20 policies total
--
-- 安全保证:
--   - 所有 bucket 为 private（public = false），必须认证
--   - school_id 从文件路径第一段解析，不可伪造
--   - INSERT 双重校验：路径前缀 + name LIKE '%/%'（禁止根目录上传）
--   - UPDATE/DELETE 双重保护：owner 匹配 OR Agent 角色
--   - 无 anon/public 角色策略，游客无法访问任何文件
-- ═══════════════════════════════════════════════════════════
