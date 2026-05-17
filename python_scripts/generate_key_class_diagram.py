from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from xml.sax.saxutils import escape
import subprocess


ROOT = Path(__file__).resolve().parents[1]
FIGURE_DIR = ROOT / "output" / "doc" / "figures"
SVG_DIR = FIGURE_DIR / "drawio"
SVG_PATH = SVG_DIR / "fig_4_2_key_classes.svg"
PNG_PATH = FIGURE_DIR / "fig_4_2_key_classes.png"
PLAYWRIGHT_MODULE = ROOT / "frontend" / "node_modules" / "playwright"

CANVAS_WIDTH = 2360
CANVAS_HEIGHT = 1360
HEADER_HEIGHT = 42
LINE_HEIGHT = 24
BODY_PADDING = 14


@dataclass(frozen=True)
class Box:
    box_id: str
    title: str
    x: int
    y: int
    w: int
    fields: tuple[str, ...] = ()
    methods: tuple[str, ...] = ()
    kind: str = "class"

    @property
    def h(self) -> int:
        body_rows = len(self.fields) + len(self.methods)
        if body_rows == 0:
            return HEADER_HEIGHT + 34
        separator_count = 1 if self.fields and self.methods else 0
        return HEADER_HEIGHT + BODY_PADDING + body_rows * LINE_HEIGHT + BODY_PADDING + separator_count * 10


@dataclass(frozen=True)
class Arrow:
    start: tuple[float, float]
    points: tuple[tuple[float, float], ...]
    end: tuple[float, float]
    label: str = ""
    dashed: bool = True

    def polyline(self) -> list[tuple[float, float]]:
        return [self.start, *self.points, self.end]


def anchor(box: Box, side: str, ratio: float = 0.5) -> tuple[float, float]:
    top = box.y
    left = box.x
    right = box.x + box.w
    bottom = box.y + box.h
    if side == "top":
        return left + box.w * ratio, top
    if side == "bottom":
        return left + box.w * ratio, bottom
    if side == "left":
        return left, top + box.h * ratio
    if side == "right":
        return right, top + box.h * ratio
    raise ValueError(f"Unknown side: {side}")


def arrow_midpoint(points: list[tuple[float, float]]) -> tuple[float, float]:
    if len(points) == 2:
        return ((points[0][0] + points[1][0]) / 2, (points[0][1] + points[1][1]) / 2)
    lengths: list[float] = []
    total = 0.0
    for start, end in zip(points, points[1:]):
        length = ((end[0] - start[0]) ** 2 + (end[1] - start[1]) ** 2) ** 0.5
        lengths.append(length)
        total += length
    target = total / 2
    walked = 0.0
    for index, length in enumerate(lengths):
        if walked + length >= target:
            start, end = points[index], points[index + 1]
            ratio = 0 if length == 0 else (target - walked) / length
            return (
                start[0] + (end[0] - start[0]) * ratio,
                start[1] + (end[1] - start[1]) * ratio,
            )
        walked += length
    return points[-1]


def label_box_size(text: str) -> tuple[int, int]:
    return max(48, len(text) * 18), 28


def box_svg(box: Box) -> str:
    header_fill = "#5B9BD5"
    border = "#7B92B0"
    body_fill = "#FFFFFF"
    lines: list[str] = [
        f'<rect x="{box.x}" y="{box.y}" width="{box.w}" height="{box.h}" rx="4" fill="{body_fill}" stroke="{border}" stroke-width="2"/>',
        f'<rect x="{box.x}" y="{box.y}" width="{box.w}" height="{HEADER_HEIGHT}" rx="4" fill="{header_fill}" stroke="{header_fill}" stroke-width="2"/>',
        f'<text x="{box.x + box.w / 2}" y="{box.y + 27}" text-anchor="middle" font-family="Microsoft YaHei, SimSun, Arial, sans-serif" font-size="22" font-weight="700" fill="#FFFFFF">{escape(box.title)}</text>',
    ]

    if not box.fields and not box.methods:
        return "\n".join(lines)

    content_y = box.y + HEADER_HEIGHT + BODY_PADDING
    if box.fields:
        field_bottom = content_y + len(box.fields) * LINE_HEIGHT
        lines.append(
            f'<line x1="{box.x}" y1="{HEADER_HEIGHT + box.y}" x2="{box.x + box.w}" y2="{HEADER_HEIGHT + box.y}" stroke="#C8D2E0" stroke-width="1.2"/>'
        )
        for index, field in enumerate(box.fields):
            y = content_y + index * LINE_HEIGHT
            lines.append(
                f'<text x="{box.x + 14}" y="{y + 18}" font-family="Consolas, Microsoft YaHei, SimSun, monospace" font-size="16" fill="#3F5877">{escape(field)}</text>'
            )
        if box.methods:
            divider_y = field_bottom + 8
            lines.append(
                f'<line x1="{box.x}" y1="{divider_y}" x2="{box.x + box.w}" y2="{divider_y}" stroke="#C8D2E0" stroke-width="1.2"/>'
            )
            content_y = divider_y + 10

    for index, method in enumerate(box.methods):
        y = content_y + index * LINE_HEIGHT
        lines.append(
            f'<text x="{box.x + 14}" y="{y + 18}" font-family="Consolas, Microsoft YaHei, SimSun, monospace" font-size="16" fill="#3F5877">{escape(method)}</text>'
        )
    return "\n".join(lines)


