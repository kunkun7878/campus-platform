# Phase 7 完整任务清单（三轮审查终版）

<!-- 生成日期：2026-05-24 | 审查轮次：3轮 | 审查Agent数：4个 -->

> 关联文档：PHASE7-PLAN-001（初版，已废弃）、审查报告 REVIEW-001/002/003

---

## 一、Phase 7 目标

完成 MVP 收口，**除支付接口、天气API、UI素材设计图外，所有功能流程必须100%可用且已验证通过**。

不留占位屏、不留"即将开放"、不留假数据链路、不留运行时Bug。

---

## 二、前置步骤 P0：数据库部署（启动前必须完成）

Phase 0-6 编写了完整的数据库代码但从未在 Supabase 上执行。**38张表、RLS策略、Storage Bucket、Edge Function 全部只存在于代码文件中，线上环境是空的。**

| # | 任务 | 说明 |
|:--:|------|------|
| P0-1 | **执行18个Migration** | 在 Supabase Dashboard SQL Editor 按顺序执行 00-17 全部 Migration SQL |
| P0-2 | **创建4个Storage Bucket** | 执行 `supabase/storage/buckets.sql`，创建 avatars/community-images/chat-images/lost-found-images |
| P0-3 | **部署7个Edge Function** | 部署 runner-order-lifecycle / runner-after-sale / market-purchase / lost-item-lifecycle / community-moderation / push-notification / _shared |
| P0-4 | **配置 Supabase 环境变量** | FCM_SERVICE_ACCOUNT_JSON / ALIYUN_ACCESS_KEY_ID / ALIYUN_ACCESS_KEY_SECRET |
| P0-5 | **编写 SUPABASE_SETUP.md** | 上述操作的分步指南 + 验证SQL + FAQ |
| P0-6 | **验证数据库可用** | 执行验证SQL确认38张表存在、RLS生效、EdgeFn可调用 |

---

## 三、全量任务清单

### 模块 A：7个占位屏实现

| # | 任务 | 子任务 | 依赖 | 复杂 |
|:--:|------|--------|------|:--:|
| A1 | **WalletScreen** | (1) 新建 WalletTransactionEntity + DAO + Dto + Mapper (2) IUserRepository 扩展 (3) WalletViewModel (4) WalletScreen UI（余额卡片 + 流水列表）| wallet_transactions无Room Entity | 中 |
| A2 | **AddressManageScreen** | (1) AddressManageViewModel (2) AddressManageScreen UI（卡片列表 + CRUD + 默认地址）| 数据层就绪 | 低 |
| A3 | **CouponsScreen** | (1) CouponsViewModel (2) CouponsScreen UI（Tab切换 + 券卡片）| 数据层就绪 | 中 |
| A4 | **InviteScreen** | (1) 新建 InviteCodeEntity + InviteRecordEntity + DAO + Dto (2) IInviteRepository + InviteRepository (3) invite-code EdgeFn (4) InviteViewModel + Screen UI | 无Room Entity | 高 |
| A5 | **FeedbackScreen** | (1) FeedbackViewModel (2) FeedbackScreen UI（表单 + 提交）| 数据层就绪 | 低 |
| A6 | **AboutScreen** | (1) 静态Compose UI（Logo + 版本号 + 链接列表）| 无依赖 | 极低 |
| A7 | **AnnouncementDetailScreen** | (1) AnnouncementDetailViewModel (2) UI（标题 + 时间 + 内容 + 优先级）| 数据层就绪 | 低 |

---

### 模块 B：Agent后台（P0+P1）

