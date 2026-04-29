"""
使用 Playwright 对项目各页面截图，用于论文插图
"""
import os
import time
from playwright.sync_api import sync_playwright

BASE_URL = "http://127.0.0.1:5173"
OUT_DIR = r"E:\AI\house\project\daojishi\毕业设计论文材料\screenshots"
os.makedirs(OUT_DIR, exist_ok=True)

ADMIN_USER  = ("admin",  "648666")
AGENT_USER  = ("agent1", "648666")

def login(page, username, password):
    page.goto(f"{BASE_URL}/login", wait_until="networkidle")
    time.sleep(1.5)
    # username input has placeholder="用户名"
    page.fill("input[placeholder='用户名']", username)
    # password input has type="password"
    page.fill("input[type='password']", password)
    # click the submit button (el-button with @click=handleLogin)
    page.click(".submit-btn")
    page.wait_for_load_state("networkidle")
    time.sleep(2.5)

def take(page, name, url=None, wait=2):
    if url:
        page.goto(url, wait_until="networkidle")
    time.sleep(wait)
    path = os.path.join(OUT_DIR, f"{name}.png")
    page.screenshot(path=path, full_page=False)
    print(f"  ✔ {name}.png")
    return path

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=80)
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        print("=== 截图开始 ===")

        # 1. 首页（未登录）
        take(page, "01_home", f"{BASE_URL}/")

        # 2. 登录页
        take(page, "02_login", f"{BASE_URL}/login")

        # 3. 注册页
        take(page, "03_register", f"{BASE_URL}/register")

        # 4. 管理员后台
        login(page, *ADMIN_USER)
        take(page, "04_admin_dashboard", f"{BASE_URL}/admin")

        # 5. 经纪人工作台
        login(page, *AGENT_USER)
        take(page, "05_agent_dashboard", f"{BASE_URL}/agent")

        # 6. 房源列表
        take(page, "06_property_list", f"{BASE_URL}/properties")

        # 7. 房源详情（取列表第一张卡片链接）
        try:
            page.goto(f"{BASE_URL}/properties", wait_until="networkidle")
            time.sleep(2)
            # find first router-link inside a card
            link = page.query_selector("a[href*='/property/']")
            if link:
                href = link.get_attribute("href")
                take(page, "07_property_detail", f"{BASE_URL}{href}")
            else:
                take(page, "07_property_detail", f"{BASE_URL}/property/1")
        except Exception as e:
            print(f"  ! property detail: {e}")
            take(page, "07_property_detail", f"{BASE_URL}/property/1")

        # 8. 地图找房
        take(page, "08_map_view", f"{BASE_URL}/map", wait=4)

        # 9. 个人中心
        take(page, "09_profile", f"{BASE_URL}/profile")

        # 10. 发布房源表单
        take(page, "10_property_form", f"{BASE_URL}/property/new")

        browser.close()
        print(f"\n✔ 所有截图已保存至: {OUT_DIR}")

if __name__ == "__main__":
    run()
