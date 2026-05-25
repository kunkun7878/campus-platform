# 校园聚合平台

> 关联：[[PROJECT_HOME]] · [[campus_work_rules]]

```
⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔
⛔                                                                    ⛔
⛔  🔒 经理铁律（campus-manager-rules）— 每会话必须执行，不可跳过          ⛔
⛔                                                                    ⛔
⛔  1. 派生 Agent 必须写 Skill要求：xxx。不写 = 违规。                    ⛔
⛔  2. 任务包只描述问题+边界，不写具体代码方案。                           ⛔
⛔  3. 执行Agent 先出思路 → 经理确认方向 → 再动手。                      ⛔
⛔  4. 经理不写代码。所有问题打回执行Agent修改。                          ⛔
⛔  5. 审查Agent 必须独立派生，不给引导性提示。                           ⛔
⛔                                                                    ⛔
⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔
```

## 每次开始工作前（必须逐条执行，不可跳步）

-1. **加载 campus-manager-rules Skill** ← 🔒 铁律
0. 加载 campus-memory-sync Skill
1. 读 [[PROJECT_HOME]]
2. 读 [[campus_status|当前状态]]
3. 读 [[iteration_current|当前迭代]]
4. 读 [[campus_work_rules|工作规则]] §7（角色与流程）+ §7.1.1（Skill分配表）+ §7.5（任务终点）
5. 读 [[campus_open_questions|待确认问题]]
6. 调用 campus-memory-sync（节点A）

## 派生 Agent 铁律 ⛔

**不写 Skill要求 就派生 = 违规。** 每次派生必须在 prompt 中写：

```
Skill要求：xxx, xxx
```

**按需配 Skill，不堆砌。** 对照表见 [[campus_work_rules]] §7.1.1——从表中选该任务需要的 Skill，2-5个即可。

## 任务类型与终点

| 用户说什么 | 怎么做 | 停在哪 |
|-----------|--------|--------|
| "分析/审查" | 经理分析 or 派生2-3个执行Agent并行 → 汇总 | 汇报即停，不派生审查Agent |
| "改/修/做" | 经理写任务包(问题+边界) → 执行Agent出思路 → 确认方向 → 执行Agent动手 → 审查Agent独立审查 → 经理判定 | 审查通过即停 |
| "规划" | 产出计划文档 | 用户确认即停 |

**核心：到达终点即停止。不自追加阶段。不循环审查。**

## 任务包格式

```
## 问题
（描述要解决什么，不写具体怎么修）

## 边界
- 涉及范围：哪些文件/模块
- 禁止碰：哪些文件/模块不能动
- 参考：campus_rules.md / campus_ui_decisions.md

## Skill要求
（从 §7.1.1 表中选择该任务需要的 skill，2-5 个）
```

## 当前关键数据

- 原型：34 screen，`prototype/campus-miniapp-prototype.html`
- Android：Kotlin 2.1.20 + Compose + AGP 8.9.0 + Gradle 8.11.1
- Skills：54个（`.claude/skills/`），分配见 §7.1.1
- 阶段：Phase 7 完成 ✅
> 最新状态以 [[campus_status]] 为准
- GitHub：https://github.com/kunkun7878/campus-platform

## Obsidian

用 Obsidian 打开本目录作为 vault。入口：[[PROJECT_HOME]]。
