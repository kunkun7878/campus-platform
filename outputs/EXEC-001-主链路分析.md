# EXEC-001：主链路页面深度分析

> 执行智能体：主链路执行智能体  
> 执行时间：2026-05-11  
> 任务编号：EXEC-001  
> 分析范围：7 个主链路页面（home → publish → publish-hub → order-detail → order-list → after-sale-apply → after-sale-detail）  
> 输入文件：
> - `STRUCT-001-结构完整性盘点.md`（结构规范智能体产出）
> - `campus-miniapp-prototype.html`（完整原型，1425 行）
> - `campus_ui_decisions.md`（UI/交互决策）
> - `campus_rules.md`（产品硬规则）
> 
> 分析方法：逐 page 过 5 维度（交互缺陷 / 视觉问题 / 数据问题 / 功能缺失 / 代码问题）  
> 严重度标注：🔴 致命（阻断级） / 🟡 严重（影响体验或可维护性） / 🟢 建议（改进方向）

---

## 一、home（首页跑腿视图）

> 原型位置：`#screen-home`（含 hero 区公告 + left-rail + view-panel 主内容）  
> 关键子结构：`.announcement` / `.left-rail` / `.view-panel` / `.runner-scroll` / `.card-grid.two-col` / `.runner-filter-bar`

### 1.1 交互缺陷

🔴 **致命 — 4 个快捷入口均跳转同一目标，无类型预选**  
*证据：* `#screen-home` 中 `.category-row` 内的 4 个 `.category-pill`（帮取/帮送/帮买/万能帮）全部使用 `data-screen-target="publish"`，无任何差异化参数。用户无论点击哪个入口，都进入完全相同的发布表单，且表单中的 `sub-nav`（帮取/帮送/帮买/万能帮）不会自动选中对应 tab。  
*影响：* 快捷入口沦为纯装饰按钮，丧失"快捷"语义。

🔴 **致命 — market / lost 子视图缺失列表，直接跳到唯一详情页**  
*证据：* `viewConfigs` 中 `market.directScreen = 'goods-detail'`、`lost.directScreen = 'lost-detail'`（JS `viewConfigs` 对象），`switchView()` 函数检测到 `directScreen` 后直接调用 `showScreen(config.directScreen)`。且 `#panel-market` 和 `#panel-lost` 两个 div 均为空（`<div class="sub-panel" id="panel-market"></div>` 同理 lost）。  
*影响：* 用户点击左栏「二手」或「失物」后，看到的是单一硬编码示例项，无法浏览多件商品/失物。这直接违反 `campus_rules.md`「首页存在子视图切换：runner / market / lost」（L17-18）的意图。

🟡 **严重 — runner-filter-bar 4 个 filter-chip 切换是纯视觉行为，无实际过滤**  
*证据：* JS 中 `filterChips.forEach((chip) => chip.addEventListener('click', () => { filterChips.forEach((node) => node.classList.remove('active')); chip.classList.add('active'); }));` — 仅切换 active 类名，未调用任何过滤函数，4 张跑腿卡片始终全部显示。  
*影响：* 用户点击「待接单」「配送中」「已完成」时，卡片列表不产生任何变化，造成"按钮坏了"的错觉。

🟡 **严重 — 公告区点击跳转 announcement-detail，但 detail 页无返回列表的明确路径**  
*证据：* `.announcement` 有 `data-screen-target="announcement-detail"`。`screen-announcement-detail` 有 `back-btn`，但「返回」只能回到上一页（可能是 home 或其他来源页），无法在公告详情中直接切换到另一条公告。  
*影响：* 若未来有多条公告，用户无法在详情页内切换公告。

🟢 **建议 — 检索栏为纯展示，无输入交互**  
*证据：* `.search-bar` 内仅有 `<span>⌕</span>` 和 `<span class="search-text" id="searchHint">…</span>`，无 `<input>` 元素。JS 的 `updateHeader()` 仅更新 `searchHint.textContent`。  
*影响：* 用户无法实际搜索，搜索栏沦为状态展示条。

🟢 **建议 — 第 2 张跑腿卡片跳转到 after-sale-apply 而非 order-detail**  
*证据：* "药店帮买需求增长"卡片的 `data-screen-target="after-sale-apply"`，而其他 3 张均为 `"order-detail"`。  
*影响：* 用户点击这个卡片后直接进入售后申请表单，而非查看订单，行为不一致，容易困惑。

### 1.2 视觉问题

