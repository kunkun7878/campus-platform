// ═══════════════════════════════════════════════════════════════
// _shared/sensitive-words.ts
// 敏感词库 + Aho-Corasick 自动机 + moderate() 审核入口
//
// 风险分级:
//   block  — 命中硬黑名单 (严重违规), 不存库, 直接拒绝
//   review — 命中联系方式 / 软灰名单, 存库 status='pending_review'
//   pass   — 无命中, 存库 status='published'
//
// 联系方式 (手机/QQ/微信/邮箱) → review 而非 block
// ═══════════════════════════════════════════════════════════════

// ── 联系方式正则 (review 级) ──────────────────────────────────

/** 中国大陆手机号: 1[3-9]xxxxxxxxx */
const RE_PHONE = /1[3-9]\d{9}/;

/** QQ号: 5-11位数字, 不匹配更长的纯数字串 (排除 qq 邮箱里的数字) */
const RE_QQ = /(?<![@.\d])[1-9]\d{4,10}(?![@.\d])/;

/** 微信号: wxid_xxx / 字母+数字+_- 6-20位 */
const RE_WECHAT = /(?:wxid_[\w\-]+|(?<![a-zA-Z0-9])[a-zA-Z][a-zA-Z\d_\-]{5,19}(?![a-zA-Z\d_\-]))/;

/** 邮箱地址 */
const RE_EMAIL = /[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}/;

/** URL (http/https) */
const RE_URL = /https?:\/\/[^\s]+/;

// ── Aho-Corasick 自动机 ─────────────────────────────────────

class ACNode {
  children: Map<string, ACNode> = new Map();
  fail: ACNode | null = null;
  output: string[] = []; // 在此节点结束的模式串列表
}

class AhoCorasick {
  private root: ACNode = new ACNode();
  private built = false;

  /** 添加一个模式串 (H5: lowercase for case-insensitive matching) */
  addPattern(pattern: string): void {
    if (!pattern || pattern.length === 0) return;
    const lower = pattern.toLowerCase();
    let node = this.root;
    for (const ch of lower) {
      if (!node.children.has(ch)) {
        node.children.set(ch, new ACNode());
      }
      node = node.children.get(ch)!;
    }
    node.output.push(pattern);
    this.built = false;
  }

  /** 批量添加 */
  addPatterns(patterns: string[]): void {
    for (const p of patterns) this.addPattern(p);
  }

  /** BFS 构建失败指针 */
  buildFailure(): void {
    const queue: ACNode[] = [];

    // 第一层节点的 fail 指向 root
    for (const [, child] of this.root.children) {
      child.fail = this.root;
      queue.push(child);
    }

    while (queue.length > 0) {
      const current = queue.shift()!;
      for (const [ch, child] of current.children) {
        queue.push(child);
        // 沿着 fail 链找最长后缀
        let failNode = current.fail;
        while (failNode !== null && !failNode.children.has(ch)) {
          failNode = failNode.fail;
        }
        child.fail = failNode ? failNode.children.get(ch)! : this.root;
        // 合并 output
        if (child.fail.output.length > 0) {
          child.output.push(...child.fail.output);
        }
      }
    }

    this.built = true;
  }

  /** 在文本中搜索所有匹配的模式串 (去重) */
  search(text: string): string[] {
    if (!this.built) this.buildFailure();
    const result = new Set<string>();
    let node = this.root;

    for (const ch of text) {
      // 沿 fail 链回退直到匹配
      while (node !== this.root && !node.children.has(ch)) {
        node = node.fail!;
      }
      if (node.children.has(ch)) {
        node = node.children.get(ch)!;
      }
      // 收集所有命中
      if (node.output.length > 0) {
        for (const w of node.output) {
          result.add(w);
        }
      }
    }

    return [...result];
  }
}

// ── 词库 ────────────────────────────────────────────────────

/** 硬黑名单 (block 级) — 命中即拒绝, 不存库 */
const BLOCK_WORDS: string[] = [
  // 严重色情/低俗
  "裸聊",
  "约炮",
  "一夜情",
  "嫖娼",
  "卖淫",
  "援交",
  "包养",
  "大保健",
  "上门服务",
  "同城约",
  "色情",
  "黄色网站",
  "成人视频",
  "激情视频",

  // 赌博
  "赌博",
  "赌场",
  "网赌",
  "博彩",
  "时时彩",
  "六合彩",
  "赌球",
  "百家乐",
  "老虎机",
  "德州扑克现金",
  "棋牌现金",
  "下注",
  "盘口",
  "赔率",

  // 毒品/违禁品
  "毒品",
  "吸毒",
  "冰毒",
  "大麻",
  "海洛因",
  "摇头丸",
  "K粉",
  "麻古",
  "罂粟",
  "迷药",
  "听话水",
  "枪支",
  "弹药",
  "管制刀具",
  "假币",
  "假钞",

  // 诈骗/违法
  "诈骗",
  "刷单",
  "兼职打字",
  "日赚",
  "日结",
  "轻松赚钱",
  "在家赚钱",
  "无门槛",
  "高额回报",
  "稳赚",
  "内部消息",
  "内幕",
  "代开发票",
  "办证",
  "刻章",
  "信用卡套现",
  "花呗套现",
  "白条套现",
  "网贷",
  "高利贷",
  "无抵押贷款",
  "裸贷",

  // 政治敏感
  "法轮功",
  "falun",
  "六四",
  "天安门",
  "台独",
  "藏独",
  "疆独",
  "港独",
  "反共",
  "反华",
  "颠覆国家",

  // 其他严重违规
  "人肉搜索",
  "开盒",
  "社工库",
  "拖库",
  "撞库",
  "黑产",
  "刷粉",
  "刷量",
  "水军",
  "买粉丝",
  "买赞",
  "买评论",
];

