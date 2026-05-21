# 校园聚合平台 - 当前迭代

<!-- last_sync: 2026-05-21T19:30 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_decisions]] · [[campus_rules]] · [[campus_work_rules]]

## 迭代：Android MVP 正式开发

状态：**Phase 0 完成 ✅ → 准备进入 Phase 1**

### 本轮目标
从 HTML 原型阶段进入 Android 原生应用正式开发，按 8 Phase（Phase 0-7）推进至 MVP，34 screen + 17 张数据库表全部可用。

### 8 Phase 路线图（Phase 0-7）

| Phase | 内容 | 状态 |
|-------|------|:--:|
| 0 | 环境搭建（AS + SDK + 项目创建 + 依赖配置 + git init） | ✅ 完成 |
| 1 | 项目骨架 + 主题 + 导航（5 Tab + 34 route 占位） | 待开始 |
| 2 | 认证 + 选校（Supabase Auth + 3表 + 3 screen） | 待开始 |
| 3 | 数据层基座（17张表DDL + Room + Retrofit + Repository） | 待开始 |
| 4 | 跑腿全链路（8 screen） | 待开始 |
| 5 | 二手交易（4 screen） | 待开始 |
| 6 | 失物招领 + 社区（9 screen + 实时聊天） | 待开始 |
| 7 | 收口补齐（推送 + 三态 + 图片 + 全链路验证） | 待开始 |

### Phase 0 任务（当前）

- [x] 安装 Android Studio 2025.3.4
- [x] 安装 JDK 21（已预装）
- [x] 新建 CampusPlatform 项目
- [x] 配置 gradle/libs.versions.toml（28版本号 + 55依赖）
- [x] 配置 Compose + Hilt + Navigation + Retrofit + Room + Coil + DataStore
- [x] MainActivity 改为 Compose + Hilt
- [x] 创建 CampusApplication（Hilt入口）
- [x] Skills 扩充至 55 个（全部安装，具体激活状态见 campus_work_rules.md §7.1.1）
- [x] 规则文档审查 + 修复12项问题
- [x] 编译验证（./gradlew assembleDebug）✅
- [x] git init + .gitignore + 首次 commit → 已推送 GitHub ✅
