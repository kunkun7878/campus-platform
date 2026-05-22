# 校园聚合平台项目记忆说明

<!-- last_sync: 2026-05-22T15:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_rules]] · [[campus_work_rules]]

位置：`C:\Users\admin\Desktop\校园聚合平台\project_memory\`

这个目录是"校园聚合平台"项目的长期记忆层。**这是唯一 master 副本。**

目标：
- 让项目规则、决策、状态、待确认问题可长期沉淀
- 让后续多 agent / 多轮会话能够共享一致项目上下文
- 让内容保持 Markdown 兼容，可直接被 Obsidian 作为 vault 使用

---

## 文件分层（5层，14个文件。本表亦维护于 campus-memory-sync SKILL.md）

### Layer 1 — 协作规则
| 文件 | 存什么 |
|------|--------|
| `campus_work_rules.md` | 诚实原则、修改纪律、记忆同步、冲突处理、交付标准、规则优先级、多Agent流程。所有协作类"禁止"项唯一来源 |

### Layer 2 — 产品规则
| 文件 | 存什么 |
|------|--------|
| `campus_rules.md` | 产品硬约束：首页结构、学校隔离、社区要求、视觉风格 |

### Layer 3 — 状态追踪
| 文件 | 存什么 |
|------|--------|
| `campus_status.md` | 当前阶段、已完成、阻塞、下一步 |
| `iteration_current.md` | 当前迭代目标与任务清单 |
| `campus_session_log.md` | 每次推进的时间线记录 |

### Layer 4 — 知识库
| 文件 | 存什么 |
|------|--------|
| `campus_decisions.md` | 已确认技术/路线/过程管理决策 |
| `campus_ui_decisions.md` | UI风格、配色变量、布局约定、交互模式 |
| `campus_glossary.md` | 业务术语 + 协作术语统一定义 |
| `codebase_map.md` | 已验证页面清单、切换机制、数据来源 |
| `runtime_notes.md` | 预览方式、最小验证步骤、已知限制 |
| `page_state_template.md` | 页面/状态/链路/组件盘点模板 |

### Layer 5 — 问题管理
| 文件 | 存什么 |
|------|--------|
| `campus_open_questions.md` | 已闭环 + 待用户确认 + 待推进 |
| `campus_conflicts.md` | 规则冲突记录、影响评估、用户决策 |

---

## 读取顺序

**日常启动**（含5项读取+2项Skill调用）：使用 [[CLAUDE]] 的统一清单（7步）。

**完整上下文恢复**（新 agent 首次接入、灾难恢复后）：
1. `README.md`（本文件）
2. `campus_status.md`
3. `iteration_current.md`
4. `campus_rules.md` + `campus_work_rules.md`
5. `campus_open_questions.md`
6. `campus_decisions.md`
7. `codebase_map.md`
8. `runtime_notes.md`
9. `campus_glossary.md`
10. `campus_ui_decisions.md`
11. `campus_conflicts.md`（仅在怀疑冲突时读取）

---

## 维护原则

- 产品硬规则 → `campus_rules.md`
- 协作纪律 → `campus_work_rules.md`（所有禁止项的唯一来源）
- 已确认决策 → `campus_decisions.md`
- 当前状态与下一步 → `campus_status.md`
- 迭代任务 → `iteration_current.md`
- 每次推进记录 → `campus_session_log.md`
- 待确认问题 → `campus_open_questions.md`
- 规则冲突 → `campus_conflicts.md`
- 代码/页面事实 → `codebase_map.md`
- 运行环境 → `runtime_notes.md`
- 术语概念 → `campus_glossary.md`
- UI/交互决策 → `campus_ui_decisions.md`

注意：
- 不存敏感密钥
- 未验证内容标注"待确认"
- 规则冲突记录到 `campus_conflicts.md` 并等待用户拍板
- 每个文件头保留 `<!-- last_sync: ISO时间 CST -->` 标记
- Skills（campus-memory-sync / campus-project-guard）负责强制执行同步和冲突检测

---

## 灾难恢复

如果发现此目录部分或全部文件缺失：

1. 检查桌面项目目录是否仍然存在
2. 检查是否有 git 备份可恢复
3. 若部分文件缺失：从已有文件重建，优先恢复 `campus_rules.md` 和 `campus_status.md`
4. 若全部缺失：从本 README 的结构说明重建空白骨架，然后填入当前会话已知的最新信息
5. **不要在没有记忆文件的情况下继续执行高风险操作（修改代码、变更规则、大范围重构）**
6. 将恢复过程写入 `campus_session_log.md` 第一条

## 会话日志归档策略

`campus_session_log.md` 在每个迭代阶段结束时归档：
- 归档路径：`archive/session_log_YYYY-MM.md`
- 主文件重置为空模板，顶部保留指向归档文件的链接
- 归档触发条件：当前阶段标记为"已完成"时执行
