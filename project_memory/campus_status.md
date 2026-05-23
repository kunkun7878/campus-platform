# 校园聚合平台 - 当前状态

<!-- last_sync: 2026-05-22T20:00 CST -->

> 关联：[[PROJECT_HOME]] · [[iteration_current]] · [[campus_rules]] · [[campus_decisions]] · [[campus_open_questions]] · [[codebase_map]] · [[runtime_notes]] · [[campus_ui_decisions]] · [[campus_session_log]]

## 当前阶段
**Android 正式开发 — Phase 3 完成 ✅ → Phase 4 待开始**
原型阶段已完成（34 screen HTML 高保真原型）
多Agent工作流验证：**21轮实战全部通过** ✅（含 Phase 3 完整流程）

## 当前已完成
- HTML 高保真原型：34 screen ✅
- 多Agent工作流：21轮实战验证通过 ✅
- 项目记忆系统：14个Markdown文件建立 + 持续维护 ✅
- Skills体系：54个 skill 全部安装激活 ✅
- 技术栈确定：Compose + Hilt + Navigation + Retrofit + Room + Coil + Supabase ✅
- Phase 0: 环境搭建（AS + SDK + 项目创建 + 依赖 + git） ✅
- Phase 1: 项目骨架 + 主题 + 导航（5 Tab + 34 route） ✅
- Phase 2: 认证 + 选校 ✅
- Phase 3: 数据层基座 ✅
  - SQL 层：36 张表 DDL + 15 Migration + 15 Revert
  - Room 层：25 Entity + 7 DAO + 7 Mapper + AppDatabase + TypeConverters
  - Repository 层：16 接口 + 16 实现 + NetworkModule + RepositoryModule
  - ViewModel 层：37 ViewModel + 全部 36 Screen 改造 + MainActivity/NavGraph 重构
  - 编译验证：BUILD SUCCESSFUL
  - 审查验证：10轮审查，全部通过
  - 新增文件：30 SQL + 95 Kotlin = 125 个文件
  - SQL 待用户在 Supabase Dashboard 执行

## 已知限制
- `verifyOtp()` SDK 兼容性问题（OtpType.Phone enum），密码登录正常可用
- 微信登录代码搁置（需微信开放平台企业认证）
- SQL migrations 需用户在 Supabase Dashboard 执行
- Room destructive migration 为开发阶段策略，生产发布前需替换
- DataStore 已移除 authToken（Supabase SDK 自行管理 session）

## 当前阻塞
- Android Studio 中文插件暂无适配（平台253，等待JetBrains更新）

## 下一步
1. Phase 4: 跑腿全链路（8 screen — Home/Publish/OrderDetail/OrderList 等）
