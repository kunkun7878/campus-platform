# Phase 7 任务详单（终版）

<!-- 生成：2026-05-24 | 审查：3轮5个Agent | 总计：47个子任务 | 状态：待执行 -->

---

## 前置模块 P0：Supabase 数据库部署

> 说明：Phase 0-6 编写了完整数据库代码但从未在线上执行。部署是全部模块的前置条件。

| # | 任务 | 详细步骤 | 产出 |
|:--:|------|---------|------|
| P0-1 | 执行18个Migration | Supabase Dashboard → SQL Editor → 按 00→17 顺序逐文件执行。每个执行后确认无报错 | 38张表 + RLS策略生效 |
| P0-2 | 创建4个Storage Bucket | 执行 `supabase/storage/buckets.sql` | avatars / community-images / chat-images / lost-found-images + 20条RLS |
| P0-3 | 部署6个Edge Function | `supabase functions deploy` ×6 | runner-order-lifecycle / runner-after-sale / market-purchase / lost-item-lifecycle / community-moderation / push-notification<br>注：`_shared` 为共享库（无 `index.ts`，仅被其他 EdgeFn import），无需部署 |
| P0-4 | 配置环境变量 | Supabase Dashboard → Settings → Edge Functions：`FIREBASE_SERVICE_ACCOUNT`(FCM服务帐号JSON，push-notification 必需) / `ALIYUN_ACCESS_KEY_ID` / `ALIYUN_ACCESS_KEY_SECRET`（预留，F1 云 AI 审核使用；当前 6 个 EdgeFn 不依赖此二变量，部署时不配置不阻塞）。验证 `SUPABASE_SERVICE_ROLE_KEY` 已自动注入 | 4个env var就位 |
| P0-5 | 编写 SUPABASE_SETUP.md | 分步操作指南：Migration执行顺序 + 验证SQL(每表1条SELECT) + Bucket创建 + EdgeFn部署CLI命令 + env var配置 + CORS排查 + FAQ | 操作手册 |
| P0-6 | 验证数据库可用 | 执行验证SQL：38表SELECT count(*)确认存在 / RLS验证 / 种子数据 `SELECT * FROM schools/campuses` / EdgeFn curl测试 | 线上环境Ready |

---

## 模块 D：运行时Bug修复

> 第二轮审查发现，必须最先修，否则后续任务基于Bug代码构建

| # | 任务 | Bug描述 | 文件:行号 | 修复 |
|:--:|------|---------|----------|------|
| D1 | 售后EdgeFn缺action参数 | `AfterSaleApplyViewModel` 调用 `runner-after-sale` 时 `buildJsonObject` 缺 `"action": "create"`，EdgeFn返回400 | `AfterSaleApplyViewModel.kt` | 加一行 `put("action", "create")` |
| D2 | 市场订单操作非原子 | `MarketOrderDetailViewModel.cancelOrder/confirmComplete` 分两步更新 order+listing，第二步失败导致不一致 | `MarketOrderDetailViewModel.kt` | 改造为EdgeFn单次事务（扩展 market-purchase 加 cancel/complete action） |
| D3 | OTP验证不可用 | `AuthRepository.verifyOtp()` 硬编码 `throw UnsupportedOperationException` | `AuthRepository.kt` | SDK升级后实现正确 `verifyPhoneOtp` |

---

## 模块 E：Phase 6 Migration 同步到 Room

> 第二轮审查发现。Migration 17 新增字段从未同步到 Android Room Entity 层

| # | Entity文件 | 缺失内容 | 同步到 |
|:--:|------|------|------|
| E1 | CommunityEntities.kt | CommunityPostEntity: +`reviewReason: String?` +`STATUS_PENDING_REVIEW` +`SECTION_LOST_FOUND/SECOND_HAND/HELP/ANNOUNCEMENT`<br>CommunityCommentEntity: +`reviewReason: String?` +`STATUS_PENDING_REVIEW` | Entity + Dto + Mapper |
| E2 | LostFoundEntities.kt | LostFoundItemEntity: +`returnedAt: String?` | Entity + Dto + Mapper |
| E3 | MessageEntities.kt | ConversationEntity: +`sourceType: String?` +`sourceId: String?` +`lastMessageSenderId: String?` | Entity + Dto + Mapper |
| E4 | UserEntities.kt | NotificationEntity: +`priority: String` +`pushSent: Boolean` +`pushSentAt: String?` +`TYPE_LOST_FOUND/COMMUNITY/GROUP_CHAT` | Entity + Dto + Mapper |
| E5 | RunnerEntities.kt | OrderTimelineEntity: +`updatedAt: String?`<br>AfterSaleTimelineEntity: +`updatedAt: String?`<br>⚠️ 注：此二字段实际来自 Migration 11（Phase 4），非 Migration 17。字段仍缺失，仅修正归属描述 | Entity + Dto + Mapper |
| E6 | DI验证 | 确认 AppDatabase version 从 3 升级到 4 + 所有新Entity注册 + Mapper完整性校验。Mapper 文件清单：`CommunityMappers.kt` / `LostFoundMappers.kt` / `MessageMappers.kt` / `UserMappers.kt` / `RunnerMappers.kt` |

