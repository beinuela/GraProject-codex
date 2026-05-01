from __future__ import annotations

from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "output" / "doc" / "figures"

LINE = (0, 0, 0)
BG = (255, 255, 255)


def choose_font(size: int, bold: bool = False):
    candidates = [
        "C:/Windows/Fonts/msyhbd.ttc" if bold else "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/simhei.ttf" if bold else "C:/Windows/Fonts/simsun.ttc",
    ]
    for candidate in candidates:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


FONT_FLOW = choose_font(28)
FONT_BRANCH = choose_font(24)
FONT_ENTITY_TITLE = choose_font(30, bold=True)
FONT_ENTITY_TABLE = choose_font(22)
FONT_ENTITY_FIELD = choose_font(20)
FONT_REL = choose_font(24)


def box(cx: int, cy: int, w: int, h: int) -> tuple[int, int, int, int]:
    return (cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2)


def midpoint(a: tuple[int, int], b: tuple[int, int]) -> tuple[int, int]:
    return ((a[0] + b[0]) // 2, (a[1] + b[1]) // 2)


def multiline_size(draw: ImageDraw.ImageDraw, text: str, font, spacing: int = 8) -> tuple[int, int]:
    bbox = draw.multiline_textbbox((0, 0), text, font=font, spacing=spacing, align="center")
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def draw_centered_text(draw: ImageDraw.ImageDraw, rect, text: str, font, spacing: int = 8) -> None:
    w, h = multiline_size(draw, text, font, spacing)
    x1, y1, x2, y2 = rect
    draw.multiline_text(
        ((x1 + x2 - w) / 2, (y1 + y2 - h) / 2),
        text,
        font=font,
        fill=LINE,
        spacing=spacing,
        align="center",
    )


def draw_terminal(draw: ImageDraw.ImageDraw, rect, text: str) -> None:
    draw.rounded_rectangle(rect, radius=(rect[3] - rect[1]) // 2, outline=LINE, width=4, fill=BG)
    draw_centered_text(draw, rect, text, FONT_FLOW)


def draw_rect_box(draw: ImageDraw.ImageDraw, rect, text: str) -> None:
    draw.rectangle(rect, outline=LINE, width=4, fill=BG)
    draw_centered_text(draw, rect, text, FONT_FLOW)


def draw_process(draw: ImageDraw.ImageDraw, rect, text: str) -> None:
    draw.rounded_rectangle(rect, radius=36, outline=LINE, width=4, fill=BG)
    draw_centered_text(draw, rect, text, FONT_FLOW)


def draw_decision(draw: ImageDraw.ImageDraw, rect, text: str) -> None:
    x1, y1, x2, y2 = rect
    points = [((x1 + x2) // 2, y1), (x2, (y1 + y2) // 2), ((x1 + x2) // 2, y2), (x1, (y1 + y2) // 2)]
    draw.polygon(points, outline=LINE, width=4, fill=BG)
    draw_centered_text(draw, rect, text, FONT_FLOW)


def draw_entity(draw: ImageDraw.ImageDraw, rect, title: str, table: str, fields: Iterable[str]) -> None:
    x1, y1, x2, y2 = rect
    draw.rectangle(rect, outline=LINE, width=4, fill=BG)
    divider_y = y1 + 64
    draw.line([(x1, divider_y), (x2, divider_y)], fill=LINE, width=3)
    draw.text((x1 + 18, y1 + 14), title, font=FONT_ENTITY_TITLE, fill=LINE)
    table_bbox = draw.textbbox((0, 0), table, font=FONT_ENTITY_TABLE)
    draw.text((x2 - (table_bbox[2] - table_bbox[0]) - 18, y1 + 18), table, font=FONT_ENTITY_TABLE, fill=LINE)
    cursor_y = divider_y + 18
    for field in fields:
        draw.text((x1 + 18, cursor_y), field, font=FONT_ENTITY_FIELD, fill=LINE)
        cursor_y += 32


def draw_relation(draw: ImageDraw.ImageDraw, rect, text: str) -> None:
    x1, y1, x2, y2 = rect
    points = [((x1 + x2) // 2, y1), (x2, (y1 + y2) // 2), ((x1 + x2) // 2, y2), (x1, (y1 + y2) // 2)]
    draw.polygon(points, outline=LINE, width=4, fill=BG)
    draw_centered_text(draw, rect, text, FONT_REL)


def draw_polyline(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], arrow: bool = True, width: int = 4) -> None:
    draw.line(points, fill=LINE, width=width)
    if not arrow or len(points) < 2:
        return
    sx, sy = points[-2]
    ex, ey = points[-1]
    if ex == sx and ey == sy:
        return
    if abs(ex - sx) >= abs(ey - sy):
        direction = 1 if ex > sx else -1
        head = [(ex, ey), (ex - 18 * direction, ey - 10), (ex - 18 * direction, ey + 10)]
    else:
        direction = 1 if ey > sy else -1
        head = [(ex, ey), (ex - 10, ey - 18 * direction), (ex + 10, ey - 18 * direction)]
    draw.polygon(head, fill=LINE)


def draw_label(draw: ImageDraw.ImageDraw, x: int, y: int, text: str) -> None:
    bbox = draw.textbbox((0, 0), text, font=FONT_BRANCH)
    pad_x = 10
    pad_y = 6
    bg_box = (x - pad_x, y - pad_y, x + (bbox[2] - bbox[0]) + pad_x, y + (bbox[3] - bbox[1]) + pad_y)
    draw.rectangle(bg_box, fill=BG)
    draw.text((x, y), text, font=FONT_BRANCH, fill=LINE)


def point_on_polyline(points: list[tuple[int, int]], ratio: float = 0.5) -> tuple[float, float]:
    if not points:
        return (0.0, 0.0)
    if len(points) == 1:
        return (float(points[0][0]), float(points[0][1]))

    segments: list[tuple[tuple[int, int], tuple[int, int], float]] = []
    total = 0.0
    for start, end in zip(points, points[1:]):
        length = ((end[0] - start[0]) ** 2 + (end[1] - start[1]) ** 2) ** 0.5
        segments.append((start, end, length))
        total += length

    if total == 0:
        return (float(points[0][0]), float(points[0][1]))

    remaining = max(0.0, min(1.0, ratio)) * total
    for index, (start, end, length) in enumerate(segments):
        if remaining <= length or index == len(segments) - 1:
            fraction = 0.0 if length == 0 else remaining / length
            return (
                start[0] + (end[0] - start[0]) * fraction,
                start[1] + (end[1] - start[1]) * fraction,
            )
        remaining -= length

    return (float(points[-1][0]), float(points[-1][1]))


def draw_label_on_polyline(
    draw: ImageDraw.ImageDraw,
    points: list[tuple[int, int]],
    text: str,
    ratio: float = 0.5,
    offset: tuple[int, int] | None = None,
) -> None:
    center_x, center_y = point_on_polyline(points, ratio)
    if offset is None:
        start_x, start_y = points[0]
        end_x, end_y = points[-1]
        if abs(end_x - start_x) >= abs(end_y - start_y):
            offset = (0, -28)
        else:
            offset = (30, 0)

    bbox = draw.textbbox((0, 0), text, font=FONT_BRANCH)
    text_w = bbox[2] - bbox[0]
    text_h = bbox[3] - bbox[1]
    draw_label(
        draw,
        round(center_x + offset[0] - text_w / 2),
        round(center_y + offset[1] - text_h / 2),
        text,
    )


def flow_point(rect, side: str) -> tuple[int, int]:
    x1, y1, x2, y2 = rect
    if side == "top":
        return ((x1 + x2) // 2, y1)
    if side == "bottom":
        return ((x1 + x2) // 2, y2)
    if side == "left":
        return (x1, (y1 + y2) // 2)
    if side == "right":
        return (x2, (y1 + y2) // 2)
    raise ValueError(side)


def draw_architecture_diagram(path: Path) -> None:
    image = Image.new("RGB", (2200, 2500), BG)
    draw = ImageDraw.Draw(image)
    boxes = {
        "browser": box(1100, 180, 560, 170),
        "frontend": box(1100, 460, 560, 170),
        "router": box(1100, 740, 560, 170),
        "gateway": box(1100, 1020, 560, 170),
        "controller": box(500, 1450, 520, 170),
        "service": box(1100, 1450, 520, 170),
        "persistence": box(1700, 1450, 520, 170),
        "db": box(1700, 2030, 560, 170),
    }
    labels = {
        "browser": "浏览器访问",
        "frontend": "前端表示层",
        "router": "路由与状态",
        "gateway": "接口访问层",
        "controller": "控制层",
        "service": "服务层",
        "persistence": "数据访问层",
        "db": "MySQL 数据库",
    }
    for key, rect in boxes.items():
        draw_rect_box(draw, rect, labels[key])

    for src, dst in [("browser", "frontend"), ("frontend", "router"), ("router", "gateway")]:
        draw_polyline(draw, [flow_point(boxes[src], "bottom"), flow_point(boxes[dst], "top")])

    branch_y = 1210
    draw_polyline(draw, [flow_point(boxes["gateway"], "bottom"), (1100, branch_y)], arrow=False)
    draw.line([(500, branch_y), (1700, branch_y)], fill=LINE, width=4)
    for key in ["controller", "service", "persistence"]:
        top = flow_point(boxes[key], "top")
        draw_polyline(draw, [(top[0], branch_y), top])

    draw_polyline(draw, [flow_point(boxes["controller"], "right"), flow_point(boxes["service"], "left")])
    draw_polyline(draw, [flow_point(boxes["service"], "right"), flow_point(boxes["persistence"], "left")])
    draw_polyline(draw, [flow_point(boxes["persistence"], "bottom"), flow_point(boxes["db"], "top")])
    image.save(path)


def draw_modules_diagram(path: Path) -> None:
    image = Image.new("RGB", (2500, 1850), BG)
    draw = ImageDraw.Draw(image)
    boxes = {
        "core": box(1250, 170, 640, 170),
        "auth": box(360, 610, 430, 160),
        "base": box(940, 610, 430, 160),
        "inventory": box(1520, 610, 430, 160),
        "support": box(2100, 610, 430, 160),
        "apply": box(360, 1180, 430, 160),
        "transfer": box(940, 1180, 430, 160),
        "warning": box(1520, 1180, 430, 160),
        "analytics": box(2100, 1180, 430, 160),
    }
    labels = {
        "core": "校园物资智能管理系统",
        "auth": "认证与权限",
        "base": "基础数据",
        "inventory": "仓储库存",
        "support": "系统支撑",
        "apply": "申领审批",
        "transfer": "调拨协同",
        "warning": "预警与补货",
        "analytics": "统计分析",
    }
    for key, rect in boxes.items():
        draw_rect_box(draw, rect, labels[key])

    first_bus_y = 400
    second_bus_y = 970
    trunk_x = flow_point(boxes["core"], "bottom")[0]
    trunk_top = flow_point(boxes["core"], "bottom")
    draw_polyline(draw, [trunk_top, (trunk_x, second_bus_y)], arrow=False)
    draw.line([(360, first_bus_y), (2100, first_bus_y)], fill=LINE, width=4)
    draw.line([(360, second_bus_y), (2100, second_bus_y)], fill=LINE, width=4)

    for key in ["auth", "base", "inventory", "support"]:
        top = flow_point(boxes[key], "top")
        draw_polyline(draw, [(top[0], first_bus_y), top], arrow=False)
    for key in ["apply", "transfer", "warning", "analytics"]:
        top = flow_point(boxes[key], "top")
        draw_polyline(draw, [(top[0], second_bus_y), top], arrow=False)
    image.save(path)


def draw_apply_flow(path: Path) -> None:
    image = Image.new("RGB", (1680, 2416), BG)
    draw = ImageDraw.Draw(image)
    shapes = {
        "start": ("terminal", box(840, 160, 320, 150), "创建申领单"),
        "draft": ("process", box(840, 420, 320, 150), "保存草稿"),
        "submit": ("process", box(840, 690, 320, 150), "提交申领单"),
        "urgent": ("decision", box(840, 970, 320, 170), "紧急等级≥2？"),
        "approve": ("process", box(300, 1310, 360, 160), "审批人审核"),
        "fast": ("process", box(1380, 1310, 360, 160), "快速审批"),
        "pass": ("decision", box(840, 1640, 320, 170), "审批通过？"),
        "reject": ("process", box(1380, 1640, 380, 160), "驳回并记录意见"),
        "stockout": ("process", box(840, 1910, 360, 160), "库存匹配\n批次出库"),
        "receive": ("process", box(840, 2140, 360, 160), "部门用户签收"),
        "end": ("terminal", box(840, 2330, 340, 140), "状态为 RECEIVED"),
    }
    for kind, rect, text in shapes.values():
        if kind == "terminal":
            draw_terminal(draw, rect, text)
        elif kind == "process":
            draw_process(draw, rect, text)
        else:
            draw_decision(draw, rect, text)

    draw_polyline(draw, [flow_point(shapes["start"][1], "bottom"), flow_point(shapes["draft"][1], "top")])
    draw_polyline(draw, [flow_point(shapes["draft"][1], "bottom"), flow_point(shapes["submit"][1], "top")])
    draw_polyline(draw, [flow_point(shapes["submit"][1], "bottom"), flow_point(shapes["urgent"][1], "top")])

    urgent_bottom = flow_point(shapes["urgent"][1], "bottom")
    approve_top = flow_point(shapes["approve"][1], "top")
    fast_top = flow_point(shapes["fast"][1], "top")
    draw_polyline(draw, [urgent_bottom, (840, 1090), (300, 1090), approve_top])
    draw_polyline(draw, [urgent_bottom, (840, 1090), (1380, 1090), fast_top])
    draw_label(draw, 545, 1040, "否")
    draw_label(draw, 1085, 1040, "是")

    pass_top = flow_point(shapes["pass"][1], "top")
    draw_polyline(draw, [flow_point(shapes["approve"][1], "bottom"), (300, 1460), (740, 1460), (740, pass_top[1])])
    draw_polyline(draw, [flow_point(shapes["fast"][1], "bottom"), (1380, 1460), (940, 1460), (940, pass_top[1])])

    draw_polyline(draw, [flow_point(shapes["pass"][1], "right"), flow_point(shapes["reject"][1], "left")])
    draw_label(draw, 1100, 1610, "否")
    draw_polyline(draw, [flow_point(shapes["pass"][1], "bottom"), flow_point(shapes["stockout"][1], "top")])
    draw_label(draw, 870, 1740, "是")
    draw_polyline(draw, [flow_point(shapes["stockout"][1], "bottom"), flow_point(shapes["receive"][1], "top")])
    draw_polyline(draw, [flow_point(shapes["receive"][1], "bottom"), flow_point(shapes["end"][1], "top")])
    image.save(path)


def draw_transfer_flow(path: Path) -> None:
    image = Image.new("RGB", (1680, 2256), BG)
    draw = ImageDraw.Draw(image)
    shapes = {
        "start": ("terminal", box(840, 160, 320, 140), "创建调拨单"),
        "recommend": ("process", box(840, 420, 420, 180), "选择目标仓\n参考候选来源仓"),
        "submit": ("process", box(840, 690, 320, 150), "提交调拨单"),
        "approve": ("process", box(840, 940, 320, 150), "审批人审核"),
        "pass": ("decision", box(840, 1210, 320, 170), "审批通过？"),
        "reject": ("process", box(1320, 1210, 340, 150), "驳回并结束"),
        "execute": ("process", box(840, 1500, 420, 170), "执行调拨\n同步两端库存"),
        "receive": ("process", box(840, 1780, 320, 150), "调入仓签收"),
        "end": ("terminal", box(840, 2020, 340, 140), "状态为 RECEIVED"),
    }
    for kind, rect, text in shapes.values():
        if kind == "terminal":
            draw_terminal(draw, rect, text)
        elif kind == "process":
            draw_process(draw, rect, text)
        else:
            draw_decision(draw, rect, text)

    for seq in [("start", "recommend"), ("recommend", "submit"), ("submit", "approve"), ("approve", "pass"), ("execute", "receive"), ("receive", "end")]:
        draw_polyline(draw, [flow_point(shapes[seq[0]][1], "bottom"), flow_point(shapes[seq[1]][1], "top")])
    draw_polyline(draw, [flow_point(shapes["pass"][1], "right"), flow_point(shapes["reject"][1], "left")])
    draw_label(draw, 1065, 1180, "否")
    draw_polyline(draw, [flow_point(shapes["pass"][1], "bottom"), flow_point(shapes["execute"][1], "top")])
    draw_label(draw, 870, 1320, "是")
    image.save(path)


def draw_warning_flow(path: Path) -> None:
    image = Image.new("RGB", (1680, 2256), BG)
    draw = ImageDraw.Draw(image)
    shapes = {
        "start": ("terminal", box(840, 150, 320, 140), "触发定时扫描"),
        "scan": ("process", box(840, 430, 400, 180), "检查库存与批次"),
        "risk": ("decision", box(840, 780, 320, 170), "发现异常？"),
        "noop": ("process", box(1320, 780, 400, 160), "无预警\n结束扫描"),
        "record": ("process", box(840, 1080, 400, 170), "生成预警记录"),
        "view": ("process", box(840, 1360, 400, 170), "查看待处理预警"),
        "handle": ("process", box(840, 1640, 400, 170), "填写说明\n更新状态"),
        "notify": ("process", box(840, 1920, 400, 170), "按需发送通知"),
        "end": ("terminal", box(840, 2170, 320, 140), "进入统计分析"),
    }
    for kind, rect, text in shapes.values():
        if kind == "terminal":
            draw_terminal(draw, rect, text)
        elif kind == "process":
            draw_process(draw, rect, text)
        else:
            draw_decision(draw, rect, text)

    for seq in [("start", "scan"), ("scan", "risk"), ("record", "view"), ("view", "handle"), ("handle", "notify"), ("notify", "end")]:
        draw_polyline(draw, [flow_point(shapes[seq[0]][1], "bottom"), flow_point(shapes[seq[1]][1], "top")])
    risk_to_record = [flow_point(shapes["risk"][1], "bottom"), flow_point(shapes["record"][1], "top")]
    draw_polyline(draw, risk_to_record)
    draw_label_on_polyline(draw, risk_to_record, "是")
    risk_to_noop = [flow_point(shapes["risk"][1], "right"), flow_point(shapes["noop"][1], "left")]
    draw_polyline(draw, risk_to_noop)
    draw_label_on_polyline(draw, risk_to_noop, "否")
    image.save(path)


def draw_auth_flow(path: Path) -> None:
    image = Image.new("RGB", (1600, 2672), BG)
    draw = ImageDraw.Draw(image)
    shapes = {
        "login": ("terminal", box(520, 140, 320, 140), "提交账号密码"),
        "verify": ("process", box(520, 390, 320, 150), "校验用户信息"),
        "issue": ("process", box(520, 650, 320, 150), "签发令牌"),
        "init": ("process", box(520, 930, 460, 170), "保存令牌并加载菜单"),
        "api": ("process", box(520, 1250, 440, 170), "携带 Token 访问接口"),
        "unauth": ("decision", box(520, 1560, 320, 170), "收到 401？"),
        "refresh": ("process", box(1180, 1560, 360, 150), "调用刷新接口"),
        "rotated": ("decision", box(1180, 1820, 320, 170), "刷新成功？"),
        "logout": ("process", box(1380, 1820, 420, 160), "清理令牌并返回登录页"),
        "retry": ("process", box(1180, 2130, 420, 170), "更新令牌并重放请求"),
        "done": ("terminal", box(520, 2400, 320, 140), "继续访问页面"),
    }
    for kind, rect, text in shapes.values():
        if kind == "terminal":
            draw_terminal(draw, rect, text)
        elif kind == "process":
            draw_process(draw, rect, text)
        else:
            draw_decision(draw, rect, text)

    for seq in [("login", "verify"), ("verify", "issue"), ("issue", "init"), ("init", "api"), ("api", "unauth")]:
        draw_polyline(draw, [flow_point(shapes[seq[0]][1], "bottom"), flow_point(shapes[seq[1]][1], "top")])
    draw_polyline(draw, [flow_point(shapes["unauth"][1], "bottom"), (520, 2330), flow_point(shapes["done"][1], "top")])
    draw_label(draw, 555, 1960, "否")
    draw_polyline(draw, [flow_point(shapes["unauth"][1], "right"), flow_point(shapes["refresh"][1], "left")])
    draw_label(draw, 850, 1525, "是")
    draw_polyline(draw, [flow_point(shapes["refresh"][1], "bottom"), flow_point(shapes["rotated"][1], "top")])
    draw_polyline(draw, [flow_point(shapes["rotated"][1], "right"), flow_point(shapes["logout"][1], "left")])
    draw_label(draw, 1290, 1790, "否")
    draw_polyline(draw, [flow_point(shapes["rotated"][1], "bottom"), flow_point(shapes["retry"][1], "top")])
    draw_label(draw, 1210, 1960, "是")
    draw_polyline(draw, [flow_point(shapes["retry"][1], "bottom"), (1180, 2250), (520, 2250), (520, flow_point(shapes["done"][1], "top")[1])])
    image.save(path)


def draw_transfer_recommend_flow(path: Path) -> None:
    image = Image.new("RGB", (1680, 2624), BG)
    draw = ImageDraw.Draw(image)
    shapes = {
        "start": ("terminal", box(840, 140, 320, 140), "发起调拨需求"),
        "input": ("process", box(840, 380, 420, 170), "输入目标校区\n物资与数量"),
        "calc": ("process", box(840, 650, 360, 150), "计算最短路径"),
        "candidate": ("process", box(840, 920, 420, 170), "筛选候选来源仓"),
        "rank": ("process", box(840, 1190, 420, 170), "按距离排序\n返回推荐结果"),
        "submit": ("process", box(840, 1460, 420, 170), "选择来源仓\n提交调拨单"),
        "approve": ("decision", box(840, 1740, 320, 170), "审批通过？"),
        "reject": ("process", box(1320, 1740, 340, 150), "驳回并结束"),
        "execute": ("process", box(840, 2020, 420, 170), "执行调拨\n迁移批次"),
        "receive": ("process", box(840, 2280, 360, 150), "调入仓签收"),
        "end": ("terminal", box(840, 2490, 340, 140), "状态为 RECEIVED"),
    }
    for kind, rect, text in shapes.values():
        if kind == "terminal":
            draw_terminal(draw, rect, text)
        elif kind == "process":
            draw_process(draw, rect, text)
        else:
            draw_decision(draw, rect, text)

    for seq in [("start", "input"), ("input", "calc"), ("calc", "candidate"), ("candidate", "rank"), ("rank", "submit"), ("submit", "approve"), ("execute", "receive"), ("receive", "end")]:
        draw_polyline(draw, [flow_point(shapes[seq[0]][1], "bottom"), flow_point(shapes[seq[1]][1], "top")])
    draw_polyline(draw, [flow_point(shapes["approve"][1], "right"), flow_point(shapes["reject"][1], "left")])
    draw_label(draw, 1065, 1710, "否")
    draw_polyline(draw, [flow_point(shapes["approve"][1], "bottom"), flow_point(shapes["execute"][1], "top")])
    draw_label(draw, 870, 1860, "是")
    image.save(path)


def draw_rbac_er(path: Path) -> None:
    image = Image.new("RGB", (3424, 1744), BG)
    draw = ImageDraw.Draw(image)
    boxes = {
        "dept": box(520, 320, 760, 250),
        "user": box(1712, 320, 820, 270),
        "role": box(2910, 320, 760, 250),
        "token": box(1120, 1280, 820, 250),
        "log": box(2300, 1280, 820, 250),
        "rel_dept_user": box(1110, 320, 260, 170),
        "rel_user_role": box(2320, 320, 260, 170),
        "rel_user_token": box(1470, 820, 260, 170),
        "rel_user_log": box(1950, 820, 260, 170),
    }
    draw_entity(draw, boxes["dept"], "部门", "sys_dept", ["部门编号", "部门名称", "上级部门", "版本号"])
    draw_entity(draw, boxes["user"], "用户", "sys_user", ["用户编号", "用户账号", "姓名", "状态", "dept_id / role_id"])
    draw_entity(draw, boxes["role"], "角色", "sys_role", ["角色编号", "角色编码", "角色名称", "角色说明"])
    draw_entity(draw, boxes["token"], "刷新令牌", "auth_refresh_token", ["令牌编号", "user_id", "token_hash", "expire_at / revoked"])
    draw_entity(draw, boxes["log"], "登录日志", "login_log", ["user_id", "username", "login_ip", "登录状态 / login_time"])
    draw_relation(draw, boxes["rel_dept_user"], "隶属")
    draw_relation(draw, boxes["rel_user_role"], "拥有")
    draw_relation(draw, boxes["rel_user_token"], "签发")
    draw_relation(draw, boxes["rel_user_log"], "产生")

    draw_polyline(draw, [flow_point(boxes["dept"], "right"), flow_point(boxes["rel_dept_user"], "left")], arrow=False)
    draw_label(draw, 860, 260, "1")
    draw_polyline(draw, [flow_point(boxes["rel_dept_user"], "right"), flow_point(boxes["user"], "left")], arrow=False)
    draw_label(draw, 1340, 260, "N")
    draw_polyline(draw, [flow_point(boxes["user"], "right"), flow_point(boxes["rel_user_role"], "left")], arrow=False)
    draw_label(draw, 2080, 260, "N")
    draw_polyline(draw, [flow_point(boxes["rel_user_role"], "right"), flow_point(boxes["role"], "left")], arrow=False)
    draw_label(draw, 2560, 260, "1")

    draw_polyline(draw, [flow_point(boxes["user"], "bottom"), (1580, 560), flow_point(boxes["rel_user_token"], "top")], arrow=False)
    draw_label(draw, 1530, 640, "1")
    draw_polyline(draw, [flow_point(boxes["rel_user_token"], "bottom"), (1120, 980), flow_point(boxes["token"], "top")], arrow=False)
    draw_label(draw, 1220, 1030, "N")

    draw_polyline(draw, [flow_point(boxes["user"], "bottom"), (1840, 560), flow_point(boxes["rel_user_log"], "top")], arrow=False)
    draw_label(draw, 1890, 640, "1")
    draw_polyline(draw, [flow_point(boxes["rel_user_log"], "bottom"), (2300, 980), flow_point(boxes["log"], "top")], arrow=False)
    draw_label(draw, 2190, 1030, "N")
    image.save(path)


def draw_inventory_er(path: Path) -> None:
    image = Image.new("RGB", (2896, 1936), BG)
    draw = ImageDraw.Draw(image)
    boxes = {
        "category": box(420, 340, 620, 230),
        "material": box(1340, 340, 680, 280),
        "warehouse": box(2470, 340, 620, 230),
        "inventory": box(1340, 1320, 720, 280),
        "batch": box(2470, 1320, 720, 280),
        "rel_cat_material": box(860, 340, 240, 160),
        "rel_material_inventory": box(1340, 820, 240, 160),
        "rel_wh_inventory": box(1960, 820, 240, 160),
        "rel_inventory_batch": box(1960, 1320, 240, 160),
    }
    draw_entity(draw, boxes["category"], "分类", "material_category", ["id", "category_name", "remark"])
    draw_entity(draw, boxes["material"], "物资档案", "material_info", ["id", "material_code", "material_name", "safety_stock", "spec / unit"])
    draw_entity(draw, boxes["warehouse"], "仓库", "warehouse", ["id", "warehouse_name", "campus", "manager"])
    draw_entity(draw, boxes["inventory"], "库存", "inventory", ["id", "material_id", "warehouse_id", "current_qty", "locked_qty"])
    draw_entity(draw, boxes["batch"], "库存批次", "inventory_batch", ["id", "batch_no", "in_qty", "remain_qty", "expire_date"])
    draw_relation(draw, boxes["rel_cat_material"], "归类")
    draw_relation(draw, boxes["rel_material_inventory"], "形成")
    draw_relation(draw, boxes["rel_wh_inventory"], "存放")
    draw_relation(draw, boxes["rel_inventory_batch"], "细分")

    draw_polyline(draw, [flow_point(boxes["category"], "right"), flow_point(boxes["rel_cat_material"], "left")], arrow=False)
    draw_label(draw, 620, 280, "1")
    draw_polyline(draw, [flow_point(boxes["rel_cat_material"], "right"), flow_point(boxes["material"], "left")], arrow=False)
    draw_label(draw, 1060, 280, "N")

    draw_polyline(draw, [flow_point(boxes["material"], "bottom"), flow_point(boxes["rel_material_inventory"], "top")], arrow=False)
    draw_label(draw, 1230, 610, "1")
    draw_polyline(draw, [flow_point(boxes["rel_material_inventory"], "bottom"), flow_point(boxes["inventory"], "top")], arrow=False)
    draw_label(draw, 1230, 1060, "N")

    draw_polyline(draw, [flow_point(boxes["warehouse"], "bottom"), (2470, 620), flow_point(boxes["rel_wh_inventory"], "top")], arrow=False)
    draw_label(draw, 2330, 600, "1")
    draw_polyline(draw, [flow_point(boxes["rel_wh_inventory"], "left"), (1700, 820), (1700, 1180), flow_point(boxes["inventory"], "right")], arrow=False)
    draw_label(draw, 1735, 960, "N")

    draw_polyline(draw, [flow_point(boxes["inventory"], "right"), flow_point(boxes["rel_inventory_batch"], "left")], arrow=False)
    draw_label(draw, 1640, 1260, "1")
    draw_polyline(draw, [flow_point(boxes["rel_inventory_batch"], "right"), flow_point(boxes["batch"], "left")], arrow=False)
    draw_label(draw, 2250, 1260, "N")
    image.save(path)


def draw_business_er(path: Path) -> None:
    image = Image.new("RGB", (3296, 1984), BG)
    draw = ImageDraw.Draw(image)
    boxes = {
        "apply": box(360, 360, 620, 250),
        "item": box(1240, 360, 700, 280),
        "stockout": box(2720, 360, 700, 280),
        "transfer": box(620, 1380, 700, 280),
        "warning": box(1700, 1380, 700, 280),
        "notify": box(2760, 1380, 620, 250),
        "rel_apply_item": box(820, 360, 240, 160),
        "rel_apply_stock": box(2060, 360, 240, 160),
        "rel_transfer_warning": box(1180, 1380, 240, 160),
        "rel_warning_notify": box(2230, 1380, 240, 160),
    }
    draw_entity(draw, boxes["apply"], "申领单", "apply_order", ["id", "dept_id", "applicant_id", "status"])
    draw_entity(draw, boxes["item"], "申领明细", "apply_order_item", ["id", "apply_order_id", "material_id", "apply_qty"])
    draw_entity(draw, boxes["stockout"], "出库单", "stock_out", ["id", "apply_order_id", "warehouse_id", "operator_id"])
    draw_entity(draw, boxes["transfer"], "调拨单", "transfer_order", ["id", "from_warehouse_id", "to_warehouse_id", "status"])
    draw_entity(draw, boxes["warning"], "预警记录", "warning_record", ["id", "warning_type", "handle_status", "handler_id"])
    draw_entity(draw, boxes["notify"], "通知消息", "notification", ["id", "title", "target_user_id", "is_read"])
    draw_relation(draw, boxes["rel_apply_item"], "包含")
    draw_relation(draw, boxes["rel_apply_stock"], "生成")
    draw_relation(draw, boxes["rel_transfer_warning"], "触发")
    draw_relation(draw, boxes["rel_warning_notify"], "发送")

    draw_polyline(draw, [flow_point(boxes["apply"], "right"), flow_point(boxes["rel_apply_item"], "left")], arrow=False)
    draw_label(draw, 590, 300, "1")
    draw_polyline(draw, [flow_point(boxes["rel_apply_item"], "right"), flow_point(boxes["item"], "left")], arrow=False)
    draw_label(draw, 1040, 300, "N")

    draw_polyline(draw, [flow_point(boxes["apply"], "top"), (360, 150), (2060, 150), flow_point(boxes["rel_apply_stock"], "top")], arrow=False)
    draw_label(draw, 1180, 120, "1")
    draw_polyline(draw, [flow_point(boxes["rel_apply_stock"], "right"), flow_point(boxes["stockout"], "left")], arrow=False)
    draw_label(draw, 2380, 300, "1")

    draw_polyline(draw, [flow_point(boxes["transfer"], "right"), flow_point(boxes["rel_transfer_warning"], "left")], arrow=False)
    draw_label(draw, 910, 1320, "1")
    draw_polyline(draw, [flow_point(boxes["rel_transfer_warning"], "right"), flow_point(boxes["warning"], "left")], arrow=False)
    draw_label(draw, 1450, 1320, "N")
    draw_polyline(draw, [flow_point(boxes["warning"], "right"), flow_point(boxes["rel_warning_notify"], "left")], arrow=False)
    draw_label(draw, 1970, 1320, "1")
    draw_polyline(draw, [flow_point(boxes["rel_warning_notify"], "right"), flow_point(boxes["notify"], "left")], arrow=False)
    draw_label(draw, 2470, 1320, "N")
    image.save(path)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    draw_architecture_diagram(OUTPUT_DIR / "fig_3_1_architecture.png")
    draw_modules_diagram(OUTPUT_DIR / "fig_3_2_modules.png")
    draw_apply_flow(OUTPUT_DIR / "fig_2_1_apply_flow.png")
    draw_transfer_flow(OUTPUT_DIR / "fig_2_2_transfer_flow.png")
    draw_warning_flow(OUTPUT_DIR / "fig_2_3_warning_flow.png")
    draw_auth_flow(OUTPUT_DIR / "fig_4_1_auth_flow.png")
    draw_transfer_recommend_flow(OUTPUT_DIR / "fig_4_2_transfer_recommend_flow.png")
    draw_rbac_er(OUTPUT_DIR / "fig_3_3_rbac_er.png")
    draw_inventory_er(OUTPUT_DIR / "fig_3_4_inventory_er.png")
    draw_business_er(OUTPUT_DIR / "fig_3_5_business_er.png")


if __name__ == "__main__":
    main()
