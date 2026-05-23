package com.campus.platform.navigation

/**
 * 每个 Screen 在 Scaffold 中的脚手架配置。
 *
 * - [showBottomNav]  当前页是否展示底部导航栏。
 * - [showHeroBar]    当前页是否展示顶部 HeroBar（带学校、天气等）。
 * - [label]          可选的页面标题（未设置时 Scaffold 不显示文字）。
 * - [brand]          HeroBar 标题文字，对应 HTML 原型 screenConfigs.brand。
 * - [meta]           HeroBar 副标题行（位置+天气），对应 HTML 原型 screenConfigs.meta。
 * - [search]         搜索框 placeholder，对应 HTML 原型 screenConfigs.search。
 *                    值为 null 时表示不展示搜索框。
 */
data class CampusScreenConfig(
    val showBottomNav: Boolean,
    val showHeroBar: Boolean,
    val label: String? = null,
    val brand: String? = null,
    val meta: String? = null,
    val search: String? = null,
)

/**
 * 根据当前 route 查找对应 Screen 的脚手架配置。
 *
 * 规则：
 * - 五个主 Tab + 各自直属子屏默认展示 BottomNav。
 * - Profile 子屏（wallet 等 12 条）不展示 BottomNav，以保留更多纵向空间。
 * - Auth / Global 不展示 BottomNav。
 * - brand / meta / search 值与 HTML 原型 screenConfigs 保持一致。
 */
fun campusScreenConfigFor(currentRoute: String?): CampusScreenConfig {
    if (currentRoute == null) return CampusScreenConfig(showBottomNav = false, showHeroBar = false)

    return when {
        // ── Bottom Nav 五个根 Tab ──
        currentRoute == CampusRoutes.Home.route -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = true,
                brand = "校园跑腿", meta = "主校区 · 晴 26°C · 默认进入跑腿平台",
                search = "搜索跑腿需求、快递、帮买、公告",
            )
        }

        currentRoute == CampusRoutes.PublishHub.route -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = true,
                brand = "发布", meta = "主校区 · 晴 26°C · 今天想发布什么？",
                search = null,
            )
        }

        currentRoute == CampusRoutes.Community.route -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = true,
                brand = "校园社区", meta = "主校区 · 发现精彩",
                search = "搜索帖子、频道、社区内容",
            )
        }

        currentRoute == CampusRoutes.Message.route -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = true,
                brand = "消息中心", meta = "主校区 · 晴 26°C · 订单、售后、失物消息汇总",
                search = "搜索聊天记录、系统通知",
            )
        }

        currentRoute == CampusRoutes.Profile.route -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = true,
                brand = "个人中心", meta = "主校区 · 晴 26°C · 订单、发布、售后统一管理",
                search = "搜索我的订单、发布、社群",
            )
        }

        // ── 发布子屏 ──
        currentRoute == "publish" -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "发布需求", search = "搜索发布模板、常用地址、跑腿规则",
            )
        }

        // ── 市集子屏（保留 BottomNav，方便返回） ──
        currentRoute.contains("GoodsDetail") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "校园市集", search = "搜索商品、卖家、分类",
            )
        }

        currentRoute.contains("LostDetail") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "失物招领", search = "搜索失物、地点、时间",
            )
        }

        currentRoute.contains("LostClaim") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "认领申请", search = "搜索认领规则、问题提示",
            )
        }

        currentRoute.startsWith("market-publish") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "发布二手", search = "搜索二手发布模板、分类、规则",
            )
        }

        currentRoute.startsWith("lost-publish") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "发布失物", search = "搜索失物发布规则、寻物模板",
            )
        }

        currentRoute.startsWith("order-detail") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "跑腿订单", search = "搜索订单号、状态、跑腿员消息",
            )
        }

        currentRoute.startsWith("order-list") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "我的订单", search = "搜索订单、筛选状态",
            )
        }

        currentRoute.startsWith("after-sale-apply") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "申请售后", search = "搜索售后类型、规则说明",
            )
        }

        currentRoute.startsWith("after-sale-detail") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "售后详情", search = "搜索售后编号、补充材料",
            )
        }

        // ── 社区子屏 ──
        currentRoute.startsWith("post-detail") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "帖子详情", search = "搜索帖子、评论、作者",
            )
        }

        currentRoute.startsWith("post-create") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "发布帖子", search = "搜索发布规则、模板",
            )
        }

        currentRoute.startsWith("group-chat") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "聊天官方群",
            )
        }

        // ── 消息子屏 ──
        currentRoute.startsWith("chat-detail") -> {
            CampusScreenConfig(showBottomNav = true, showHeroBar = false, brand = "聊天")
        }

        currentRoute.contains("AnnouncementDetail") -> {
            CampusScreenConfig(
                showBottomNav = true, showHeroBar = false,
                brand = "公告详情", search = "搜索公告内容、学校通知",
            )
        }

        // ── 个人中心子屏（12 条，不展示 BottomNav） ──
        currentRoute.startsWith("profile/wallet") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "我的钱包")
        }

        currentRoute.startsWith("profile/runner-apply") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "成为跑腿员")
        }

        currentRoute.startsWith("profile/address-manage") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "地址管理")
        }

        currentRoute.startsWith("profile/coupons") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "我的优惠券")
        }

        currentRoute.startsWith("profile/invite") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "邀请好友")
        }

        currentRoute.startsWith("profile/feedback") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "意见反馈")
        }

        currentRoute.startsWith("profile/about") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "关于我们")
        }

        currentRoute.startsWith("profile/my-published") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "我发布的")
        }

        currentRoute.startsWith("profile/my-sold") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "我卖出的")
        }

        currentRoute.startsWith("profile/my-bought") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "我买到的")
        }

        currentRoute.startsWith("profile/my-favorites") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false, brand = "我的收藏")
        }

        // ── 认证 & 全局 ──
        currentRoute.startsWith("login") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false)
        }

        currentRoute.startsWith("register") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false)
        }

        currentRoute.startsWith("password-reset") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false)
        }

        currentRoute.startsWith("account-delete") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false)
        }

        currentRoute.startsWith("school-select") -> {
            CampusScreenConfig(showBottomNav = false, showHeroBar = false)
        }

        // ── 兜底 ──
        else -> CampusScreenConfig(showBottomNav = false, showHeroBar = false)
    }
}
