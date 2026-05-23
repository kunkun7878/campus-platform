# 校园聚合平台 - 已确认决策

<!-- last_sync: 2026-05-23T10:00 CST -->

> 关联：[[PROJECT_HOME]] · [[campus_status]] · [[campus_rules]] · [[campus_work_rules]] · [[campus_open_questions]] · [[iteration_current]] · [[codebase_map]] · [[campus_ui_decisions]]

## 产品路线决策
1. 当前阶段：Android 原生应用正式开发（Kotlin + Jetpack Compose + Supabase），基于 HTML 原型作为 UI 参考，分 8 Phase（Phase 0-7）推进至 MVP。原型已进入收尾阶段，不再作为主要开发目标。
2. 后期考虑微信小程序，当前暂不投入。

## 工程与工具决策
3. DeepSeek 当前应视为文本模型，不承担包含图片历史的会话任务。
4. Skills 已安装到 `.claude/skills/`（campus-memory-sync + campus-project-guard）。
5. 使用 Obsidian 作为长期知识库查看/浏览外壳，与 Claude Code 的 Edit 工具协作读写同一套 Markdown 文件。

## 项目过程管理决策
6. ~~OpenClaw 原生记忆保留使用~~ → 已废弃。项目已从 OpenClaw 迁移到 Claude Code。
7. 项目专属长期记忆层（`project_memory/`）已建立并持续维护。
8. 多 Agent 协作已落地：经理+执行Agent+审查Agent，详见 `campus_work_rules.md` §7。
9. 所有校园聚合平台相关文件集中放在 `C:\Users\admin\Desktop\校园聚合平台\`。
10. Claude Code 的 CLAUDE.md 作为会话入口，自动引导读取项目记忆。
11. 54 个 skill（52 Agent + 2 项目管理），按 §7.1.1 分配。

## 已确认的后续决策
- ✅ 记忆体系主编辑环境：Claude Code 的 Edit 工具 + Obsidian 查看
- ✅ 多 Agent 角色：不需要固定常驻，按需派生执行Agent和审查Agent
- ✅ Obsidian 目录布局：整个 `校园聚合平台/` 作为 vault 根目录
- ✅ 不使用额外的 memory 插件/wiki 层，project_memory/ Markdown 文件足够
- ✅ 54 个 skill（52 Agent + 2 项目管理），按 §7.1.1 分配

## Phase 2 认证+选校决策（2026-05-22）

### 认证方式
1. 手机号+SMS验证码（主力），阿里云短信服务
2. 手机号+密码登录（辅助），密码规则：最小8位 + 至少字母+数字 + 实时强度指示器
3. 微信一键登录（搁置，需微信开放平台企业认证）
4. 邮箱验证登录（备用），邮箱选填，填了必须验证后才能用于登录
5. CAPTCHA 图形验证码（发送SMS前必须通过），防机器人和脚本
6. 密码错误锁定：6次→60秒 / 累计9次→5分钟 / 引导走SMS登录

### 选校机制
7. 原型语义修正：school-select screen 实际选的是 Campus，系统自动推导 School
8. 选校后不可自由切换学校（硬规则），客服申诉为唯一换校途径
9. 同校多校区可自由切换 campus_id（如川师大狮子山↔成龙↔遂宁）

### 数据模型
10. 4 张自定义表：profiles / schools / campuses / wechat_identities
11. Supabase 项目：campus-platform（ap-southeast-1 新加坡）
12. 种子数据：四川师范大学（狮子山/成龙/遂宁3校区）+ 四川邮电职业技术学院（锦江1校区）
13. RLS：所有表启用，以 school_id 为最小隔离单位，同校可互读基本资料

### 工程决策
14. Admin 管理后台：Phase 2-5 不做，用 SQL migration + Supabase Dashboard；Phase 7+ 再做
15. 本地开发：Supabase CLI + Docker Desktop（待配置）
16. 设备风险检测：暂不做

### 功能决策
17. 游客模式：不需要，所有功能需登录
18. 账号注销：Phase 2 支持（软删除 status=2 + deleted_at）
19. Session：自动登录（SDK 默认），退出登录清除 session，切换设备重登
20. 用户协议：Phase 2 做占位，后期可替换

## Phase 3 数据层设计决策（2026-05-22）

### 数据库范围
21. 36 张表全覆盖（4已有 + 32新增），一次性补齐，不按 Phase 分批
22. 每张内容表必须有 school_id + RLS 策略 + revert 脚本

### Room 策略
23. Supabase 为权威数据源（source of truth），Room 为本地缓存
24. SQLCipher 全库加密，Phase 3 启用
25. Entity 与 DTO 分离，通过 Mapper 扩展函数转换
26. 新 Repository 引入接口（domain/repository/），旧 Repository 保持不动

### ViewModel
27. Phase 3 全面引入 ViewModel，新旧 Screen 全部迁移

### RLS 设计
28. login_codes：service_role only，客户端无 policy（参照 wechat_identities）
29. profiles 敏感字段（runner_status/invite_code）：trigger 保护，用户不可自改；balance 权威来源已移至 wallets.balance
30. announcements.school_id：NULL = 全平台公告，有值 = 仅该校可见
31. wallet_transactions：append-only，无 updated_at
32. group_messages/members：通过 JOIN official_groups.school_id 实现学校隔离
33. 所有内容表 ON DELETE RESTRICT（防误删级联），user_addresses 除外（SET NULL）

### Migration 管理
34. 每个 migration 必须有配套 revert，使用 IF NOT EXISTS/DROP IF EXISTS 确保幂等
35. RLS 策略集中在各模块 migration 中，不单独分离
36. service_role 路径必须在所有 trigger 中显式豁免