🟡 **严重 — 跑腿卡片的 visual-bubble 绝对定位在窄屏下可能遮挡文字**  
*证据：* CSS L704-706：`.bubble-a { width:64px; height:64px; top:12px; right:18px; }` `.bubble-b { width:38px; height:38px; top:58px; right:64px; }`，均为 `position:absolute`。当 `.feed-visual` 高度仅 104px 时，两个装饰圆覆盖了右上角大片区域。在小屏（≤420px）下 `card-grid.two-col` 变为单列，卡片更窄，视觉 pill 可能被气泡遮挡。  
*影响：* `.visual-pill` 的 `z-index:1` 高于 `.visual-bubble`（无 z-index），但如果 bubble 过大或 pill 文字过长，pill 可读性降低。

🟡 **严重 — runner-filter-bar 无水平滚动视觉提示**  
*证据：* CSS L729-730：`.runner-filter-bar { display:inline-flex; overflow-x:auto; … }`。无 fade-mask、箭头或 shadow 提示用户可横向滚动。  
*影响：* 当未来增加更多 filter 选项时，用户可能不知道可以滑动。

🟢 **建议 — brand-block h1 (28px) 与各 screen 的 title-stack h2 (22px) 字号不一致**  
*证据：* `.brand-block h1, .title-stack h2 { font-size: 28px; }` 但 `.title-stack h2 { font-size: 22px; }`。home 的 hero 区使用 28px 标题，但切换到 publish/order-detail 等页面后 hero 标题变为 22px（通过 `pageBrand.textContent` + `.brand-block h1` 渲染，但 `.title-stack h2` 是独立元素）。  
*影响：* 跨页面标题字号不统一，品牌感削弱。

### 1.3 数据问题

🟡 **严重 — 4 张跑腿卡片全为硬编码，无分页/加载机制**  
*证据：* 4 张 `article.feed-card` 的标题、描述、价格（¥3.99/¥2.50/¥5.00/¥6.00）、点赞数（23/11/16/9）、时间（2/5/8/12 分钟前）均为静态 HTML。无 `data-*` 属性承载真实数据模型。  
*影响：* 原型无法演示动态数据场景，真实开发需完全重写渲染逻辑。

🟢 **建议 — heroMeta 所有 screen 使用相同硬编码天气/学校**  
*证据：* `screenConfigs` 中所有 14 个 screen 的 `meta` 字段均为 `'主校区 · 晴 26°C · …'`（仅后缀不同）。天气数据为虚假静态值。  
*影响：* 学校隔离规则（`campus_rules.md` L23-24）在原型中无任何数据层面体现。

### 1.4 功能缺失

🔴 **致命 — 缺失二手商品列表视图（`#panel-market` 为空 div）**  
- 与 STRUCT-001 发现一致：market 子视图直接跳到 `goods-detail`，无列表。

🔴 **致命 — 缺失失物列表视图（`#panel-lost` 为空 div）**  
- 与 STRUCT-001 发现一致。

🟡 **严重 — 无下拉刷新 / 无限滚动 / 骨架屏**  
- `runner-scroll` 仅为一个带 `overflow-y: auto` 的 div，无任何加载状态处理。

🟡 **严重 — 无空态组件**  
- 当 filter 切换到无匹配结果时，无"暂无跑腿需求"之类的空态占位。

### 1.5 代码问题

🟡 **严重 — 事件绑定方式为 querySelectorAll 静态绑定，不支持动态新增节点**  
*证据：* JS 中 `document.querySelectorAll('[data-screen-target]').forEach((trigger) => trigger.addEventListener('click', …));` 在脚本初始化时执行一次。任何后续动态插入的带 `data-screen-target` 的元素都不会获得事件。应改用事件委托（如 `document.addEventListener('click', (e) => { const t = e.target.closest('[data-screen-target]'); … })`）。  
*影响：* 真实项目中 JS 动态渲染卡片时需额外处理事件。

🟡 **严重 — filterWrap 点击外部关闭存在事件冒泡竞争**  
*证据：* `filterButton.addEventListener('click', () => filterMenu.classList.toggle('open'));` 与 `document.addEventListener('click', (event) => { if (!filterWrap.contains(event.target)) closeFilterMenu(); });` 在同一次点击中先后触发。`toggle('open')` 打开菜单后，`document` 的 click 监听器因为 `filterWrap.contains(event.target)` 返回 true 而不会关闭。此为正确行为，但依赖 DOM 包含关系，若未来 filterWrap 结构变化容易出错。建议使用 `e.stopPropagation()` 显式控制。

🟢 **建议 — category-pill 使用 `<div>` 而非 `<button>`，无键盘可访问性**  
*证据：* `.category-pill` 为 `<div>` 元素，无 `tabindex`、无 `role="button"`、无键盘事件。  
*影响：* 无障碍访问（a11y）不达标。

---

## 二、publish（发布跑腿）

> 原型位置：`#screen-publish`  
> 关键子结构：`.sub-nav` / `.form-card` / `.primary-btn`