| # | 任务 | 子任务 | 依赖 | 复杂 |
|:--:|------|--------|------|:--:|
| B-R | **Agent路由注册** | CampusRoutes 新增8条Agent路由 + CampusNavGraph 注册8个 composable destination | 无 | 中 |
| B0 | **Agent入口改造 + ProfileScreen重构** | (1) ProfileViewModel 从空壳填充（加载Profile + Agent状态 + 未读审核数） (2) ProfileScreen 新增：用户信息卡片（头像+昵称+学校）+ "服务中心"Section（Wallet/AddressManage/Coupons/Feedback/Invite/About/RunnerApply菜单项）+ "设置"Section + is_agent时显示Agent入口卡片 | 无 | 中 |
| B1 | **AgentDashboardScreen** | (1) AgentDashboardViewModel（待审计数查询） (2) UI（功能卡片导航 + 计数角标）| B0, B-R | 低 |
| B2 | **AgentReviewListScreen** | (1) AgentReviewListViewModel（pending_review帖子/评论分页） (2) UI（Tab切换 + 列表 + 风险等级） (3) CommunityRepository 加 getPendingReviewPosts/getPendingReviewComments | B0, B-R | 中 |
| B3 | **AgentReviewDetailScreen** | (1) AgentReviewDetailViewModel（内容+敏感词高亮+操作） (2) UI（完整内容 + 操作栏：通过/拒绝/隐藏 + 原因弹窗） (3) EdgeFn: community-moderation 新增 review action（含Agent身份校验+学校隔离） (4) CommunityRepository 加 updatePostStatus/updateCommentStatus | B2 | 中 |
| B4 | **AgentUserListScreen** | (1) AgentUserListViewModel（本校用户列表+搜索） (2) UI（搜索框 + 用户列表 + 状态标识）| B0, B-R | 低 |
| B5 | **AgentUserDetailScreen** | (1) AgentUserDetailViewModel（用户详情+封禁/解封） (2) UI（信息卡片 + 操作按钮 + 确认弹窗）| B4 | 低 |
| B6 | **AgentAnnouncementListScreen** | (1) AgentAnnouncementListViewModel + IVerscRepository 加 upsert/delete (2) UI（列表 + FAB + 状态pill）| B0, B-R | 低 |
| B7 | **AgentAnnouncementEditScreen** | (1) AgentAnnouncementEditViewModel (2) UI（表单：标题+内容+优先级+发布） (3) AnnouncementEntity 加 status 字段 + Migration 加 announcements.status 列 | B6 | 中 |
| B8 | **AgentRunnerReviewScreen** | (1) AgentRunnerReviewViewModel（申请列表+审批） (2) UI（列表 + 展开详情 + 通过/拒绝 + 原因） (3) IRunnerApplicationRepository 加 approve/reject | B0, B-R | 中 |

---

### 模块 C：遗漏功能补全

| # | 任务 | 当前状态 | 修复方案 | 复杂 |
|:--:|------|---------|---------|:--:|
| C1 | **PublishHub 解除禁用** | 失物发布 + 帖子发布 `isComingSoon=true`，弹出Toast | `isComingSoon=false`，填充正确的 onClick 导航 | 极低 |
| C2 | **ProfileScreen 服务中心菜单** | 7个Screen的入口缺失（Wallet/AddressManage/Coupons/Feedback/Invite/About/RunnerApply） | 已合并入 B0 | — |
| C3 | **AfterSaleApplyScreen 图片上传** | "图片上传功能即将上线"占位 | 替换为 ImagePickerButton 真实上传。需要新建 Storage Bucket: after-sale | 中 |
| C4 | **RunnerApplyScreen 身份证上传** | URL文本输入框 + "图片上传即将上线" | 替换为 ImagePickerButton 真实上传。需要新建 Storage Bucket: identity | 中 |
| C5 | **PasswordResetScreen 真实实现** | "暂不支持在线重置密码" | 实现：手机号验证→验证码→新密码→Supabase Auth updateUser | 中 |
| C6 | **MyPublished 跑腿/失物 tab 接通** | `DEVELOPING_INDICES = setOf(1, 3)` 返回假数据 | 接通 RunnerTaskRepository + LostFoundRepository 真实数据查询 | 中 |
| C7 | **MyFavorites 跑腿/失物/帖子 tab 接通** | `DEVELOPING_INDICES = setOf(1, 3, 4)` 返回假数据 | 接通对应Repository的收藏数据查询 | 中 |
| C8 | **GoodsDetailScreen 编辑功能** | "编辑功能即将上线" Snackbar | 实现编辑：标题/描述/价格/图片修改 + 调用 MarketRepository.updateListing | 中 |
| C9 | **AfterSaleDetailScreen 补充材料** | "补充材料功能即将上线" Toast | 实现补充材料上传 + EdgeFn update action | 中 |
| C10 | **UserAgreementScreen + PrivacyPolicyScreen** | LoginScreen/RegisterScreen 中有 TODO 指向不存在页面 | 新建2个静态文本Screen + 路由 + NavGraph注册 | 低 |

