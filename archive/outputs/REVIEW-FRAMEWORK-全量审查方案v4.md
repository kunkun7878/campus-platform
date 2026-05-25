# 校园聚合平台 全量审查方案 v4

<!-- 制定日期: 2026-05-25 | v3→v4 改进: 修复14项P0问题 -->

> **这是校园聚合平台项目的标准审查流程。所有后续功能审查、问题排查、验收测试均须按此流程执行。**

---

## 审查准则（v4 新增）

**Agent审查时必须遵守以下判据：**

### 功能完整性判据
| 检查项 | 通过标准 | 不通过标准 |
|--------|---------|-----------|
| Screen存在 | Composable文件存在且被NavGraph注册 | 文件不存在或注册缺失 |
| ViewModel绑定 | `hiltViewModel()` 调用存在 | Screen未获取ViewModel，或只用空壳 |
| 数据加载 | `init{}`或`LaunchedEffect`触发加载 | load函数定义但从未被调用 |
| 导航闭环 | onClick→navigate→目标路由在NavGraph注册 | 导航到不存在的路由 |
| 三态处理 | Loading/Empty/Error UI均有对应组件 | 白屏、空列表无提示、异常无处理 |

### UI匹配度判据
| 检查项 | 通过标准 | 不通过标准 |
|--------|---------|-----------|
| 布局结构 | 元素层级/排列方向与原型一致 | 布局方式完全不同（如原型左侧Rail→Android顶部FilterChip） |
| 关键配色 | 渐变色/品牌色与design-tokens一致 | 非Theme色、没有渐变 |
| 组件样式 | 卡片/按钮/输入框样式与原型一致 | 使用Material3默认样式替代原型定制样式 |
| 文字大小 | 字号/粗细与原型typography一致 | 明显偏大或偏小 |

### 证据标准（v4 新增）
- 每个"通过"可附简要说明（1行代码引用）
- 每个"不通过"必须附：文件路径:行号 + 简短描述
- **不使用adb截图**（Agent环境无adb权限），改用代码级证据

---

## 流程总览

```
阶段1: 准备（90min）
  ├ 1.1 经理写HTML解析脚本（30min）
  ├ 1.2 执行脚本产出parsed-data.json（5min）
  ├ 1.3 全局Design Token提取（1 Agent）
  ├ 1.4 Screen规范（5 Agent并行）
  ├ 1.5 交叉校验（1 Agent）
  └ 1.6 生成Android对照清单（1 Agent）
      ↓
阶段1.5: 烟雾测试（30min）
  ├ 模拟器环境检查 → 跳过OTP → 核心页面不崩溃
  └ **失败则阻断后续阶段，回到代码修复**
      ↓
阶段2: 分层并行审查（3-5h）
  ├ Layer A: 共享层（1 Agent）—— 组件库/导航/DI/API/权限
  ├ Layer B: Screen均分（5-6 Agent）—— 每人6-8屏，逐项对照阶段1.6清单
  └ Layer C: 横向切面（1-2 Agent）—— 路由/安全/数据流
      ↓
阶段3: 汇总+去重+交叉验证（1h）
  ├ 3.1 去重（以"代码位置+问题类型"为联合主键）
  ├ 3.2 交叉抽样（10%随机复查）
  ├ 3.3 高风险Screen复查
  └ 3.4 人工复核检查点 ★
      ↓
阶段4: 修复+验证（分批）
  ├ 4.1 P0修复 → 4.2 重新烟雾测试 → 4.3 修复层定向复查
  ├ 4.4 P1修复 → 4.5 交叉验证
  └ 4.6 人工验收 ★
```

## 阶段1: 准备（90min）

### 1.1 经理写HTML解析脚本（30min）★v4 明确责任
- **执行人：经理（当前主会话）**
- 用 Node.js + jsdom 或 Python + BeautifulSoup 解析原型HTML
- 脚本功能：
  1. 提取 `:root` CSS变量 → `designTokens.explicit`
  2. 提取所有非变量硬编码颜色/渐变/尺寸 → `designTokens.implicit`
  3. 遍历34个Screen的DOM树 → `screens[].domTree`
  4. 提取每个元素的计算后CSS属性 → `screens[].computedStyles`
  5. 识别 data-* 交互属性 → `screens[].interactions`
  6. 提取JS中 screenConfigs → `screenConfigs`
- 产出：`parsed-data.json`
- 验收：执行 `node parse.js` 无error + JSON文件能解析

### 1.2 执行脚本（5min）
- 产出 `parsed-data.json`
- 验收：JSON包含34个screen节点、CSS变量数=`:root`定义数

