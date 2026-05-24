# 校园聚合平台 - 代码/页面事实地图

<!-- last_sync: 2026-05-24T18:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[runtime_notes]]

## 主文件路径
- HTML 原型：`C:\Users\admin\Desktop\校园聚合平台\prototype\campus-miniapp-prototype.html`（34 screen，约5200行）
- Android 项目：`C:\Users\admin\Desktop\校园聚合平台\android\`（Kotlin + Compose）

## 关联文件
- `project_memory/`：项目长期记忆主副本（14个Markdown文件）
- `archive/legacy_openclaw/`：OpenClaw 时代多Agent规范文档（已归档）
- `android/`：Android 原生应用项目（Gradle + Kotlin DSL + Compose）

### Android 项目结构概要
```
android/
├── build.gradle.kts           （根构建，插件声明）
├── settings.gradle.kts        （项目名 + 模块）
├── gradle.properties          （JVM参数 + Compose设置）
├── gradle/libs.versions.toml  （版本目录，28版本 + 55依赖）
├── app/
│   ├── build.gradle.kts       （应用构建，Compose+Hilt+Room+Retrofit+Coil）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/campus/platform/
│       │   ├── MainActivity.kt        （Compose入口 + Hilt）
│       │   ├── CampusApplication.kt   （Hilt Application）
│       │   ├── navigation/
│       │   │   ├── CampusRoutes.kt          （路由定义 — sealed class + @Serializable 顶层类型）
│       │   │   ├── CampusNavGraph.kt        （NavHost + 5 嵌套图 + saveState/restoreState）
│       │   │   ├── CampusBottomNavItem.kt   （底部导航 5 Tab 枚举）
│       │   │   └── CampusScreenConfig.kt    （每屏脚手架配置）
│       │   └── ui/
│       │       ├── component/
│       │       │   ├── CampusMainScaffold.kt  （主脚手架：HeroBar + BottomNav 控制）
│       │       │   ├── CampusHeroBar.kt       （顶部品牌/天气栏）
│       │       │   ├── CampusBottomNav.kt     （底部导航栏 — hasRoute<T>() 判定）
│       │       │   └── ScreenPlaceholder.kt   （占位屏组件）
│       │       └── screen/
│       │           ├── HomeSubView.kt              （首页三子视图枚举）
│       │           ├── auth/                       （Login, Register）
│       │           ├── community/                  （Community, GroupChat, PostCreate, PostDetail）
│       │           ├── global/                     （SchoolSelect）
│       │           ├── home/                       （Home, GoodsDetail, LostDetail, LostClaim, AnnouncementDetail）
│       │           ├── market/                     （LostPublish, MarketPublish, MarketOrderDetail）
│       │           ├── message/                    （ChatDetail, Message）
│       │           ├── profile/                    （About, Wallet, Coupons 等 12 屏）
│       │           ├── publish/                    （PublishHub, Publish）
│       │           └── runner/                     （AfterSaleApply, AfterSaleDetail, OrderDetail, OrderList）
│       └── res/
```

## 已验证页面/screen 清单（来自真实 HTML + Phase 2-6 实现）— 共38个

| 页面/screen | 来源 | 状态 |
|-------------|------|------|
| school-select | 真实 HTML `screenConfigs` | ✅ Phase 2 已实现（学校-校区两级选择器） |
| home | 真实 HTML `screenConfigs` | 已验证（HorizontalPager + FilterChip） |
| publish | 真实 HTML `screenConfigs` | 已验证 |
| publish-hub | 真实 HTML `screenConfigs` | 已验证 |
| order-detail | 真实 HTML `screenConfigs` | ✅ Phase 4 已实现（已迁至 runner/） |
| order-list | 真实 HTML `screenConfigs` | ✅ Phase 4 已实现（已迁至 runner/） |
| after-sale-apply | 真实 HTML `screenConfigs` | ✅ Phase 4 已实现（已迁至 runner/） |
| after-sale-detail | 真实 HTML `screenConfigs` | ✅ Phase 4 已实现（已迁至 runner/） |
| **market-order-detail** | Phase 5 新增 | ✅ Phase 5 已实现（@Serializable 类型安全路由） |
| goods-detail | 真实 HTML `screenConfigs` | 已验证（@Serializable 类型安全路由，已移至 home/） |
| lost-detail | 真实 HTML `screenConfigs` | 已验证（@Serializable 类型安全路由，已移至 home/） |
| lost-claim | 真实 HTML `screenConfigs` | 已验证（@Serializable 类型安全路由，已移至 home/） |
| community | 真实 HTML `screenConfigs` | 已验证 |
| message | 真实 HTML `screenConfigs` | 已验证（3条message-row→chat-detail） |
| profile | 真实 HTML `screenConfigs` | 已验证 |
| announcement-detail | 真实 HTML `screenConfigs` | 已验证（@Serializable 类型安全路由，已移至 home/） |
| **chat-detail** | 真实 HTML `screenConfigs` | 已验证（Batch2-T8新增，微信级气泡UI） |
| **post-detail** | 真实 HTML `screenConfigs` | 已验证（Batch2-T9新增，评论+互动） |
| **post-create** | 真实 HTML `screenConfigs` | 已验证（Batch2-T10新增，发帖表单） |
| **group-chat** | 真实 HTML `screenConfigs` | 已验证（Batch2-T11新增，群聊多人气泡） |
| login | 真实 HTML `screenConfigs` | ✅ Phase 2 已实现（双模式：OTP + 密码） |
| register | 真实 HTML `screenConfigs` | ✅ Phase 2 已实现（3步分步表单 + 密码强度） |
| **password-reset** | Phase 2 新增 | ✅ Phase 2 已实现 |
| **account-delete** | Phase 2 新增 | ✅ Phase 2 已实现 |
| market-publish | 真实 HTML `screenConfigs` | ✅ Phase 5 已实现 |
| lost-publish | 真实 HTML `screenConfigs` | ✅ Phase 6 已实现 |
| wallet | 真实 HTML `screenConfigs` | 已有占位组件，待 Phase 7 实现业务内容 |
| runner-apply | 真实 HTML `screenConfigs` | 已有占位组件，待 Phase 7 实现业务内容 |
| address-manage | 真实 HTML `screenConfigs` | 已有占位组件，待 Phase 7 实现业务内容 |
| coupons | 真实 HTML `screenConfigs` | 已有占位组件，待 Phase 7 实现业务内容 |
| invite | 真实 HTML `screenConfigs` | 已有占位组件，待 Phase 7 实现业务内容 |
| feedback | 真实 HTML `screenConfigs` | 已有占位组件，待 Phase 7 实现业务内容 |
| about | 真实 HTML `screenConfigs` | 已有占位组件，待 Phase 7 实现业务内容 |
| my-published | 真实 HTML `screenConfigs` | ✅ Phase 5 已实现（跨类型列表 + data-type 分发） |
| my-sold | 真实 HTML `screenConfigs` | ✅ Phase 5 已实现 |
| my-bought | 真实 HTML `screenConfigs` | ✅ Phase 5 已实现 |
| my-favorites | 真实 HTML `screenConfigs` | ✅ Phase 5 已实现（AnimatedVisibility fadeOut） |

## Supabase 数据库

### Migration 文件（18 个 + 17 Revert）

| Migration | 文件 | 表 | 模块 |
|-----------|------|-----|------|
| 00 | create_profiles | profiles + trigger | 用户 |
| 01 | create_schools_campuses | schools, campuses | 学校 |
| 02 | seed_data | 种子数据（2学校4校区） | 学校 |
| 03 | rls_policies | RLS 全策略 + helper 函数 | 安全 |
| 04 | wechat_identities | wechat_identities | 认证 |
| 05 | add_indexes | profiles.school_id 索引 | 性能 |
| 06 | runner_module | runner_tasks, runner_orders, runner_reviews, user_addresses | 跑腿 + 地址 |
| 07 | market_module | market_listings, market_orders, user_favorites | 二手 + 收藏 |
| 08 | lost_found_module | lost_found_items, lost_found_claims | 失物 |
| 09 | community_module | community_posts, community_comments, official_groups | 社区 |
| 10 | notifications | notifications | 通知 |
| 11 | runner_after_sale_supplement | runner_applications, order_timeline, after_sales, after_sale_timeline | 跑腿 + 售后 |
| 12 | messaging_social | post_likes, conversations, messages, group_messages, group_members | 消息 + 点赞 |
| 13 | wallet_system | wallets, wallet_transactions, announcements, coupons, user_coupons | 钱包 + 系统 |
| 14 | misc_alter_profiles | feedbacks, invite_codes, invite_records, login_codes, attachments + ALTER profiles | 杂项 + 扩展 |
| 15 | fix_runner_rls | 修复 4 处 runner RLS 策略漏洞 | 安全修复 |
| 16 | harden_market_orders_rls | 加强 market_orders INSERT RLS（listing 有效性 + seller 一致性 + 买家≠卖家） | 安全加固 |
| 17 | phase6_schema | fcm_tokens, moderation_logs + 6 trigger + 30+ DDL 变更 | Phase 6 基础设施 |

### 数据库表完整清单（38 张）

| 模块 | 表名 | Migration | 隔离策略 |
|------|------|-----------|---------|
| 用户 | profiles | 00 | school_id（已有） |
| 学校 | schools | 01 | 全局可读（已有） |
| 学校 | campuses | 01 | 全局可读（已有） |
| 认证 | wechat_identities | 04 | user_id（已有） |
| 跑腿 | runner_tasks | 06 | school_id |
| 跑腿 | runner_orders | 06 | school_id |
| 跑腿 | runner_reviews | 06 | school_id |
| 地址 | user_addresses | 06 | user_id |
| 二手 | market_listings | 07 | school_id |
| 二手 | market_orders | 07 | school_id |
| 收藏 | user_favorites | 07 | user_id |
| 失物 | lost_found_items | 08 | school_id |
| 失物 | lost_found_claims | 08 | school_id |
| 社区 | community_posts | 09 | school_id |
| 社区 | community_comments | 09 | school_id |
| 社区 | official_groups | 09 | school_id |
| 通知 | notifications | 10 | user_id |
| 跑腿 | runner_applications | 11 | school_id |
| 跑腿 | order_timeline | 11 | JOIN runner_orders |
| 售后 | after_sales | 11 | school_id |
| 售后 | after_sale_timeline | 11 | JOIN after_sales |
| 社区 | post_likes | 12 | JOIN community_posts |
| 消息 | conversations | 12 | participant |
| 消息 | messages | 12 | JOIN conversations |
| 消息 | group_messages | 12 | JOIN official_groups |
| 消息 | group_members | 12 | user_id + JOIN |
| 钱包 | wallets | 13 | user_id |
| 钱包 | wallet_transactions | 13 | user_id |
| 系统 | announcements | 13 | school_id(NULL=全平台) |
| 系统 | coupons | 13 | Agent管理 |
| 系统 | user_coupons | 13 | user_id |
| 系统 | feedbacks | 14 | user_id |
| 系统 | invite_codes | 14 | user_id |
| 系统 | invite_records | 14 | inviter_id |
| 认证 | login_codes | 14 | service_role only |
| 附件 | attachments | 14 | user_id |
| 推送 | fcm_tokens | 17 | user_id |
| 审核 | moderation_logs | 17 | Agent 可见 |

## 页面切换机制

### HTML 原型（参考）
- `screenConfigs` + `showScreen()` + `historyStack` + `data-screen-target`

### Android 路由架构
- **字符串路由**：CampusRoutes sealed class 集中管理大部分路由（tab 根页、profile 子屏、社区/消息子屏等），通过 `CampusRoutes.xxx.route` 引用
- **类型安全路由**：GoodsDetail / LostDetail / LostClaim / AnnouncementDetail 使用顶层 @Serializable data class，通过 composable<T>() 导航，无需手动声明 arguments/NavType
- **嵌套图类型**：HomeGraph / PublishGraph / CommunityGraph / MessageGraph / ProfileGraph（@Serializable data object），供 navigation<T>() 创建独立 back stack，CampusBottomNav 通过 `hasRoute<T>()` 判定当前所处 Tab
- **Tab 导航**：singleTop + saveState + restoreState 模式，避免重复入栈并保持各 Tab 滚动位置
- **返回栈**: NavController.popBackStack()

## 首页视图切换机制
- 首页不仅有主 screen `home`
- 还使用 `viewConfigs` 做二级内容切换：
  - `runner`（跑腿 — 快递、帮买、公告）
  - `market`（二手物品 — 同校面交）
  - `lost`（失物招领 — 寻物/招领）
- Android 实现：FilterChip 选择器 + HorizontalPager 滑动切换（HomeScreen.kt）
- `switchView(view)` 负责切换首页子视图或直达对应详情页

## 底部导航
当前底部导航项已验证为：
- 首页（home）— HomeTab 嵌套图（HomeGraph）
- 发布（publish-hub）— PublishTab 嵌套图（PublishGraph）
- 社区（community）— CommunityTab 嵌套图（CommunityGraph）
- 消息（message）— MessageTab 嵌套图（MessageGraph）
- 我的（profile）— ProfileTab 嵌套图（ProfileGraph）

选中判定：通过 `NavDestination.hasRoute<T>()` 匹配嵌套图类型，自动覆盖嵌套图内所有子页面。

## 当前代码组织方式
- 单文件 HTML 原型
- CSS 与 JS 内联在同一文件中
- UI 以移动端小程序样式模拟为主
- 通过 `.screen` + `.active` 控制页面显示
- Android 端：Kotlin 2.1.20 + Compose + Navigation Compose 2.9.0

## 数据来源
- 当前为静态展示型原型
- 内容主要为写死在 HTML 中的演示数据
- 暂未发现真实接口请求或外部数据依赖

## 运行事实
- 当前原型理论上可直接双击浏览器打开
- 更稳妥的预览方式仍建议使用本地静态服务
- 当前未发现构建流程依赖

## 验证状态追踪
| 验证项目 | 状态 |
|----------|------|
| 页面清单准确性 | 已验证 |
| screen 切换机制 | 已验证 |
| 数据来源 | 初步验证 |
| 代码组织方式 | 已验证 |
| 运行方式 | 初步验证 |
| 类型安全路由迁移（4 条） | 已验证（BUILD SUCCESSFUL） |
| Navigation hasRoute<T>() | 已验证（BUILD SUCCESSFUL） |
| saveState/restoreState | 已验证（BUILD SUCCESSFUL） |
| Phase 4 跑腿全链路 | 已验证（BUILD SUCCESSFUL） |
| Phase 5 二手交易全链路 | 已验证（BUILD SUCCESSFUL） |
| Phase 6 失物+社区+聊天+FCM | 已验证（BUILD SUCCESSFUL） |
