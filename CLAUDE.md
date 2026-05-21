# 校园聚合平台

> 关联：[[PROJECT_HOME]] · [[campus_work_rules]]

Claude Code 会话入口。系统地图见 [[PROJECT_HOME]]。

## 每次开始工作前（必须逐条执行）

0. **加载 campus-manager-rules Skill** ← 🔒 经理铁律，不可跳过
1. 读 [[PROJECT_HOME]]
2. 读 [[campus_status|当前状态]]
3. 读 [[iteration_current|当前迭代]]
4. 读 [[campus_work_rules|工作规则]] §7（角色与流程）和 §7.1.1（Skill分配表）和 §7.5（任务终点）
5. 读 [[campus_open_questions|待确认问题]]
6. 调用 campus-memory-sync（节点A）

## 派生 Agent 铁律

**每次派生 Agent，prompt 中必须写：`Skill要求：xxx`**

对照表（详见 [[campus_work_rules]] §7.1.1，完整55 Skill 分配）：

经理的 brainstorming、writing-plans 等 11 个 skill 由经理在当前会话直接调用 Skill 工具，不派生 Agent。

| 派生角色 | Skill要求（当前激活） | 数量 |
|---------|---------------------|:--:|
| 执行Agent | compose-*, android-*, kotlin-*, supabase, claude-api, verification-before-completion, receiving-code-review | 24 |
| 审查Agent | systematic-debugging, qa, ubiquitous-language, android-security-best-practices, supabase-postgres-best-practices | 5 |

**不写 Skill 要求就派生 = 违规。**

## 任务终点（详见 [[campus_work_rules]] §7.5）

- 用户说"分析" → 产出结果 → 汇报 → **停**
- 用户说"改/修" → 执行Agent → 审查Agent → 经理判定通过 → campus-memory-sync → **停**
- 用户说"规划" → 产出计划 → 用户确认 → **停**

到达终点即停止。不自追加阶段。不循环审查。

## 原型

`campus-miniapp-prototype.html` — 34 screen，单文件 HTML+CSS+JS。原型为视觉参考使用，当前主开发目标为 Android 原生应用。

## Skills

项目管理：campus-memory-sync、campus-project-guard（经理手动调用）
Agent 工作 skill：53 个（+ 项目管理 2 个 = 共 55 个，详见 campus_work_rules §7.1.1）

## Obsidian

用 Obsidian 打开本目录作为 vault。入口文件：[[PROJECT_HOME]]。