### 2.1 交互缺陷

🔴 **致命 — 「确认发布」按钮直接跳转 order-detail，无提交反馈**  
*证据：* `<button class="primary-btn" type="button" data-screen-target="order-detail">确认发布并查看订单</button>`  
*影响：* 用户点击「确认发布」后直接跳到订单详情页，无 "发布中…" → "发布成功 ✔" 的过渡状态（Toast/加载/成功反馈）。若真实请求失败（网络错误/校验不通过），用户看不到任何错误提示。与 STRUCT-001 关于"所有表单提交均直接跳转，无即时反馈"的发现一致。

🟡 **严重 — sub-nav 4 个 tab-chip 切换无联动表单行为**  
*证据：* JS 中 tab-chip 的事件处理仅切换 `active` 类名（`document.querySelectorAll('[data-chip-group]').forEach(…)`），不联动表单字段。例如：切换到「帮买」tab 时，表单仍然显示"取货地点/送达地点"，而非「帮买」应有的"购买店铺/商品名称"字段。  
*影响：* 4 种跑腿类型共用同一表单结构，字段语义不匹配。

🟡 **严重 — 「添加图片」「追加赏金」「售后说明」3 个 tag-btn 无 click 处理逻辑**  
*证据：* `<button class="tag-btn">添加图片</button>` `<button class="tag-btn">追加赏金</button>` `<button class="tag-btn">售后说明</button>` 这三个按钮均无 `data-screen-target` 且 JS 中无任何绑定（原型 JS 仅处理 `data-screen-target`、`data-nav-target`、`data-back`、`data-chip-group`、`filter-chip`）。  
*影响：* 点击无任何响应，用户误以为功能损坏。

🟢 **建议 — 赏金输入框 value 为 "¥4.50"，含 ¥ 符号，不是数值**  
*证据：* `<input value="¥4.50" />`。  
*影响：* 真实场景下，用户修改金额时可能需要先删除 ¥ 符号，体验不佳。

### 2.2 视觉问题

🟡 **严重 — form-card 的 label 与 input 间距不一致**  
*证据：* `.form-card label { margin-bottom:8px; }`，`.form-card input { margin-bottom:12px; }`。label 与下一个元素（通常是 input）的间距为 8px，input 之间的间距（下一个 input 的 margin-top 为 0，上一个 input 的 margin-bottom 为 12px）为 12px。form-card 中没有统一的内边距体系。  
*影响：* 视觉节奏不均匀。

🟢 **建议 — form-card 内 `.split` 区域的 label-input 间距在双列布局中更为紧凑**  
*证据：* `<div class="split"><div><label>取货地点</label><input … /></div><div><label>送达地点</label><input … /></div></div>` — 每个 `<div>` 内的 label-input 对之间无额外 wrapper，内边距完全依赖全局 form-card 规则。  
*影响：* 双列紧凑布局可接受，但若字段标签长度不一（如"期望时间" vs "性别限制"），两列宽度不均衡。

### 2.3 数据问题

🟡 **严重 — 表单所有字段预填硬编码值，无 placeholder 机制**  
*证据：* 所有 `<input>` 和 `<textarea>` 使用 `value="…"` 而非 `placeholder="…"`。真实用户打开表单时会看到已有内容的"脏"表单，需手动清空再填写。  
*影响：* 演示数据与真实数据混杂，且无法区分"默认值"和"用户输入"。

🟢 **建议 — 性别限制和自动取消 select 选项固定，无「请选择」default 项**  
*证据：* `<select><option>不限</option><option>仅女生</option><option>仅男生</option></select>` 和 `<select><option>20 分钟</option><option>30 分钟</option><option>60 分钟</option></select>` 均无 placeholder option（如 `disabled selected` 的默认项）。  
*影响：* 用户可能无意中提交默认选中值，缺少"主动选择"提示。

### 2.4 功能缺失

🔴 **致命 — 无表单校验**  
*证据：* 整个 submit 流程中无任何 JS 校验逻辑：「确认发布」按钮仅执行 `showScreen('order-detail')`。无标题非空校验、无赏金格式校验、无必填字段标记。  
*影响：* 用户可提交空标题或非法金额，直接跳到订单详情页，造成严重数据质量问题。

🟡 **严重 — 「添加图片」无实际功能**  
*证据：* 按钮仅作展示，无 file input 或图片选择逻辑。`campus_ui_decisions.md` 组件约定中亦无 ImagePicker 组件定义。  
*影响：* 图片上传是跑腿发布核心辅助功能，当前完全不可用。

🟡 **严重 — 无草稿保存机制**  
- 用户填一半退出，所有内容丢失。

🟡 **严重 — 地址字段为自由文本输入，无预设地址选择**  
- 产品规则中有「地址管理」概念（profile 页 L1281-1283），但发布表单无法从中选取已保存地址。

