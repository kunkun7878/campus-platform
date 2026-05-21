---
name: campus-project-guard
description: 校园聚合平台项目规则守护。检查每次变更是否违反已有规则、是否产生新冲突。Use when rules change, before/after tasks, or when checking if new requirements conflict with existing rules.
---

# Campus Project Guard

守护校园聚合平台项目的规则一致性。**只做冲突检测，不重复 [[campus_work_rules]] 已定义的诚实/交付/修改原则。** 发现冲突时必须暂停并显式呈现给用户。

## 职责范围

本 skill 只负责一件事：**检测新要求/新变更是否与已有规则冲突。**

其他职责归属：
- 诚实/交付/修改原则 → [[campus_work_rules]] §1/§2/§5
- 记忆同步 → campus-memory-sync skill
- 禁止项 → [[campus_work_rules]] §7.4（唯一来源）

## 监控文件（6个，覆盖冲突可能发生的所有面）

| 文件 | 冲突维度 |
|------|---------|
| `campus_rules.md` | 新要求 vs 产品硬规则 |
| `campus_work_rules.md` | 新流程 vs 协作纪律 |
| `campus_decisions.md` | 新决策 vs 已确认决策 |
| `campus_conflicts.md` | 冲突累积记录 |
| `campus_status.md` | 新方向 vs 当前阶段 |
| `campus_open_questions.md` | 新假设 vs 待确认项 |

为何不是全部 14 个文件：其余 8 个文件（iteration_current、campus_session_log、ui_decisions、glossary、codebase_map、runtime_notes、page_state_template、README）是事实记录或模板，不产生规则冲突，不需要 guard 检测。

## 会话开始检查

（与 [[CLAUDE]] 统一清单保持一致）

1. 读 `campus_status.md` — 当前阶段与阻塞
2. 读 `iteration_current.md` — 本轮目标
3. 读 `campus_rules.md` + `campus_work_rules.md` — 刷新约束
4. 读 `campus_open_questions.md` — 待确认项
5. 概述当前已知规则、约束与待确认项

**本 skill 需手动调用，不会自动触发。** 经理在会话开始时主动调用。

## 规则变更检查

本 skill 只做冲突检测。当新增任何要求时：
1. 判断该要求是否与已有规则冲突
2. 无冲突 → 不动作（由 campus-memory-sync 或经理负责写入正确文件）
3. 有冲突 → 按下方格式展示冲突 → 暂停 → 等用户拍板
4. 若规则未明确 → 标记"待确认"，由经理决定是否写入 `campus_open_questions.md`

写入操作（包括更新 campus_conflicts、campus_rules 等）统一由 campus-memory-sync skill 或经理执行，本 skill 不直接写文件。

## 冲突展示格式

```
规则编号：[CF-XXX]
原规则：[原文]
新规则：[原文]
冲突原因：[说明]
影响：[哪些任务/页面受影响]
建议：[推荐处理方式]
```

## 项目规则优先级

> 详见 [[campus_work_rules]] §6（权威来源）。此处不重复复述。
