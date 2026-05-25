# Supabase 数据库部署指南 — 校园聚合平台

> 适用：campus-platform 项目（ap-southeast-1）
> Supabase URL：`https://fzmdhllxzyyzfpxkqpdy.supabase.co`

---

## 前置准备

### 1. 登录 Supabase Dashboard

浏览器打开：https://supabase.com/dashboard

选择项目：**campus-platform**

### 2. 获取 Supabase CLI（仅 Edge Function 部署需要）

```bash
# 安装 Supabase CLI（Windows）
scoop bucket add supabase https://github.com/supabase/scoop-bucket.git
scoop install supabase

# 登录
supabase login
# 会打开浏览器，选择 Supabase 账号登录

# 链接到项目
cd "C:\Users\admin\Desktop\校园聚合平台"
supabase link --project-ref fzmdhllxzyyzfpxkqpdy
```

> 如果不想装 CLI，Edge Function 也可以在 Dashboard 手动创建（见步骤四-B），但 CLI 更方便。

---

## 步骤一：执行数据库 Migration（38张表 + RLS + 种子数据）

### 1.1 打开 SQL Editor

Supabase Dashboard → 左侧菜单 → **SQL Editor** → 点击 **New query**

### 1.2 逐文件执行（必须按编号顺序！）

Migration 文件位于：`C:\Users\admin\Desktop\校园聚合平台\supabase\migrations\`

**每执行一个文件后，确认左下角显示 "Success. No rows returned" 再继续下一个。**

| 顺序 | 文件名 | 创建内容 | 预计耗时 |
|:--:|------|------|:--:|
| 1 | `20260522000000_create_profiles.sql` | profiles表 + trigger | <1秒 |
| 2 | `20260522000001_create_schools_campuses.sql` | schools + campuses表 | <1秒 |
| 3 | `20260522000002_seed_data.sql` | 种子数据（2学校4校区） | <1秒 |
| 4 | `20260522000003_rls_policies.sql` | RLS基础策略 + helper函数 | <1秒 |
| 5 | `20260522000004_wechat_identities.sql` | wechat_identities表 | <1秒 |
| 6 | `20260522000005_add_indexes.sql` | 索引 | <1秒 |
| 7 | `20260522000006_runner_module.sql` | runner_tasks/orders/reviews + user_addresses | <1秒 |
| 8 | `20260522000007_market_module.sql` | market_listings/orders + user_favorites | <1秒 |
| 9 | `20260522000008_lost_found_module.sql` | lost_found_items/claims | <1秒 |
| 10 | `20260522000009_community_module.sql` | community_posts/comments + official_groups | <1秒 |
| 11 | `20260522000010_notifications.sql` | notifications表 | <1秒 |
| 12 | `20260522000011_runner_after_sale_supplement.sql` | runner_applications + order_timeline + after_sales | <1秒 |
| 13 | `20260522000012_messaging_social.sql` | post_likes + conversations + messages + group_messages/members | <1秒 |
| 14 | `20260522000013_wallet_system.sql` | wallets + wallet_transactions + announcements + coupons + user_coupons | <1秒 |
| 15 | `20260522000014_misc_alter_profiles.sql` | feedbacks + invite_codes/records + login_codes + attachments + profiles扩展 | <1秒 |
| 16 | `20260523000015_fix_runner_rls.sql` | 修复4处RLS漏洞 | <1秒 |
| 17 | `20260523000016_harden_market_orders_rls.sql` | 加固market_orders INSERT RLS | <1秒 |
| 18 | `20260523000017_phase6_schema.sql` | **最大文件**：fcm_tokens + moderation_logs + 6 trigger + 30+ DDL变更 | 3-5秒 |

### 1.3 验证

执行完毕后，在 SQL Editor 中运行以下验证SQL：

```sql
-- 确认38张表都存在
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
-- 应该看到 38 行

-- 确认种子数据
SELECT id, name FROM schools;       -- 应显示 2 所学校
SELECT id, name, school_id FROM campuses;  -- 应显示 4 个校区

