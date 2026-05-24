# 校园聚合平台 - 项目规则（产品硬规则）

<!-- last_sync: 2026-05-23T23:30 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_decisions]] · [[iteration_current]] · [[codebase_map]]

## 1. 项目定位
- 当前项目是"校园聚合平台"
- 当前阶段：Android 原生应用正式开发（Kotlin + Jetpack Compose + Supabase）
- 基于 HTML 高保真原型（34 screen）作为 UI 参考，分 8 Phase（Phase 0-7）推进 Android MVP。后期考虑微信小程序

## 2. 产品硬规则
### 首页
- 首页主打跑腿业务
- 首页存在子视图切换：`runner` / `market` / `lost`

### 二手交易
- 保持独立入口，不并入首页主流

### 失物招领
- 保持独立入口，不并入首页主流

### 学校隔离
- 内容按学校隔离展示
- 学生选校后不可自由切换学校
- 选校后不可自由切换学校（可联系客服申诉），同校多校区可自由切换
- Android 阶段：所有表必须启用 Supabase Row Level Security（RLS），按 school_id 字段隔离数据
- Phase 3 数据层建表时同步编写 RLS 策略，审查Agent 必须专项检查 RLS 策略完整性

### 社区
- 需包含校园墙
- 需包含讨论区
- 需包含官方群结构
- 官方置顶群方向包括：聊天 / 交友 / 兼职

### 代理后台
- 未来要支持学校代理权限与代理后台

### 视觉风格
- 保持蓝白校园风
- "我的"页面保持高信息密度

## 3. 当前已确认
- 主原型文件路径：`C:\Users\admin\Desktop\校园聚合平台\prototype\campus-miniapp-prototype.html`
- 原型为单文件 HTML + 内联 CSS/JS，通过 `screenConfigs` + `showScreen()` 切换页面
- 底部导航：首页 / 发布 / 社区 / 消息 / 我的

- 选校后不可切换的例外机制：客服申诉（Phase 2 已确认）

## 4. 当前待补充规则
- 具体优先优化模块顺序：待确认
- 社区审核与管理规则：待确认
- 订单/售后状态流转规则：待确认
