from __future__ import annotations

import math
import shutil
import zipfile
from datetime import datetime
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
DOCX_PATH = ROOT / "Existing Thesis Draft" / "校园物资智能管理系统设计与实现-定稿工作稿.docx"
OUTPUT_DIR = ROOT / "output" / "diagrams" / "thesis-refresh"
BACKUP_PATH = ROOT / "Existing Thesis Draft" / f"校园物资智能管理系统设计与实现-定稿工作稿.docx.bak.{datetime.now():%Y%m%d-%H%M%S}-diagram-refresh"

PALETTE = {
    "bg_top": "#f7fbff",
    "bg_bottom": "#edf4ff",
    "ink": "#17233b",
    "text": "#273552",
    "muted": "#61728f",
    "line": "#50627f",
    "border": "#d9e6fb",
    "white": "#ffffff",
    "blue": ("#2f6fed", "#e8f0ff"),
    "teal": ("#22b8a9", "#e3fbf7"),
    "orange": ("#f59f45", "#fff2df"),
    "red": ("#ef6b6b", "#ffe7e7"),
    "violet": ("#7b61ff", "#f0ebff"),
    "slate": ("#5f6d88", "#edf2fb"),
}

FIGURE_SPECS = {
    "fig_4_1_apply_flow.png": {"size": (1900, 1200), "renderer": "render_apply_flow"},
    "fig_3_1_architecture.png": {"size": (1800, 1200), "renderer": "render_architecture"},
    "fig_3_2_module_map.png": {"size": (1800, 1180), "renderer": "render_module_map"},
    "fig_3_3_rbac_entity.png": {"size": (1900, 1240), "renderer": "render_rbac_entities"},
    "fig_3_4_inventory_entity.png": {"size": (1900, 1240), "renderer": "render_inventory_entities"},
    "fig_3_5_business_entity.png": {"size": (1900, 1240), "renderer": "render_business_entities"},
    "fig_4_2_transfer_flow.png": {"size": (1900, 1200), "renderer": "render_transfer_flow"},
}

DOCX_REPLACEMENTS = {
    "word/media/image1.png": OUTPUT_DIR / "fig_4_1_apply_flow.png",
    "word/media/image2.png": OUTPUT_DIR / "fig_3_1_architecture.png",
    "word/media/image3.png": OUTPUT_DIR / "fig_3_2_module_map.png",
    "word/media/image4.png": OUTPUT_DIR / "fig_3_3_rbac_entity.png",
    "word/media/image5.png": OUTPUT_DIR / "fig_3_4_inventory_entity.png",
    "word/media/image6.png": OUTPUT_DIR / "fig_3_5_business_entity.png",
    "word/media/image10.png": OUTPUT_DIR / "fig_4_2_transfer_flow.png",
}


def hex_rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[idx: idx + 2], 16) for idx in (0, 2, 4))


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


FONT_KICKER = choose_font(20, bold=True)
FONT_TITLE = choose_font(42, bold=True)
FONT_SUBTITLE = choose_font(24)
FONT_CARD_TITLE = choose_font(34, bold=True)
FONT_CARD_SUBTITLE = choose_font(19, bold=False)
FONT_BODY = choose_font(22, bold=False)
FONT_BODY_SMALL = choose_font(18, bold=False)
FONT_CHIP = choose_font(18, bold=True)
FONT_ENTITY = choose_font(28, bold=True)
FONT_ENTITY_TABLE = choose_font(18, bold=False)
FONT_FIELD = choose_font(17, bold=False)


def new_canvas(size: tuple[int, int], glow: tuple[str, str] = ("blue", "teal")) -> Image.Image:
    width, height = size
    top = hex_rgb(PALETTE["bg_top"])
    bottom = hex_rgb(PALETTE["bg_bottom"])
    image = Image.new("RGBA", size, (255, 255, 255, 255))
    draw = ImageDraw.Draw(image)
    for y in range(height):
        ratio = y / max(height - 1, 1)
        color = tuple(int(top[i] * (1 - ratio) + bottom[i] * ratio) for i in range(3))
        draw.line([(0, y), (width, y)], fill=color)
    for idx, tone in enumerate(glow):
        accent = hex_rgb(PALETTE[tone][0])
        blob = Image.new("RGBA", size, (0, 0, 0, 0))
        blob_draw = ImageDraw.Draw(blob)
        if idx == 0:
            box = (-160, -120, width * 0.55, height * 0.46)
        else:
            box = (width * 0.52, height * 0.42, width + 180, height + 120)
        blob_draw.ellipse(box, fill=accent + (52,))
        blob = blob.filter(ImageFilter.GaussianBlur(80))
        image.alpha_composite(blob)
    return image


