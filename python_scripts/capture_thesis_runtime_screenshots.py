from __future__ import annotations

import os
import time
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

try:
    from playwright.sync_api import TimeoutError as PlaywrightTimeoutError
    from playwright.sync_api import sync_playwright
except ImportError as exc:  # pragma: no cover
    raise SystemExit("Missing dependency: playwright. Install with `python -m pip install playwright`.") from exc


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "output" / "playwright" / "thesis-runtime"
BASE_URL = os.environ.get("THESIS_BASE_URL", "http://localhost:5173")
LOGIN_PASSWORD = os.environ.get("THESIS_DEMO_PASSWORD", "Abc@123456")
_custom_browser = os.environ.get("PLAYWRIGHT_BROWSER_PATH", "").strip()
BROWSER_CANDIDATES = [
    Path(_custom_browser) if _custom_browser else None,
    Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
    Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
]


def choose_font(size: int):
    for candidate in [
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simsun.ttc",
        r"C:\Windows\Fonts\arial.ttf",
    ]:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


def launch_browser(playwright):
    browser_path = next((path for path in BROWSER_CANDIDATES if path is not None and path.exists()), None)
    if browser_path is None:
        raise SystemExit("No compatible browser found. Expected Edge or Chrome.")
    return playwright.chromium.launch(
        executable_path=str(browser_path),
        headless=True,
        args=["--disable-gpu", "--window-size=1440,1100"],
    )


def new_context(browser):
    return browser.new_context(viewport={"width": 1440, "height": 1100}, locale="zh-CN")


def wait_for_ready(page, marker: str):
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(800)
    page.wait_for_selector(f"text={marker}", timeout=15000)


def login(page, username: str, target_path: str):
    page.goto(f"{BASE_URL}/login", wait_until="domcontentloaded")
    wait_for_ready(page, "校园物资智能管理系统")
    page.get_by_placeholder("输入用户名").fill(username)
    page.get_by_placeholder("输入密码").fill(LOGIN_PASSWORD)
    page.get_by_role("button", name="登录系统").click()
    page.wait_for_url("**/dashboard", timeout=15000)
    page.goto(f"{BASE_URL}{target_path}", wait_until="domcontentloaded")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1000)


def screenshot_login(browser):
    context = new_context(browser)
    page = context.new_page()
    page.goto(f"{BASE_URL}/login", wait_until="domcontentloaded")
    wait_for_ready(page, "校园物资智能管理系统")
    page.screenshot(path=str(OUTPUT_DIR / "fig_4_3_login.png"))
    context.close()


def screenshot_apply_create(browser):
    context = new_context(browser)
    page = context.new_page()
    login(page, "dept", "/apply/list")
    wait_for_ready(page, "申领审批")
    page.get_by_role("button", name="新建申领").click()
    page.wait_for_selector("text=新建申领", timeout=10000)
    page.wait_for_timeout(600)
    page.screenshot(path=str(OUTPUT_DIR / "fig_4_4_apply_dept.png"))
    context.close()


def screenshot_apply_approve(browser):
    context = new_context(browser)
    page = context.new_page()
    login(page, "approver", "/apply/list")
    wait_for_ready(page, "申领审批")
    page.wait_for_selector("text=SUBMITTED", timeout=10000)
    page.screenshot(path=str(OUTPUT_DIR / "fig_4_5_apply_approver.png"))
    context.close()


def screenshot_stock_out(browser):
    context = new_context(browser)
    page = context.new_page()
    login(page, "warehouse", "/inventory/stock-out")
    wait_for_ready(page, "出库管理")
    page.get_by_role("button", name="新建出库单").click()
    page.wait_for_selector("text=新建出库单", timeout=10000)
    page.wait_for_timeout(600)
    page.screenshot(path=str(OUTPUT_DIR / "fig_4_5_stockout_warehouse.png"))
    context.close()


def click_dialog_select(dialog, index: int, option_text: str, page):
    dialog.locator(".el-select").nth(index).locator(".el-select__wrapper").click()
    option = page.locator(".el-select-dropdown:visible .el-select-dropdown__item").filter(has_text=option_text).first
    option.click()


def screenshot_transfer_recommend(browser):
    context = new_context(browser)
    page = context.new_page()
    login(page, "warehouse", "/transfer/list")
    wait_for_ready(page, "调拨管理")
    page.get_by_role("button", name="新建调拨").click()
    page.wait_for_selector(".el-dialog:visible", timeout=10000)
    page.wait_for_timeout(1200)
    dialog = page.locator(".el-dialog:visible").last
    click_dialog_select(dialog, 0, "东风校区分仓", page)
    click_dialog_select(dialog, 2, "急救包", page)
    page.get_by_role("button", name="智能推荐").click()
    page.wait_for_selector("text=AI 调度推荐方案", timeout=10000)
    page.wait_for_timeout(600)
    page.screenshot(path=str(OUTPUT_DIR / "fig_4_6_transfer_recommend.png"))
    context.close()


def screenshot_warning(browser):
    context = new_context(browser)
    page = context.new_page()
    login(page, "warehouse", "/warning/list")
    wait_for_ready(page, "预警管理")
    page.wait_for_selector("text=未处理", timeout=10000)
    page.wait_for_selector("text=已处理", timeout=10000)
    page.screenshot(path=str(OUTPUT_DIR / "fig_4_7_warning.png"))
    context.close()


def screenshot_analytics(browser):
    context = new_context(browser)
    page = context.new_page()
    login(page, "admin", "/analytics/charts")
    wait_for_ready(page, "统计分析")
    page.wait_for_selector(".chart-card", timeout=10000)
    page.wait_for_timeout(2500)
    page.screenshot(path=str(OUTPUT_DIR / "fig_4_8_analytics.png"))
    context.close()


def compose_approval_stockout() -> Path:
    left = Image.open(OUTPUT_DIR / "fig_4_5_apply_approver.png").convert("RGB")
    right = Image.open(OUTPUT_DIR / "fig_4_5_stockout_warehouse.png").convert("RGB")
    target_height = max(left.height, right.height)
    if left.height != target_height:
        left = left.resize((int(left.width * target_height / left.height), target_height))
    if right.height != target_height:
        right = right.resize((int(right.width * target_height / right.height), target_height))
    margin = 24
    header = 80
    canvas = Image.new("RGB", (left.width + right.width + margin * 3, target_height + header + margin), "white")
    draw = ImageDraw.Draw(canvas)
    title_font = choose_font(26)
    label_font = choose_font(22)
    draw.text((margin, 20), "审批与出库执行界面", font=title_font, fill=(0, 0, 0))
    draw.text((margin, 48), "左：审批人申领审批视图    右：仓库管理员出库管理视图", font=label_font, fill=(70, 70, 70))
    canvas.paste(left, (margin, header))
    canvas.paste(right, (left.width + margin * 2, header))
    output = OUTPUT_DIR / "fig_4_5_approval_stockout_combo.png"
    canvas.save(output)
    return output


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = launch_browser(playwright)
        try:
            screenshot_login(browser)
            screenshot_apply_create(browser)
            screenshot_apply_approve(browser)
            screenshot_stock_out(browser)
            screenshot_transfer_recommend(browser)
            screenshot_warning(browser)
            screenshot_analytics(browser)
            combo = compose_approval_stockout()
        except PlaywrightTimeoutError as exc:
            raise SystemExit(f"Screenshot capture timed out: {exc}") from exc
        finally:
            browser.close()
    print(combo)


if __name__ == "__main__":
    main()