def arrow_svg(arrow: Arrow) -> str:
    points = arrow.polyline()
    points_attr = " ".join(f"{x},{y}" for x, y in points)
    dash = ' stroke-dasharray="8 6"' if arrow.dashed else ""
    pieces = [
        f'<polyline points="{points_attr}" fill="none" stroke="#6B84A3" stroke-width="2.2"{dash} marker-end="url(#depArrow)"/>'
    ]
    if arrow.label:
        cx, cy = arrow_midpoint(points)
        width, height = label_box_size(arrow.label)
        pieces.append(
            f'<rect x="{cx - width / 2}" y="{cy - 18}" width="{width}" height="{height}" rx="4" fill="#FFFFFF" stroke="none"/>'
        )
        pieces.append(
            f'<text x="{cx}" y="{cy + 1}" text-anchor="middle" font-family="Microsoft YaHei, SimSun, Arial, sans-serif" font-size="15" fill="#5B6F89">{escape(arrow.label)}</text>'
        )
    return "\n".join(pieces)


def svg_to_png(svg_path: Path, png_path: Path) -> None:
    html = (
        "<!doctype html><html><head><meta charset='utf-8'>"
        "<style>html,body{margin:0;background:#fff;}svg{display:block;margin:0 auto;}</style>"
        f"</head><body>{svg_path.read_text(encoding='utf-8')}</body></html>"
    )
    html_path = svg_path.with_suffix(".preview.html")
    html_path.write_text(html, encoding="utf-8")
    node_script = f"""
const {{ chromium }} = require({json.dumps(str(PLAYWRIGHT_MODULE))});
(async () => {{
  const browser = await chromium.launch();
  const page = await browser.newPage({{
    viewport: {{ width: {CANVAS_WIDTH + 40}, height: {min(CANVAS_HEIGHT + 40, 1400)} }},
    deviceScaleFactor: 2
  }});
  await page.goto({json.dumps(html_path.as_uri())}, {{ waitUntil: 'load' }});
  await page.screenshot({{ path: {json.dumps(str(png_path))}, fullPage: true }});
  await browser.close();
}})().catch(err => {{
  console.error(err);
  process.exit(1);
}});
"""
    try:
        subprocess.run(["node", "-"], input=node_script, text=True, cwd=str(ROOT), check=True)
    finally:
        if html_path.exists():
            html_path.unlink()