### 2.5 代码问题

🟡 **严重 — primary-btn 使用 `type="button"` 而非 `type="submit"`**  
*证据：* `<button class="primary-btn" type="button" data-screen-target="order-detail">…</button>`。  
*影响：* 语义不正确，且若未来包裹在 `<form>` 中使用，不会触发表单提交事件。

🟢 **建议 — form-card 内无 `<form>` 包裹标签**  
*证据：* 表单字段直接放在 `<div class="form-card">` 内，无 `<form>` 元素。  
*影响：* 无法使用原生表单 API（如 `FormData`、`checkValidity()`），且无障碍语义缺失。

---

## 三、publish-hub（统一发布入口）

> 原型位置：`#screen-publish-hub`  
> 关键子结构：`.mini-grid` / `.mini-card` × 4

### 3.1 交互缺陷

🔴 **致命 — 「发二手」「发失物」入口跳转到详情预览页而非发布表单**  
*证据：* `<div class="mini-card" data-screen-target="goods-detail">…<strong>发二手</strong>…</div>` 和 `<div class="mini-card" data-screen-target="lost-detail">…<strong>发失物</strong>…</div>`  
*影响：* 用户期望进入「发布二手」表单，实际看到的是一个硬编码的示例商品详情页（iPad 10 64G 蓝色 ¥1299）。发失物同理。这两个入口完全丧失了"发布"语义。这与 STRUCT-001 中 codebase_map 对 publish-hub「发帖子→community」的发现属于同类问题。

🔴 **致命 — 「发帖子」跳转到 community 而非发帖表单页**  
*证据：* `<div class="mini-card" data-screen-target="community">…<strong>发帖子</strong>…</div>`  
*影响：* 进入社区视图而非发帖编辑页，用户需在社区中再找到发帖入口（但 community 也无发帖入口）。与 STRUCT-001 L7「发帖页」缺失一致。

🟡 **严重 — 「发布跑腿」入口正确跳转 publish，但与另外 3 个入口的跳转逻辑不一致**  
*证据：* 仅「发布跑腿」跳转到的 `publish` screen 是一个真正的可填写表单。其他 3 个跳转到的是详情展示页或社区列表页。  
*影响：* 4 个入口的行为语义分裂——1 个真发布、1 个真列表、2 个假预览。

🟢 **建议 — publish-hub 无"我知道了，最近发布"之类的引导文案或快捷历史**  
*影响：* 入口页功能单一，缺少上下文引导。

### 3.2 视觉问题

🟡 **严重 — mini-card 的 ico 高度（36px）与 strong (12px) + span (10px) 的文本行高比例略显头重脚轻**  
*证据：* `.mini-card .ico { width:36px; height:36px; }`，`.mini-card strong { font-size:12px; }`，`.mini-card span { font-size:10px; }`。ico 占卡片高度的主导地位，文本区信息密度低。  
*影响：* 4 个 icon 视觉上占据 >50% 的卡片面积，文本信息相对薄弱。

🟢 **建议 — 4 个 mini-card 等宽排列，但文本长度不一**  
*证据：* 「帮取/帮送/帮买」vs「寻物/招领」vs「校园墙/讨论区」，副标题长度差异明显。  
*影响：* 等宽布局下，短文本卡片留白较多。

### 3.3 数据问题

🟡 **严重 — publish-hub 硬编码无动态入口配置**  
*证据：* 4 个 mini-card 均为静态 HTML，无配置对象驱动。  
*影响：* 若未来增加「发布活动」「发布兼职」等入口，需手动复制 HTML。

### 3.4 功能缺失

🟡 **严重 — 无可配置的入口管理**  
- 未来需支持按权限（普通用户/跑腿员/代理）显示不同入口集合，当前无权限过滤机制。

🟢 **建议 — 缺少"最近发布历史"模块**  
- 用户可能希望快速复用上次发布的模板。

### 3.5 代码问题

🟢 **建议 — page-top 右侧使用空 `<span></span>` 占位而非 semantical placeholder**  
*证据：* `<span></span>` 仅用于占据 flex 空间以保持标题居中。  
*影响：* 若未来需在右侧放置按钮，需替换空 span，维护成本低但不够清晰。

---

## 四、order-detail（订单详情）

> 原型位置：`#screen-order-detail`  
> 关键子结构：`.detail-card` / `.sheet-card` / `.timeline`

### 4.1 交互缺陷

🟡 **严重 — 「联系跑腿员」按钮跳转到 message 列表而非与该跑腿员的对话详情**  
*证据：* `<button class="ghost-btn" type="button" data-screen-target="message">联系跑腿员</button>`  
*影响：* 用户进入消息列表后，需手动找到「校跑腿小吴」的对话，而非直接打开对话。这与 STRUCT-001「聊天详情页缺失」一致。

