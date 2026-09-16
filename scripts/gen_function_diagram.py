# -*- coding: utf-8 -*-
"""生成「智能导诊系统功能结构图」—— 横向两边布局（患者端左 / 管理端右），紧凑排版
设计目标：插入 Word 宽度 15.5cm 时，叶子字号 ≈ 7.5pt（六号），模块名 ≈ 7pt"""

MARGIN = 24          # 左右外边距
SIDE_GAP = 46        # 左右两侧间距
LEAF_W, LEAF_GAP = 24, 2
PITCH = LEAF_W + LEAF_GAP
LEAF_FONT, VSTEP, LEAF_H = 15, 18, 168
MOD_FONT, MOD_H, MOD_GAP = 14, 28, 12
L2_W, L2_H, L2_FONT = 150, 30, 15
ROOT_W, ROOT_H, ROOT_FONT = 200, 40, 20

C = dict(
    root="#FFFFFF",
    pat_fill="#FFFFFF", pat_bd="#222222", pat_txt="#111111",
    adm_fill="#FFFFFF", adm_bd="#222222", adm_txt="#111111",
    mod_txt="#111111",
    leaf_pat_fill="#FFFFFF", leaf_pat_bd="#555555",
    leaf_adm_fill="#FFFFFF", leaf_adm_bd="#555555",
    leaf_txt="#222222",
    conn="#444444",
)

PAT = dict(label="患者端功能模块", fill=C["pat_fill"], bd=C["pat_bd"], txt=C["pat_txt"],
           is_pat=True,
           mods=[("用户管理", ["注册登录", "个人信息"]),
                 ("智能导诊对话", ["症状描述导入", "SSE流式回复", "多轮对话追问", "智能推荐候选科室"]),
                 ("挂号", ["科室列表选择", "就诊登记"]),
                 ("行为埋点", ["推荐结果触达", "就诊意向选择", "就诊结果回填"])])
ADM = dict(label="管理端功能模块", fill=C["adm_fill"], bd=C["adm_bd"], txt=C["adm_txt"],
           is_pat=False,
           mods=[("知识库管理", ["文档异步入库", "症状科室映射", "版本与变更日志"]),
                 ("反馈审核", ["待审队列", "证据快照还原", "根因归因标注", "审批通过或驳回"]),
                 ("统计看板", ["导诊量统计", "准确率趋势", "错误分布分析", "知识盲区榜"]),
                 ("用户管理", ["用户封禁与解封", "敏感词库管理"]),
                 ("系统配置", ["LLM配置", "检索参数配置", "聚合阈值配置"])])


def span_of(n):
    return n * PITCH - LEAF_GAP


def measure(side):
    widths = [max(span_of(len(leaves)), len(label) * MOD_FONT + 14) for label, leaves in side['mods']]
    return widths, sum(widths) + MOD_GAP * (len(widths) - 1)


pat_w, PAT_TOTAL = measure(PAT)
adm_w, ADM_TOTAL = measure(ADM)
W = MARGIN * 2 + PAT_TOTAL + SIDE_GAP + ADM_TOTAL

# 纵向
ROOT_Y = 14
BUS1_Y = ROOT_Y + ROOT_H + 18
L2_Y = BUS1_Y + 12
BUS2_Y = L2_Y + L2_H + 16
MOD_Y = BUS2_Y + 16
BUS3_Y = MOD_Y + MOD_H + 16
LEAF_Y = BUS3_Y + 14
# 底部留白 62：无头浏览器视口比窗口矮约 58px，靠这段留白保证内容不被裁掉
H = LEAF_Y + LEAF_H + 62

parts = []
add = parts.append


def rect(x, y, w, h, fill, stroke, sw=1.0, rx=0):
    add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" stroke="{stroke}" '
        f'stroke-width="{sw}" rx="{rx}"/>')


def text(x, y, s, size, fill, bold=False, anchor="middle"):
    b = ' font-weight="bold"' if bold else ''
    add(f'<text x="{x}" y="{y}" font-size="{size}" fill="{fill}" text-anchor="{anchor}"{b}>{s}</text>')


def vtext(cx, y0, s, size, fill, step):
    inner = "".join(f'<tspan x="{cx}" y="{y0 + i * step}">{ch}</tspan>' for i, ch in enumerate(s))
    add(f'<text font-size="{size}" fill="{fill}" text-anchor="middle">{inner}</text>')