-- 确认 RLS 已启用
SELECT tablename FROM pg_tables
WHERE schemaname = 'public' AND rowsecurity = true;
-- 应该看到所有 38 张表
```

---

## 步骤二：创建 Storage Bucket（图片存储）

### 2.1 执行 Bucket SQL

SQL Editor → New query → 打开 `supabase/storage/buckets.sql` → 全选 → Run

这会创建 4 个 Bucket：

| Bucket ID | 用途 | 大小限制 | 允许格式 | 公开 |
|-----------|------|:--:|------|:--:|
| `avatars` | 用户头像 | 2MB | jpeg, png, webp | 否 |
| `community-images` | 社区帖子图片 | 10MB | jpeg, png, webp, gif | 否 |
| `chat-images` | 聊天图片 | 10MB | jpeg, png, webp, gif | 否 |
| `lost-found-images` | 失物招领图片 | 10MB | jpeg, png, webp, gif | 否 |

### 2.2 验证

在 Dashboard → 左侧菜单 → **Storage** → 确认 4 个 Bucket 出现在列表中。

---

## 步骤三：部署 Edge Function

### 方式 A：使用 Supabase CLI（推荐）

```bash
cd "C:\Users\admin\Desktop\校园聚合平台"

# 逐个部署（每个约 10-30 秒）
supabase functions deploy runner-order-lifecycle
supabase functions deploy runner-after-sale
supabase functions deploy market-purchase
supabase functions deploy lost-item-lifecycle
supabase functions deploy community-moderation
supabase functions deploy push-notification

# 也可以一次性全部部署
supabase functions deploy --no-verify-jwt
```

### 方式 B：Dashboard 手动创建（无 CLI 时）

1. Dashboard → 左侧菜单 → **Edge Functions**
2. 点击 **Create a new function**
3. 函数名填入对应名称（如 `runner-order-lifecycle`）
4. 将 `supabase/functions/<函数名>/index.ts` 的内容复制粘贴到代码编辑器
5. 点击 **Deploy**
6. 对以下 6 个函数重复上述步骤：
   - `runner-order-lifecycle`
   - `runner-after-sale`
   - `market-purchase`
   - `lost-item-lifecycle`
   - `community-moderation`
   - `push-notification`

> ⚠️ `_shared/sensitive-words.ts` 不是独立 EdgeFn，它被 `community-moderation` import。在 Dashboard 手动创建 `community-moderation` 时，需要把 `sensitive-words.ts` 的内容也一并粘贴到 index.ts 顶部（替换 `import { moderate } from "../_shared/sensitive-words.ts"` 行）。

### 3.1 验证

部署完成后，在终端用 curl 测试（以 runner-order-lifecycle 为例）：

```bash
curl -X POST "https://fzmdhllxzyyzfpxkqpdy.supabase.co/functions/v1/runner-order-lifecycle" \
  -H "Authorization: Bearer <ANON_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"action": "ping"}'
```

> 将 `<ANON_KEY>` 替换为：`sb_publishable_PP67H5XKBPHfuEEe-i3IkA_BUgWfG6B`（见 `android/local.properties:12`）

---

## 步骤四：配置环境变量

### 4.1 需要配置的变量

Dashboard → 左侧菜单 → **Project Settings** → **Edge Functions** → 找到 Secrets 区域

| 变量名 | 值 | 说明 |
|--------|---|------|
| `FIREBASE_SERVICE_ACCOUNT` | 服务帐号JSON（整段粘贴） | FCM推送用。注意：变量名是 `FIREBASE_SERVICE_ACCOUNT`，不是 `FCM_SERVICE_ACCOUNT_JSON` |
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AccessKey ID | 云AI内容审核用 |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret | 云AI内容审核用 |

### 4.2 验证

Dashboard → Settings → API → 确认 **service_role key** 存在（以 `eyJ` 开头）。这是 Supabase 自动注入 EdgeFn 的，不需要手动配置。

---

## 步骤五：配置 Android 端

### 5.1 确认 local.properties

文件：`C:\Users\admin\Desktop\校园聚合平台\android\local.properties`

应包含（Phase 2 已配置，核对即可）：

```properties
SUPABASE_URL=https://fzmdhllxzyyzfpxkqpdy.supabase.co
SUPABASE_ANON_KEY=sb_publishable_PP67H5XKBPHfuEEe-i3IkA_BUgWfG6B
```

### 5.2 确认 FCM 配置文件

文件：`C:\Users\admin\Desktop\校园聚合平台\android\app\google-services.json`

Phase 6 已配置，核对文件存在即可。

---

## 步骤六：全环境验证

### 6.1 数据库验证 SQL

```sql
-- 所有表存在
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'public';
-- 预期：38