def add_shadow(canvas: Image.Image, box: tuple[int, int, int, int], radius: int = 28) -> None:
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_box = (box[0], box[1] + 10, box[2], box[3] + 10)
    shadow_draw.rounded_rectangle(shadow_box, radius=radius, fill=(23, 35, 59, 44))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    canvas.alpha_composite(shadow)


def draw_chip(draw: ImageDraw.ImageDraw, pos: tuple[int, int], label: str, tone: str, font=FONT_CHIP) -> tuple[int, int, int, int]:
    x, y = pos
    fill = hex_rgb(PALETTE[tone][1])
    accent = hex_rgb(PALETTE[tone][0])
    text_box = draw.textbbox((0, 0), label, font=font)
    width = text_box[2] - text_box[0] + 28
    height = text_box[3] - text_box[1] + 16
    box = (x, y, x + width, y + height)
    draw.rounded_rectangle(box, radius=height // 2, fill=fill, outline=accent, width=2)
    draw.text((x + 14, y + 8), label, font=font, fill=accent)
    return box


def wrap_text(draw: ImageDraw.ImageDraw, text: str, font, max_width: int) -> list[str]:
    words = list(text)
    lines: list[str] = []
    current = ""
    for token in words:
        candidate = current + token
        box = draw.textbbox((0, 0), candidate, font=font)
        if current and box[2] - box[0] > max_width:
            lines.append(current)
            current = token
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines


def draw_multiline(draw: ImageDraw.ImageDraw, pos: tuple[int, int], lines: list[str], font, fill, spacing: int = 8) -> int:
    x, y = pos
    cursor = y
    for line in lines:
        draw.text((x, cursor), line, font=font, fill=fill)
        box = draw.textbbox((x, cursor), line, font=font)
        cursor = box[3] + spacing
    return cursor


def draw_poly_arrow(
    draw: ImageDraw.ImageDraw,
    points: list[tuple[int, int]],
    label: str | None = None,
    label_pos: tuple[int, int] | None = None,
    dashed: bool = False,
    width: int = 5,
) -> None:
    color = hex_rgb(PALETTE["line"])
    for start, end in zip(points, points[1:]):
        if dashed:
            draw_dashed_segment(draw, start, end, color, width)
        else:
            draw.line([start, end], fill=color, width=width)
    start, end = points[-2], points[-1]
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    angle = math.atan2(dy, dx)
    head = 16
    wing = 7
    p1 = (end[0] - head * math.cos(angle) + wing * math.sin(angle), end[1] - head * math.sin(angle) - wing * math.cos(angle))
    p2 = (end[0] - head * math.cos(angle) - wing * math.sin(angle), end[1] - head * math.sin(angle) + wing * math.cos(angle))
    draw.polygon([end, p1, p2], fill=color)
    if label and label_pos:
        text_box = draw.textbbox((0, 0), label, font=FONT_BODY_SMALL)
        pad = 10
        box = (
            label_pos[0] - pad,
            label_pos[1] - pad,
            label_pos[0] + (text_box[2] - text_box[0]) + pad,
            label_pos[1] + (text_box[3] - text_box[1]) + pad - 2,
        )
        draw.rounded_rectangle(box, radius=16, fill=(255, 255, 255, 236), outline=hex_rgb(PALETTE["border"]), width=2)
        draw.text(label_pos, label, font=FONT_BODY_SMALL, fill=hex_rgb(PALETTE["muted"]))


def draw_dashed_segment(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], color, width: int = 4) -> None:
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    length = math.hypot(dx, dy)
    if length == 0:
        return
    dash = 16
    gap = 10
    ux = dx / length
    uy = dy / length
    distance = 0
    while distance < length:
        seg_start = (start[0] + ux * distance, start[1] + uy * distance)
        seg_end = (start[0] + ux * min(distance + dash, length), start[1] + uy * min(distance + dash, length))
        draw.line([seg_start, seg_end], fill=color, width=width)
        distance += dash + gap