---

## 模块 A：7个占位屏实现

| # | 任务 | 子任务 | 数据层 | 复杂度 |
|:--:|------|--------|:--:|:--:|
| A1 | **WalletScreen** | (1) 新建 WalletTransactionEntity + DAO + Dto + Mapper (2) IUserRepository 扩展 (3) WalletViewModel（余额查询+流水列表+刷新） (4) WalletScreen UI（余额卡片+流水列表+收支汇总+充值/提现按钮标记'即将开放'） | 缺Entity | 中 |
| A2 | **AddressManageScreen** | (1) AddressManageViewModel（CRUD+默认地址） (2) AddressManageScreen UI（卡片列表+新增/编辑弹窗+设为默认） | 就绪 | 低 |
| A3 | **CouponsScreen** | (1) CouponsViewModel（Tab 未使用/已使用/已过期+领券+过期处理） (2) CouponsScreen UI（Tab切换+券卡片+操作按钮） | 就绪 | 中 |
| A4 | **InviteScreen** | (1) 新建 InviteCodeEntity + InviteRecordEntity + DAO + Dto + Mapper (2) IInviteRepository + InviteRepository (3) invite-code EdgeFn (4) InviteViewModel + Screen UI（邀请码卡片+奖励规则+分享按钮+记录列表） | 缺Entity | 高 |
| A5 | **FeedbackScreen** | (1) FeedbackViewModel（表单管理+提交） (2) FeedbackScreen UI（类型下拉+描述+联系方式+提交） | 就绪 | 低 |
| A6 | **AboutScreen** | (1) Compose静态UI（Logo+版本号+功能介绍+链接列表：用户协议/隐私政策/合作联系/代理申请占位） | 不需要 | 极低 |
| A7 | **AnnouncementDetailScreen** | (1) AnnouncementDetailViewModel (2) UI（标题+时间+内容+优先级标签）<br>⚠️ 数据层标记为"部分就绪"：Entity 存在但 `IMiscRepository` 缺少 `getAnnouncementById()` 方法，需在任务中补充 | 部分就绪 | 低 |

---

## 模块 B：Agent后台

| # | 任务 | 子任务 | 依赖 | 复杂度 |
|:--:|------|--------|:--:|:--:|
| B-R | **Agent路由注册** | (1) CampusRoutes 新增8条Agent路由 (2) CampusNavGraph 注册8个composable (3) CampusScreenConfig 加固：`currentRoute.startsWith` 精确匹配 + 硬编码 `"publish"` 改用 `CampusRoutes.Publish.route` | E1-E6 | 中 |
| B0 | **ProfileScreen重构** | (1) ProfileViewModel 从空壳填充（加载Profile+Agent状态+未读审核数） (2) ProfileScreen：用户信息卡片（头像+昵称+学校）+ "服务中心"Section（Wallet/AddressManage/Coupons/Feedback/Invite/About/RunnerApply 7个菜单项）+ "设置"Section + is_agent时显示Agent入口卡片（待审计数角标） | B-R | 中 |
| B1 | **AgentDashboardScreen** | (1) AgentDashboardViewModel (2) UI：功能卡片导航（内容审核/用户管理/公告管理/跑腿员审批/审核日志 + 计数角标）。待审计数通过 Supabase query 直查 `community_posts WHERE status='pending_review' AND school_id=$schoolId` | B-R, B0 | 低 |
| B2 | **AgentReviewListScreen** | (1) AgentReviewListViewModel（pending_review帖子/评论分页） (2) UI（Tab切换帖子/评论+列表+风险等级） (3) CommunityRepository 加 getPendingReviewPosts/getPendingReviewComments | B-R | 中 |
| B3 | **AgentReviewDetailScreen** | (1) AgentReviewDetailViewModel（内容+敏感词高亮+操作） (2) UI（内容展示+底部操作栏：通过/拒绝/隐藏+原因弹窗） (3) community-moderation/index.ts 新增 review action（Agent身份校验+学校隔离+moderation_logs写入 + `_shared/sensitive-words.ts` 中新增 `cloudModerate()` 函数骨架，调用阿里云内容安全 API 进行敏感词检测）。F1 云 AI 审核接入已并入本任务同步完成 (4) CommunityRepository 加 updatePostStatus/updateCommentStatus | B2 | 中 |
| B4 | **AgentUserListScreen** | (1) AgentUserListViewModel（本校用户列表+搜索） (2) UI（搜索框+用户列表+状态标识） | B-R | 低 |
| B5 | **AgentUserDetailScreen** | (1) AgentUserDetailViewModel（用户详情+封禁/解封） (2) UI（信息卡片+操作按钮+确认弹窗） | B4 | 低 |
| B6 | **AgentAnnouncementListScreen** | (1) AgentAnnouncementListViewModel (2) UI（列表+FAB新建+状态pill） (3) IMiscRepository 加 upsertAnnouncement/deleteAnnouncement | B-R | 低 |
| B7 | **AgentAnnouncementEditScreen** | (1) AgentAnnouncementEditViewModel (2) UI（表单：标题+内容+优先级+发布） (3) AnnouncementEntity 加 status 字段 + Migration 18: announcements 加 status 列 | B6 | 中 |
| B8 | **AgentRunnerReviewScreen** | (1) AgentRunnerReviewViewModel（申请列表+审批） (2) UI（列表+展开详情+通过/拒绝+原因） (3) IRunnerApplicationRepository 加 approve/reject | B-R | 中 |