🟡 **严重 — 「申请售后」按钮直接跳转到售后申请页，无二次确认**  
*证据：* `<button class="ghost-btn" type="button" data-screen-target="after-sale-apply">申请售后</button>`  
*影响：* 无「确认申请售后？」弹窗，可能误触。且未传递订单上下文（订单号 RL20260420001），售后申请页无法知道是针对哪个订单。

🟡 **严重 — timeline 节点仅有 3 个硬编码，无"已完成"等结尾状态**  
*证据：* `.timeline` 内包含：「19:08 已发布」「19:18 已接单」「19:26 配送中」——3 个节点后 timeline 直接结束。无"已送达""已完成""已评价"等后续节点。  
*影响：* 配送中状态之后再无时间线更新，不完整。

🟢 **建议 — detail-card 整体无 data-screen-target，不可点击进入更深层详情**  
*证据：* `.detail-card` 元素自身无可点击行为。  
*影响：* 在订单列表中点击订单卡片进入此页是合理的，但若需要查看「跑腿员详情」「评价详情」需额外入口。

### 4.2 视觉问题

🟡 **严重 — order-summary 中的 `.price`（¥4.50）和 `.status-badge`（配送中）横向排列但无明确视觉分组线**  
*证据：* `<div class="order-summary"><span class="price">¥4.50</span><span class="status-badge">配送中</span></div>`。`.order-summary { justify-content: space-between; gap:10px; }`  
*影响：* 价格和状态的语义距离仅靠 flex space-between 分隔，缺少视觉分隔线。

🟢 **建议 — timeline 的 timeline-dot 使用蓝色渐变，与「配送中」当前节点的语义关联不突出**  
*证据：* `.timeline-dot { background: linear-gradient(180deg, var(--brand), var(--brand-deep)); }`，所有 3 个节点使用相同颜色。  
*影响：* 已完成节点与进行中节点无视觉区分（通常"已完成"应使用绿色/灰色）。

### 4.3 数据问题

🟡 **严重 — 订单号硬编码「RL20260420001」，售后受理编号硬编码「AF2026042007」，但两个 screen 之间未关联**  
*证据：* order-detail 内无 `data-order-id` 属性，after-sale-apply 也无需订单号参数。  
*影响：* 从订单详情跳转到售后申请时，售后申请页不知道来源订单，无法建立关联。

🟡 **严重 — 「19:18 接单」「19:26 配送中」时间硬编码**  
*影响：* 无法演示实时更新或动态计时。

### 4.4 功能缺失

🟡 **严重 — 无配送中的实时地图/位置追踪**  
- 跑腿配送场景的核心体验是"跑腿员到哪了"，当前仅用静态 timeline 文本表示。

🟡 **严重 — 无取消订单按钮**  
- 「配送中」状态下应有取消/投诉入口（或说明取消规则）。

🟡 **严重 — 无确认收货按钮**  
- 无"我已收到"的完结操作，订单状态无法流转到"已完成"。

🟢 **建议 — 无评价入口**  
- 完成后应允许对跑腿员评分。

### 4.5 代码问题

🟡 **严重 — order-detail 与其他 detail-card screen（goods-detail、lost-detail、after-sale-detail、announcement-detail）使用完全相同的 HTML 结构模板但通过手动复制**  
*证据：* `.detail-card` + `.detail-banner` + `.detail-body` + `.detail-actions` 的结构在 5 个 screen 中重复出现，CSS 变量一致但 HTML 完全独立。  
*影响：* 符合 STRUCT-001 中"detail-card 重复 6 次未组件化"的发现。

---

## 五、order-list（订单列表）

> 原型位置：`#screen-order-list`  
> 关键子结构：`.sub-nav` / `.order-card` × 2

### 5.1 交互缺陷

🔴 **致命 — 4 个 tab-chip（全部/进行中/已完成/售后中）切换无实际过滤**  
*证据：* JS 中 tab-chip 的点击处理仅为类名切换（同 publish 页 sub-nav 逻辑）。2 张 order-card 始终全部显示，无论选中哪个 tab。  
*影响：* 与 home 页 runner-filter-bar 同样的问题：用户点击 tab 后列表不变，产生困惑。

🟡 **严重 — 第 1 张 order-card 跳转到 order-detail，第 2 张跳转到 after-sale-detail，行为不一致**  
*证据：* `<div class="order-card" data-screen-target="order-detail">…` vs `<div class="order-card" data-screen-target="after-sale-detail">…`  
*影响：* 用户看到两张订单卡片外观一致，但点击后跳转到完全不同类型的页面（订单详情 vs 售后详情）。