def line(x1, y1, x2, y2, sw=1.0):
    add(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{C["conn"]}" stroke-width="{sw}"/>')


def leaf_centers(c, n):
    return [c + round((i - (n - 1) / 2) * PITCH) for i in range(n)]


add(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" '
    f'font-family="Microsoft YaHei, SimSun, sans-serif">')
add(f'<rect x="0" y="0" width="{W}" height="{H}" fill="white"/>')

# ---------------- 布局两侧 ----------------
sides = []
x = MARGIN
for side, widths, total in ((PAT, pat_w, PAT_TOTAL), (ADM, adm_w, ADM_TOTAL)):
    mods = []
    mx = x
    for (label, leaves), w in zip(side['mods'], widths):
        c = mx + w / 2
        mods.append(dict(label=label, leaves=leaves, w=w, c=c, lcs=leaf_centers(c, len(leaves))))
        mx += w + MOD_GAP
    sides.append(dict(side=side, mods=mods, x0=x, x1=x + total, c=x + total / 2))
    x += total + SIDE_GAP

# ---------------- 根节点 ----------------
ROOT_CX = round((sides[0]['c'] + sides[1]['c']) / 2)
root_y = ROOT_Y
rect(ROOT_CX - ROOT_W / 2, root_y, ROOT_W, ROOT_H, C["root"], "#111111", sw=1.4, rx=4)
text(ROOT_CX, root_y + 27, "智能导诊系统", ROOT_FONT, "#111111", bold=True)

# 根 → 两端
line(ROOT_CX, root_y + ROOT_H, ROOT_CX, BUS1_Y)
line(sides[0]['c'], BUS1_Y, sides[1]['c'], BUS1_Y)
for s in sides:
    line(s['c'], BUS1_Y, s['c'], L2_Y)

# ---------------- 两侧子树 ----------------
for s in sides:
    side = s['side']
    rect(s['c'] - L2_W / 2, L2_Y, L2_W, L2_H, side['fill'], side['bd'], sw=1.2, rx=3)
    text(s['c'], L2_Y + 21, side['label'], L2_FONT, side['txt'], bold=True)
    line(s['c'], L2_Y + L2_H, s['c'], BUS2_Y)
    line(s['mods'][0]['c'], BUS2_Y, s['mods'][-1]['c'], BUS2_Y)
    for m in s['mods']:
        line(m['c'], BUS2_Y, m['c'], MOD_Y)
        bd = C["pat_bd"] if side['is_pat'] else C["adm_bd"]
        rect(m['c'] - m['w'] / 2, MOD_Y, m['w'], MOD_H, "white", bd, rx=2)
        text(m['c'], MOD_Y + 19, m['label'], MOD_FONT, C["mod_txt"], bold=True)
        line(m['c'], MOD_Y + MOD_H, m['c'], BUS3_Y)
        line(min(m['lcs']), BUS3_Y, max(m['lcs']), BUS3_Y)
        fill = C["leaf_pat_fill"] if side['is_pat'] else C["leaf_adm_fill"]
        lbd = C["leaf_pat_bd"] if side['is_pat'] else C["leaf_adm_bd"]
        for lx, lt in zip(m['lcs'], m['leaves']):
            line(lx, BUS3_Y, lx, LEAF_Y)
            rect(lx - LEAF_W / 2, LEAF_Y, LEAF_W, LEAF_H, fill, lbd)
            vtext(lx, LEAF_Y + 16, lt, LEAF_FONT, C["leaf_txt"], VSTEP)

add('</svg>')
svg = "\n".join(parts)
html = ("<!DOCTYPE html>\n<html lang=\"zh-CN\"><head><meta charset=\"utf-8\">\n"
        "<style>html,body{margin:0;padding:0;background:#fff;}</style>\n</head><body>\n"
        + svg + "\n</body></html>")

with open(r"C:\Users\陈增\Desktop\设计\系统功能结构图.html", "w", encoding="utf-8") as f:
    f.write(html)

# 插入宽度 15.5cm 时的等效字号
CM = 15.5
for name, px in (("叶子", LEAF_FONT), ("模块名", MOD_FONT), ("侧标题", L2_FONT), ("根", ROOT_FONT)):
    print(f'{name} 等效字号 = {px / W * CM * 28.3465:.2f} pt')
print(f'画布 {W}x{H}  显示 {CM}cm x {H / W * CM:.2f}cm')
