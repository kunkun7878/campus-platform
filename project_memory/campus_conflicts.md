# 校园聚合平台 - 规则冲突记录

<!-- last_sync: 2026-05-21T13:00 CST -->

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
