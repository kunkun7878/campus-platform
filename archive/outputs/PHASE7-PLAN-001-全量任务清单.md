# Phase 7 全量任务清单

<!-- 生成日期：2026-05-24 | 状态：待用户最终确认 -->

> 关联：[[campus_status]] · [[iteration_current]] · [[campus_decisions]] · [[campus_rules]]

---

## 一、Phase 7 目标

完成 MVP 收口，**除支付接口、天气API、UI素材设计图外，所有功能流程必须100%可用**。不留占位屏、不留"即将开放"、不留假数据链路。

---

## 二、全量任务清单

### 模块 A：6个占位屏实现

| # | 任务 | 子任务 | 依赖 | 复杂 |
|:--:|------|--------|------|:--:|
| A1 | **WalletScreen** | (1) 新建 WalletTransactionEntity + WalletTransactionDao + WalletTransactionDto + Mapper (2) IUserRepository 扩展 getWalletTransactions/refreshWalletTransactions (3) WalletViewModel（余额+流水+刷新） (4) WalletScreen UI（余额卡片 + 交易流水列表 + 收支汇总）| 数据层缺口 | 中 |
| A2 | **AddressManageScreen** | (1) AddressManageViewModel（CRUD + 默认地址管理） (2) AddressManageScreen UI（地址卡片列表 + 新增/编辑 + 设为默认）| 数据层就绪 | 低 |
| A3 | **CouponsScreen** | (1) CouponsViewModel（Tab筛选未使用/已使用/已过期 + 领券 + 过期处理） (2) CouponsScreen UI（Tab切换 + 券卡片列表 + 立即使用/已使用/已过期状态）| 数据层就绪 | 中 |
| A4 | **InviteScreen** | (1) 新建 InviteCodeEntity + InviteRecordEntity + InviteDao + InviteCodeDto + InviteRecordDto + Mapper (2) 新建 IInviteRepository + InviteRepository (3) EdgeFn: invite-code（邀请码生成，service_role，因RLS限制仅Agent可INSERT invite_codes） (4) InviteViewModel（邀请码展示+复制+分享 + 记录列表） (5) InviteScreen UI（邀请码卡片 + 奖励规则 + 分享按钮 + 邀请记录列表）| 数据层缺口 | 高 |
| A5 | **FeedbackScreen** | (1) FeedbackViewModel（表单管理 + 提交） (2) FeedbackScreen UI（类型下拉 + 描述输入 + 联系方式 + 提交按钮）| 数据层就绪 | 低 |
| A6 | **AboutScreen** | (1) 纯静态Compose UI（Logo + 版本号 BuildConfig + 功能介绍 + 链接列表：用户协议/隐私政策/合作联系/代理申请占位）| 无依赖 | 极低 |
| A7 | **AnnouncementDetailScreen** | (1) AnnouncementDetailViewModel（加载公告内容） (2) AnnouncementDetailScreen UI（标题 + 时间 + 内容Body + 优先级标签 + 来源）| 数据层就绪 | 低 |

---

### 模块 B：Agent后台 8屏（P0+P1）