🟡 **严重 — 订单列表仅有 2 条硬编码订单，无法演示「全部」tab 下多订单场景**  
*影响：* 无法验证滚动、分页、tab 切换等交互。

### 5.2 视觉问题

🟡 **严重 — order-card 无 feed-visual 区域，与 home 页 feed-card 视觉风格不一致**  
*证据：* home 页的跑腿卡片有 `.feed-visual`（104px 彩色渐变背景 + bubbles + pill），而 order-list 的 `.order-card` 仅有 `.order-body`（纯白底 + padding）。  
*影响：* 同一产品中"卡片"组件视觉风格分裂。

🟢 **建议 — order-card 之间使用 `margin-bottom:10px`（继承自 `.feed-card`），但间距无 section 级别的统一管理**  
*影响：* 若未来增加不同类型卡片（如带图片的、带进度条的），间距需逐个调整。

### 5.3 数据问题

🟡 **严重 — 订单无唯一 ID 属性（如 `data-order-id`），tab 过滤无数据基础**  
*证据：* 两张 order-card 均无 `data-status` 或 `data-type` 属性，JS 无从过滤。  
*影响：* 无法实现真实的数据驱动列表。

🟡 **严重 — 第 2 张卡片的 `meta-pill` 文本「售后中」+「待补材料」暗示售后状态，但在「全部」tab 下应与普通订单区分**  
*影响：* 售后订单视觉上与普通订单相同（除了 pill 标签不同），用户可能无法快速区分。

### 5.4 功能缺失

🟡 **严重 — 无列表空态（如"暂无订单"）**  
🟡 **严重 — 无下拉刷新 / 上拉加载更多**  
🟡 **严重 — 无订单搜索功能**（page-top 的描述「搜索订单、筛选状态」仅为文案）  
🟢 **建议 — 无订单排序（按时间/按金额）**

### 5.5 代码问题

🟡 **严重 — order-card 与 feed-card 共享 CSS 规则（`.feed-card,.order-card,.community-card,.message-row { … }`）但结构不同**  
*证据：* feed-card 有 `.feed-visual` + `.feed-body`，order-card 仅有 `.order-body`。  
*影响：* 共享的 CSS 规则（如 `border-radius:20px`、`box-shadow`）合理，但结构差异意味着无法用同一组件模板渲染。

---

## 六、after-sale-apply（售后申请）

> 原型位置：`#screen-after-sale-apply`  
> 关键子结构：`.form-card` / `.primary-btn`

### 6.1 交互缺陷

🔴 **致命 — 「提交申请」按钮直接跳转到 after-sale-detail，无提交反馈**  
*证据：* `<button class="primary-btn" type="button" data-screen-target="after-sale-detail">提交申请并查看详情</button>`  
*影响：* 与 publish 页完全相同的问题：无加载态、无成功/失败 Toast、无字段校验。用户无法判断提交是否真的成功。

🟡 **严重 — 「上传损坏照片」「补充聊天记录」按钮无响应**  
*证据：* 两个 `<button class="tag-btn">` 无任何 JS 事件绑定，与 publish 页"添加图片"同类问题。

🟡 **严重 — 售后申请页无来源订单信息展示**  
*证据：* 表单中不包含关联订单号、订单金额、跑腿员信息等上下文。  
*影响：* 用户无法确认"我在对哪个订单申请售后"。

🟢 **建议 — 售后类型 select 选项固定 4 项，无「请选择售后类型」默认项**  
*证据：* `<select><option>物品损坏</option><option>超时严重</option>…</select>` — 无 blank/disabled 默认选项，可能无意中以默认值提交。

### 6.2 视觉问题

🟡 **严重 — form-card 中间件结构与 publish 页 form-card 几乎重复但字段不同**  
*证据：* 同样是 `.form-card` > `label` > `input/textarea/select` + `.split` + `.detail-actions`，仅字段名和按钮标签不同。  
*影响：* 两处表单视觉高度一致是好事，但完全通过复制 HTML 实现，未来维护成本高。

### 6.3 数据问题

🟡 **严重 — 「凭证数量」字段 value 为 "3 张图片"，非数值**  
*证据：* `<input value="3 张图片" />`  
*影响：* 该字段应展示关联图片数量或上传计数，当前为静态文本，无法反映真实上传状态。

### 6.4 功能缺失

🔴 **致命 — 无表单校验（同 publish）**  
🟡 **严重 — 无图片上传功能（同 publish）**  
🟡 **严重 — 缺少售后规则/说明入口**（用户可能不清楚哪些情况可以售后）  
🟢 **建议 — 缺少"取消申请"或"草稿保存"**

### 6.5 代码问题

