package com.campus.platform.ui.screen.home

/**
 * 首页子视图枚举 — 用于 HomeScreen 内部 Tab 切换（不涉及路由）。
 *
 * 对应 HTML 原型 viewConfigs（第 4319-4322 行）定义的三个子视图：
 * - Runner  跑腿（快递、帮买、公告）
 * - Market  二手物品（同校面交）
 * - Lost    失物招领（寻物/招领）
 *
 * 每个子视图携带与原型一致的 search 提示文字，
 * 后续 Phase 可用于 HomeScreen 搜索框 placeholder。
 */
enum class HomeSubView(val label: String, val searchHint: String) {
    Runner("跑腿", "搜索跑腿需求、快递、帮买、公告"),
    Market("二手物品", "搜索二手物品、卖家、同校面交信息"),
    Lost("失物招领", "搜索失物、招领、地点、时间、公告"),
}
