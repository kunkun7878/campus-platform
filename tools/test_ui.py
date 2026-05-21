from playwright.sync_api import sync_playwright
import os

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 430, "height": 900})
    page.goto('http://127.0.0.1:8127/campus-miniapp-prototype.html')
    page.wait_for_timeout(2000)

    out = 'C:\\Users\\admin\\Desktop\\校园聚合平台\\test_screenshots'
    os.makedirs(out, exist_ok=True)

    # === 1. Login page ===
    print("=== 1. LOGIN PAGE ===")
    page.screenshot(path=f'{out}\\01-login.png')
    phone = page.locator('.phone')
    box = phone.bounding_box()
    print(f"Phone size: {box['width']:.0f}x{box['height']:.0f}")

    # Login
    phone_input = page.locator('#loginPhone')
    if phone_input.is_visible():
        phone_input.fill('13812345678')
        page.locator('#loginCodeInput').fill('123456')
        page.locator('#loginAgree').check()
        page.locator('#loginSubmitBtn').click()
        page.wait_for_timeout(2000)

    # === 2. School select ===
    print("=== 2. SCHOOL SELECT ===")
    page.screenshot(path=f'{out}\\02-school-select.png')
    school_cards = page.locator('.school-card')
    if school_cards.count() > 0:
        school_cards.first.click()
        page.wait_for_timeout(1000)

    # === 3. Home page ===
    print("=== 3. HOME PAGE ===")
    page.screenshot(path=f'{out}\\03-home.png', full_page=False)

    # Check phone size
    box = phone.bounding_box()
    print(f"Phone size after login: {box['width']:.0f}x{box['height']:.0f}")

    # Check rail items
    rail_items = page.locator('.rail-item')
    print(f"Rail items: {rail_items.count()}")

    # Check feed cards
    feed_cards = page.locator('.feed-card')
    print(f"Feed cards: {feed_cards.count()}")

    # Click market rail
    market_rail = page.locator('.rail-item[data-view="market"]')
    if market_rail.count() > 0:
        market_rail.click()
        page.wait_for_timeout(500)
        page.screenshot(path=f'{out}\\04-home-market.png', full_page=False)

    # Click lost rail
    lost_rail = page.locator('.rail-item[data-view="lost"]')
    if lost_rail.count() > 0:
        lost_rail.click()
        page.wait_for_timeout(500)
        page.screenshot(path=f'{out}\\05-home-lost.png', full_page=False)

    # Check scroll containers
    screen_scr = page.locator('.screen-scroll').count()
    runner_scr = page.locator('.runner-scroll').count()
    print(f"Scroll containers: screen-scroll={screen_scr}, runner-scroll={runner_scr}")

    # === 4. Publish page ===
    print("=== 4. PUBLISH PAGE ===")
    # Click bottom nav publish
    page.locator('[data-nav-target="publish-hub"]').click()
    page.wait_for_timeout(1000)
    page.screenshot(path=f'{out}\\06-publish-hub.png', full_page=False)

    # Check big cards
    big_cards = page.locator('.publish-big-card')
    print(f"Publish big cards: {big_cards.count()}")

    # Check sub buttons on runner card
    sub_btns = page.locator('.publish-sub-btn')
    print(f"Publish sub buttons: {sub_btns.count()}")

    # Click first sub button
    if sub_btns.count() > 0:
        sub_btns.first.click()
        page.wait_for_timeout(1000)
        page.screenshot(path=f'{out}\\07-publish-form.png', full_page=False)

    # Go back and test market publish
    page.go_back()
    page.wait_for_timeout(500)
    market_card = page.locator('[data-screen-target="market-publish"]')
    if market_card.count() > 0:
        market_card.first.click()
        page.wait_for_timeout(1000)
        page.screenshot(path=f'{out}\\08-market-publish.png', full_page=False)
    page.go_back()
    page.wait_for_timeout(500)

    # === 5. Community ===
    print("=== 5. COMMUNITY ===")
    page.locator('[data-nav-target="community"]').click()
    page.wait_for_timeout(1000)
    page.screenshot(path=f'{out}\\09-community.png', full_page=False)

    # Check channel tabs
    channel_tabs = page.locator('.channel-tab')
    print(f"Channel tabs: {channel_tabs.count()}")

    # Check post cards
    post_cards = page.locator('.post-card')
    print(f"Post cards: {post_cards.count()}")

    # Click a post
    if post_cards.count() > 0:
        post_cards.first.click()
        page.wait_for_timeout(1000)
        page.screenshot(path=f'{out}\\10-post-detail.png', full_page=False)

    # Check comment input
    comment_input = page.locator('#postDetailCommentInput')
    print(f"Comment input exists: {comment_input.count() > 0}")

    # Test comment
    if comment_input.count() > 0:
        comment_input.fill('测试评论功能')
        page.locator('#postDetailCommentSend').click()
        page.wait_for_timeout(500)
        page.screenshot(path=f'{out}\\11-post-comment.png', full_page=False)

    page.go_back()
    page.wait_for_timeout(500)

    # === 6. Profile ===
    print("=== 6. PROFILE ===")
    page.locator('[data-nav-target="profile"]').click()
    page.wait_for_timeout(1000)
    page.screenshot(path=f'{out}\\12-profile.png', full_page=False)

    # Check trade items
    trade_items = page.locator('.trade-item')
    print(f"Trade items: {trade_items.count()}")

    # Check status items
    status_items = page.locator('.status-item')
    print(f"Status items: {status_items.count()}")

    # Click "我发布的"
    my_pub = page.locator('[data-screen-target="my-published"]')
    if my_pub.count() > 0:
        my_pub.first.click()
        page.wait_for_timeout(1000)
        page.screenshot(path=f'{out}\\13-my-published.png', full_page=False)
    page.go_back()
    page.wait_for_timeout(500)

    # Click "我卖出的"
    my_sold = page.locator('[data-screen-target="my-sold"]')
    if my_sold.count() > 0:
        my_sold.first.click()
        page.wait_for_timeout(1000)
        page.screenshot(path=f'{out}\\14-my-sold.png', full_page=False)
    page.go_back()
    page.wait_for_timeout(500)

    # === 7. Check all buttons for dead elements ===
    print("=== 7. DEAD ELEMENT CHECK ===")
    # Go through main screens and check for buttons without handlers
    page.locator('[data-nav-target="home"]').click()
    page.wait_for_timeout(500)

    # Check hub-pending elements
    hub_pending = page.locator('.hub-pending')
    print(f"Hub-pending elements on home: {hub_pending.count()}")

    # Check all screens have scroll
    page.locator('[data-nav-target="message"]').click()
    page.wait_for_timeout(500)
    page.screenshot(path=f'{out}\\15-message.png', full_page=False)

    # Check chat
    msg_row = page.locator('.message-row')
    if msg_row.count() > 0:
        msg_row.first.click()
        page.wait_for_timeout(1000)
        page.screenshot(path=f'{out}\\16-chat-detail.png', full_page=False)
    page.go_back()

    # === Summary ===
    print("\n=== SUMMARY ===")
    print(f"Phone fixed size: {box['width']:.0f}x{box['height']:.0f}")
    print(f"Target: 390x844")
    overflow = page.evaluate("() => { const p = document.querySelector('.phone'); const s = getComputedStyle(p); return {overflow: s.overflow, width: s.width, height: s.height}; }")
    print(f"Phone CSS: {overflow}")

    # Check content overflow
    content_overflow = page.evaluate("() => { const c = document.querySelector('.content'); const s = getComputedStyle(c); return {overflow: s.overflow, flex: s.flex}; }")
    print(f"Content CSS: {content_overflow}")

    # Count screens
    screen_count = page.evaluate("() => document.querySelectorAll('.screen').length")
    print(f"Total screens: {screen_count}")
    screen_configs = page.evaluate("() => Object.keys(screenConfigs).length")
    print(f"Screen configs: {screen_configs}")

    browser.close()
    print("\nDone! Screenshots saved to test_screenshots/")
