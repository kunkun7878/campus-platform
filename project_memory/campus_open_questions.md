# 校园聚合平台 - 待确认问题

<!-- last_sync: 2026-05-21T19:30 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_decisions]] · [[campus_rules]] · [[codebase_map]] · [[runtime_notes]] · [[campus_ui_decisions]] · [[iteration_current]]

## 已确认（已闭环）

1. ✅ 主原型文件路径：`C:\Users\admin\Desktop\校园聚合平台\campus-miniapp-prototype.html`
2. ✅ 多 Agent 采用「经理 + 执行Agent + 审查Agent 按需派生」模式
3. ✅ 多 Agent 工作流首次实战已通过（P0-1选校机制）
4. ✅ Obsidian 作为人工知识库 + Claude Code Edit 工具协作
5. ✅ Skills 已安装到 `.claude/skills/`（24个，全部激活）
6. ✅ 项目从 OpenClaw 迁移到 Claude Code 完成
7. ✅ 原型阶段 P0 修复：12/15 已修复 + 3/15 降级缓解
8. ✅ 聊天系统（chat-detail + group-chat + post-detail + post-create）全部完成
9. ✅ market/lost 列表视图：面板内列表 + 详情页
10. ✅ 技术栈：Kotlin + Compose + Hilt + Navigation + Retrofit + Room + Coil + Supabase
11. ✅ 后端方案：Supabase（零后端代码，建表即API）
12. ✅ 开发路线：8 Phase（Phase 0-7）Android MVP
13. ✅ 工作流规则：取消经理修小问题，审查结论简化至通过/打回/阻塞
14. ✅ 14项P0修复顺序：已按经理→执行Agent→审查Agent完成
15. ✅ 22个新增screen问题：已纳入8 Phase（Phase 0-7）中
16. ✅ 聊天系统到群聊页面程度：已完成

## 待用户确认

1. Android Studio 中文插件：等待 JetBrains 更新到支持平台 253（2025.3）
2. Supabase 项目：待 Phase 1 完成后注册创建（Phase 2 认证开发前需要）。当前状态：未注册，GitHub 仓库已就绪（kunkun7878/campus-platform）。
3. Firebase 项目：何时创建？（Phase 7 FCM 推送前需要）

## 原有待确认问题（待推进）

4. 选校后不可自由切换，是否存在申诉/人工切换场景？
5. 社区的审核、发言、官方群管理规则由谁定义？
6. 代理后台的具体功能范围和权限边界？
