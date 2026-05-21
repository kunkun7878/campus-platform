# 校园聚合平台 Project Home

> 系统入口：[[CLAUDE]]（Claude Code 自动加载） | Obsidian 新手：[[OBSIDIAN_QUICKSTART]]
> 工作流步骤见 [[CLAUDE]] 的"每次开始工作前"清单，此处不重复。

---

## 系统架构（6 层）

### Layer 0 — 入口层
| 文件 | 用途 |
|------|------|
| [[CLAUDE]] | Claude Code 会话入口，含统一启动清单 |
| **PROJECT_HOME**（本文件） | 系统地图，Obsidian 浏览起点 |
| [[OBSIDIAN_QUICKSTART]] | Obsidian 零基础快速上手 |

### Layer 1 — 协作规则
| 文件                          | 用途                                                      |
| --------------------------- | ------------------------------------------------------- |
| [[campus_work_rules\|工作规则]] | 诚实原则、修改纪律、记忆同步、冲突处理、交付标准、规则优先级、多Agent流程。所有协作类"禁止"项的唯一来源 |
|                             |                                                         |

### Layer 2 — 产品规则
| 文件 | 用途 |
|------|------|
| [[campus_rules\|产品规则]] | 产品硬约束：首页结构、学校隔离、社区要求、视觉风格 |

### Layer 3 — 状态追踪
| 文件 | 用途 |
|------|------|
| [[campus_status\|当前状态]] | 阶段、已完成、阻塞、下一步 |
| [[iteration_current\|当前迭代]] | 本轮目标与任务清单 |
| [[campus_session_log\|会话日志]] | 每次推进的时间线记录 |

### Layer 4 — 知识库
| 文件 | 用途 |
|------|------|
| [[campus_decisions\|已确认决策]] | 产品路线、工程工具、过程管理决策 |
| [[campus_ui_decisions\|UI决策]] | 视觉风格、配色变量、布局约定、交互模式 |
| [[campus_glossary\|术语表]] | 业务术语 + 协作术语统一定义 |
| [[codebase_map\|代码地图]] | 已验证页面清单、切换机制、数据来源 |
| [[runtime_notes\|运行说明]] | 预览方式、最小验证步骤、已知限制 |
| [[page_state_template\|盘点模板]] | 页面/状态/链路/组件盘点模板 |

### Layer 5 — 问题管理
| 文件 | 用途 |
|------|------|
| [[campus_open_questions\|待确认问题]] | 已闭环 + 待用户确认 + 待推进 |
| [[campus_conflicts\|规则冲突]] | 冲突记录、影响评估、用户决策 |

### 执行层 — Skills（24个，`.claude/skills/`）

**项目管理（2个，经理调用）：**
| Skill | 用途 |
|-------|------|
| campus-memory-sync | 记忆文件同步（7个触发节点） |
| campus-project-guard | 规则冲突检测 |

**Agent 工作 skill（22个）：** 完整分配见 [[campus_work_rules]] §7.1.1。全部激活。

---

## 当前关键事实

- 原型：`campus-miniapp-prototype.html`，34 screen，单文件 HTML+CSS+JS，`screenConfigs` + `showScreen()` 切换
- 风格：蓝白校园风，10个 CSS 变量
- 审查：第一轮多Agent审查 → 46条去重问题（原15项P0，12项已修复，3项降级）
- 协作：经理 + 执行Agent（DeepSeek V4 Pro）+ 审查Agent（GPT-5.4）
- 记忆：`project_memory/` 是唯一 master 副本，14个 Markdown 文件

## 历史产出

- [[outputs/QA-001-综合审查报告\|QA-001 综合审查报告]]
- [[outputs/STRUCT-001-结构完整性盘点\|STRUCT-001 结构完整性盘点]]
- [[outputs/EXEC-001-主链路分析\|EXEC-001 主链路分析]]
- [[outputs/EXEC-002-次链路分析\|EXEC-002 次链路分析]]
- [[outputs/REVIEW-001-P0-1-选校机制审查报告\|REVIEW-001 选校机制审查]]