def draw_card(
    canvas: Image.Image,
    box: tuple[int, int, int, int],
    tone: str,
    title: str,
    kicker: str | None = None,
    subtitle: str | None = None,
    lines: list[str] | None = None,
    chips: list[str] | None = None,
) -> None:
    add_shadow(canvas, box, radius=30)
    draw = ImageDraw.Draw(canvas)
    fill = hex_rgb(PALETTE["white"])
    border = hex_rgb(PALETTE["border"])
    accent = hex_rgb(PALETTE[tone][0])
    accent_fill = hex_rgb(PALETTE[tone][1])
    x1, y1, x2, y2 = box
    draw.rounded_rectangle(box, radius=30, fill=fill, outline=border, width=3)
    draw.rounded_rectangle((x1, y1, x2, y1 + 16), radius=30, fill=accent)
    cursor_y = y1 + 26
    if kicker:
        chip = draw_chip(draw, (x1 + 22, cursor_y), kicker, tone, FONT_KICKER)
        cursor_y = chip[3] + 16
    draw.text((x1 + 24, cursor_y), title, font=FONT_CARD_TITLE, fill=hex_rgb(PALETTE["ink"]))
    cursor_y += 54
    if subtitle:
        draw.text((x1 + 24, cursor_y), subtitle, font=FONT_CARD_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))
        cursor_y += 34
    if chips:
        chip_x = x1 + 24
        chip_y = cursor_y
        for chip in chips:
            chip_box = draw_chip(draw, (chip_x, chip_y), chip, tone)
            chip_x = chip_box[2] + 12
            if chip_x > x2 - 160:
                chip_x = x1 + 24
                chip_y = chip_box[3] + 12
        cursor_y = chip_y + 52
    if lines:
        for line in lines:
            wrapped = wrap_text(draw, line, FONT_BODY, x2 - x1 - 56)
            for idx, wrapped_line in enumerate(wrapped):
                prefix = "• " if idx == 0 else "  "
                draw.text((x1 + 28, cursor_y), prefix + wrapped_line, font=FONT_BODY, fill=hex_rgb(PALETTE["text"]))
                cursor_y += 34
            cursor_y += 6


def draw_entity_card(
    canvas: Image.Image,
    box: tuple[int, int, int, int],
    tone: str,
    title: str,
    table: str,
    fields: list[str],
    footer: str,
) -> None:
    add_shadow(canvas, box, radius=28)
    draw = ImageDraw.Draw(canvas)
    x1, y1, x2, y2 = box
    fill = hex_rgb(PALETTE["white"])
    border = hex_rgb(PALETTE["border"])
    accent = hex_rgb(PALETTE[tone][0])
    accent_fill = hex_rgb(PALETTE[tone][1])
    draw.rounded_rectangle(box, radius=28, fill=fill, outline=border, width=3)
    draw.rounded_rectangle((x1, y1, x2, y1 + 92), radius=28, fill=accent_fill)
    draw.text((x1 + 26, y1 + 22), title, font=FONT_ENTITY, fill=hex_rgb(PALETTE["ink"]))
    draw.text((x1 + 26, y1 + 58), table, font=FONT_ENTITY_TABLE, fill=accent)

    col_count = 2
    gap = 12
    card_w = (x2 - x1 - 52 - gap) // col_count
    chip_h = 42
    start_y = y1 + 118
    for idx, field in enumerate(fields):
        col = idx % col_count
        row = idx // col_count
        fx = x1 + 26 + col * (card_w + gap)
        fy = start_y + row * (chip_h + 12)
        box_field = (fx, fy, fx + card_w, fy + chip_h)
        draw.rounded_rectangle(box_field, radius=18, fill=(248, 251, 255), outline=border, width=2)
        draw.text((fx + 14, fy + 10), field, font=FONT_FIELD, fill=hex_rgb(PALETTE["text"]))
    draw.rounded_rectangle((x1 + 22, y2 - 64, x2 - 22, y2 - 24), radius=18, fill=accent_fill)
    draw.text((x1 + 28, y2 - 53), footer, font=FONT_BODY_SMALL, fill=accent)