### 1.3 全局Design Token提取（1 Agent）
- 输入：`parsed-data.json`
- 产出 `design-tokens.md`：6大类Token（Color/Typography/Spacing/Shape/Shadow/Gradient）
- **关键：隐含Token必须显式命名并给出M3 Compose映射**

### 1.4 Screen规范（5 Agent并行）★v4 明确依赖
- **前置条件：1.2和1.3必须完成**
- 5组分工（每组6-9屏）：
  - Agent-1: home + order-detail/order-list/after-sale-* + announcement（6屏）
  - Agent-2: goods-detail/lost-detail/lost-claim + market/lost/publish-hub（7屏）
  - Agent-3: community/post-detail/post-create + message/chat-detail/group-chat（6屏）
  - Agent-4: profile/wallet/my-*/coupons/invite/feedback/about（8屏）
  - Agent-5: school-select/runner-apply/address + agent-* + login/register（9屏）
- 每屏产出 `screen-{name}.md`：布局树 + 逐元素规范表 + 交互表 + 状态表

### 1.5 跨Screen交叉校验（1 Agent）
- 检查：Token一致性、组件一致性、交互完整性、M3映射

### 1.6 生成Android对照清单（1 Agent）
- 产出 `android-implementation-checklist.md`
- **输入来源v4澄清：来自阶段1.4的Screen规范 + 1.3的Token**

---

## 阶段1.5: 烟雾测试（30min）★v4 新增失败处理

**准入条件：编译 BUILD SUCCESSFUL + APK可安装**

| 步骤 | 内容 | 通过标准 | 失败处理 |
|------|------|---------|---------|
| 1.5.1 | `adb devices` + `adb install` + `curl supabase` | 模拟器连接、APK安装成功、Supabase可达 | 修复环境→重试，最多3次→人工介入 |
| 1.5.2 | 启动App + logcat检查 | 5秒内进程存在、无FATAL异常 | **阻断阶段2，回到代码修复** |
| 1.5.3 | 绕过OTP登录 | 进入主界面 | 如失败→不影响审查，阶段2的认证模块标记为"需手动测试" |
| 1.5.4 | adb点击5Tab + logcat | 每个Tab可导航、无崩溃 | 任一失败→**阻断阶段2** |

**若1.5失败：阻断阶段2，回到代码修复→重新编译→重新1.5，直到通过。**

---

## 阶段2: 分层并行审查（3-5h）★v4 明确分层

### Layer A: 共享层（1 Agent）★v4 明确范围定义

共享层范围（审查清单）：
- [ ] 所有共享Composable组件（RunnerTaskCard/MarketFeedCard/LostItemCard/PostCard/ChatBubble等）
- [ ] 所有共享ViewModel模式（UiState定义、错误处理）
- [ ] 导航架构（NavGraph结构、路由定义、ScreenConfig）
- [ ] DI绑定完整性（RepositoryModule/AuthModule/DatabaseModule）
- [ ] Repository层接口-实现一致性
- [ ] RLS策略覆盖率

### Layer B: Screen均分（5-6 Agent）★v4 解决分工失衡
- 按Screen数均分，每人6-8屏（不是按模块）
- SD Agent合并到手 的消息模块与相邻小模块合并
- **每屏审查维度**（4项）：
  1. **UI对照**：对照阶段1.6清单，逐元素比对
  2. **调用链**：ViewModel每个public方法是否被Screen调用？init{}是否触发？
  3. **四态**：Loading/Empty/Error/Success是否都有处理？
  4. **导航**：onClick→navigate→目标注册→返回键

- **证据要求v4修订**：不用adb截图，用代码级证据（文件:行号）

### Layer C: 横向切面（1-2 Agent）
- C1: 路由/导航/数据流一致性
- C2: 权限/RLS/安全

### 共享机制v4替换 ★

**原方案"三级共享板"在Agent无法通信环境下不可行。v4替换为：**

```
L1: 阶段内无共享（Agent独立工作，允许重复发现）
L2: 经理在阶段2结束后汇总所有Agent报告 → 去重 → 标记系统性问题
L3: 去重后的统一问题清单发给所有Agent用于阶段3复查
```

---

## 阶段3: 汇总+去重+交叉验证（1h）★v4 补充

### 3.1 去重（经理执行）★v4 明确定义
- 去重主键：`代码位置（文件:行号）+ 问题类型（UI/逻辑/导航/数据流）`
- 同一问题被多个Agent发现→保留描述最详细的报告
- 标记"系统性问题"：≥3个Screen共有的问题