def build_boxes() -> dict[str, Box]:
    return {
        "auth": Box(
            "auth",
            "AuthService",
            80,
            72,
            500,
            fields=(
                "- jwtTokenProvider: JwtTokenProvider",
                "- loginLogService: LoginLogService",
                "- authRefreshTokenMapper: Mapper",
            ),
            methods=(
                "+ login(LoginRequest): LoginResponse",
                "+ refresh(String): LoginResponse",
                "+ me(): Map<String,Object>",
            ),
        ),
        "warning": Box(
            "warning",
            "WarningService",
            1120,
            72,
            470,
            fields=(
                "- warningRecordMapper: WarningRecordMapper",
                "- inventoryMapper: InventoryMapper",
                "- operationLogService: OperationLogService",
            ),
            methods=(
                "+ list(PageQuery,...): PageResult",
                "+ scan(): void",
                "+ handle(Long,String): void",
            ),
        ),
        "ai": Box(
            "ai",
            "AiAnalysisService",
            1660,
            72,
            560,
            fields=(
                "- promptTemplateService: PromptTemplateService",
                "- deepSeekLlmClient: DeepSeekLlmClient",
                "- warningRecordMapper: WarningRecordMapper",
            ),
            methods=(
                "+ analyzeWarning(Long): AiTaskResponse",
                "- buildWarningSnapshot(...): Map",
                "- buildFallbackResult(...): Result",
            ),
        ),
        "apply": Box(
            "apply",
            "ApplyService",
            80,
            410,
            460,
            fields=(
                "- inventoryService: InventoryService",
                "- operationLogService: OperationLogService",
            ),
            methods=(
                "+ create(ApplyCreateRequest): Map",
                "+ submit(Long): void",
                "+ approve(Long,String): void",
            ),
        ),
        "inventory": Box(
            "inventory",
            "InventoryService",
            600,
            392,
            520,
            fields=(
                "- inventoryMapper: InventoryMapper",
                "- batchMapper: InventoryBatchMapper",
                "- operationLogService: OperationLogService",
            ),
            methods=(
                "+ list(PageQuery,...): PageResult",
                "+ stockIn(StockInRequest): void",
                "+ stockOut(StockOutRequest): void",
            ),
        ),
        "transfer": Box(
            "transfer",
            "TransferService",
            1180,
            410,
            520,
            fields=(
                "- inventoryMapper: InventoryMapper",
                "- batchMapper: InventoryBatchMapper",
                "- operationLogService: OperationLogService",
            ),
            methods=(
                "+ create(TransferCreateRequest): Map",
                "+ execute(Long): void",
                "+ recommendTransfer(...): List",
            ),
        ),
        "analytics": Box(
            "analytics",
            "AnalyticsService",
            1760,
            430,
            420,
            fields=("- jdbcTemplate: JdbcTemplate",),
            methods=(
                "+ overview(): Map",
                "+ inboundOutboundTrend(): List",
                "+ expiryStats(): List",
            ),
        ),
        "smart": Box(
            "smart",
            "SmartService",
            1760,
            700,
            420,
            fields=("- jdbcTemplate: JdbcTemplate",),
            methods=(
                "+ forecast(Long,int): Map",
                "+ replenishmentSuggestions(Integer): List",
            ),
        ),
        "jwt": Box("jwt", "JwtTokenProvider", 670, 102, 260),
        "login_log": Box("login_log", "LoginLogService", 670, 214, 260),
        "refresh_mapper": Box("refresh_mapper", "AuthRefreshTokenMapper", 670, 326, 300),
        "inventory_mapper": Box("inventory_mapper", "InventoryMapper", 650, 760, 260),
        "batch_mapper": Box("batch_mapper", "InventoryBatchMapper", 960, 760, 320),
        "warning_mapper": Box("warning_mapper", "WarningRecordMapper", 1320, 760, 320),
        "operation_log": Box("operation_log", "OperationLogService", 930, 1090, 320),
        "prompt": Box("prompt", "PromptTemplateService", 1600, 1020, 340),
        "deepseek": Box("deepseek", "DeepSeekLlmClient", 1980, 1020, 260),
        "jdbc": Box("jdbc", "JdbcTemplate", 1820, 1210, 300),
    }