| # | 任务 | 子任务 | 依赖 | 复杂 |
|:--:|------|--------|------|:--:|
| B0 | **Agent入口改造** | ProfileScreen 添加用户信息卡片（头像+昵称+学校）+ is_agent=true 时显示"Agent管理后台"入口卡片 + 待审计数角标 | ProfileScreen 修改 | 低 |
| B1 | **AgentDashboardScreen** | (1) AgentDashboardViewModel（待审计数查询） (2) UI（功能卡片导航：内容审核/用户管理/公告管理/跑腿员审批/审核日志，各带计数角标）| B0 | 低 |
| B2 | **AgentReviewListScreen** | (1) AgentReviewListViewModel（分页加载pending_review帖子/评论） (2) UI（Tab切换帖子/评论 + 列表项含标题/摘要/作者/时间/风险等级 + 下拉刷新 + 分页） (3) CommunityRepository 加 getPendingReviewPosts/getPendingReviewComments | B0 | 中 |
| B3 | **AgentReviewDetailScreen** | (1) AgentReviewDetailViewModel（加载完整帖子/评论 + 敏感词高亮 + 操作） (2) UI（完整内容展示含图片 + 敏感词高亮 + 底部操作栏：通过/拒绝/隐藏 + 原因输入弹窗） (3) CommunityRepository 加 updatePostStatus/updateCommentStatus + moderation_logs 写入 | B2 | 中 |
| B4 | **AgentUserListScreen** | (1) AgentUserListViewModel（本校用户列表 + 搜索） (2) UI（搜索框 + 用户列表项含头像/昵称/注册时间/状态 + 点击进详情）| B0 | 低 |
| B5 | **AgentUserDetailScreen** | (1) AgentUserDetailViewModel（用户全量信息 + 封禁/解封操作） (2) UI（用户信息卡片 + 操作按钮：封禁/解封 + 确认弹窗）| B4 | 低 |
| B6 | **AgentAnnouncementListScreen** | (1) AgentAnnouncementListViewModel（公告列表） (2) UI（公告列表 + FAB新建 + 状态pill） + IMiscRepository 加 upsertAnnouncement/deleteAnnouncement | B0 | 低 |
| B7 | **AgentAnnouncementEditScreen** | (1) AgentAnnouncementEditViewModel（新建/编辑公告） (2) UI（表单：标题+内容+优先级+发布按钮）| B6 | 低 |
| B8 | **AgentRunnerReviewScreen** | (1) AgentRunnerReviewViewModel（申请列表 + 审批操作） (2) UI（申请列表含申请人/学号/时间/状态 + 展开详情含身份信息 + 通过/拒绝按钮 + 原因输入） + IRunnerApplicationRepository 加 approveApplication/rejectApplication | B0 | 中 |

---

### 模块 C：遗漏功能补全

| # | 任务 | 当前状态 | 修复方案 | 复杂 |
|:--:|------|---------|---------|:--:|
| C1 | **PublishHub 解除禁用** | 失物发布和帖子发布 `isComingSoon=true`，点击弹"敬请期待"。但 LostPublishScreen 和 PostCreateScreen 已实现 | 改2行：`isComingSoon=false`，navigation 解除 | 极低 |
| C2 | **ProfileScreen 用户信息卡片** | 顶部无头像+昵称+学校信息展示，直接就是菜单列表 | 新增用户信息区：头像（AsyncImage+默认Avatar）+ 昵称 + 学校名称 + 校区。AuthRepository.getProfile() 已有数据 | 低 |
| C3 | **AfterSaleApplyScreen 图片上传** | 显示"图片上传功能即将上线"占位卡片 | 替换为 MultiImagePicker/ImagePickerButton 真实上传组件。ImageUploadRepository 已就绪，Direct bucket 为 chat-images 或新建 after-sale bucket | 中 |
| C4 | **RunnerApplyScreen 身份证上传** | URL文本输入框 + "图片上传即将上线，请先输入图片URL" | 替换为 ImagePickerButton 真实上传。ImageUploadRepository 已就绪 | 中 |
| C5 | **PasswordResetScreen 真实实现** | 显示"暂不支持在线重置密码" | 实现完整密码重置流程：手机号验证 → 验证码 → 新密码 → Supabase Auth updateUser。AuthRepository 已有 resetPassword 方法可以扩展 | 中 |

---

### 模块 D：Edge Function 后端收口

