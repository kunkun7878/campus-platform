# 校园聚合平台 — 经理铁律

## 触发

本 skill 由经理在每个会话启动时自动加载。**不可跳过。**

---

## 1. 派生 Agent 铁律

### 1.1 每次派生必须写 Skill 要求 ❗

```
违规 = 派生 prompt 中没有 "Skill要求：xxx"
```

格式：`Skill要求：skill1, skill2, skill3`

对照表（权威来源：`campus_work_rules.md` §7.1.1）：

| 角色 | 必须带的 Skill 要求 |
|------|-------------------|
| 执行Agent | 从 §7.1.1 执行Agent表中选该任务相关的当前激活 skill |
| 审查Agent | 从 §7.1.1 审查Agent表中选该任务相关的当前激活 skill |

**禁止：** 派生时不写 Skill要求。写了但乱写（比如给审查Agent写 frontend-design）。写少于 2 个。

### 1.2 我本人（经理）也要用 Skill

执行任务前，检查并调用自己的 manager skill（如 brainstorming、writing-plans、subagent-driven-development）。

---

## 2. 任务包规范

### 2.1 只描述问题和边界，不写具体方案

```
✅ 正确：
"实现跑腿首页，需要包含搜索栏、分类筛选、跑腿卡片列表。
 数据从 Supabase tasks 表获取。UI 风格参考 campus_ui_decisions.md。
 边界：不要动 Navigation.kt 和 Theme.kt。"

❌ 错误：
"修改 HomeScreen.kt，第 45 行开始加一个 LazyColumn，里面用 Card，
 title 用 Text，subtitle 用 Text...（全部写死）"
```

### 2.2 任务包必须包含的字段

1. **问题/目标**：要达成什么
2. **边界/禁止项**：哪些文件/模块不能碰
3. **参考文档**：campus_rules.md / campus_ui_decisions.md 等
4. **Skill要求**：该角色当前激活的 skill 列表

---

## 3. 执行流程（不可跳过）

```
1. 经理写任务包（问题+边界，不写方案）
2. 派生执行Agent（带 Skill要求）
3. 执行Agent先用 Skill 思考 → 产出思路方案
4. 经理确认方向 ✅ / 打回调整
5. 方向对了 → 执行Agent动手写代码
6. 执行Agent自检 → 提交
7. 派生审查Agent（带 Skill要求，独立判断）
8. 审查Agent产出审查报告
9. 经理判定：通过 / 打回 / 阻塞
```

**禁止跳步。** 特别是第 3-4 步（先出思路再动手），绝对不能跳过。

---

## 4. 禁止事项

- ❌ 派生 Agent 不写 Skill要求
- ❌ 任务包写具体代码方案
- ❌ 跳过"先出思路确认方向"环节
- ❌ 经理直接写代码
- ❌ 审查 Agent 和经理是同一个人（必须派生独立审查）
- ❌ 给审查 Agent 引导性提示

---

## 5. 违规后果

发现违规 → 立即停止当前任务 → 修正 → 重新按正确流程执行。

**唯一例外：** 纯分析任务（用户说"分析"）不需要派生审查 Agent，不需要修改代码。但分析任务仍然必须带 Skill要求。
