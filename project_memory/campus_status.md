# 校园聚合平台 - 当前状态

<!-- last_sync: 2026-05-22T14:00 CST -->

> 关联：[[PROJECT_HOME]] · [[iteration_current]] · [[campus_rules]] · [[campus_decisions]] · [[campus_open_questions]] · [[codebase_map]] · [[runtime_notes]] · [[campus_ui_decisions]] · [[campus_session_log]]

## 当前阶段
**Android 正式开发 — Phase 2 完成 ✅ → Phase 3 待开始**
原型阶段已完成（34 screen HTML 高保真原型）
多Agent工作流验证：**15轮实战全部通过** ✅（含 Phase 2 审查）

## 当前已完成
- HTML 高保真原型：34 screen ✅
- 多Agent工作流：15轮实战验证通过 ✅
- 项目记忆系统：14个Markdown文件建立 + 持续维护 ✅
- Skills体系：54个 skill 全部安装激活 ✅
- 技术栈确定：Compose + Hilt + Navigation + Retrofit + Room + Coil + Supabase ✅
- Phase 0: 环境搭建（AS + SDK + 项目创建 + 依赖 + git） ✅
- Phase 1: 项目骨架 + 主题 + 导航（5 Tab + 34 route） ✅
- Phase 2: 认证 + 选校 ✅
  - Supabase 项目创建：campus-platform (ap-southeast-1)
  - 6 个 SQL Migration（profiles/schools/campuses/wechat_identities + RLS + auth_triggers）
  - Auth 模块：AuthRepository / AuthValidator / SchoolRepository / AuthModule
  - AuthGuard 登录守卫（Login/SchoolSelect/Home 三态判定）
  - 6 个 Screen：Login(双模式) / Register(3步) / PasswordReset / AccountDelete / SchoolSelect(两级)
  - 2 个组件：PasswordStrengthBar / CaptchaDialog
  - Supabase Kotlin SDK 3.1.2 集成
  - 编译通过：./gradlew assembleDebug BUILD SUCCESSFUL
  - SQL 待用户手动执行（Dashboard SQL Editor）

## 已知限制
- `verifyOtp()` SDK 兼容性问题（OtpType.Phone enum），密码登录正常可用
- 微信登录代码搁置（需微信开放平台企业认证）
- SQL migrations 需用户在 Supabase Dashboard 执行

## 当前阻塞
- Android Studio 中文插件暂无适配（平台253，等待JetBrains更新）

## 下一步
1. Phase 3: 数据层基座（17张表DDL + Room + Retrofit + Repository）
