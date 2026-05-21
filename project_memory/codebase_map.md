# 校园聚合平台 - 代码/页面事实地图

<!-- last_sync: 2026-05-21T13:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[runtime_notes]]

## 主文件路径
- HTML 原型：`C:\Users\admin\Desktop\校园聚合平台\campus-miniapp-prototype.html`（34 screen，约5200行）
- Android 项目：`C:\Users\admin\Desktop\校园聚合平台\android\`（Kotlin + Compose）

## 关联文件
- `project_memory/`：项目长期记忆主副本（14个Markdown文件）
- `legacy_openclaw/`：OpenClaw 时代多Agent规范文档（已归档）
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
│       │   └── CampusApplication.kt   （Hilt Application）
│       └── res/
```

## 已验证页面/screen 清单（来自真实 HTML）— 共34个

| 页面/screen | 来源 | 状态 |
|-------------|------|------|
| school-select | 真实 HTML `screenConfigs` | 已验证（P0-1新增） |
| home | 真实 HTML `screenConfigs` | 已验证 |
| publish | 真实 HTML `screenConfigs` | 已验证 |
| publish-hub | 真实 HTML `screenConfigs` | 已验证 |
| order-detail | 真实 HTML `screenConfigs` | 已验证 |
| order-list | 真实 HTML `screenConfigs` | 已验证 |
| after-sale-apply | 真实 HTML `screenConfigs` | 已验证 |
| after-sale-detail | 真实 HTML `screenConfigs` | 已验证 |
| goods-detail | 真实 HTML `screenConfigs` | 已验证 |
| lost-detail | 真实 HTML `screenConfigs` | 已验证 |
| lost-claim | 真实 HTML `screenConfigs` | 已验证 |
| community | 真实 HTML `screenConfigs` | 已验证 |
| message | 真实 HTML `screenConfigs` | 已验证（3条message-row→chat-detail） |
| profile | 真实 HTML `screenConfigs` | 已验证 |
| announcement-detail | 真实 HTML `screenConfigs` | 已验证 |
| **chat-detail** | 真实 HTML `screenConfigs` | 已验证（Batch2-T8新增，微信级气泡UI） |
| **post-detail** | 真实 HTML `screenConfigs` | 已验证（Batch2-T9新增，评论+互动） |
| **post-create** | 真实 HTML `screenConfigs` | 已验证（Batch2-T10新增，发帖表单） |
| **group-chat** | 真实 HTML `screenConfigs` | 已验证（Batch2-T11新增，群聊多人气泡） |
| login | 真实 HTML `screenConfigs` | 待迁移 Android |
| register | 真实 HTML `screenConfigs` | 待迁移 Android |
| market-publish | 真实 HTML `screenConfigs` | 待迁移 Android |
| lost-publish | 真实 HTML `screenConfigs` | 待迁移 Android |
| wallet | 真实 HTML `screenConfigs` | 待迁移 Android |
| runner-apply | 真实 HTML `screenConfigs` | 待迁移 Android |
| address-manage | 真实 HTML `screenConfigs` | 待迁移 Android |
| coupons | 真实 HTML `screenConfigs` | 待迁移 Android |
| invite | 真实 HTML `screenConfigs` | 待迁移 Android |
| feedback | 真实 HTML `screenConfigs` | 待迁移 Android |
| about | 真实 HTML `screenConfigs` | 待迁移 Android |
| my-published | 真实 HTML `screenConfigs` | 待迁移 Android |
| my-sold | 真实 HTML `screenConfigs` | 待迁移 Android |
| my-bought | 真实 HTML `screenConfigs` | 待迁移 Android |
| my-favorites | 真实 HTML `screenConfigs` | 待迁移 Android |

## 页面切换机制

### HTML 原型（参考）
- `screenConfigs` + `showScreen()` + `historyStack` + `data-screen-target`

### Android（目标实现）
- Navigation Compose: NavHost + NavController + 类型安全路由
- 返回栈: NavController.popBackStack()
- 底部导航: NavigationBar + NavController.navigate()
- （Phase 1 实施时补充具体实现细节）

## 首页视图切换机制
- 首页不仅有主 screen `home`
- 还使用 `viewConfigs` 做二级内容切换：
  - `runner`
  - `market`
  - `lost`
- `switchView(view)` 负责切换首页子视图或直达对应详情页

## 底部导航
当前底部导航项已验证为：
- 首页（home）
- 发布（publish）
- 社区（community）
- 消息（message）
- 我的（profile）

## 当前代码组织方式
- 单文件 HTML 原型
- CSS 与 JS 内联在同一文件中
- UI 以移动端小程序样式模拟为主
- 通过 `.screen` + `.active` 控制页面显示

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
