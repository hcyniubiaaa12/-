# -*- coding: utf-8 -*-
"""拷贝一份「开题报告-修订版」→「-含图」，并把已插入的系统功能结构图换成高清版 + 加图题"""
import re
import shutil
import sys
import zipfile

sys.stdout.reconfigure(encoding='utf-8')

SRC = r'C:\Users\陈增\Desktop\设计\智能导诊系统-开题报告-修订版.docx'
DST = r'C:\Users\陈增\Desktop\设计\智能导诊系统-开题报告-修订版-含图.docx'
PNG = r'C:\Users\陈增\Desktop\设计\系统功能结构图.png'

CAPTION = '图 1  系统功能结构图'
# 目标显示宽度：单元格可用宽 15.94cm - 左右内边距 ≈ 15.5cm
TARGET_CX = int(15.5 * 360000)          # EMU

# 读取新 PNG 实际像素尺寸，按真实宽高比设置高度
_png = open(PNG, 'rb').read()
import struct
NATIVE_W, NATIVE_H = struct.unpack('>II', _png[16:24])
TARGET_CY = round(TARGET_CX * NATIVE_H / NATIVE_W)
print(f'新图 {NATIVE_W}x{NATIVE_H}px → 显示 {TARGET_CX/360000:.2f}cm x {TARGET_CY/360000:.2f}cm')

src_zip = zipfile.ZipFile(SRC)
doc = src_zip.read('word/document.xml').decode('utf-8')

# ---- 1. 定位图表 drawing（rId8 → media/image2.png）----
pos = doc.find('r:embed="rId8"')
assert pos > 0, '未找到 rId8 图片'
d_start = doc.rfind('<w:drawing>', 0, pos)
d_end = doc.find('</w:drawing>', pos) + len('</w:drawing>')
block = doc[d_start:d_end]
print('drawing 块内 cx/cy 对：', re.findall(r'cx="(\d+)" cy="(\d+)"', block))

old_cx = int(re.search(r'<wp:extent cx="(\d+)"', block).group(1))
factor = TARGET_CX / old_cx


def scaled(m):
    """同时按比例缩放 wp:extent 与 a:ext，并强制高度等于 TARGET_CY（避免两次舍入误差）"""
    cx = TARGET_CX
    cy = TARGET_CY
    return f'cx="{cx}" cy="{cy}"'


new_block = re.sub(r'cx="(\d+)" cy="(\d+)"', scaled, block)
doc = doc[:d_start] + new_block + doc[d_end:]
print(f'缩放系数 {factor:.4f} → 新 wp:extent cx={TARGET_CX} ({TARGET_CX/360000:.2f}cm)')

# ---- 2. 在图片所在段落之后插入图题段落 ----
pos = doc.find('r:embed="rId8"')
d_start = doc.rfind('<w:drawing>', 0, pos)
d_end = doc.find('</w:drawing>', pos) + len('</w:drawing>')
p_start = max(doc.rfind('<w:p ', 0, d_start), doc.rfind('<w:p>', 0, d_start))
p_end = doc.find('</w:p>', d_end) + len('</w:p>')

RPR = ('<w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体"/>'
       '<w:sz w:val="21"/><w:szCs w:val="21"/></w:rPr>')
caption_p = (
    '<w:p><w:pPr>'
    '<w:ind w:firstLineChars="0" w:firstLine="0"/>'
    '<w:jc w:val="center"/>'
    '<w:spacing w:before="60" w:after="180" w:line="320" w:lineRule="auto"/>'
    f'{RPR}'
    '</w:pPr>'
    f'<w:r>{RPR}<w:t>{CAPTION}</w:t></w:r>'
    '</w:p>'
)
doc = doc[:p_end] + caption_p + doc[p_end:]
print('图题已插入：', CAPTION)

# ---- 3. 写新文件（替换高清 PNG + 新 document.xml）----
new_png = open(PNG, 'rb').read()
with zipfile.ZipFile(DST, 'w', zipfile.ZIP_DEFLATED) as out:
    for item in src_zip.infolist():
        data = src_zip.read(item.filename)
        if item.filename == 'word/document.xml':
            data = doc.encode('utf-8')
        elif item.filename == 'word/media/image2.png':
            data = new_png
            print(f'替换 {item.filename}: {len(src_zip.read(item.filename))} -> {len(data)} bytes')
        out.writestr(item, data)
src_zip.close()
print('已生成：', DST)
