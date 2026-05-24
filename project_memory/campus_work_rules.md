# 校园聚合平台 - 工作规则（协作纪律）

<!-- last_sync: 2026-05-24T16:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_rules]] · [[campus_decisions]] · [[campus_status]] · [[campus_conflicts]]

本文件是所有协作类规则和禁止项的唯一来源。Skills 不重复定义此处已有规则。

---

## 1. 诚实原则
- 做不到的事情必须直接说明
- 未验证的结果不能说已完成
- 不虚报运行成功、不伪造测试结果
- 不在无法验证时说"可运行"
- 不会就不会，做不到就做不到，禁止答非所问

## 2. 修改原则
- 修改前先理解现有结构
- 尽量局部演进，不随意大拆重构
- 在核心业务规则未明确前，不擅自补全高风险业务逻辑
- 修改代码时要说明涉及哪些文件、哪些模块、哪些机制

## 3. 记忆同步原则
- 关键节点必须同步更新项目记忆文件
- 同步规则和触发时机见 `campus-memory-sync` skill（A/B/C/D/E/H/I 七个节点）
- **谁触发**：经理在每个触发点主动调用 campus-memory-sync
- 未确认内容必须标注"待确认"
- 不硬写、不流水账、不伪造"已同步"

## 4. 冲突处理原则
- 旧规则与新规则冲突时，不得私自覆盖
- 必须记录到 [[campus_conflicts]]
- 必须向用户展示冲突内容并等待拍板

## 5. 交付原则
- 每次修改后必须说明：改了哪、为什么、怎么验证
- 每次任务完成后必须更新 [[campus_status]] 和 [[campus_session_log]]
- 每次发现新问题或待确认项必须补充到 [[campus_open_questions]]
- 审查报告存入 `archive/outputs/REVIEW-XXX-任务名.md`

