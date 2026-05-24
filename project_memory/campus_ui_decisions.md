# 校园聚合平台 - UI/交互/视觉决策

<!-- last_sync: 2026-05-23T23:30 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_rules]] · [[codebase_map]]
>
> ⚠️ **阶段说明**：本文件记录 HTML 原型阶段的视觉风格决策（CSS变量、screen切换、data属性绑定），供 Android Compose 实现时作为视觉参考。Android 阶段的具体 UI 实现使用 Compose MaterialTheme + Navigation Compose，等效映射见 [Android 映射] 节。

> 目的：统一记录已确认的 UI 风格、交互模式、组件约定，防止多 agent / 多轮迭代中视觉风格漂移。

## 整体风格
- 蓝白校园风
- 简洁、现代、不花哨
- 优先保证可读性和可用性
- 当前原型采用“移动端小程序模拟壳”视觉风格

## 已从真实原型提取的配色变量
- `--bg: #f5f8ff`
- `--panel: rgba(255,255,255,0.92)`
- `--panel-strong: rgba(255,255,255,0.97)`
- `--text: #1b2850`
- `--muted: #7c89a6`
- `--brand: #2d6bff`
- `--brand-deep: #184acc`
- `--accent: #12b7ae`
- `--warm: #ff9a62`
- `--rose: #f45f89`

## 页面布局

### 首页
- 主打跑腿卡片
- 跑腿卡片为首页视觉重心
- 二手/失物不占用首页主要空间，而通过切换或独立入口进入
- 首页顶部包含品牌标题、搜索栏、筛选入口、公告区

### 二手交易
- 独立详情 screen：`goods-detail`
- 与首页跑腿内容在信息结构上分离

### 失物招领
- 独立详情 screen：`lost-detail`
- 认领申请使用单独 screen：`lost-claim`

### 我的页面
- 高信息密度
- 包含订单状态、跑腿员入口、我的交易、常用功能、其他入口等模块
- 明显偏“小程序个人中心”布局

### 社区
- 独立 screen：`community`
- 方向仍符合官方群置顶 + 讨论区 + 校园墙 的结构目标

## 交互模式

### 页面切换
- 使用 `screenConfigs + showScreen()` 机制
- 使用 `historyStack` 支持回退
- 通过 `data-screen-target` 和 `data-nav-target` 做交互绑定
- 不刷新整个页面，单文件内切换视图

### 首页内容切换
- 首页还存在 `viewConfigs` 二级视图切换机制
- 当前视图包括：
  - `runner`
  - `market`
  - `lost`
- 其中 `market` / `lost` 可以直达详情页

### 底部导航
已从原型确认标签项：
- 首页
- 发布
- 社区
- 消息
- 我的

### 搜索与筛选
- 搜索栏与筛选器位于顶部 hero 区
- 筛选器为顶部按钮 + 下拉菜单形式
- 首页切换不同视图时，搜索提示文案会同步变化

### 弹窗 vs 独立页面
- 当前原型主要偏向独立 screen 跳转
- 公告详情、订单详情、售后、认领申请等均使用独立 screen

## 组件约定
- 按钮：主操作蓝色填充，次要操作浅底或描边
- 卡片：白色底、圆角、浅阴影
- 页面容器：手机壳式居中预览布局
- screen 切换：`.screen` + `.active`

## 响应式
- 当前以移动端小程序宽高比为主
- 通过 `.phone` 容器限制最大宽度（430px）
- 适合桌面浏览器预览移动端效果

## 待继续提取
- 字体/字号体系更细颗粒度梳理
- 图标风格统一性规则
- 动画/过渡效果统一规范
- 社区页内部结构的更完整设计约束

## Android Compose 等效映射（Phase 1 实施时填充）

| HTML 原型 | Android Compose |
|-----------|----------------|
| CSS `--brand: #2d6bff` | `MaterialTheme.colorScheme.primary` |
| CSS `--bg: #f5f8ff` | `MaterialTheme.colorScheme.background` |
| `showScreen(key)` | `navController.navigate(route)` |
| `historyStack` | `NavController.backStack` |
| `data-screen-target` | `onClick = { navController.navigate(route) }` |
| `.phone` 430px | `Modifier.widthIn(max = 430.dp)` |

> 上表将在 Phase 1 实施时根据实际 Compose 实现填充完整。