-- RLS 全部启用
SELECT COUNT(*) AS rls_count FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = true;
-- 预期：38

-- 种子数据正确
SELECT 'schools' AS tbl, COUNT(*) AS cnt FROM schools
UNION ALL SELECT 'campuses', COUNT(*) FROM campuses;
-- 预期：schools=2, campuses=4
```

### 6.2 Edge Function 验证

```bash
# 测试每个 EdgeFn（预期返回 JSON，不含 error）
curl -s "https://fzmdhllxzyyzfpxkqpdy.supabase.co/functions/v1/runner-order-lifecycle" \
  -H "Authorization: Bearer sb_publishable_PP67H5XKBPHfuEEe-i3IkA_BUgWfG6B" \
  -d '{}' | head -50
```

### 6.3 App 端验证

```bash
cd "C:\Users\admin\Desktop\校园聚合平台\android"
./gradlew assembleDebug
# 预期：BUILD SUCCESSFUL

# 安装到模拟器/真机
./gradlew installDebug
```

打开 App → 注册新用户 → 选学校 → 进入首页 → 确认底部5个Tab正常显示。

---

## 常见问题 FAQ

### Q1：执行 Migration 时报错 "relation already exists"
**原因**：之前执行过部分 Migration。
**解决**：跳过该文件，继续下一个。或在 SQL Editor 中先执行 `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` 清空重建。

### Q2：Edge Function 部署报错 "Function already exists"
**解决**：加 `--no-verify-jwt` 标志重新部署。

### Q3：curl 测试 EdgeFn 返回 401
**原因**：anon key 错误或过期。
**解决**：Dashboard → Settings → API → 复制 anon/public key，确认是 `sb_publishable_` 开头的。

### Q4：App 登录失败
**可能原因**：
1. Supabase URL 或 anon key 配置错误 → 检查 `local.properties`
2. 手机号认证未在 Supabase 中启用 → Dashboard → Authentication → Providers → Phone → 确认已启用
3. Migration 未执行 → 回到步骤一

### Q5：通知推送不工作
**可能原因**：
1. `FIREBASE_SERVICE_ACCOUNT` 环境变量名写错了（不是 `FCM_SERVICE_ACCOUNT_JSON`！）
2. 服务帐号 JSON 格式错误 → 确认是完整的 JSON 对象（含 `type`, `project_id`, `private_key` 等字段）
3. FCM 未在 Supabase Dashboard 配置 → 回到步骤四

### Q6：图片上传失败
**可能原因**：Storage Bucket 未创建 → 回到步骤二

---

## 部署检查清单

| # | 步骤 | 验证方法 | ✓ |
|:--:|------|------|:--:|
| 1 | 18个Migration全部执行 | `SELECT COUNT(*) FROM pg_tables WHERE schemaname='public'` = 38 | |
| 2 | 种子数据正确 | `SELECT * FROM schools` 显示2条 | |
| 3 | RLS全部启用 | 38张表 rowsecurity=true | |
| 4 | 4个Storage Bucket创建 | Dashboard Storage 页可见4个bucket | |
| 5 | 6个EdgeFn部署成功 | Dashboard Edge Functions 页可见6个fn | |
| 6 | FIREBASE_SERVICE_ACCOUNT 配置 | Dashboard Settings → Secrets 可见 | |
| 7 | local.properties 配置正确 | URL 和 ANON_KEY 不为空 | |
| 8 | App 编译通过 | `gradlew assembleDebug` BUILD SUCCESSFUL | |
| 9 | App 可注册/登录 | 注册新用户 → 选校 → 进入首页 | |
