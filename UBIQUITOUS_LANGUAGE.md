# Ubiquitous Language — 校园聚合平台 Phase 2 认证与选校

## 身份与认证

| Term | Definition | Aliases to avoid |
|------|-----------|-----------------|
| **用户 (User)** | 系统中通过认证的账号主体，由 `auth.users` 标识 | 账号、登录人 |
| **档案 (Profile)** | 用户在平台内的公开身份信息（昵称、头像、学校归属） | 用户资料、个人信息 |
| **手机号验证码登录 (Phone OTP Login)** | 通过手机号接收 SMS 验证码完成登录，为主要认证方式 | 短信登录 |
| **微信一键登录 (WeChat OAuth Login)** | 通过微信 OAuth 授权获取 union_id，绑定已有账号或自动创建 | 微信授权登录、微信登录 |
| **邮箱登录 (Email Login)** | 通过邮箱+密码登录，为备用认证方式 | 邮箱密码登录 |
| **绑定 (Bind)** | 将微信 union_id 与现有用户档案关联的操作 | 关联、连接 |

## 学校与校区

| Term | Definition | Aliases to avoid |
|------|-----------|-----------------|
| **学校 (School)** | 一所独立的大学/学院，是内容隔离的顶级边界 | 院校、大学 |
| **校区 (Campus)** | 某学校下的一个物理园区。同一学校可有多个校区 | 分校、院区 |
| **选校 (School Selection)** | 用户首次选择归属学校。选择后不可随意切换 | 学校绑定 |
| **校区切换 (Campus Switch)** | 用户在同一学校内切换当前校区。不改变 school_id | 切换校园 |
| **学校隔离 (School Isolation)** | 不同学校之间数据互不可见的硬规则，通过 RLS 强制执行 | 数据隔离、按校隔离 |
| **客服申诉 (CS Appeal)** | 用户更换已绑定学校的唯一途径：联系客服人工处理 | 学校申诉、人工换校 |

## 权限与角色

| Term | Definition | Aliases to avoid |
|------|-----------|-----------------|
| **普通用户 (User)** | 默认角色，只能查看/修改自己的档案，受 RLS 限制 | 学生 |
| **管理员 (Admin)** | 平台全局管理员，可绕过 RLS 处理申诉、管理数据 | 超级管理员、客服管理员 |
| **代理 (Agent)** | 学校层级的授权管理者（Phase 3+），限于特定学校 | 学校代理 |

## Relationships

- 一个 **学校 (School)** 拥有 1+ 个 **校区 (Campus)**
- 一个 **用户 (User)** 拥有恰好 1 个 **档案 (Profile)**（通过 `auth.users.id` 一对一关联）
- 一个 **档案 (Profile)** 归属恰好 1 个 **学校 (School)**（选校后设置，不可随意更改）
- 一个 **档案 (Profile)** 当前关联 0-1 个 **校区 (Campus)**（可在同校内切换）
- **档案 (Profile)** 可关联 0-1 个微信账号（wechat_union_id 唯一约束）
- **管理员 (Admin)** 可变更任意 **档案 (Profile)** 的 school_id（通过 Service Role API）

## Flagged ambiguities

### A1：原型中"校区"被当作"学校"选择项

- **问题**：HTML 原型 `school-select` screen 将 "主校区/东校区/西校区/北校区" 作为选校卡片，变量名为 `selectedSchool`，JS 中 `data-school-id` 取值为 `main/east/west/north`。这与需求模型中 School 和 Campus 的分层不一致。
- **影响**：若照搬原型字段语义到数据库，会导致 `schools` 表中混入校区实体，学校隔离粒度错误。
- **建议**：数据模型中 School 是独立表（一所大学，如"XX大学"），Campus 是其子表（主校区/东校区等）。原型中的 `school-select` screen 实际展示的是 **Campus 卡片**——用户点击 Campus 后，系统通过 `campus.school_id` 自动完成 School 绑定。Android 实现时 screen 名称保留 `school-select`，但数据绑定逻辑修正为 Campus → School 推导。

### A2：原型中注册表单的"选择学校"下拉实际是校区

- **问题**：`register` screen 中 `<select id="registerSchool">` 的选项是 `main/east/west/north`（即校区），但字段标记为"学校"。
- **建议**：Android 实现时保持 UI 文案简洁（"选择学校"），但后端数据绑定到 Campus → 推导 School。

## Example dialogue

> **Dev:** "用户注册时在 register screen 选择了'主校区'，数据库里写的是什么？"
> **Domain expert:** "register screen 展示的是 **Campus** 选项。用户选'主校区'后，系统查找该 Campus 所属的 School（XX大学），在 Profile 里同时写入 `school_id` 和 `current_campus_id`。"
> **Dev:** "那之后用户想切换到东校区，是通过什么操作？"
> **Domain expert:** "校区切换是修改 Profile 的 `current_campus_id`，不碰 `school_id`。但前提是东校区和主校区属于同一所 **School**。如果用户想换到另一所大学的校区，那属于 **学校申诉**，必须经过客服人工处理。"
> **Dev:** "RLS 隔离是按 school_id 还是 campus_id？"
> **Domain expert:** "按 **school_id** 隔离。同一学校的所有校区之间内容互通。这样北校区的人能看到主校区发布的跑腿需求。"
> **Dev:** "用户能用微信一键登录，这在数据层意味着什么？"
> **Domain expert:** "微信 OAuth 授权后获取 `union_id`。系统先查 Profile 表 `wechat_union_id` 是否已存在：已存在则直接登录（跳过 phone 验证），不存在则走正常注册流程并**绑定** union_id。一个 Profile 最多绑定一个微信，一个微信也只能绑定一个 Profile。"
