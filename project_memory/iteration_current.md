# 校园聚合平台 - 当前迭代

<!-- last_sync: 2026-05-24T16:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_decisions]] · [[campus_rules]] · [[campus_work_rules]]

## 迭代：Android MVP 正式开发

状态：**Phase 5 完成 ✅ → Phase 6 完成 ✅ → Phase 7 待开始**

### 本轮目标
从 HTML 原型阶段进入 Android 原生应用正式开发，按 8 Phase（Phase 0-7）推进至 MVP，34 screen + 38 张数据库表全部可用。

### 8 Phase 路线图（Phase 0-7）

| Phase | 内容                                                    |  状态  |
| ----- | ----------------------------------------------------- | :--: |
| 0     | 环境搭建（AS + SDK + 项目创建 + 依赖配置 + git init）               | ✅ 完成 |
| 1     | 项目骨架 + 主题 + 导航（5 Tab + 34 route 占位）                   | ✅ 完成 |
| 2     | 认证 + 选校（Supabase Auth + 4表 + 6 screen）                | ✅ 完成 |
| 3     | 数据层基座（38表DDL + Room + Repository + ViewModel）         | ✅ 完成 |
| 4     | 跑腿全链路（8 screen + 2 Edge Function）                     | ✅ 完成 |
| 5     | 二手交易（7 screen + 1 Edge Function + 6 组件 + UiState 改造）  | ✅ 完成 |
| 6     | 失物招领 + 社区（10 screen + Realtime + 图片 + FCM + 3 EdgeFn） | ✅ 完成 |
| 7     | 收口补齐（云AI审核 + Agent后台 + 全链路验证）                         | 待开始  |
|       |                                                       |      |

### Phase 4 任务（已完成 ✅）

- [x] RLS 漏洞修复（migration 15 — 4 处策略修复 + revert）
- [x] Supabase Edge Function（runner-order-lifecycle + runner-after-sale，共 946 行 TS）
- [x] Timeline 数据层补全（2 Entity + 2 DTO + 8 DAO 查询 + 2 Repository 接口 + 2 实现 + DI）
- [x] Screen 目录重组（4 Screen + 4 ViewModel 从 market/ 迁到 runner/）
- [x] 6 个通用 UI 组件（RunnerTaskCard / OrderStatusTimeline / RunnerTypeFilter / OrderCard / RunnerEmptyState / RunnerPriceTag）
- [x] 8 个 Screen + 8 个 ViewModel 从占位→完整业务实现
- [x] OrderDetailScreen 含评价嵌入（星级评分 + 文字评价，表单→只读展示转换）
- [x] OrderListScreen 含 Tab 合并（我发布的/我接的单 + 状态子筛选）
- [x] Supabase Functions SDK 集成（functions-kt 依赖 + AuthModule install + build.gradle.kts）
- [x] 编译验证：BUILD SUCCESSFUL

### Phase 3 任务（已完成 ✅）

- [x] SQL Migration 层（38张表 + 18 Migration + 17 Revert）
- [x] Android Room 层（25 Entity + 7 DAO + 7 Mapper + AppDatabase + DataStore）
- [x] Android Repository 层（16 接口 + 16 实现 + NetworkModule + RepositoryModule）
- [x] Android ViewModel 层（35 ViewModel + 34 Screen 改造 + MainActivity/NavGraph 重构）
- [x] 编译验证：BUILD SUCCESSFUL
- [x] 8 轮审查全部通过
- [ ] SQL 待用户在 Supabase Dashboard 执行

### Phase 2 任务（已完成 ✅）

- [x] Supabase 项目创建：campus-platform (ap-southeast-1)
- [x] 6 个 SQL Migration（profiles/schools/campuses/wechat_identities/RLS + auth_triggers）
- [x] 种子数据：四川师范大学（3校区）+ 四川邮电职业技术学院（1校区）
- [x] AuthRepository + AuthValidator + SchoolRepository + AuthModule (Hilt DI)
- [x] AuthGuard 登录守卫（Login/SchoolSelect/Home 三态判定）
- [x] LoginScreen 重写（手机号+密码/OTP 双模式 + CAPTCHA）
- [x] RegisterScreen 重写（3步分步表单 + 密码强度条）
- [x] PasswordResetScreen / AccountDeleteScreen 新建
- [x] SchoolSelectScreen 重写（学校-校区两级选择器）
- [x] PasswordStrengthBar / CaptchaDialog 组件
- [x] Supabase Kotlin SDK 3.1.2 集成 + Gradle 配置
- [x] 编译验证（./gradlew assembleDebug BUILD SUCCESSFUL）
- [x] 审查通过（2项注意事项：OTP SDK兼容性 + 协议复选框）
- [x] 联调测试通过（curl：登录→profiles→schools→campuses）
- [x] SQL 待用户手动执行（Supabase Dashboard SQL Editor）

### Phase 1 任务（已完成 ✅）

- [x] 创建 42 个新 Kotlin 文件（navigation + component + screen）
- [x] 35 条类型安全路由（34 screen + 5 嵌套图标记 + 1 global）
- [x] 5 嵌套 NavGraph（HomeTab/PublishTab/CommunityTab/MessageTab/ProfileTab）
- [x] CampusHeroBar（brand + meta + search 三层，按路由配置显隐）
- [x] CampusBottomNav（5 Tab + 点击已选中弹回根）
- [x] ScreenPlaceholder 占位模板（33 screen 统一使用）
- [x] HomeScreen HorizontalPager + FilterChip（runner/market/lost 子视图切换）
- [x] 底部导航显隐逻辑（SchoolSelect/Login/Register 隐藏）
- [x] 编译验证（./gradlew assembleDebug BUILD SUCCESSFUL）
- [x] 审查打回修复循环（4项问题 → 复审通过）

### Phase 0 任务（已完成 ✅）

- [x] 安装 Android Studio 2025.3.4
- [x] 安装 JDK 21（已预装）
- [x] 新建 CampusPlatform 项目
- [x] 配置 gradle/libs.versions.toml（28版本号 + 55依赖）
- [x] 配置 Compose + Hilt + Navigation + Retrofit + Room + Coil + DataStore
- [x] MainActivity 改为 Compose + Hilt
- [x] 创建 CampusApplication（Hilt入口）
- [x] Skills 扩充至 54 个（全部安装，具体激活状态见 campus_work_rules.md §7.1.1）
- [x] 规则文档审查 + 修复12项问题
- [x] 编译验证（./gradlew assembleDebug）✅
- [x] git init + .gitignore + 首次 commit → 已推送 GitHub ✅
