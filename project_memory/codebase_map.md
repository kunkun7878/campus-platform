# 校园聚合平台 - 代码/页面事实地图

<!-- last_sync: 2026-05-22T14:00 CST -->

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
│       │           ├── market/                     （AfterSale*, LostPublish, MarketPublish, Order*）
│       │           ├── message/                    （ChatDetail, Message）
│       │           ├── profile/                    （About, Wallet, Coupons 等 12 屏）
│       │           └── publish/                    （PublishHub, Publish）
│       └── res/
```

## 已验证页面/screen 清单（来自真实 HTML）— 共34个

| 页面/screen | 来源 | 状态 |
|-------------|------|------|
| school-select | 真实 HTML `screenConfigs` | ✅ Phase 2 已实现（学校-校区两级选择器） |
| home | 真实 HTML `screenConfigs` | 已验证（HorizontalPager + FilterChip） |
| publish | 真实 HTML `screenConfigs` | 已验证 |
| publish-hub | 真实 HTML `screenConfigs` | 已验证 |
| order-detail | 真实 HTML `screenConfigs` | 已验证 |
| order-list | 真实 HTML `screenConfigs` | 已验证 |
| after-sale-apply | 真实 HTML `screenConfigs` | 已验证 |
| after-sale-detail | 真实 HTML `screenConfigs` | 已验证 |
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
| market-publish | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| lost-publish | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| wallet | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| runner-apply | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| address-manage | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| coupons | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| invite | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| feedback | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| about | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| my-published | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| my-sold | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| my-bought | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |
| my-favorites | 真实 HTML `screenConfigs` | 已有占位组件，待对应 Phase 实现业务内容 |

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