🟡 **严重 — after-sale-apply 与 publish 的 form-card 结构高度相似但完全独立，未抽象为表单组件**  
*证据：* 两个 screen 的 HTML 结构中，`.form-card` + `.detail-actions` + `.primary-btn` 的模式完全一致，仅字段不同。  
*影响：* 与 STRUCT-001 的"组件缺口清单"（form-card 重复 3 次未组件化）一致。

---

## 七、after-sale-detail（售后详情）

> 原型位置：`#screen-after-sale-detail`  
> 关键子结构：`.detail-card` / `.sheet-card` / `.timeline`

### 7.1 交互缺陷

🔴 **致命 — 「补充材料」按钮无响应**  
*证据：* `<button class="ghost-btn">补充材料</button>` — 无 `data-screen-target`、无 JS 事件绑定。  
*影响：* 售后详情页的核心操作（补充材料）完全不可用，用户停滞在当前页。

🟡 **严重 — 「联系平台客服」跳转到 message 列表而非客服对话**  
*证据：* `<button class="ghost-btn" type="button" data-screen-target="message">联系平台客服</button>`  
*影响：* 与 order-detail「联系跑腿员」同问题：跳到消息列表而非特定对话。

🟡 **严重 — timeline 第三个节点「待用户补充材料」为占位文本，无交互入口**  
*证据：* `.timeline-content` 内容为 `<strong>待用户补充材料</strong><span>补齐后将进入协商阶段…</span>` — 纯展示文本，无可点击的「去补充」链接。  
*影响：* 用户需回到卡片顶部点击「补充材料」按钮，路径不直观。

### 7.2 视觉问题

🟡 **严重 — detail-card 中「售后中」「待补材料」标签使用 `.meta-pill` + `.meta-pill.success`，但"待补材料"用 `.success`（青绿色）语义不符**  
*证据：* `.meta-pill.success { background:#e7fbf7; color:#0a847e; }` — 青绿色在 UI 常规语义中代表"成功/正常"，但"待补充材料"是一个待办/警告状态。  
*影响：* 颜色语义混乱，用户可能误解为"已通过"。

🟢 **建议 — timeline 第三个节点"待用户补充材料"的 timeline-dot 与其他节点颜色相同，未作状态区分**  
*证据：* 所有 `.timeline-dot` 使用相同蓝色渐变，无论已完成/进行中/待处理。  
*影响：* 时间线视觉缺乏状态语义。

### 7.3 数据问题

🟡 **严重 — 售后状态仅覆盖 3 个阶段（提交→初审→待补充），缺失完整流转**  
*证据：* timeline 仅 3 个节点。缺失状态包括：审核中、审核通过、协商中、已解决、已拒绝、已关闭。  
*影响：* 售后流程在原型中仅覆盖约 30%。

🟡 **严重 — 页面 title 描述"继续补充材料"但 detail-card 中无反映剩余可补充项数量**  
*证据：* detail-body 中 `<p>状态：待补充材料。平台已初步受理，需要你补 1 张商品外箱照片。</p>` — "1 张"数量硬编码在文本中。  
*影响：* 无法展示动态的待补充项列表。

### 7.4 功能缺失

🟡 **严重 — 无审核状态中转（审核中 / 审核通过 / 审核拒绝）**  
- 与 STRUCT-001 发现一致：「审核态仅 after-sale-detail 有初步迹象」。

🟡 **严重 — 无撤销售后申请功能**  
🟡 **严重 — 无协商对话/留言板**  
- 售后沟通仅在 message 列表中进行，售后详情页内无内嵌对话。

🟢 **建议 — 缺少售后规则/预计处理时效展示**

### 7.5 代码问题

🟡 **严重 — after-sale-detail 与 order-detail 共享 `.detail-card + .sheet-card + .timeline` 结构但完全独立复制**  
*证据：* 两个 screen 的 HTML 结构模式完全一致，仅内容文本不同。  
*影响：* 符合 STRUCT-001 组件缺口清单：detail-card(×6)、timeline(×2)、sheet-card 均未组件化。

---

## 八、自检清单

> 对照执行智能体工作说明（`04-执行智能体工作说明.md` §6）逐项确认。