| # | 任务 | 子任务 | 依赖 | 复杂 |
|:--:|------|--------|------|:--:|
| D1 | **云AI审核接入** | (1) `_shared/sensitive-words.ts` 新增 `cloudModerate()` 函数，调用阿里云内容安全API (2) `community-moderation/index.ts` review分支追加 cloudModerate() 调用 (3) Supabase Dashboard 配置 ALIYUN_ACCESS_KEY_ID/ALIYUN_ACCESS_KEY_SECRET 环境变量 | 阿里云AccessKey | 中 |
| D2 | **悬赏金冻结补全** | 检查 `lost-item-lifecycle/index.ts` publish action 是否已从钱包扣款冻结悬赏金。若缺失，补全：publish时 wallets.balance -= reward + wallet_transactions 插入冻结记录 | 需验证当前代码 | 中 |
| D3 | **悬赏金过期退款** | (1) 新建 `reward-expiry/index.ts` EdgeFn（查询 active 超30天的失物 + reward>0，逐笔 refund 到钱包 + 写 transactions + 更新 lost_found_items status=closed） (2) 新建 `.github/workflows/reward-expiry-cron.yml`（每天凌晨3:00 UTC触发） (3) 新建 Migration 18：lost_found_items 加 reward_frozen_at 字段 + 索引 | D2完成后 | 中 |
| D4 | **Invite EdgeFn** | 新建 `invite-code/index.ts`（生成邀请码 + 注册时验证 + 记录邀请关系）。因 invite_codes/invite_records RLS 仅 Agent/service_role 可 INSERT | A4完成后 | 低 |

---

### 模块 E：文档 + 验证

| # | 任务 | 子任务 | 复杂 |
|:--:|------|--------|:--:|
| E1 | **SUPABASE_SETUP.md** | 数据库部署指南：18个Migration执行顺序 + 验证SQL + 4个Storage Bucket创建 + 7个Edge Function部署 + 环境变量配置 + 常见问题FAQ | 中 |
| E2 | **全链路验证 15条** | 注册登录→选校→发布跑腿→接单→完成→评价 / 二手交易全流程 / 失物发布→认领→归还确认 / 社区发帖→评论→Agent审核 / 私聊→群聊→Realtime / 钱包流水 / 优惠券领取 / 邀请→注册→奖励 / 推送通知→deep link / 账户删除 | 高 |
| E3 | **Project Memory 全量同步** | campus_status + iteration_current + campus_session_log + campus_decisions + codebase_map + campus_open_questions + campus_conflicts + runtime_notes 全部更新到 Phase 7 完成状态 | 中 |

---

## 三、任务依赖关系图

```
Module D (EdgeFn)
  D1 ─────────────────────────┐
  D2 ─── D3 ──────────────────┤
  D4 (after A4)               │
                              │
Module A (占位屏)              │
  A4 (Invite数据层) ──────────┤
  A1 (Wallet数据层) ──────────┤
  A2 A3 A5 A6 A7 ─────────────┤
                              │
Module B (Agent后台)           │
  B0 ── B1 B2 B3 B4 B5 B6 B7 B8 (B0 先做，其余可并行)
                              │
Module C (遗漏补全)             │
  C1 C2 C3 C4 C5 ─────────────┤
                              │
Module E (文档+验证)           │
  E1 ─── E2 ─── E3 (顺序依赖)
```

---

## 四、批次执行计划

### 批次1：地基（数据层 + 入口改造）
A1数据层, A4数据层, B0, C1, C2

### 批次2：主体UI（占位屏 + Agent后台）
A1 Screen, A2, A3, A4 Screen, A5, A6, A7, B1-B8

### 批次3：后端收口
D1, D2, D3, D4, C3, C4, C5

### 批次4：文档 + 验证 + 记忆同步
E1, E2, E3

---

## 五、统计

| 维度 | 数量 |
|------|:--:|
| 总任务数 | 31 个子任务 |
| 新建 Kotlin 文件 | ~40（Screen + ViewModel + Entity + DAO + Repository + DI） |
| 新建 TS 文件 | ~2 EdgeFn + 1 共享模块修改 |
| 新建 SQL 文件 | ~1 Migration + 1 Revert |
| 新建 文档 | SUPABASE_SETUP.md |
| 修改现有文件 | ~15（路由 + ProfileScreen + PublishHub + Repository 扩展等） |
| 预估代码行数 | ~4,500 Kotlin + ~300 TS + ~100 SQL + ~100 YAML |