/** 软灰名单 (review 级关键词) — 命中即 review */
const REVIEW_WORDS: string[] = [
  // 疑似广告/营销
  "加微信",
  "加我微信",
  "私聊",
  "私我",
  "私信",
  "VX",
  "vx",
  "薇信",
  "微我",
  "扣扣",
  "企鹅号",
  "看我主页",
  "点我头像",
  "关注公众号",
  "扫码",
  "二维码",

  // 疑似兼职/招聘
  "招人",
  "招募",
  "招聘",
  "兼职",
  "暑假工",
  "寒假工",
  "实习内推",

  // 争议性
  "代课",
  "代考",
  "代写",
  "代做",
  "论文代写",
  "替考",
  "枪手",
  "作弊",
  "答案",
  "考前答案",
  "四六级答案",
  "考研答案",

  // 校园敏感
  "举报",
  "投诉",
  "曝光",
  "挂人",
  "避雷",
  "扒皮",

  // 轻微低俗
  "傻逼",
  "傻B",
  "傻b",
  "尼玛",
  "卧槽",
  "我操",
  "草泥马",
  "特么",
  "妈的",
  "他妈",
  "你妈",
  "操你",
  "艹",
  "特么的",
  "你妹",
  "滚蛋",
  "去死",
  "智障",
  "脑残",
  "废物",
  "垃圾人",
  "贱人",
  "骚货",
  "婊子",
  "绿茶婊",
  "心机婊",
];

// ── 全局单例 ────────────────────────────────────────────────

const blockAC = new AhoCorasick();
blockAC.addPatterns(BLOCK_WORDS);

const reviewAC = new AhoCorasick();
reviewAC.addPatterns(REVIEW_WORDS);

// 初始化构建 (静态导入即构建)
blockAC.buildFailure();
reviewAC.buildFailure();

// ── 辅助函数 ────────────────────────────────────────────────

/** 提取正则命中的原文片段 (用于返回给客户端) */
function extractMatches(text: string, regex: RegExp): string[] {
  const matches = text.matchAll(new RegExp(regex.source, "g" + (regex.flags.includes("i") ? "" : "i")));
  const seen = new Set<string>();
  const result: string[] = [];
  for (const m of matches) {
    const v = m[0];
    if (!seen.has(v)) {
      seen.add(v);
      result.push(v);
      if (result.length >= 5) break; // 最多返回 5 个
    }
  }
  return result;
}

// ── moderate() — 审核入口 ──────────────────────────────────

export type RiskLevel = "block" | "review" | "pass";

export interface ModerationResult {
  /** 风险等级 */
  level: RiskLevel;
  /** 命中的敏感词/模式 */
  matched: string[];
  /** 拒绝原因 (block/review 时有值) */
  reason: string;
}

/**
 * 对内容执行敏感词审核。
 * 文本会先做 lowerCase 处理以保证大小写不敏感匹配。
 *
 * 判定顺序:
 *  1. block 词库 (AC 自动机) → 命中 → block
 *  2. review 词库 (AC 自动机) → 命中 → review
 *  3. 联系方式正则 → 命中 → review (不是 block)
 *  4. 无命中 → pass
 */
export function moderate(text: string): ModerationResult {
  if (!text || text.trim().length === 0) {
    return { level: "pass", matched: [], reason: "" };
  }

  const normalized = text.toLowerCase();
  const matched: string[] = [];

  // 1. 硬黑名单 — block
  const blockHits = blockAC.search(normalized);
  if (blockHits.length > 0) {
    // 返回原始大小写的命中词 (从原文本截取)
    return {
      level: "block",
      matched: blockHits,
      reason: `内容包含违规词: ${blockHits.slice(0, 5).join("、")}`,
    };
  }

  // 2. 软灰名单 — review
  const reviewWordHits = reviewAC.search(normalized);
  matched.push(...reviewWordHits);

  // 3. 联系方式正则 — review
  const phoneHits = extractMatches(text, RE_PHONE);
  const qqHits = extractMatches(text, RE_QQ);
  const wechatHits = extractMatches(text, RE_WECHAT);
  const emailHits = extractMatches(text, RE_EMAIL);
  const urlHits = extractMatches(text, RE_URL);

  const contactHits = [...phoneHits, ...qqHits, ...wechatHits, ...emailHits, ...urlHits];
  matched.push(...contactHits);

  if (matched.length > 0) {
    // 构造命中类型摘要
    const categories: string[] = [];
    if (reviewWordHits.length > 0) categories.push("敏感词");
    if (phoneHits.length > 0) categories.push("手机号");
    if (qqHits.length > 0) categories.push("QQ号");
    if (wechatHits.length > 0) categories.push("微信号");
    if (emailHits.length > 0) categories.push("邮箱");
    if (urlHits.length > 0) categories.push("链接");

    const uniqueMatched = [...new Set(matched)];
    return {
      level: "review",
      matched: uniqueMatched,
      reason: `内容触发审核 (${categories.join("、")}): ${uniqueMatched.slice(0, 5).join("、")}`,
    };
  }

  // 4. 全部通过
  return { level: "pass", matched: [], reason: "" };
}

/**
 * 仅检查内容是否包含联系方式 (手机/QQ/微信/邮箱/URL)。
 * 用于需要单独判断联系方式的场景。
 */
export function hasContactInfo(text: string): boolean {
  if (!text) return false;
  return (
    RE_PHONE.test(text) ||
    RE_QQ.test(text) ||
    RE_WECHAT.test(text) ||
    RE_EMAIL.test(text) ||
    RE_URL.test(text)
  );
}