## 6. 项目规则优先级
- 以后凡是校园聚合平台相关任务，必须以桌面项目目录 `C:\Users\admin\Desktop\校园聚合平台\` 下的规则与记忆文件为准
- 执行前至少刷新读取 `project_memory/README.md`、`campus_status.md`、`campus_rules.md`、`campus_work_rules.md`、`campus_open_questions.md`
- 涉及多 Agent 协作时，遵守本文件 §7
- 若用户口头新要求与文件冲突，必须先展示冲突，再等待用户拍板
- 不得声称"已永久记住所有文件全部内容"；应以文件作为权威规则源，关键任务前主动复核

---

## 7. 多Agent协作规则

### 7.1 角色定义
- **经理（当前主会话）**：拆任务、派单、审查判定、打回、收口、同步记忆。不亲自做大量代码修改。
- **执行Agent（按需派生，模型：DeepSeek V4 Pro）**：接明确任务包，编写代码，自检后提交。
- **审查Agent（按需派生，模型：GPT-5.4）**：独立审查执行Agent产出，发现遗漏、不一致、破坏。

### 7.1.1 Skill 分配（派生 Agent 时在 prompt 中要求使用）

**经理 skill（12个）：**（全部现在激活，负责流程管理，不参与代码编写）

| Skill | 用途 |
|-------|------|
| **campus-manager-rules** | **🔒 经理铁律：每次会话必须加载。强制 Skill要求/任务包格式/执行流程** |
| subagent-driven-development | 每任务派生Agent + 两阶段审查 |
| dispatching-parallel-agents | 独立任务并行派发 |
| brainstorming | 需求澄清，设计前强制使用 |
| writing-plans | 结构化实现计划 |
| requesting-code-review | 标准化审查请求 |
| grill-me | 派单前逐层追问压力测试 |
| executing-plans | 结构化执行计划，追踪任务状态 |
| git-guardrails-claude-code | 分支安全防护 |
| design-an-interface | 多方案API/模块接口设计 |
| domain-model | 领域模型 vs 计划校验 |
| finishing-a-development-branch | 验证测试→合并→清理 |

---

<!-- 2026-05-21 补入 frontend-design 和 kotlin-multiplatform-expect-actual，现在 23→24，后期 9→10 -->
**执行Agent skill（24个现在 + 10个后期）：**

| 类别 | Skill | 用途 | 激活 |
|------|-------|------|:--:|
| **Compose UI** | compose-best-practices | Compose UI 开发最佳实践 | 现在 |
| | compose-state-authoring | 状态创建、读写分离与状态提升 | 现在 |
| | compose-state-holder-ui-split | 状态持有与UI拆分 | 现在 |
| | compose-side-effects | 副作用API选择与Key管理 | 现在 |
| | compose-slot-api-pattern | 可复用组件槽位设计 | 现在 |
| | compose-modifier-and-layout-style | Modifier链与布局 | 现在 |
| | compose-focus-navigation | 焦点与导航 | 现在 |
| **架构与构建** | android-dependency-injection | Hilt DI 配置 | 现在 |
| | android-navigation-best-practices | Navigation Compose 路由 | 现在 |
| | android-networking | Retrofit + OkHttp | 现在 |
| | android-local-storage | Room + DataStore | 现在 |
| | android-build-infra | Gradle + 版本目录 | 现在 |
| | android-auth-identity | 认证 + Credential Manager | 现在 |
| | android-background-work | WorkManager + 后台任务 | 现在 |
| | android-accessibility-best-practices | 无障碍适配 | 现在 |
| **Kotlin** | kotlin-coroutines-best-practices | 协程通用最佳实践 | 现在 |
| | kotlin-coroutines-structured-concurrency | 协程作用域与取消 | 现在 |
| | kotlin-flow-state-event-modeling | StateFlow/SharedFlow/Channel | 现在 |
| | kotlin-types-value-class | 值类 vs 数据类 | 现在 |
| | kotlin-multiplatform-expect-actual | KMP expect/actual 平台抽象 | 后期 |
| **后端** | supabase | Supabase 全产品API | 现在 |
| | claude-api | Edge Function / FCM 开发 | 现在 |
| **质量** | frontend-design | 高质量前端界面设计 | 现在 |
| | verification-before-completion | 强制自检：先验证再声明完成 | 现在 |
| | receiving-code-review | 正确接收和处理审查反馈 | 现在 |
| **性能** | compose-recomposition-performance | 重组性能诊断 | 后期 |
| | compose-stability-diagnostics | 稳定性诊断 | 后期 |
| | compose-state-deferred-reads | 延迟读取优化 | 后期 |
| | android-performance-best-practices | 启动/内存/渲染优化 | 后期 |
| **测试** | compose-ui-testing-patterns | Compose UI 测试 | 后期 |
| | test-driven-development | TDD 方法论 | 后期 |
| | tdd | 深模块/接口设计 TDD | 后期 |
| **发布** | firebase-best-practices | FCM + Crashlytics | 后期 |
| | play-store-readiness | 上架准备 + 数据安全 | 后期 |

---

**审查Agent skill（5个现在 + 3个后期）：**

| 类别 | Skill | 用途 | 激活 |
|------|-------|------|:--:|
| **审查** | systematic-debugging | 系统化调试，先找根因再提修复 | 现在 |
| | qa | 系统化质量保证审查 | 现在 |
| | ubiquitous-language | 术语一致性检查 | 现在 |
| **安全** | android-security-best-practices | 权限/加密/密钥/RLS 审查 | 现在 |
| **数据库** | supabase-postgres-best-practices | SQL 优化 + RLS 策略审查 | 现在 |
| **架构** | improve-codebase-architecture | 架构改进机会探索 | 后期 |
| | webapp-testing | Playwright 浏览器测试（保留） | 后期 |
| **测试** | android-testing-best-practices | JUnit + MockK + 测试金字塔 | 后期 |

---

**激活状态说明：**
- **现在** = Android Phase 0 起即激活，派生 Agent 时必须带
- **后期** = Phase 3（数据层就绪）后激活，标注在任务包中
- 上表 54 个 Skill 已全部安装到 `.claude/skills/`
<!-- 注：campus_decisions.md (#11) 写 "54 个 skill（52 Agent + 2 项目管理）"，与 §7.1.1 表中 12+34+8=54 的总数一致，但细分 52 vs 54 存在 2 的差异。52=54-2（减去 campus-memory-sync + campus-project-guard 两个项目管理 skill？），待后续核实。 -->


**派生 prompt 中引用 skill：** 派生 Agent 时，在 prompt 中显式写 "Skill要求：[列出该角色当前激活的 skill 名称]"，Agent 会在执行时调用对应 skill。

### 7.2 派发规则
- 每次派生执行Agent必须给明确任务包：做什么、范围、规范、禁止项、产出要求
- 执行Agent的prompt必须自包含（不依赖当前会话上下文）
- 两个任务互不依赖时可并行派生2个执行Agent
- 审查Agent必须在执行Agent完成后派生，拿到的prompt不包含执行Agent的修改说明（独立判断）
- 审查Agent 必须检查 RLS 策略正确性（学校隔离），作为安全审查的固定检查项
- 任务包参考模板：`archive/legacy_openclaw/07-任务包模板.md`（术语替换：主智能体→经理、结构规范Agent→审查Agent）
- 审查单参考模板：`archive/legacy_openclaw/08-审查单模板.md`（结论三类：通过/打回/阻塞，"部分通过"已于2026-05-21作废）
- 审查报告存放：`archive/outputs/REVIEW-XXX-任务名.md`

### 7.2.1 任务包反模式（禁止把 Agent 变成打字员）

**任务包只描述问题+边界，不写具体方案。** 如果任务包里写了文件路径、行号、旧代码、新代码，Agent 就只会机械执行——不会自己分析、不会发现连带问题、不会质疑经理的判断。

| 违规（❌） | 正确（✅） |
|-----------|----------|
| "文件 xxx.sql 第229行，把 `USING (author_id = auth.uid() OR ...)` 改为 `USING (public.is_agent())`" | "SQL 审查发现 community_comments 和 community_posts 的删除策略不一致。请分析是否构成安全问题，设计方案并修复。" |
| "文件 HomeScreen.kt 第45行加一个 LazyColumn..." | "首页需要展示跑腿卡片列表。数据从 Supabase 获取。边界：不动 Navigation.kt。" |
| "在 UserMappers.kt 加一个 toDto() 方法，映射 id/name/email 三个字段..." | "ProfileEntity 缺少对应的 DTO 转换。请检查所有 Entity 的 Mapper 完整性并补全。" |
| "修改 AppDatabase.kt 第12行，version 从 1 改为 2，加 Migration(1,2)..." | "Room schema 需要升级。请分析需要新增哪些 Entity，设计 Migration 策略。" |

**核心原则：告诉 Agent "要解决什么问题"，不告诉它"怎么解决"。** Agent 必须自己读文件、自己分析、自己设计方案。如果 Agent 的方案有问题，打回让它重新分析——而不是经理直接告诉它正确答案。

### 7.3 审查与打回
- 审查结论只用：通过 / 打回 / 阻塞
- **所有问题一律打回执行Agent修改**，经理不直接修改代码。不论问题大小。
- 打回必须带修复清单与复审标准
- 打回后必须复审，通过前不得宣布完成
- 打回→修改→审查 循环不受硬性次数上限，但连续3次打回仍未通过时，经理必须介入分析根因（任务包定义不当/执行Agent模型能力不足/规则冲突），决定重写任务包或升级为用户决策
- **阻塞**：任务因外部依赖（规则不清、用户未确认、技术不可行）无法继续 → 记录到 [[campus_open_questions]] → 暂停本任务 → 进入下一个可执行任务或等待用户

### 7.4 禁止
- 禁止绕过审查直接宣布完成
- 禁止经理同时扮演审查角色（必须派生独立Agent审查）
- 禁止给审查Agent任何引导性提示（如"执行Agent已经修了XX"），审查Agent必须独立判断
- 禁止静默覆盖已有规则
- 禁止伪造"已同步"或谎报审查通过
- 禁止把待确认事项写成已确认
- **禁止派生任何 Agent 时不写 Skill 要求**（prompt 中必须包含 `Skill要求：xxx`，参照 §7.1.1）
- **禁止分析任务进入审查/打回循环**（分析任务到汇报即结束，不派生审查Agent，不自追加优化）
- 禁止任务包写具体代码方案
- 禁止跳过"先出思路确认方向"环节
- 禁止经理直接写代码

### 7.5 任务终点（何时停止）

| 任务类型 | 终点 | 禁止继续 |
|---------|------|---------|
| **分析任务** | 小问题：经理自己分析汇报。大分析：派生2-3个执行Agent并行 → 经理汇总结果 → 汇报给用户 | 不派生审查Agent、不修改文件、不自我追加 |
| **修改任务** | 审查Agent 审查通过 → 经理判定通过 → campus-memory-sync 收口 | 通过后不追加修改、不循环审查 |
| **规划任务** | 产出计划文档 → 用户确认 | 未经用户确认不进入执行 |

**核心原则：到达终点即停止。用户说"分析"就到汇报为止，用户说"改"就到审查通过为止。经理不自行为任务追加阶段。**

---

## 8. 安全规则

### 8.1 API Key 管理
- Supabase anon key：可存在于客户端代码中（公开）
- Supabase service_role key：仅限 Edge Function 使用，**禁止**出现在 Android 客户端代码、build.gradle、res 资源中
- Firebase server key（FCM）：仅限 Edge Function 使用
- 所有 key 通过 local.properties 注入构建，local.properties 已加入 .gitignore

### 8.2 数据安全
- 所有数据库表必须启用 RLS
- 审查Agent 每次审查必须检查 RLS 策略完整性
- 用户数据隔离以 school_id 为最小隔离单位
- 安全审查检查项已纳入审查Agent固定清单（见 §7.2），Phase 3 建表时首次执行，之后每 Phase 重复

---

## 9. 紧急与异常流程

### 9.1 Hotfix 快速通道

紧急 bug 修复使用快速通道，不跳过审查但压缩流程：

| 维度 | 常规修改 | Hotfix |
|------|---------|--------|
| 思路确认 | 先出思路→经理确认→动手 | 可跳过，执行Agent直接修 |
| 审查 | 必须派生审查Agent | 必须派生审查Agent |
| 打回上限 | 3次后经理介入分析根因 | 最多2轮，仍不过则经理直接决策（临时修复+正式修复队列） |
| 记忆同步 | 全流程同步 | 仅节点E同步，事后补详细记录 |

- Hotfix 完成后必须开一个普通修改任务做正式修复，消除临时方案。
- 由用户口头声明"这是 hotfix"即触发快速通道。

### 9.2 编译失败恢复

```
编译失败 → 执行Agent自检（verification-before-completion skill）
         → 不能自愈 → 经理判定根因三类：
            A. 执行Agent改动导致 → 打回修改
            B. 环境/配置问题 → 经理排查（.gradle / local.properties / JDK）
            C. 上游依赖问题 → 记录到 campus_open_questions，阻塞任务
```

- 编译失败不影响其他并行任务。并行Agent之间环境隔离。

### 9.3 SQL Migration 执行失败

1. **每个 migration 必须附带 revert 回滚脚本**，由执行Agent产出、审查Agent验证。
2. **执行权在用户**：Agent 只产出 migration + revert，用户在 Supabase Dashboard 手动执行。
3. **失败处理**：用户回滚到上一个正常状态 → Agent 分析根因 → 重新产出修正版。

### 9.4 分支冲突（Merge Conflict）

```
冲突发生 → 执行Agent停止当前工作 → 报告冲突文件清单给经理
         → 经理决策：
            A. 简单冲突（冲突行 < 20，无业务逻辑歧义）→ 经理手动解决
            B. 复杂冲突（冲突行 >= 20，或涉及业务规则）→ 派生独立执行Agent解决
            C. 同一文件被两个并行Agent同时修改 → 任务拆分不当，经理重排顺序
```

- 经理派发并行任务时，不得让两个Agent同时修改同一个文件。