---

## 模块 C：遗漏功能补全

| # | 任务 | 当前状态 | 修复 | 复杂度 |
|:--:|------|---------|------|:--:|
| C1 | **PublishHub解除禁用** | 失物+帖子 `isComingSoon=true` | `isComingSoon=false` + 正确onClick导航 + PublishHubViewModel填充基础状态 | 极低 |
| C2 | **CampusScreenConfig加固** | 字符串contains模糊匹配，硬编码路由 | 已并入 B-R | — |
| C3 | **AfterSaleApply图片上传** | "图片上传功能即将上线"占位 | 替换为ImagePickerButton真实上传。新建 Storage Bucket: after-sale | 中 |
| C4 | **RunnerApply身份证上传** | URL文本输入框+"图片上传即将上线" | 替换为ImagePickerButton真实上传。新建 Storage Bucket: identity | 中 |
| C5 | **PasswordResetScreen实现** | "暂不支持在线重置密码" | 手机号验证→验证码→新密码→Supabase Auth updateUser | 中 |
| C6 | **MyPublished接通跑腿/失物** | `DEVELOPING_INDICES = setOf(1, 3)` 假数据 | 接通RunnerTaskRepository+LostFoundRepository真实查询 | 中 |
| C7 | **MyFavorites接通跑腿/失物/帖子** | `DEVELOPING_INDICES = setOf(1, 3, 4)` 假数据 | 接通对应Repository收藏数据查询 | 中 |
| C8 | **GoodsDetailScreen编辑** | "编辑功能即将上线"Snackbar | 标题/描述/价格/图片编辑 + MarketRepository.updateListing | 中 |
| C9 | **AfterSaleDetail补充材料** | "补充材料功能即将上线"Toast | 补充材料上传+EdgeFn update action | 中 |
| C10 | **UserAgreementScreen + PrivacyPolicyScreen** | LoginScreen/RegisterScreen有TODO指向不存在页 | 新建2个静态文本Screen+路由+NavGraph注册+链接 | 低 |

> ⚠️ **C6/C7 UiState 架构风险**：当前 `MyPublishedViewModel` 和 `MyFavoritesViewModel` 的 UiState 泛型固定为 `MarketListingDto`。接通跑腿/失物多类型数据需要架构级改造（sealed class 或 union type 统一不同实体），复杂度可能显著高于当前「中」评估。建议提前设计 `PublishedItem` / `FavoriteItem` 密封类后再并行开发 C6/C7。

---

## 模块 F：Edge Function + Migration 收口

| # | 任务 | 子任务 | 依赖 | 复杂度 |
|:--:|------|--------|:--:|:--:|
| F1 | **云AI审核接入** | ⚠️ 已完全并入 B3 — B3 第(3)子任务同步实现 community-moderation review action + `_shared/sensitive-words.ts` 中 `cloudModerate()` 函数骨架（调用阿里云内容安全 API）。本任务不独立执行 | B3 + 阿里云AK | — |
| F2 | **悬赏金流水补全** | lost-item-lifecycle 3处钱包余额变更缺wallet_transactions INSERT：publish_item(冻结扣款) / resolve_item(转交悬赏) / close_item(退还悬赏) | P0-3 | 中 |
| F3 | **悬赏金过期退款** | (1) 新建 reward-expiry/index.ts (2) GitHub Actions cron `.github/workflows/reward-expiry-cron.yml` (3) Migration 19: lost_found_items 加 reward_frozen_at 列+索引 | F2 | 中 |
| F4 | **Invite EdgeFn** | 新建 invite-code/index.ts（邀请码生成+注册验证+关系记录），service_role | A4 + P0-3 | 低 |
| F5 | **favoriteCount** | Migration 20: market_listings 加 favorite_count 列 + user_favorites INSERT/DELETE trigger 自动更新计数。清除3处TODO注释 | P0-1 | 低 |

