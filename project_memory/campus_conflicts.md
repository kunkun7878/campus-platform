# 校园聚合平台 - 规则冲突记录

<!-- last_sync: 2026-05-23T23:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_rules]] · [[campus_work_rules]] · [[campus_status]]

## CF-003：skill 路径与实际 master 不一致

### 时间
2026-05-07 22:39 发现，22:50 修复

### 原规则
- `campus-project-guard` 声明持久化目录为 `.trae/project_memory/`

### 新规则 / 实际情况
- 实际 master 目录为 `project_memory/`（桌面项目记忆 README 明确标注"唯一 master 副本"）

### 冲突影响
- 后续 session 加载 `campus-project-guard` 后可能读错目录
- 与 `campus-memory-sync` 的路径逻辑不一致

### 用户最终选择
- 修正 `campus-project-guard` 全部路径引用，对齐 `project_memory/` 实际目录

### 历史结论
- 已闭环

---

## CF-004：决策 #33 ON DELETE CASCADE vs RESTRICT 系统性冲突

### 时间
2026-05-23 22:00 发现，用户已确认修复

### 原规则
- 决策 #33：所有内容表 ON DELETE RESTRICT（防误删级联），user_addresses 除外（SET NULL）

### 新规则 / 实际情况
- 全部 15 个 migration 中，所有内容表间外键均使用 ON DELETE CASCADE，无一遵守 #33 的 RESTRICT 要求
- 涉及：auth.users 引用的所有表（profiles/runner_tasks/orders/market_listings/lost_found_items/community_posts/comments/post_likes/conversations/messages/group_messages/group_members/wallets/wallet_transactions）
- 涉及：内容表间引用（删 post → 级联删除 comments/likes；删 conversation → 级联删除 messages；删 official_group → 级联删除 group_messages/members）

### 冲突影响
- 决策文件与实际执行之间存在根本性断裂
- 后续 Agent 如果按 #33 假设 RESTRICT 行为来设计代码，会产生错误
- Phase 1-5 的所有执行 Agent 已经在 CASCADE 语义下完成了全部逻辑

### 用户最终选择
- 修正决策 #33：school_id 外键 ON DELETE RESTRICT，auth.users 和内容表间外键 ON DELETE CASCADE（归档为决策 #66）

### 历史结论
- 已闭环