| # | 检查项 | 状态 | 说明 |
|---|-------|:----:|------|
| 1 | 页面是否完整 | ✅ | 7 个主链路页面均已分析，每个 screen 独立成节 |
| 2 | 状态是否齐全 | ❌ | **致命缺口**：所有 7 个 screen 均缺失空态/加载态/错误态。已分别在每页"功能缺失"维度指出 |
| 3 | 是否有入口与出口 | ⚠️ | home→publish 入口完整；publish→order-detail 出口存在但无提交反馈；order-list→order-detail/after-sale-detail 出口存在但入口上下文缺失；publish-hub→goods-detail/lost-detail/community 出口不正确（应指向发布表单而非详情页） |
| 4 | 是否使用统一组件 | ❌ | **致命缺口**：detail-card(×6)、form-card(×3)、timeline(×2)、page-top(×13) 均通过硬复制 HTML 实现，未组件化 |
| 5 | 是否使用统一命名 | ✅ | CSS 类名体系一致（`.feed-card`/`.form-card`/`.detail-card`/`.sheet-card` 等），CSS 变量体系完整 |
| 6 | 是否存在未解释交互 | ❌ | **致命缺口**："添加图片""追加赏金""售后说明""上传损坏照片""补充聊天记录""补充材料"——6 个按钮无任何 JS 处理，点击无响应 |
| 7 | 是否满足可开发交付要求 | ❌ | **不满足**。当前原型可演示静态流程，但缺少动态数据模型、表单校验、状态管理、组件抽象，无法直接转为开发规格 |

---

## 九、关键发现汇总

### 致命级（🔴）问题统计

| # | 问题 | 影响页面 |
|---|------|---------|
| 1 | 表单提交无反馈/无校验，直接跳转 | publish, after-sale-apply |
| 2 | market/lost 无列表视图，子视图切换失效 | home |
| 3 | publish-hub 中 3/4 入口未指向发布表单 | publish-hub |
| 4 | 「补充材料」核心操作无响应 | after-sale-detail |
| 5 | 所有 filter-chip/tab-chip 切换无实际过滤 | home, order-list, publish |
| 6 | "添加图片"系列按钮均无功能 | publish, after-sale-apply |
| 7 | 4 个 category-pill 快捷入口跳转同一目标且无类型预选 | home |

### 严重级（🟡）问题统计

| # | 问题 | 影响页面 |
|---|------|---------|
| 1 | 联系跑腿员/客服跳转到消息列表而非特定对话 | order-detail, after-sale-detail |
| 2 | 所有列表页无下拉刷新/无限滚动/骨架屏 | home, order-list |
| 3 | 订单列表和首页卡片视觉风格不一致 | order-list vs home |
| 4 | after-sale-detail 颜色语义混乱（待补材料用 success 青绿色） | after-sale-detail |
| 5 | 售后状态流转不完整（仅 3/9 节点） | after-sale-detail |
| 6 | 所有数据硬编码，无 data-* 属性承载数据模型 | 全部 7 页 |
| 7 | 详细组件（detail-card/form-card/timeline/page-top）未抽象化 | 全部 7 页 |
| 8 | JS 事件绑定为静态 querySelectorAll 而非事件委托 | 全部（JS 层） |
| 9 | 所有 screenConfigs 的 meta 硬编码相同天气/学校 | 全部（数据层） |
| 10 | order-detail 无确认收货/取消订单/评价操作 | order-detail |

### 建议级（🟢）问题统计

| # | 问题 | 影响页面 |
|---|------|---------|
| 1 | 检索栏为纯展示无输入 | home |
| 2 | 无障碍（a11y）不达标（div 代 button） | home, publish-hub |
| 3 | 表单无 `<form>` 标签语义 | publish, after-sale-apply |
| 4 | 赏金输入框含 ¥ 符号非纯数值 | publish |
| 5 | timeline 节点无状态色区分 | order-detail, after-sale-detail |

---

## 十、与 STRUCT-001 交叉验证

| STRUCT-001 发现 | EXEC-001 验证结果 | 一致性 |
|-----------------|-----------------|:------:|
| market/lost 缺失列表视图 | 在 home 1.1 中确认，并补充了 `directScreen` 机制的技术细节 | ✅ 一致 |
| 发布/表单类 screen 提交直接跳转无反馈 | 在 publish 2.1 + after-sale-apply 6.1 中确认 | ✅ 一致 |
| detail-card/form-card/timeline 重复未组件化 | 在 order-detail 4.5 + after-sale-detail 7.5 + publish 6.5 中确认 | ✅ 一致 |
| 审核态仅 after-sale-detail 有初步迹象 | 在 after-sale-detail 7.3 中确认，补充了缺失的 6 个状态节点 | ✅ 增强 |
| profile 页"成为跑腿员"无 target | EXEC-001 未覆盖 profile（不在主链路 7 页中），由 STRUCT-001 单独记录 | — |
| 空态/加载态/错误态全缺失 | 在每个页面的 4 维「功能缺失」中逐一指出 | ✅ 增强 |
| 社区帖子/群聊不可点击 | EXEC-001 未覆盖 community（不在主链路），但 publish-hub 3.1 确认"发帖子→community"而非发帖表单 | ✅ 交叉验证 |

---

*本报告由主链路执行智能体在 2026-05-11 产出，待提交质量审查智能体进行下一步审查。*
