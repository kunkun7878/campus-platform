---
name: campus-memory-sync
description: 校园聚合平台项目记忆同步。在关键节点（任务前后、规则变更、代码修改后）强制同步 project_memory/ 下的记忆文件。Use when working on 校园聚合平台, after code changes, before/after tasks, or when rules/decisions change.
---

# Campus Memory Sync

同步校园聚合平台项目的长期记忆体系。

## 记忆文件位置

主副本：`C:\Users\admin\Desktop\校园聚合平台\project_memory\`

位置以 `project_memory/README.md` 的声明为准。若 README 不可达，暂停询问用户。

## 核心文件（14个，按系统6层组织。本表权威描述见 `project_memory/README.md` §文件分层）

| 层 | 文件 | 存什么 |
|----|------|--------|
| L1 协作规则 | `campus_work_rules.md` | 协作纪律（所有禁止项唯一来源） |
| L2 产品规则 | `campus_rules.md` | 产品硬规则 |
| L3 状态追踪 | `campus_status.md` | 当前阶段、已完成、阻塞、下一步 |
| L3 状态追踪 | `iteration_current.md` | 当前迭代目标与任务清单 |
| L3 状态追踪 | `campus_session_log.md` | 会话推进记录 |
| L4 知识库 | `campus_decisions.md` | 已确认技术/路线决策 |
| L4 知识库 | `campus_ui_decisions.md` | UI/交互/视觉决策 |
| L4 知识库 | `campus_glossary.md` | 术语表 |
| L4 知识库 | `codebase_map.md` | 代码/页面事实地图 |
| L4 知识库 | `runtime_notes.md` | 运行环境与预览说明 |
| L4 知识库 | `page_state_template.md` | 页面与状态盘点模板 |
| L5 问题管理 | `campus_open_questions.md` | 待确认问题 |
| L5 问题管理 | `campus_conflicts.md` | 规则冲突记录 |
| — | `README.md` | 记忆体系说明、读取顺序、灾难恢复 |

## 触发节点（7个：A-E 核心 + H-I 安全网。F/G 为 OpenClaw 原版预留位，Claude Code 版未使用）

### A. 会话开始 — 每次新会话启动时
- 读 `campus_status.md`、`iteration_current.md`、`campus_rules.md`、`campus_work_rules.md`
- 确保上次会话的"下一步"与当前状态衔接

### B. 规则/决策确认后 — 用户拍板新规则或决策时
- 新规则 → `campus_rules.md` 或 `campus_work_rules.md`
- 新决策 → `campus_decisions.md`
- 新 UI 决策 → `campus_ui_decisions.md`
- 新冲突 → `campus_conflicts.md` → 暂停等用户拍板

### C. 代码修改前 — 派执行Agent之前
- 记录修改意图到 `campus_session_log.md`

### D. 代码修改后 — 执行Agent完成、审查通过后
- 更新 `campus_session_log.md`（改了什么、为什么、怎么验证）
- 新代码事实 → `codebase_map.md`
- 新运行信息 → `runtime_notes.md`

### E. 任务完成前 — 一个任务包闭环时
- 更新 `campus_status.md`、`iteration_current.md`、`campus_session_log.md`
- 新待确认问题 → `campus_open_questions.md`

### H. 会话启动补救 — A节点执行后额外检查
- 检查 `campus_session_log.md` 最后一条是否与本会话衔接
- 若上次会话后有未记录空窗 → 补"会话中断/恢复"标记
- 若上次记录的"下一步"与当前 `campus_status.md` 不一致 → 暂停，先对齐

### I. 防遗忘周期检查 — 每完成 5 次实质性动作后
"实质性动作"定义（满足任一算一次）：
- 派生一次 Agent（执行或审查）
- 完成一次文件编辑（Read 不算，Edit/Write 算）
- 写入一次记忆文件
- 完成一次审查判定
- 产出一份审查报告

检查内容：
- 回顾最近 5 个动作是否产生了需要写入记忆文件的内容
- 若有变更未写入 → 补写
- 若 `campus_open_questions.md` 中有超过 7 天未更新的待确认项 → 提醒用户

## 同步原则

1. **无新增不硬写**：没有新内容不为了动作而写
2. **未确认标注"待确认"**：不把猜测写入正式规则
3. **冲突先暂停**：不私自覆盖旧规则
4. **last_sync 标记**：修改文件后更新 `<!-- last_sync: ISO时间 CST -->`
5. **修改后必说清**：改了什么、为什么、怎么验证

## 最小执行清单

每次触发时确认：
- [ ] 规则/决策需要更新？
- [ ] 状态变了？
- [ ] 迭代任务状态变了？
- [ ] 有新的术语或UI决策？
- [ ] 有实质推进需要记录？
- [ ] 有新的代码/环境事实？
- [ ] 有冲突？
- [ ] 有待确认问题？