### 3.2 交叉抽样（复查Agent）★v4 解决上下文传递
- 随机抽取10%审查结果
- **打包原审查的完整上下文**：原Agent报告 + 相关代码段 + 审查清单
- 由另一Agent独立重新审查
- 一致→有效，不一致→标记为"需人工判定"

### 3.3 高风险Screen复查
- 选取标准：发现数>15 / 核心流程Screen
- Top 6-8个Screen

### 3.4 人工复核 ★v4 新增
- **经理审核汇总报告，逐条确认P0/P1项**
- 确认修复优先级排序合理

---

## 阶段4: 修复+验证（分批）★v4 新增验证闭环

### 优先级定义 ★v4 明确
| 级别 | 定义 | 触发条件 |
|:--:|------|------|
| P0 | 阻断：无法使用 | 编译失败、启动崩溃、核心链路断裂 |
| P1 | 严重：功能缺失 | 页面无数据、UI与原型严重不符、数据不一致 |
| P2 | 建议：体验问题 | 样式偏差、缺少空态引导 |
| P3 | 锦上添花 | 性能优化、代码整洁 |

### 修复流程 ★v4 新增验证步骤
```
修复 → 编译验证 → 验证闭环 ↓

P0: 修完 → 重新1.5烟雾测试 → 重新Layer B该Screen审查 → 通过才算修复完成
P1: 修完 → 编译 → 定向复查该Screen → 抽查通过
P2: 统一修 → 修完抽查10%
P3: 排期修，不阻塞
```

### 4.6 人工验收 ★v4 新增
- **经理确认P0/P1全部闭环**
- **用户可选确认（让用户在模拟器上跑一遍核心链路）**

---

## 异常处理（v4 新增）

| 阶段 | 异常场景 | 处理 |
|------|---------|------|
| 1.1 | 脚本执行失败 | 经理debug脚本，最多3次→降级为Agent手动解析 |
| 1.2-1.6 | 任一Agent产出不合格 | 打回给该Agent修正，最多2次→经理介入 |
| 1.5 | 烟雾测试失败 | 阻断阶段2，回到代码修复 |
| 2 | 任一Agent超时或产出不全 | 该Agent负责的Screen标记为"待复查"，不阻断其他Agent |
| 3.2 | 交叉抽样发现一致性问题 | 扩大抽样到20%，仍不一致→人工全量复查 |
| 4 | 修复引入新问题 | 回退修复→重新分析→重新修复 |

---

## Agent能力约束（v4 新增）

| 约束 | 影响 | 应对 |
|------|------|------|
| Agent无Bash权限 | 无法执行adb/编译 | 编译由经理在阶段切换时执行 |
| Agent不能互相通信 | 无实时共享板 | 改为阶段结束后经理汇总分发 |
| SendMessage不可用 | 无法持续Agent会话 | 所有Agent任务一次性自包含，不问问题 |
| 单Agent上下文~2000行 | 超大Screen可能截断 | 每个Screen独立，不跨Screen组合 |

---

## v3→v4 改进清单

| v3问题 | v4修复 |
|--------|--------|
| 审查准则缺失（P0#3） | 新增"审查准则"章节，定义通过/不通过标准 |
| 共享板不可行（P0#16-17） | 替换为经理汇总模式 |
| adb截图不可行（P0#15） | 改为代码级证据 |
| 数据流矛盾（P0#2,6,9） | 明确每阶段输入/输出/前置条件 |
| Agent分工不明（P0#13-14） | 明确Layer A/B/C定义和每层Agent数 |
| 脚本责任不明（P0#5） | 明确经理负责 |
| 修复无验证（P0#20） | 新增阶段4修复验证闭环 |
| 烟雾测试失败无处理（P0#11-12） | 新增失败处理+阻断规则 |
| 缺少人工检查点（P1#23） | 新增3.4和4.6人工复核 |
| 去重规则缺失（P1#19） | 定义去重主键 |
| 异常无兜底（P2#25） | 新增异常处理章节 |
| Agent能力约束未知（P1#24） | 新增Agent能力约束章节 |
| 编号混乱（P1#1） | 统一为1→1.5→2→3→4 |

---

## 核心原则

1. **审查前先有规范**：对照原型逐元素清单，不是"感觉像不像"
2. **分工均衡**：按Screen数均分，不是按模块名
3. **证据驱动**：每个"不通过"必须有文件:行号
4. **先跑通再深入**：烟雾测试不通过，不进入阶段2
5. **共享层先审**：公共组件/路由/状态单独审查
6. **修复必验证**：修完必须编译+复查，P0/P1必须重新验证 ★v4新增
7. **异常有兜底**：每个阶段有失败处理+最大重试+人工介入路径 ★v4新增
