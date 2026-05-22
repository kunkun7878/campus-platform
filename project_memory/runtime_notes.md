# 校园聚合平台 - 运行环境与预览说明

<!-- last_sync: 2026-05-22T20:00 CST -->

> 关联：[[PROJECT_HOME]] · [[codebase_map]]

## 当前真实项目文件
- HTML 原型：`C:\Users\admin\Desktop\校园聚合平台\prototype\campus-miniapp-prototype.html`（34 screen）
- Android 项目：`C:\Users\admin\Desktop\校园聚合平台\android\`
- 项目记忆根目录：`C:\Users\admin\Desktop\校园聚合平台\project_memory\`
- Supabase Migrations：`C:\Users\admin\Desktop\校园聚合平台\supabase\migrations\`（15 个 + 15 Revert）

## Supabase Migration 执行

### 执行方式
在 Supabase Dashboard → SQL Editor 中，按编号顺序逐个打开 migration 文件，选中全部内容后点击 Run。

### 执行顺序
```
00_create_profiles → 01_create_schools_campuses → 02_seed_data →
03_rls_policies → 04_wechat_identities → 05_add_indexes →
06_runner_module → 07_market_module → 08_lost_found_module →
09_community_module → 10_notifications →
11_runner_after_sale_supplement → 12_messaging_social →
13_wallet_system → 14_misc_alter_profiles
```

### 回滚
如执行失败，在 `revert/` 子目录下找到对应编号的 revert 脚本，逆序执行。

### Supabase 项目
- 项目名：campus-platform
- 区域：ap-southeast-1（新加坡）
- URL：https://fzmdhllxzyyzfpxkqpdy.supabase.co
- anon key：已配置在 `android/local.properties`

## HTML 原型运行
### 方式一：直接双击打开
浏览器打开 `prototype/campus-miniapp-prototype.html`

### 方式二：本地静态服务
```bash
cd C:\Users\admin\Desktop\校园聚合平台
python -m http.server 8124
```
访问 `http://127.0.0.1:8124/prototype/campus-miniapp-prototype.html`

## Android 项目运行
### 编译
```bash
cd C:\Users\admin\Desktop\校园聚合平台\android
./gradlew assembleDebug
```
产物：`app/build/outputs/apk/debug/app-debug.apk`

### 安装到模拟器
```bash
./gradlew installDebug
```
或在 Android Studio 中直接 Run

### 环境要求
- JDK 21（已安装）
- Android SDK 35（通过 Android Studio SDK Manager 安装）
- Gradle 8.11.1（项目自带 wrapper，无需额外安装）

## 当前已知限制
- HTML 原型为静态演示，无真实后端
- Android 项目首次编译已验证通过（./gradlew assembleDebug，6秒）✅
- Android Studio 中文插件暂无适配（平台253）
- Supabase 项目已创建（campus-platform, ap-southeast-1）；Firebase 项目尚未创建（Phase 7 前创建）