---

### 模块 D：运行时Bug修复（第二轮审查发现）

| # | Bug | 位置 | 修复 | 严重度 |
|:--:|------|------|------|:--:|
| D1 | **售后功能永久不可用** | `AfterSaleApplyViewModel` 调用 `runner-after-sale` EdgeFn 时 `buildJsonObject` 缺少 `"action": "create"` | 加一行 `put("action", "create")` | 🔴 阻断 |
| D2 | **市场订单操作非原子** | `MarketOrderDetailViewModel.cancelOrder/confirmComplete` 分两步更新 order + listing | 改造为调用 EdgeFn 单次事务操作（扩展 market-purchase 或新建 market-order-lifecycle EdgeFn）| 🟡 严重 |
| D3 | **OTP验证不可用** | `AuthRepository.verifyOtp()` 硬编码 `throw UnsupportedOperationException` | 升级 Supabase Kotlin SDK → 实现正确 verifyPhoneOtp 调用 | 🟡 已知 |

---

### 模块 E：Phase 6 Migration 同步到 Room（第二轮审查发现）

Migration 17 新增了大量SQL字段，Android Room Entity 层完全未同步。**Room会静默丢弃这些字段，导致数据不一致。**

| # | Entity文件 | 缺失字段 | 影响 |
|:--:|------|------|------|
| E1 | **CommunityEntities.kt** | CommunityPostEntity: `reviewReason` + `STATUS_PENDING_REVIEW` + `SECTION_LOST_FOUND/SECOND_HAND/HELP/ANNOUNCEMENT` 常量<br>CommunityCommentEntity: `reviewReason` + `STATUS_PENDING_REVIEW` | Agent审核无法持久化 |
| E2 | **LostFoundEntities.kt** | LostFoundItemEntity: `returnedAt` | 归还时间丢失 |
| E3 | **MessageEntities.kt** | ConversationEntity: `sourceType` + `sourceId` + `lastMessageSenderId` | 会话来源标签丢失 |
| E4 | **UserEntities.kt** | NotificationEntity: `priority` + `pushSent` + `pushSentAt` + `TYPE_LOST_FOUND/COMMUNITY/GROUP_CHAT` 常量 | 推送优先级/发送状态丢失 |
| E5 | **RunnerEntities.kt** | OrderTimelineEntity: `updatedAt`<br>AfterSaleTimelineEntity: `updatedAt` | 时间线更新时间丢失 |
| E6 | **对应Mapper/Dto同步** | 上述所有缺失字段的Dto和Mapper映射函数 | 数据转换链路断裂 |

---

### 模块 F：Edge Function 后端收口

| # | 任务 | 子任务 | 依赖 | 复杂 |
|:--:|------|--------|------|:--:|
| F1 | **云AI审核接入** | (1) `_shared/sensitive-words.ts` 新增 `cloudModerate()` (2) `community-moderation/index.ts` review分支追加调用 (3) 环境变量配置 | 阿里云AccessKey + P0-4 | 中 |
| F2 | **悬赏金冻结补全** | 审查 `lost-item-lifecycle publish` action：冻结扣款已实现但**缺 wallet_transactions 流水INSERT**。补全流水记录。 | P0-3 | 中 |
| F3 | **悬赏金过期退款** | (1) 新建 `reward-expiry/index.ts` (2) GitHub Actions cron (3) Migration 18: lost_found_items 加 reward_frozen_at | F2完成后 | 中 |
| F4 | **Invite EdgeFn** | 新建 `invite-code/index.ts`（邀请码生成+注册验证+关系记录），service_role | A4完成后 + P0-3 | 低 |
| F5 | **favoriteCount trigger** | Migration 19: market_listings 加 favorite_count 列 + user_favorites INSERT/DELETE trigger 自动更新计数 | P0-1 | 低 |