def render_architecture(path: Path) -> None:
    canvas = new_canvas((1800, 1200), ("blue", "teal"))
    draw = ImageDraw.Draw(canvas)
    draw_chip(draw, (94, 74), "ARCHITECTURE / 论文架构图", "blue")
    draw.text((94, 138), "系统总体架构围绕前后端分离、业务服务编排与统一数据访问展开。", font=FONT_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))

    draw_card(
        canvas,
        (90, 300, 420, 720),
        "blue",
        "前端表现层",
        kicker="Client",
        subtitle="统一负责页面状态、路由和图表渲染",
        lines=["Vue 3 + Pinia", "Element Plus / ECharts", "Axios / Vue Router / Vite"],
    )
    draw_card(
        canvas,
        (560, 170, 940, 500),
        "teal",
        "接口与控制层",
        kicker="API Layer",
        subtitle="按业务域暴露统一 HTTP 接口",
        lines=["Auth / User / Apply / Transfer", "Inventory / Warning", "Analytics / ApiResponse 返回"],
    )
    draw_card(
        canvas,
        (560, 580, 940, 960),
        "violet",
        "业务服务层",
        kicker="Service",
        subtitle="封装认证、库存、单据与统计规则",
        lines=["AuthService 与 JWT 鉴权", "Inventory / Apply / Transfer 状态流转", "Warning / Analytics 聚合处理"],
    )
    draw_card(
        canvas,
        (1190, 170, 1580, 500),
        "orange",
        "数据访问层",
        kicker="Persistence",
        subtitle="统一负责 Mapper 调用与聚合查询",
        lines=["MyBatis-Plus Mapper", "JdbcTemplate 聚合 SQL", "异常消息与返回码集中处理"],
    )
    draw_card(
        canvas,
        (1190, 590, 1580, 960),
        "red",
        "数据存储层",
        kicker="Storage",
        subtitle="围绕同一套主数据与业务单据建模",
        lines=["MySQL 8 结构化存储", "schema.sql / seed.sql 初始化", "H2 用于本地测试与回归验证"],
    )

    draw_poly_arrow(draw, [(420, 510), (560, 510)], "HTTP / JSON", (460, 468))
    draw_poly_arrow(draw, [(750, 500), (750, 580)], "请求分发", (680, 524))
    draw_poly_arrow(draw, [(940, 760), (1040, 760), (1040, 335), (1190, 335)], "Mapper / SQL 读写", (985, 710))
    draw_poly_arrow(draw, [(1385, 500), (1385, 590)], "结构化存储", (1298, 524))
    draw_poly_arrow(draw, [(750, 960), (750, 1080), (1385, 1080), (1385, 960)], "统一日志、预警与统计结果写回数据库", (860, 1032))

    canvas.convert("RGB").save(path)


