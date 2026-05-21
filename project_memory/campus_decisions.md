# 校园聚合平台 - 已确认决策

<!-- last_sync: 2026-05-21T13:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_rules]] · [[campus_work_rules]] · [[campus_open_questions]] · [[iteration_current]] · [[codebase_map]] · [[campus_ui_decisions]]

## 产品路线决策
1. 当前阶段：Android 原生应用正式开发（Kotlin + Jetpack Compose + Supabase），基于 HTML 原型作为 UI 参考，分 7 Phase 推进至 MVP。原型已进入收尾阶段，不再作为主要开发目标。
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
11. 24 个 skill 已安装到 `.claude/skills/`（含原有的 campus-memory-sync + campus-project-guard）。分配详情见 campus_work_rules.md §7.1.1。20 个当前激活，4 个后期激活。

## 已确认的后续决策
- ✅ 记忆体系主编辑环境：Claude Code 的 Edit 工具 + Obsidian 查看
- ✅ 多 Agent 角色：不需要固定常驻，按需派生执行Agent和审查Agent
- ✅ Obsidian 目录布局：整个 `校园聚合平台/` 作为 vault 根目录
- ✅ 不使用额外的 memory 插件/wiki 层，project_memory/ Markdown 文件足够
- ✅ 24 个 skill 从原始 skill 包分析筛选，20 个当前激活 + 4 个后期激活