---

### 模块 G：文档 + 验证 + 记忆同步

| # | 任务 | 子任务 | 复杂 |
|:--:|------|--------|:--:|
| G1 | **SUPABASE_SETUP.md** | 已合并至 P0-5 | — |
| G2 | **全链路验证 15条** | 注册→选校→发布跑腿→接单→完成→评价 / 二手全流程 / 失物认领归还 / 社区发帖→Agent审核 / 私聊→群聊→Realtime / 钱包流水 / 优惠券领用 / 邀请→注册→奖励 / 推送通知→deep link / 账户删除 | 高 |
| G3 | **Project Memory 全量同步** | 14个 project_memory 文件更新到 Phase 7 完成状态 | 中 |

---

## 四、任务依赖关系

```
P0（数据库部署）──→ 所有模块
                    │
      ┌─────────────┼─────────────┐
      ▼             ▼             ▼
   E1-E6        B-R, B0        D1, D3
   (Room同步)   (路由+入口)    (Bug修复)
      │             │
      ├─────┬───────┤
      ▼     ▼       ▼
   A1-A7  B1-B8   C1,C3-C10
   (占位屏) (Agent) (遗漏补全)
      │     │       │
      └─────┴───┬───┘
                ▼
          F1-F5 (EdgeFn)
                │
                ▼
          G2 → G3 (验证→记忆同步)
```

---

## 五、批次执行计划

| 批次 | 内容 | 任务 | 预估文件 |
|:--:|------|------|:--:|
| **P0** | 数据库部署 | P0-1~P0-6 | SUPABASE_SETUP.md |
| **Batch 1** | 地基（Bug修复+数据同步+路由入口） | D1, D2, E1-E6, B-R, B0 | ~12 |
| **Batch 2** | 主体UI（占位屏+Agent后台+遗漏补全） | A1-A7, B1-B8, C1, C3-C10 | ~55 |
| **Batch 3** | 后端收口（EdgeFn） | F1-F5 | ~8 |
| **Batch 4** | 验证+记忆同步 | G2, G3 | 0 |

---

## 六、最终统计

| 维度 | 数量 |
|------|:--:|
| 总任务数 | **44 个子任务**（P0:6 + A:7 + B:10 + C:10 + D:3 + E:6 + F:5 + G:2） |
| 新建 Kotlin 文件 | ~56 |
| 新建 TS 文件 | ~3 EdgeFn + 1 共享模块修改 |
| 新建 SQL 文件 | ~2 Migration + 2 Revert |
| 新建 文档 | SUPABASE_SETUP.md |
| 修改现有文件 | ~20 |
| 预估代码行数 | ~6,500 Kotlin + ~400 TS + ~150 SQL + ~100 YAML |

---

## 七、审查历史

| 轮次 | 审查Agent | 发现 |
|:--:|------|------|
| 1 | Agent #1（清单完整性） | 10项致命遗漏（Agent路由、Entity pending_review、EdgeFn review action、favoriteCount、MyPublished/MyFavorites假数据、GoodsDetail编辑、AfterSale补充材料、ProfileScreen菜单、用户协议页、Agent权限校验） |
| 2 | Agent #2（数据层完整性） | Phase 6 Migration 17从未同步到Room层（5个Entity文件14+字段缺失）、5张表无Room Entity |
| 2 | Agent #3（导航完整性） | 8个Screen不可达（7个缺Profile入口+LostPublish被阻断）、CampusScreenConfig脆弱字符串匹配 |
| 2 | Agent #4（业务流程完整性） | 售后EdgeFn调用缺action参数（运行时Bug）、市场订单非原子操作、OTP验证不可用 |
| 3 | Agent #5（本文件审查） | 待执行 |
