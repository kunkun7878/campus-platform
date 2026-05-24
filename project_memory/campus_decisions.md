# 校园聚合平台 - 已确认决策

<!-- last_sync: 2026-05-24T16:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_rules]] · [[campus_work_rules]] · [[campus_open_questions]] · [[iteration_current]] · [[codebase_map]] · [[campus_ui_decisions]]

## 产品路线决策
1. 当前阶段：Android 原生应用正式开发（Kotlin + Jetpack Compose + Supabase），基于 HTML 原型作为 UI 参考，分 8 Phase（Phase 0-7）推进至 MVP。原型已进入收尾阶段，不再作为主要开发目标。
2. 后期考虑微信小程序，当前暂不投入。

## 工程与工具决策
3. DeepSeek 当前应视为文本模型，不承担包含图片历史的会话任务。
4. Skills 已安装到 `.claude/skills/`（campus-memory-sync + campus-project-guard）。
5. 使用 Obsidian 作为长期知识库查看/浏览外壳，与 Claude Code 的 Edit 工具协作读写同一套 Markdown 文件。

## 项目过程管理决策
6. ~~OpenClaw 原生记忆保留使用~~ → 已废弃。项目已从 OpenClaw 迁移到 Claude Code。
7. 项目专属长期记忆层（`project_memory/`）已建立并持续维护。
8. 多 Agent 协作已落地：经理+执行Agent+审查Agent，详见 `campus_work_rules.md` §7。
9. 所有校园聚合平台相关文件集中放在 `C:\Users\admin\Desktop\校园聚合平台\`。
10. Claude Code 的 CLAUDE.md 作为会话入口，自动引导读取项目记忆。
11. 54 个 skill（52 Agent + 2 项目管理），按 §7.1.1 分配。

## 已确认的后续决策
- ✅ 记忆体系主编辑环境：Claude Code 的 Edit 工具 + Obsidian 查看
- ✅ 多 Agent 角色：不需要固定常驻，按需派生执行Agent和审查Agent
- ✅ Obsidian 目录布局：整个 `校园聚合平台/` 作为 vault 根目录
- ✅ 不使用额外的 memory 插件/wiki 层，project_memory/ Markdown 文件足够
- ✅ 54 个 skill（52 Agent + 2 项目管理），按 §7.1.1 分配

## Phase 2 认证+选校决策（2026-05-22）

### 认证方式
1. 手机号+SMS验证码（主力），阿里云短信服务
2. 手机号+密码登录（辅助），密码规则：最小8位 + 至少字母+数字 + 实时强度指示器
3. 微信一键登录（搁置，需微信开放平台企业认证）
4. 邮箱验证登录（备用），邮箱选填，填了必须验证后才能用于登录
5. CAPTCHA 图形验证码（发送SMS前必须通过），防机器人和脚本
6. 密码错误锁定：6次→60秒 / 累计9次→5分钟 / 引导走SMS登录

### 选校机制
7. 原型语义修正：school-select screen 实际选的是 Campus，系统自动推导 School
8. 选校后不可自由切换学校（硬规则），客服申诉为唯一换校途径
9. 同校多校区可自由切换 campus_id（如川师大狮子山↔成龙↔遂宁）

### 数据模型
10. 4 张自定义表：profiles / schools / campuses / wechat_identities
11. Supabase 项目：campus-platform（ap-southeast-1 新加坡）
12. 种子数据：四川师范大学（狮子山/成龙/遂宁3校区）+ 四川邮电职业技术学院（锦江1校区）
13. RLS：所有表启用，以 school_id 为最小隔离单位，同校可互读基本资料

### 工程决策
14. Admin 管理后台：Phase 2-5 不做，用 SQL migration + Supabase Dashboard；Phase 7+ 再做
15. 本地开发：Supabase CLI + Docker Desktop（待配置）
16. 设备风险检测：暂不做

### 功能决策
17. 游客模式：不需要，所有功能需登录
18. 账号注销：Phase 2 支持（软删除 status=2 + deleted_at）
19. Session：自动登录（SDK 默认），退出登录清除 session，切换设备重登
20. 用户协议：Phase 2 做占位，后期可替换

## Phase 3 数据层设计决策（2026-05-22）

### 数据库范围
21. 36 张表全覆盖（Phase 3 初始），Phase 6 增至 38 张（+fcm_tokens + moderation_logs），一次性补齐，不按 Phase 分批
22. 每张内容表必须有 school_id + RLS 策略 + revert 脚本

### Room 策略
23. Supabase 为权威数据源（source of truth），Room 为本地缓存
24. SQLCipher 全库加密，Phase 3 启用
25. Entity 与 DTO 分离，通过 Mapper 扩展函数转换
26. 新 Repository 引入接口（domain/repository/），旧 Repository 保持不动

### ViewModel
27. Phase 3 全面引入 ViewModel，新旧 Screen 全部迁移

### RLS 设计
28. login_codes：service_role only，客户端无 policy（参照 wechat_identities）
29. profiles 敏感字段（runner_status/invite_code）：trigger 保护，用户不可自改；balance 权威来源已移至 wallets.balance
30. announcements.school_id：NULL = 全平台公告，有值 = 仅该校可见
31. wallet_transactions：append-only，无 updated_at
32. group_messages/members：通过 JOIN official_groups.school_id 实现学校隔离
33. 所有内容表 ON DELETE RESTRICT（防误删级联），user_addresses 除外（SET NULL）

### Migration 管理
34. 每个 migration 必须有配套 revert，使用 IF NOT EXISTS/DROP IF EXISTS 确保幂等
35. RLS 策略集中在各模块 migration 中，不单独分离
36. service_role 路径必须在所有 trigger 中显式豁免

## Phase 4 跑腿全链路决策（2026-05-23）

### 状态转换架构
37. 所有跑腿状态转换（接单/确认取件/送达/收货/取消/售后）走 Supabase Edge Function（service_role），客户端不直写状态字段。理由：当前 RLS 已预设此路径 + SELECT FOR UPDATE 防并发抢单。
38. 创建 2 个 Edge Function：`runner-order-lifecycle`（订单状态流转 + timeline 写入）、`runner-after-sale`（售后全流程）

### 评价功能
39. 评价嵌入 OrderDetailScreen：确认收货后底部展开评价表单（星级+文字），提交后表单转为只读展示。OrderList 中加"待评价"/"已评价" pill。Phase 4 先做 buyer 评 runner，双向评价留 Phase 5+

### OrderList 合并
40. 跑腿域合并：OrderListScreen 顶部 Tab 切换"我发布的/我接的单"，各 Tab 内含状态子筛选。市场模块 my-published/my-bought 保持独立（跨模块内容聚合，数据域不同）

### AfterSale 归属
41. OrderDetail/OrderList/AfterSaleApply/AfterSaleDetail 从 market/ 迁到 runner/。after_sales 表外键硬绑定 runner_orders，二手无售后表，不存在复用

### Edge Function 搭建
42. Phase 4 用 Supabase Dashboard 手动创建 Edge Function（Docker 未安装），代码手动同步到 supabase/functions/ 进 git。Phase 5+ 切 CLI

### RLS 修复
43. Phase 4 开工前修复 4 处 RLS 漏洞（新 migration 15）：runner_reviews INSERT 加订单参与者校验、after_sales INSERT 加订单参与者校验、order_timeline SELECT 加 JOIN、after_sale_timeline SELECT 加 JOIN
44. runner_orders SELECT RLS 决策（2026-05-23）：维持 school_id 隔离（市场公开模式）。

## Phase 5 二手交易决策（2026-05-23）

### 范围与架构
45. Phase 5 范围：7 screen（GoodsDetail/MarketPublish/MarketOrderDetail/my-sold/my-bought/my-published/my-favorites）+ 1 Edge Function + 6 组件 + UiState 泛化改造
46. 购买流程走 Edge Function 事务（market-purchase），参照 runner-order-lifecycle 模式。原因：RLS UPDATE policy 阻止买家修改 listing.status，需 service_role 绕过
47. MarketOrderDetail 新建，参照 runner OrderDetail 骨架（彩色状态横幅 + 商品信息卡片 + 双方信息 + 操作按钮）
48. 不包含：图片上传（P7）、评价体系（DB 无 market_reviews 表）、售后/纠纷（DB 无 market_after_sales 表）、全文搜索（P7）
49. 图片展示用 Coil AsyncImage 加载 + 色块 fallback（#EEF1FF），上传留 Phase 7

### UI/交互决策
50. GoodsDetail 底部操作栏固定在 Scaffold bottomBar：收藏(icon) | 联系卖家(outlined) | 立即购买(filled primary)
51. MarketOrderDetail 状态区用彩色大横幅：pending 橙/accepted 蓝/completed 绿/cancelled 灰
52. MarketPublish 发布成功跳转刚发布的 goods-detail
53. 已售出商品点击 → 通过 listingId 查 market_order → 跳转 MarketOrderDetail
54. 首页搜索栏在 HorizontalPager 上方共享，切换 tab 时 placeholder 变化
55. 选择器用 ExposedDropdownMenuBox，分类/成色统一
56. my-favorites 取消收藏：按钮始终显示 + AnimatedVisibility fadeOut

### 技术架构决策
57. MarketFeedCard 用 variant 枚举（HOME/MY_PUBLISHED/MY_FAVORITES），与 RunnerTaskCard 模式一致
58. ViewModel 数据查询用双 Repository 注入模式（IMarketOrderRepository + IMarketRepository）
59. MyPublishedScreen 跨类型列表按 data-type 分发跳转
60. UiState<T> 泛型全量改造（Market 7 + Runner 8 VM），逐模块独立 commit
61. 收藏操作先网络确认再更新 UI（与 runner 风格一致）
62. 取消订单：买家仅 pending 可取消，卖家 pending/accepted 可取消（accepted 需填原因）
63. Profile 不新增市场入口，用现有 my-sold/my-bought 路由
64. seller_id 客户端校验作为 UX 优化，服务端 Edge Function 做强制校验

### 数据库
65. MarketDao 补 getListingsByIds 方法，UserDao 补 getFavoritesByUserIdAndType 方法当前 `runner_orders_select_policy` 仅校验 school_id，同校所有人可见所有订单的 buyer_id、runner_id、amount、status 等敏感字段。在跑腿市场公开模式下，这些信息对建立用户间信任是必要的——跑腿员需要看到订单信息以决定是否接单，买家也需要看到竞争情况。不做更细粒度的 RLS 收紧。

## Phase 6 失物招领 + 社区 + 实时聊天决策（2026-05-23）

### 决策 #33 修复
66. 决策 #33 "所有内容表 ON DELETE RESTRICT" 与实际执行不符——全部 15 个 migration 中内容表间外键均使用 CASCADE。修正 #33 为：school_id 外键 ON DELETE RESTRICT，auth.users 和内容表间外键 ON DELETE CASCADE。

### D1-D3 已确认决策
67. D1 失物认领核验模式：发布者审核制
68. D2 社区帖子管理归属：Agent 审核
69. D3 实时聊天推送方式：Supabase Realtime

### D4-D8 用户确认决策
70. D4 失物归还确认机制：发布者单方标记
71. D5 帖子频道枚举：扩展到5个固定值（campus_wall/announcement/newbie_guide/study/gaming/discussion）
72. D6 FCM离线推送时机：Phase 6全场景推送（用户选B，FCM留Phase 6）。新增：Firebase项目创建+Migration 17扩充fcm_token+push-notification EdgeFn+FirebaseMessagingService
73. D7 悬赏金处理：发布时冻结→归还确认后转交（类似淘宝/闲鱼）
74. D8 群聊发送者信息：头像+昵称（JOIN profiles + 客户端缓存）

### Phase 6 范围决策
75. Phase 6范围：P0+P1全做（10 Screen+2 EdgeFn+Migration 17+Realtime SDK+图片上传+空态/错误态+conversation来源标签+帖子编辑/评论删除/群退出+通知deep link+通知分级）
76. Migration 17合并范围：DDL扩展+3计数trigger+moderation_logs+RLS调整+数据一致性修复

### 敏感词审核方案
77. 本地敏感词库：textfilter 40核心词条+Aho-Corasick自动机+联系方式正则。block不存库，review存库仅作者可见
78. Edge Function community-moderation：接管帖子/评论INSERT，三级风险分级。Migration 17（含原计划 Migration 18 的 status CHECK + moderation_logs 表）
<!-- 注：决策 #79 原分配给 FCM 方案细节，内容已合并入 #72 和 #80，编号保留空缺 -->
80. Firebase 项目 campus-platform 已创建，google-services.json 已配置到 android/app/，服务帐号私钥已就绪。FCM Server Key 不需要（使用服务帐号 OAuth 2.0 认证 HTTP v1 API）
