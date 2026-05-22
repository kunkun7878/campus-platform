# 校园聚合平台 - 术语/概念定义表

<!-- last_sync: 2026-05-22T15:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_rules]] · [[campus_work_rules]]

> 目的：统一项目内术语，防止多 agent / 多人理解不一致。

## 核心业务

| 术语 | 定义 | 备注 |
|------|------|------|
| 跑腿 | 用户发布代取/代送/代办需求，其他用户接单完成的业务 | 首页核心业务 |
| 代取 | 跑腿的子类型，帮人取物 |
| 发布 | 用户发起跑腿需求/二手商品/失物招领的动作 |
| 接单 | 用户接受并执行他人发布的跑腿需求 |
| 二手交易 | 校内二手物品买卖 | 独立入口 |
| 失物招领 | 发布遗失物品或招领信息 | 独立入口 |
| 认领 | 用户确认某失物为自己的并发起认领请求 |

## 社区

| 术语 | 定义 | 备注 |
|------|------|------|
| 校园墙 | 校内公共信息发布与讨论空间 | 社区模块 |
| 讨论区 | 用户自由发帖讨论的区域 |
| 官方群 | 由平台/学校官方创建并置顶的群组 | 聊天/交友/兼职 |
| 置顶群 | 始终排在社区列表顶部的官方群 |

## 订单与售后

| 术语 | 定义 | 备注 |
|------|------|------|
| 订单 | 跑腿需求被接单后生成的交易记录 |
| 售后 | 订单完成后的退换、申诉、纠纷处理 | 状态流转待确认 |
| 申请售后 | 用户发起售后请求的动作 |
| 售后详情 | 售后请求的详细信息与处理进度 |

## 系统

| 术语 | 定义 | 备注 |
|------|------|------|
| 学校隔离 | 不同学校之间的内容/数据互相不可见 | 核心硬规则 |
| 选校 | 用户首次使用时选择所属学校 | 选择后不可自由切换 |
| 代理 | 学校层级的授权管理者 | 后续支持代理后台 |
| 代理后台 | 代理管理本校内容、订单、用户的系统 |
| screen | 当前原型中单文件 HTML 内的一个视图/页面 | 通过 screenConfigs + showScreen 切换；Android 阶段：screen = Navigation route + @Composable destination |
| 高保真原型 | 单文件 HTML 实现的接近最终效果的交互原型 | 当前阶段产物（已完成），作为 Android UI 参考 |

## 协作

| 术语 | 定义 | 备注 |
|------|------|------|
| 经理 | 当前主会话，负责拆任务、派Agent、审查、打回、收口、同步记忆 | 即总控角色 |
| 执行Agent | 按需派生的子Agent，接任务包改HTML代码，自检后提交 | 模型用DeepSeek V4 Pro |
| 审查Agent | 按需派生的子Agent，独立审查执行Agent产出，产出审查报告 | 模型用GPT-5.4 |
| 任务包 | 经理派给执行Agent的标准化任务说明：做什么、范围、规范、禁止项、产出要求 | |
| 审查报告 | 审查Agent产出的独立文件：通过/打回/阻塞 + 问题清单 | "部分通过"结论已于2026-05-21作废 |
| 打回单 | 审查不通过时经理整理的问题清单+修复要求，派回执行Agent | |
| campus-memory-sync | 关键节点同步 project_memory/ 下记忆文件的 skill | 经理调用 |
| campus-project-guard | 规则守护与冲突检测的 skill | 经理调用 |
| frontend-design | 高质量前端界面设计 skill | 执行Agent |
| verification-before-completion | 强制自检：先验证再声明完成 | 执行Agent |
| receiving-code-review | 正确接收和处理审查反馈 | 执行Agent |
| systematic-debugging | 系统化调试：先找根因再提修复 | 审查Agent |
| ubiquitous-language | DDD术语提取与一致性检查 | 审查Agent |
| subagent-driven-development | 每任务派生Agent+两阶段审查 | 经理 |
| dispatching-parallel-agents | 独立任务并行派发 | 经理 |
| brainstorming | 需求澄清（原始SOP阶段A） | 经理 |
| writing-plans | 结构化实现计划（原始SOP阶段B） | 经理 |
| requesting-code-review | 标准化审查请求流程 | 经理 |
| grill-me | 派单前逐层追问压力测试计划 | 经理 |
| executing-plans | 结构化执行开发计划，追踪任务完成状态 | 经理 |
| git-guardrails-claude-code | 分支安全防护，禁止危险 git 操作 | 经理 |
| design-an-interface | 并行子Agent生成多种API/模块接口设计方案 | 经理 |
| domain-model | 挑战计划vs领域模型，更新ADR决策文档 | 经理 |
| finishing-a-development-branch | 验证测试→选择合并方式→清理分支 | 经理 |
| claude-api | Supabase API / FCM / Edge Function 开发 | 执行Agent |
| qa | 系统化质量保证审查，结构化问题清单 | 审查Agent |
| webapp-testing | Playwright浏览器自动化测试 | 审查Agent（后期） |
| test-driven-development | 先写测试→看失败→最小代码通过 TDD | 执行Agent（后期） |
| tdd | 深模块/接口设计/模拟/重构 TDD 方法论 | 执行Agent（后期） |
| improve-codebase-architecture | 探索代码库找架构改进机会 | 审查Agent（后期） |