def render_module_map(path: Path) -> None:
    canvas = new_canvas((1800, 1180), ("blue", "violet"))
    draw = ImageDraw.Draw(canvas)
    draw_chip(draw, (94, 68), "FUNCTION MAP / 论文模块图", "teal")
    draw.text((94, 130), "功能模块围绕“计划、执行、监督、分析”闭环组织，与代码目录和菜单结构保持一致。", font=FONT_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))

    center = (690, 390, 1110, 760)
    draw_card(
        canvas,
        center,
        "blue",
        "校园物资智能管理系统",
        kicker="Core Platform",
        subtitle="统一认证、统一接口、统一日志与通知",
        chips=["RBAC", "库存台账", "流程闭环", "数据分析"],
        lines=["前端后台、大屏与后端接口围绕同一业务主数据协同。", "功能模块按权限和流程路由到对应业务域。"],
    )

    modules = [
        ((80, 180, 430, 350), "blue", "认证与权限", ["登录 / 刷新", "用户 / 角色 / 部门"]),
        ((725, 170, 1075, 340), "teal", "基础数据", ["校区 / 仓库 / 库位", "分类 / 物资 / 供应商"]),
        ((1370, 180, 1720, 350), "orange", "仓储库存", ["库存总量 / 锁定量", "批次 / 入库 / 出库"]),
        ((80, 500, 430, 670), "violet", "申领审批", ["草稿 / 提交 / 审批", "签收闭环与快速审批"]),
        ((1370, 500, 1720, 670), "teal", "调拨协同", ["跨仓调拨单", "候选仓推荐与执行"]),
        ((80, 840, 430, 1010), "red", "预警与智能分析", ["库存不足 / 临期 / 过期", "补货建议 / 异常领用"]),
        ((725, 850, 1075, 1020), "blue", "统计分析", ["趋势 / 占比 / 排名", "大屏态势与图表卡片"]),
        ((1370, 840, 1720, 1010), "slate", "系统支撑", ["事件 / 通知", "登录日志 / 操作日志"]),
    ]
    for box, tone, title, lines in modules:
        draw_card(canvas, box, tone, title, subtitle="与现有菜单、接口和实体命名保持一致", lines=lines)

    cx1, cy1, cx2, cy2 = center
    anchors = {
        "top_left": (cx1 + 58, cy1 + 32),
        "top": ((cx1 + cx2) // 2, cy1),
        "top_right": (cx2 - 58, cy1 + 32),
        "left": (cx1, (cy1 + cy2) // 2 - 46),
        "right": (cx2, (cy1 + cy2) // 2 - 46),
        "bottom_left": (cx1 + 62, cy2 - 26),
        "bottom": ((cx1 + cx2) // 2, cy2),
        "bottom_right": (cx2 - 62, cy2 - 26),
    }
    link_points = [
        (((80 + 430) // 2, 350), anchors["top_left"]),
        (((725 + 1075) // 2, 340), anchors["top"]),
        (((1370 + 1720) // 2, 350), anchors["top_right"]),
        ((430, (500 + 670) // 2), anchors["left"]),
        ((1370, (500 + 670) // 2), anchors["right"]),
        (((80 + 430) // 2, 840), anchors["bottom_left"]),
        (((725 + 1075) // 2, 850), anchors["bottom"]),
        (((1370 + 1720) // 2, 840), anchors["bottom_right"]),
    ]
    for start, end in link_points:
        draw_poly_arrow(draw, [start, end])

    canvas.convert("RGB").save(path)


def render_rbac_entities(path: Path) -> None:
    canvas = new_canvas((1900, 1240), ("blue", "violet"))
    draw = ImageDraw.Draw(canvas)
    draw_chip(draw, (92, 66), "ENTITY MAP / RBAC 与组织", "blue")
    draw.text((92, 126), "围绕用户、角色、部门、刷新令牌与登录日志构成认证与审计子域。", font=FONT_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))

    boxes = {
        "dept": (70, 210, 500, 560),
        "user": (675, 180, 1230, 610),
        "role": (1400, 210, 1830, 560),
        "token": (240, 760, 840, 1130),
        "log": (1060, 760, 1660, 1130),
    }
    draw_entity_card(canvas, boxes["dept"], "blue", "部门", "sys_dept", ["id", "dept_name", "parent_id", "deleted", "version"], "组织树通过 parent_id 维护上下级结构。")
    draw_entity_card(canvas, boxes["user"], "teal", "用户", "sys_user", ["id", "username", "password", "real_name", "dept_id", "role_id", "status"], "用户同时关联部门与角色，是 RBAC 的核心实体。")
    draw_entity_card(canvas, boxes["role"], "orange", "角色", "sys_role", ["id", "role_code", "role_name", "description"], "角色编码决定菜单范围和关键操作权限。")
    draw_entity_card(canvas, boxes["token"], "violet", "刷新令牌", "auth_refresh_token", ["id", "user_id", "token_id", "token_hash", "expire_at", "revoked"], "刷新令牌承担多端登录与失效控制。")
    draw_entity_card(canvas, boxes["log"], "red", "登录日志", "login_log", ["id", "user_id", "username", "login_ip", "login_status", "login_time"], "登录行为进入审计链路，用于追踪认证结果。")

    user_left = boxes["user"][0]
    user_right = boxes["user"][2]
    user_bottom = boxes["user"][3]
    draw_poly_arrow(draw, [(boxes["dept"][2], 390), (user_left, 390)], "dept_id", (546, 360))
    draw_poly_arrow(draw, [(boxes["role"][0], 390), (user_right, 390)], "role_id", (1295, 360))
    draw_poly_arrow(draw, [((boxes["user"][0] + boxes["user"][2]) // 2 - 120, user_bottom), (540, 760)], "user_id", (700, 680))
    draw_poly_arrow(draw, [((boxes["user"][0] + boxes["user"][2]) // 2 + 120, user_bottom), (1360, 760)], "user_id", (1035, 680))

    canvas.convert("RGB").save(path)


def render_inventory_entities(path: Path) -> None:
    canvas = new_canvas((1900, 1240), ("teal", "orange"))
    draw = ImageDraw.Draw(canvas)
    draw_chip(draw, (92, 66), "ENTITY MAP / 库存与批次", "teal")
    draw.text((92, 126), "物资主数据、仓库台账与批次明细共同支撑 FEFO 和库存预警。", font=FONT_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))

    boxes = {
        "category": (70, 210, 500, 560),
        "material": (665, 180, 1235, 610),
        "warehouse": (1400, 210, 1830, 560),
        "inventory": (250, 760, 900, 1130),
        "batch": (1020, 760, 1670, 1130),
    }
    draw_entity_card(canvas, boxes["category"], "blue", "分类", "material_category", ["id", "category_name", "remark"], "分类定义物资档案的业务归属。")
    draw_entity_card(canvas, boxes["material"], "teal", "物资档案", "material_info", ["id", "material_code", "material_name", "category_id", "safety_stock", "supplier"], "安全库存阈值与供应信息在档案层维护。")
    draw_entity_card(canvas, boxes["warehouse"], "orange", "仓库", "warehouse", ["id", "warehouse_name", "campus", "address", "manager"], "仓库实体对应多校区、多节点仓储布局。")
    draw_entity_card(canvas, boxes["inventory"], "violet", "库存台账", "inventory", ["id", "material_id", "warehouse_id", "current_qty", "locked_qty"], "material_id + warehouse_id 形成唯一库存视图。")
    draw_entity_card(canvas, boxes["batch"], "red", "库存批次", "inventory_batch", ["id", "material_id", "warehouse_id", "batch_no", "in_qty", "remain_qty", "expire_date"], "批次表承载效期和余量，是 FEFO 的计算基础。")

    draw_poly_arrow(draw, [(boxes["category"][2], 390), (boxes["material"][0], 390)], "category_id", (538, 358))
    draw_poly_arrow(draw, [((boxes["material"][0] + boxes["material"][2]) // 2 - 110, boxes["material"][3]), (560, 760)], "material_id", (685, 690))
    draw_poly_arrow(draw, [((boxes["warehouse"][0] + boxes["warehouse"][2]) // 2, boxes["warehouse"][3]), (1310, 760)], "warehouse_id", (1410, 690))
    draw_poly_arrow(draw, [((boxes["material"][0] + boxes["material"][2]) // 2 + 130, boxes["material"][3]), (1345, 760)], "material_id", (1015, 690))
    draw_poly_arrow(draw, [(boxes["inventory"][2], 950), (970, 950), (970, 940), (boxes["batch"][0], 940)], "批次明细展开", (925, 900), dashed=True)

    canvas.convert("RGB").save(path)


def render_business_entities(path: Path) -> None:
    canvas = new_canvas((1900, 1240), ("violet", "red"))
    draw = ImageDraw.Draw(canvas)
    draw_chip(draw, (92, 66), "ENTITY MAP / 业务单据与通知", "violet")
    draw.text((92, 126), "申领、调拨、预警和通知共同构成业务闭环与系统反馈通道。", font=FONT_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))

    boxes = {
        "apply": (40, 210, 500, 560),
        "apply_item": (700, 210, 1180, 560),
        "transfer": (1390, 210, 1850, 560),
        "warning": (220, 760, 860, 1130),
        "notification": (1040, 760, 1680, 1130),
    }
    draw_entity_card(canvas, boxes["apply"], "blue", "申领单", "apply_order", ["id", "dept_id", "applicant_id", "urgency_level", "status", "approver_id"], "状态字段推动草稿、提交、审批和签收闭环。")
    draw_entity_card(canvas, boxes["apply_item"], "teal", "申领明细", "apply_order_item", ["id", "apply_order_id", "material_id", "apply_qty", "actual_qty"], "申领数量与实际发放数量在明细层对齐。")
    draw_entity_card(canvas, boxes["transfer"], "orange", "调拨单", "transfer_order", ["id", "from_warehouse_id", "to_warehouse_id", "status", "applicant_id", "approver_id"], "调拨状态覆盖提交、审批、执行与签收。")
    draw_entity_card(canvas, boxes["warning"], "red", "预警记录", "warning_record", ["id", "warning_type", "material_id", "warehouse_id", "handle_status", "handler_id"], "预警实体承接库存、效期与异常消耗扫描结果。")
    draw_entity_card(canvas, boxes["notification"], "violet", "通知消息", "notification", ["id", "title", "msg_type", "target_user_id", "is_read", "biz_id"], "通知通过 biz_id 回挂申领、调拨和预警业务。")

    draw_poly_arrow(draw, [(boxes["apply"][2], 390), (boxes["apply_item"][0], 390)], "apply_order_id", (542, 358))
    draw_poly_arrow(draw, [((boxes["apply"][0] + boxes["apply"][2]) // 2, boxes["apply"][3]), (480, 760)], "状态变化触发通知", (345, 690), dashed=True)
    draw_poly_arrow(draw, [((boxes["transfer"][0] + boxes["transfer"][2]) // 2, boxes["transfer"][3]), (1375, 760)], "biz_id", (1410, 690), dashed=True)
    draw_poly_arrow(draw, [(boxes["warning"][2], 945), (960, 945), (960, 945), (boxes["notification"][0], 945)], "告警消息投递", (898, 904), dashed=True)

    canvas.convert("RGB").save(path)


def render_apply_flow(path: Path) -> None:
    canvas = new_canvas((1900, 1200), ("blue", "orange"))
    draw = ImageDraw.Draw(canvas)
    draw_chip(draw, (92, 70), "PROCESS FLOW / 申领审批闭环", "orange")
    draw.text((92, 132), "申领单从草稿到签收经历提交、审批、出库与状态回写，紧急单可直接进入 APPROVED。", font=FONT_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))

    boxes = {
        "dept": (80, 360, 360, 710),
        "submit": (470, 330, 820, 720),
        "approver": (930, 170, 1270, 500),
        "warehouse": (930, 650, 1270, 1030),
        "receive": (1460, 360, 1810, 710),
    }
    draw_card(canvas, boxes["dept"], "blue", "部门用户", kicker="Step 01", subtitle="创建申领单并录入业务原因", chips=["原因", "场景", "物资明细"], lines=["完成草稿录入后发起提交。"])
    draw_card(canvas, boxes["submit"], "orange", "提交环节", kicker="Step 02", subtitle="草稿单进入待审批状态", chips=["DRAFT", "SUBMITTED", "fast_track"], lines=["普通单进入审批节点。", "紧急等级 >= 2 且 fast_track=1 时可直接通过。"])
    draw_card(canvas, boxes["approver"], "teal", "审批人", kicker="Step 03", subtitle="执行 approve / reject", chips=["APPROVED", "REJECTED"], lines=["记录 approve_time。", "写入 approve_remark。", "驳回后返回提交环节。"])
    draw_card(canvas, boxes["warehouse"], "violet", "仓库管理员", kicker="Step 04", subtitle="按批次执行出库并回写结果", chips=["FEFO", "actual_qty", "OUTBOUND"], lines=["依据库存与批次生成出库单。", "回写申领明细 actual_qty。"])
    draw_card(canvas, boxes["receive"], "red", "申请人签收", kicker="Step 05", subtitle="确认领用结果并形成闭环", chips=["receive", "RECEIVED"], lines=["签收完成后流程结束。"])

    draw_poly_arrow(draw, [(360, 535), (470, 535)], "创建草稿", (392, 496))
    draw_poly_arrow(draw, [(820, 430), (930, 430)], "普通单审批", (846, 392))
    draw_poly_arrow(draw, [(820, 615), (875, 615), (875, 840), (930, 840)], "审批通过后出库", (786, 704))
    draw_poly_arrow(draw, [(820, 350), (875, 350), (875, 300), (1100, 300), (1100, 650)], "紧急单直接 APPROVED", (900, 254))
    draw_poly_arrow(draw, [(1270, 840), (1360, 840), (1360, 535), (1460, 535)], "出库完成", (1322, 596))
    draw_poly_arrow(draw, [(1100, 500), (1100, 590), (645, 590), (645, 720)], "驳回 / REJECTED", (772, 548), dashed=True)

    canvas.convert("RGB").save(path)


def render_transfer_flow(path: Path) -> None:
    canvas = new_canvas((1900, 1200), ("teal", "violet"))
    draw = ImageDraw.Draw(canvas)
    draw_chip(draw, (92, 70), "PROCESS FLOW / 调拨执行与推荐", "teal")
    draw.text((92, 132), "调拨流程引入候选仓推荐，但最终仍以审批、执行和签收状态流转为准。", font=FONT_SUBTITLE, fill=hex_rgb(PALETTE["muted"]))

    boxes = {
        "warehouse": (90, 360, 410, 720),
        "recommend": (510, 330, 870, 730),
        "approver": (1000, 170, 1330, 500),
        "execute": (1000, 650, 1330, 1040),
        "receive": (1490, 360, 1810, 720),
    }
    draw_card(canvas, boxes["warehouse"], "blue", "仓库管理员", kicker="Step 01", subtitle="发起调拨单并给出目标仓", chips=["目标仓", "物资项", "数量"], lines=["录入调入仓和调拨原因。"])
    draw_card(canvas, boxes["recommend"], "teal", "推荐与提交", kicker="Step 02", subtitle="调用推荐接口筛选候选调出仓", chips=["/api/transfer/recommend", "Dijkstra", "SUBMITTED"], lines=["按目标校区最短距离排序。", "结合库存余量筛选候选调出仓。"])
    draw_card(canvas, boxes["approver"], "orange", "审批人", kicker="Step 03", subtitle="决定 APPROVED 或 REJECTED", chips=["approve", "reject"], lines=["审批意见写入调拨单。"])
    draw_card(canvas, boxes["execute"], "violet", "执行调拨", kicker="Step 04", subtitle="扣减调出仓并补录调入仓", chips=["OUTBOUND", "库存回写"], lines=["执行后更新双仓库存与批次。", "状态推进到待签收。"])
    draw_card(canvas, boxes["receive"], "red", "签收完成", kicker="Step 05", subtitle="receive 后状态变为 RECEIVED", chips=["RECEIVED"], lines=["流程完成后形成调拨闭环。"])

    draw_poly_arrow(draw, [(410, 540), (510, 540)], "生成调拨草稿", (438, 500))
    draw_poly_arrow(draw, [(870, 420), (1000, 420)], "提交审批", (905, 382))
    draw_poly_arrow(draw, [(870, 620), (930, 620), (930, 845), (1000, 845)], "审批通过后执行", (818, 708))
    draw_poly_arrow(draw, [(1165, 500), (1165, 620)], "APPROVED", (1092, 540))
    draw_poly_arrow(draw, [(1165, 500), (1165, 560), (680, 560), (680, 730)], "REJECTED 回退", (820, 522), dashed=True)
    draw_poly_arrow(draw, [(1330, 845), (1410, 845), (1410, 540), (1490, 540)], "签收", (1408, 598))

    canvas.convert("RGB").save(path)


def generate_figures() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for filename, spec in FIGURE_SPECS.items():
        renderer = globals()[spec["renderer"]]
        renderer(OUTPUT_DIR / filename)


def replace_docx_media() -> None:
    shutil.copy2(DOCX_PATH, BACKUP_PATH)
    tmp_path = DOCX_PATH.with_suffix(".tmp.docx")
    with zipfile.ZipFile(DOCX_PATH, "r") as src, zipfile.ZipFile(tmp_path, "w", compression=zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            replacement = DOCX_REPLACEMENTS.get(item.filename)
            if replacement is None:
                dst.writestr(item, src.read(item.filename))
            else:
                dst.writestr(item, replacement.read_bytes())
    tmp_path.replace(DOCX_PATH)


def main() -> None:
    generate_figures()
    replace_docx_media()
    print(DOCX_PATH)
    print(BACKUP_PATH)
    for path in sorted(OUTPUT_DIR.glob("*.png")):
        print(path)


if __name__ == "__main__":
    main()