def build_arrows(boxes: dict[str, Box]) -> list[Arrow]:
    return [
        Arrow(anchor(boxes["auth"], "right", 0.28), (), anchor(boxes["jwt"], "left", 0.5), "签发/解析"),
        Arrow(anchor(boxes["auth"], "right", 0.56), ((620, 250),), anchor(boxes["login_log"], "left", 0.5), "登录留痕"),
        Arrow(anchor(boxes["auth"], "right", 0.82), ((620, 370),), anchor(boxes["refresh_mapper"], "left", 0.5), "令牌轮换"),
        Arrow(anchor(boxes["apply"], "right", 0.38), (), anchor(boxes["inventory"], "left", 0.38), "锁定/释放"),
        Arrow(anchor(boxes["inventory"], "bottom", 0.26), ((735, 730),), anchor(boxes["inventory_mapper"], "top", 0.5), "汇总库存"),
        Arrow(anchor(boxes["inventory"], "bottom", 0.64), ((1040, 730),), anchor(boxes["batch_mapper"], "top", 0.45), "批次扣减"),
        Arrow(anchor(boxes["inventory"], "bottom", 0.9), ((1090, 940), (1090, 1090)), anchor(boxes["operation_log"], "top", 0.35), "日志"),
        Arrow(anchor(boxes["apply"], "bottom", 0.62), ((365, 910), (365, 1140), (930, 1140)), anchor(boxes["operation_log"], "left", 0.5), ""),
        Arrow(anchor(boxes["transfer"], "bottom", 0.24), ((1305, 730),), anchor(boxes["batch_mapper"], "top", 0.86), "镜像批次"),
        Arrow(anchor(boxes["transfer"], "bottom", 0.48), ((1435, 730), (1435, 1090)), anchor(boxes["operation_log"], "top", 0.82), "执行留痕"),
        Arrow(anchor(boxes["warning"], "bottom", 0.55), ((1378, 730),), anchor(boxes["warning_mapper"], "top", 0.45), "生成预警"),
        Arrow(anchor(boxes["warning"], "bottom", 0.78), ((1500, 940), (1500, 1090)), anchor(boxes["operation_log"], "top", 1.0), ""),
        Arrow(anchor(boxes["ai"], "bottom", 0.24), ((1795, 980),), anchor(boxes["prompt"], "top", 0.45), "组装提示词"),
        Arrow(anchor(boxes["ai"], "bottom", 0.56), ((1995, 980),), anchor(boxes["deepseek"], "top", 0.45), "调用"),
        Arrow(anchor(boxes["ai"], "bottom", 0.84), ((2125, 780), (1600, 780)), anchor(boxes["warning_mapper"], "right", 0.45), "读取上下文"),
        Arrow(anchor(boxes["analytics"], "bottom", 0.55), ((1990, 1180),), anchor(boxes["jdbc"], "top", 0.4), "聚合"),
        Arrow(anchor(boxes["smart"], "bottom", 0.5), (), anchor(boxes["jdbc"], "top", 0.72), "预测"),
    ]


def build_svg() -> str:
    boxes = build_boxes()
    arrows = build_arrows(boxes)
    parts: list[str] = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{CANVAS_WIDTH}" height="{CANVAS_HEIGHT}" viewBox="0 0 {CANVAS_WIDTH} {CANVAS_HEIGHT}">',
        "<defs>",
        '<marker id="depArrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">',
        '<path d="M 0 0 L 10 5 L 0 10" fill="#FFFFFF" stroke="#6B84A3" stroke-width="1.6"/>',
        "</marker>",
        "</defs>",
        f'<rect width="{CANVAS_WIDTH}" height="{CANVAS_HEIGHT}" fill="#FFFFFF"/>',
        '<text x="80" y="34" font-family="Microsoft YaHei, SimSun, Arial, sans-serif" font-size="24" font-weight="700" fill="#4B617D">认证与安全</text>',
        '<text x="80" y="374" font-family="Microsoft YaHei, SimSun, Arial, sans-serif" font-size="24" font-weight="700" fill="#4B617D">库存与流程协同</text>',
        '<text x="1120" y="34" font-family="Microsoft YaHei, SimSun, Arial, sans-serif" font-size="24" font-weight="700" fill="#4B617D">预警、统计与智能分析</text>',
    ]
    for box in boxes.values():
        parts.append(box_svg(box))
    for arrow in arrows:
        parts.append(arrow_svg(arrow))
    parts.append(
        '<text x="80" y="1318" font-family="Microsoft YaHei, SimSun, Arial, sans-serif" font-size="18" fill="#6A7C93">注：虚线箭头表示关键类之间的主要依赖或调用关系，类名与方法签名均来源于项目实际代码。</text>'
    )
    parts.append("</svg>")
    return "\n".join(parts)


def main() -> None:
    FIGURE_DIR.mkdir(parents=True, exist_ok=True)
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    svg_text = build_svg()
    SVG_PATH.write_text(svg_text, encoding="utf-8")
    svg_to_png(SVG_PATH, PNG_PATH)
    print(SVG_PATH)
    print(PNG_PATH)


if __name__ == "__main__":
    main()
