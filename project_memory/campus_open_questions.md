# 校园聚合平台 - 待确认问题

<!-- last_sync: 2026-05-23T23:30 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_decisions]] · [[campus_rules]] · [[codebase_map]] · [[runtime_notes]] · [[campus_ui_decisions]] · [[iteration_current]]

## 已确认（已闭环）

1. ✅ 主原型文件路径：`C:\Users\admin\Desktop\校园聚合平台\prototype\campus-miniapp-prototype.html`
2. ✅ 多 Agent 采用「经理 + 执行Agent + 审查Agent 按需派生」模式
3. ✅ 多 Agent 工作流首次实战已通过（P0-1选校机制）
4. ✅ Obsidian 作为人工知识库 + Claude Code Edit 工具协作
5. ✅ Skills 已安装到 `.claude/skills/`（54个，全部激活）
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
17. ✅ Supabase 项目已注册：campus-platform（ap-southeast-1，项目ref: fzmdhllxzyyzfpxkqpdy）。URL: https://fzmdhllxzyyzfpxkqpdy.supabase.co

## 已确认（Phase 6 闭环）

1. ✅ Firebase 项目：Phase 6 创建（用户已选方案 B，FCM 留 Phase 6）
2. ✅ 社区审核规则：Phase 6 本地敏感词（textfilter + Aho-Corasick），云端 AI 留 Phase 7
3. ✅ 官方群管理规则：Phase 6 DDL 扩展 direction 至 5 方向，Agent 管理后台留 Phase 7+
4. ✅ 决策 #33 冲突：用户确认修正（school_id RESTRICT + 内容表 CASCADE）
5. ✅ Phase 6 范围：P0+P1 全做，FCM 留 Phase 6

## 待用户确认

1. Android Studio 中文插件：等待 JetBrains 更新到支持平台 253（2025.3）

## 待推进（Phase 7+）

2. 代理后台的具体功能范围和权限边界？
3. 微信开放平台企业认证 → 微信登录
4. 云端 AI 内容审核（阿里云/腾讯云）
5. 悬赏金自动过期退款（30 天无人认领 → 解冻退回）