---

## 模块 G：验证 + 记忆同步

| # | 任务 | 子任务 | 复杂度 |
|:--:|------|--------|:--:|
| G1 | **全链路验证18条** | (1)注册→选校→首页 (2)跑腿发布→接单→完成→评价 (3)跑腿售后 (4)二手发布→购买→收货 (5)失物发布→认领→归还确认→悬赏金流转 (6)社区发帖→敏感词→Agent审核 (7)私聊→Realtime同步 (8)群聊→Realtime (9)FCM推送→deep link (10)钱包余额+流水 (11)优惠券领用 (12)邀请→注册→奖励 (13)通知中心 (14)账户删除 (15)Agent后台全流程 (16)地址管理 CRUD（验证 A2 产出） (17)公告详情展示（验证 A7 产出） (18)市场订单原子性修复验证（确认 D2 cancelOrder/confirmComplete 事务一致性） | 高 |
| G2 | **Project Memory全量同步** | 14个project_memory文件更新到Phase 7完成状态 + campus_session_log补全 + last_sync统一刷新<br>⚠️ 注：当前 8 个 memory 文件 last_sync 日期不一致（6 个在 5/24 16:00，2 个在 5/23），同步时需同时更新内容到 Phase 7 完成状态 | 中 |

---

## 执行顺序与依赖

```
P0 (Supabase部署) ────→ 全部模块的前置
                         │
Batch 1: 地基 ───────────┤
  E1→E2→E3→E4→E5→E6     │ (Room同步，需先于B-R)
         ↓               │
     B-R (路由注册)       │
         ↓               │
  D1 + D2 + D3           │ (Bug修复，互不依赖)
         ↓               │
  B0 (ProfileScreen重构)  │
                         │
Batch 2: 主体UI ─────────┤
  A1→A2→A3→A4→A5→A6→A7  │ (占位屏，可并行)
  B1→B2→B3→B4→B5→B6→B7→B8│ (Agent，B2→B3串行，B4→B5串行，B6→B7串行，组间可并行)
  C1 + C5 + C8 + C10     │ (遗漏补全，可并行)
                         │
Batch 3: 后端收口 ───────┤
  C3 + C4 (需先建Bucket)  │
  F2→F3 (悬赏金链)        │
  F4 (Invite EdgeFn)     │
  F5 (favoriteCount)     │
                         │
Batch 4: 验证 ───────────┤
  G1→G2 (顺序依赖)        │

> **隐性依赖（图中未标注的箭头，执行时须遵守）：**
> - **B0 依赖 B-R**：ProfileScreen 的 Agent 入口卡片需要 B-R 注册的 8 条 Agent 路由就位后才能正确导航
> - **B2/B3 依赖 E1**：CommunityEntity 的 `pending_review` 状态和 `reviewReason` 字段是 Agent 审核功能的数据基础，E1 必须先完成
> - **B3 依赖 P0-3**：community-moderation EdgeFn 必须先部署到 Supabase，B3 的 review action 才能调通
```

---

## 统计

| 维度 | 数量 |
|------|:--:|
| 总任务数 | **47** (P0:6 + D:3 + E:6 + A:7 + B:10 + C:10 + F:3 + G:2) |
| 新建Kotlin | ~56文件 |
| 新建TS | ~3 EdgeFn + 1 shared扩展 |
| 新建SQL | ~3 Migration + 3 Revert |
| 新建文档 | SUPABASE_SETUP.md |
| 修改文件 | ~25 |
| 代码行数 | ~6,800 Kotlin + ~500 TS + ~200 SQL + ~100 YAML |

---

## 审查历史

| 轮次 | Agent | 发现 | 状态 |
|:--:|------|------|:--:|
| 1 | 清单审查Agent | 10致命+11重要 | ✅ 已纳入 |
| 2 | 数据层Agent | Migration 17 Room未同步(5Entity,14+字段) | ✅ 模块E |
| 2 | 导航Agent | 8Screen不可达+路由问题 | ✅ B-R + C1 + B0 |
| 2 | 流程Agent | 3个运行时Bug(售后/订单/OTP) | ✅ 模块D |
| 3 | 终审Agent | 5必须修复+3建议修复 | ✅ 已合并 |
