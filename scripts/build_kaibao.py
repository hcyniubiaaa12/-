# -*- coding: utf-8 -*-
"""基于模板 document.xml 重建开题报告正文（保留封面与评语，替换第 1~7 章）"""
import re, io

SRC = 'unpacked_template/word/document.xml'
OUT = 'unpacked_template/word/document.xml'

with open(SRC, encoding='utf-8') as f:
    lines = f.readlines()

# ---------- 1. 题目行替换（封面表格第一行） ----------
OLD_TITLE = '基于IPv4和IPv6双栈技术的大型企业园区网络规划与设计'
NEW_TITLE = '基于Spring Boot和Vue的智能导诊系统设计与实现'
for i, l in enumerate(lines):
    if OLD_TITLE in l:
        lines[i] = l.replace(OLD_TITLE, NEW_TITLE)
        print('title replaced at line', i + 1)

# ---------- 2. 段落 XML 生成工具 ----------
_pid = [0x0A000000]
def paraId():
    _pid[0] += 1
    return format(_pid[0], '08X')

def esc(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

def _rpr(sz, szcs, eastasia='宋体', hint='default'):
    ea = '' if eastasia is None else ' w:eastAsia="' + eastasia + '"'
    return ('<w:rPr><w:rFonts w:hint="' + hint + '" w:ascii="Times New Roman" '
            'w:hAnsi="Times New Roman"' + ea + ' w:cs="Times New Roman"/>'
            '<w:sz w:val="' + sz + '"/><w:szCs w:val="' + szcs + '"/>'
            '<w:lang w:val="en-US" w:eastAsia="zh-CN"/></w:rPr>')

def p_common(pstyle, ind=None, spacing=None):
    s = '<w:pPr><w:pStyle w:val="' + pstyle + '"/>'
    s += '<w:keepNext w:val="0"/><w:keepLines w:val="0"/><w:pageBreakBefore w:val="0"/>'
    s += '<w:widowControl w:val="0"/><w:kinsoku/><w:wordWrap/><w:overflowPunct/>'
    s += '<w:topLinePunct w:val="0"/><w:autoSpaceDE/><w:autoSpaceDN/><w:bidi w:val="0"/>'
    s += '<w:adjustRightInd/><w:snapToGrid/>'
    if spacing:
        s += '<w:spacing ' + spacing + '/>'
    if ind:
        s += '<w:ind ' + ind + '/>'
    return s

def h1(text):
    rpr = _rpr('28', '22', eastasia=None)
    ppr = p_common('16',
                   ind='w:firstLine="560" w:firstLineChars="200"',
                   spacing='w:before="164" w:beforeLines="50" w:after="164" w:afterLines="50" w:line="300" w:lineRule="auto"')
    ppr += '<w:textAlignment w:val="auto"/><w:outlineLvl w:val="0"/>' + rpr + '</w:pPr>'
    return '<w:p w14:paraId="' + paraId() + '">' + ppr + '<w:r>' + rpr + '<w:t>' + esc(text) + '</w:t></w:r></w:p>'

def h2(text):
    rpr = _rpr('24', '21', eastasia=None)
    ppr = p_common('17',
                   spacing='w:before="164" w:beforeLines="50" w:after="164" w:afterLines="50" w:line="300" w:lineRule="auto"')
    ppr += '<w:textAlignment w:val="auto"/><w:outlineLvl w:val="1"/>' + rpr + '</w:pPr>'
    return '<w:p w14:paraId="' + paraId() + '">' + ppr + '<w:r>' + rpr + '<w:t>' + esc(text) + '</w:t></w:r></w:p>'

def body(text):
    rpr = _rpr('24', '24')
    ppr = p_common('15', spacing='w:line="300" w:lineRule="auto"')
    ppr += '<w:textAlignment w:val="auto"/>' + rpr + '</w:pPr>'
    return '<w:p w14:paraId="' + paraId() + '">' + ppr + '<w:r>' + rpr + '<w:t>' + esc(text) + '</w:t></w:r></w:p>'

def ref(text):
    rpr = _rpr('21', '21')
    ppr = p_common('15',
                   ind='w:left="420" w:hanging="420" w:leftChars="100" w:hangingChars="100"',
                   spacing='w:before="60" w:after="0" w:line="300" w:lineRule="auto"')
    ppr += '<w:textAlignment w:val="auto"/>' + rpr + '</w:pPr>'
    return '<w:p w14:paraId="' + paraId() + '">' + ppr + '<w:r>' + rpr + '<w:t>' + esc(text) + '</w:t></w:r></w:p>'

# ---------- 3. 进度安排表格 ----------
TBL_PR = ('<w:tbl>'
  '<w:tblPr><w:tblStyle w:val="12"/><w:tblW w:w="8795" w:type="dxa"/><w:tblInd w:w="0" w:type="dxa"/>'
  '<w:tblBorders><w:top w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:left w:val="single" w:color="auto" w:sz="4" w:space="0"/>'
  '<w:bottom w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:right w:val="single" w:color="auto" w:sz="4" w:space="0"/>'
  '<w:insideH w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:insideV w:val="single" w:color="auto" w:sz="4" w:space="0"/></w:tblBorders>'
  '<w:tblLayout w:type="fixed"/><w:tblCellMar><w:top w:w="0" w:type="dxa"/><w:left w:w="108" w:type="dxa"/>'
  '<w:bottom w:w="0" w:type="dxa"/><w:right w:w="108" w:type="dxa"/></w:tblCellMar></w:tblPr>'
  '<w:tblGrid><w:gridCol w:w="3107"/><w:gridCol w:w="5688"/></w:tblGrid>')

def cell(text, width, center=False, empty=False):
    rpr = _rpr('24', '24')
    jc = '<w:jc w:val="center"/>' if center else ''
    t = '' if empty else '<w:r>' + rpr + '<w:t>' + esc(text) + '</w:t></w:r>'
    ppr = p_common('15', ind='w:left="0" w:leftChars="0" w:firstLine="0" w:firstLineChars="0"',
                   spacing='w:line="300" w:lineRule="auto"')
    ppr += jc + '<w:textAlignment w:val="auto"/>' + rpr + '</w:pPr>'
    p = '<w:p w14:paraId="' + paraId() + '">' + ppr + t + '</w:p>'
    return '<w:tc><w:tcPr><w:tcW w:w="' + str(width) + '" w:type="dxa"/></w:tcPr>' + p + '</w:tc>'

def tbl_row(c1, c2, hdr=False):
    tblprx = ('<w:tblPrEx><w:tblBorders>'
              '<w:top w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:left w:val="single" w:color="auto" w:sz="4" w:space="0"/>'
              '<w:bottom w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:right w:val="single" w:color="auto" w:sz="4" w:space="0"/>'
              '<w:insideH w:val="single" w:color="auto" w:sz="4" w:space="0"/><w:insideV w:val="single" w:color="auto" w:sz="4" w:space="0"/></w:tblBorders>'
              '<w:tblCellMar><w:top w:w="0" w:type="dxa"/><w:left w:w="108" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/>'
              '<w:right w:w="108" w:type="dxa"/></w:tblCellMar></w:tblPrEx>')
    return ('<w:tr w14:paraId="' + paraId() + '">' + tblprx
            + cell(c1, 3107, center=hdr) + cell(c2, 5688, center=hdr) + '</w:tr>')

def progress_table():
    rows = [tbl_row('起讫时间', '工作内容', hdr=True)]
    data = [
        ('2026年8月~2026年10月', '需求分析、文献调研与总体方案设计'),
        ('2026年10月', '开题答辩'),
        ('2026年10月~2026年12月', '系统设计与开发环境搭建、科室知识库建设'),
        ('2026年12月', '中期检查'),
        ('2026年12月~2027年3月', '系统核心功能开发、反馈闭环实现与系统测试'),
        ('2027年3月', '毕业设计（论文）评审'),
        ('2027年3月~2027年4月', '论文撰写、修改与答辩准备'),
        ('2027年4月', '档案提交'),
    ]
    for a, b in data:
        rows.append(tbl_row(a, b))
    return TBL_PR + ''.join(rows) + '</w:tbl>'

# ---------- 4. 正文内容 ----------
blocks = []

# 1. 选题背景及研究的目的和意义
blocks += [
    ('h1', '1. 选题背景及研究的目的和意义'),
    ('h2', '1.1 选题背景'),
    ('body', '随着医院科室划分越来越精细，患者往往难以准确判断自身症状应归属哪个科室，容易因挂错科室而反复排队、延误就诊，甚至错过最佳就诊时机。传统的分诊方式主要依赖导诊护士的人工经验，无法实现7×24小时全天候服务，就诊高峰期容易排长队，夜间和节假日往往无人值守，且人工分诊质量受个人经验与状态影响，难以保证稳定统一。'),
    ('body', '近年来，大语言模型（LLM）与检索增强生成（RAG）技术快速发展，为医疗导诊提供了新的解决路径。通用大语言模型在医学领域专业知识不足、易产生幻觉，且无法对接具体医院的科室设置与导诊规则，直接用于导诊存在较大风险。因此，将RAG技术与医院自身知识库相结合，构建面向具体医院科室设置的智能导诊系统，具有重要的研究价值和现实需求。'),
    ('h2', '1.2 研究目的'),
    ('body', '本课题旨在设计与实现一个基于Spring Boot和Vue的智能导诊系统，实现以下目的：'),
    ('body', '（1）患者以自然语言描述症状后，系统基于医院知识库进行检索增强，自动推荐合理挂号科室，给出Top-3候选科室、置信度与知识溯源依据；'),
    ('body', '（2）支持多轮对话与针对性追问，在信息不足时主动补全关键信息，提升导诊准确性；'),
    ('body', '（3）设计并实现“导诊—挂号—反馈—优化”闭环机制，以患者实际挂号科室为客观基准，量化评估导诊准确率（Top-1/Top-3），通过人工审核回流修正知识库，使系统准确率可持续提升；'),
    ('body', '（4）提供7×24小时自动化导诊服务，缓解人工导诊压力，改善患者就医体验。'),
    ('h2', '1.3 研究意义'),
    ('body', '（1）现实意义：减少患者挂错科室，缓解导诊台排队压力，填补夜间无人值守空档，提升医院分诊效率与患者满意度；'),
    ('body', '（2）技术意义：探索RAG技术在医疗导诊这一垂直场景的落地方法，解决通用大语言模型在垂直领域的幻觉与专业性不足问题，并通过客观反馈闭环实现系统效果的持续优化，为同类系统提供可复用的架构范式；'),
    ('body', '（3）评估价值：提出以实际挂号科室为基准的客观准确率评估体系，弥补现有导诊系统普遍缺乏量化评估的不足。'),
]

# 2. 国内外在该方向的研究现状及分析
blocks += [
    ('h1', '2. 国内外在该方向的研究现状及分析'),
    ('body', '在传统智能分诊方面，早期研究以基于规则的专家系统为主，如临床决策支持系统（CDSS）中的分诊模块，通过预置规则与决策树实现科室推荐，但规则维护成本高、难以覆盖复杂症状。随着机器学习的发展，不少研究采用逻辑回归、支持向量机、深度学习等分类模型，依据结构化症状文本预测科室，取得了一定效果，但受限于训练数据规模与标注质量，泛化能力有限，且难以给出可解释的推荐依据。'),
    ('body', '在大语言模型医疗问答方面，国内外已涌现一批医疗大模型，如ChatDoctor、Med-PaLM 2等，在医学问答、医学考试等任务上表现出较强能力，但通用医疗大模型知识粒度较粗，难以精确映射到具体医院的科室设置，且直接对话存在知识时效性与幻觉风险。'),
    ('body', '在检索增强生成方面，RAG通过“先检索、后生成”的方式将外部知识库融入大模型生成过程，已广泛应用于垂直领域问答系统。LangChain、LlamaIndex等生态以及RAGAS等评测框架的成熟，降低了RAG系统的构建与评估门槛。'),
    ('body', '综合来看，国内外在医疗大模型与RAG技术方面已有较丰富的研究积累，但在面向具体医院科室设置的智能导诊场景下仍存在三个主要缺口：一是缺乏以客观挂号行为为依据的准确率评估机制；二是缺乏“导诊—反馈—知识库回流”的闭环优化机制；三是缺乏科室级知识库落地实践。本课题正是针对上述缺口展开研究。'),
]

# 3. 主要研究内容
blocks += [
    ('h1', '3. 主要研究内容'),
    ('body', '本课题以“检索增强生成+客观反馈闭环”为主线，主要研究内容如下：'),
    ('body', '（1）基于RAG的智能导诊链路设计：研究查询改写、向量召回、重排序、提示词构造与结构化结果解析等环节，实现“患者自然语言症状描述—科室推荐结论（Top-3+置信度+溯源）”的完整链路；'),
    ('body', '（2）医院知识库建设与管理：研究知识文档的解析、切分、向量化入库流水线，构建覆盖常见科室的蓝本知识库，支持知识文档的异步导入、版本维护与失败重试；'),
    ('body', '（3）导诊反馈闭环与准确率评估：研究三阶段埋点、以实际挂号科室为基准的Top-1/Top-3双口径准确率比对、错误模式聚合、人工审核与知识回流机制，形成可量化的系统优化闭环；'),
    ('body', '（4）双端系统实现：患者端提供自然语言导诊对话、科室推荐与模拟挂号；管理端提供知识库管理、导诊准确率看板与错误审核；'),
    ('body', '（5）实验评估：构建离线评测集，开展“纯大模型与检索增强对比”等消融实验，并结合在线闭环数据验证系统准确率的提升效果。'),
]

# 4. 研究方案及进度安排，预期达到的目标
blocks += [
    ('h1', '4. 研究方案及进度安排，预期达到的目标'),
    ('h2', '4.1 研究方案'),
    ('body', '本课题围绕“检索增强导诊+客观反馈闭环”开展研究，具体方案如下：'),
    ('body', '（1）技术选型：后端采用Spring Boot 3与Java 17，集成Spring Security与JWT实现鉴权，MyBatis-Plus实现持久化；前端采用Vue 3、Vite与Pinia，患者端使用Vant组件库，管理端使用Element Plus与ECharts；数据层采用MySQL存储业务事实数据、PostgreSQL（pgvector）存储知识向量，MinIO存储知识文档；对话模型使用DeepSeek，向量化与重排序采用阿里云text-embedding-v3与gte-rerank模型；'),
    ('body', '（2）RAG链路实现：知识文档经“解析—切分—向量化”异步流水线入库；导诊时先进行查询改写，再经向量召回（Top-20）与重排序（Top-5）获得相关片段，构造带溯源编号的提示词，由大模型流式生成回答与结构化科室推荐结果；'),
    ('body', '（3）反馈闭环实现：通过“推荐结果查看—模拟挂号选择—挂号成功确认”三阶段埋点采集患者行为，以实际挂号科室为基准进行Top-1/Top-3比对，定时对错误样本按症状语义聚类聚合，由管理员审核后回流修正知识库，形成闭环优化；'),
    ('body', '（4）实验评估：构建“症状—科室”离线评测集，开展RAG消融实验（纯大模型与检索增强对比、有无重排序对比、不同切分策略对比），并结合在线闭环数据绘制准确率变化曲线，验证系统有效性。'),
    ('h2', '4.2 进度安排'),
    ('table', progress_table()),
    ('h2', '4.3 预期达到的目标'),
    ('body', '（1）完成一个功能完整、可运行的智能导诊系统，患者端与管理端功能均达到设计要求；'),
    ('body', '（2）建成覆盖20个以上常见科室的蓝本知识库，支撑系统演示与检索；'),
    ('body', '（3）实现完整的RAG导诊链路与知识库管理功能，导诊结论附知识溯源；'),
    ('body', '（4）实现“导诊—挂号—反馈—优化”闭环，形成Top-1/Top-3准确率量化指标与趋势曲线，系统准确率可观测、可持续提升；'),
    ('body', '（5）完成消融实验与性能测试，验证检索增强与反馈闭环的有效性。'),
]

# 5. 为完成课题已具备和所需的条件
blocks += [
    ('h1', '5. 为完成课题已具备和所需的条件'),
    ('body', '（1）已掌握Java、Spring Boot、Vue、MySQL以及数据结构与算法等专业知识，具备独立开发前后端系统的能力；'),
    ('body', '（2）已了解RAG、向量检索、大语言模型提示工程等核心技术原理，并对DeepSeek、pgvector等工具进行了初步调研；'),
    ('body', '（3）本机开发环境可满足开发需求，可通过Docker快速部署MySQL、PostgreSQL（pgvector）、MinIO等中间件；'),
    ('body', '（4）所使用的大模型API（DeepSeek、阿里云百炼）提供免费额度，开发与演示成本可控；'),
    ('body', '（5）医院科室设置、分诊规则等知识资料可通过公开渠道获取，并可自主构建科室蓝本知识库语料；'),
    ('body', '（6）校图书馆及知网等数据库文献资源丰富，可满足文献调研需要。'),
]

# 6. 预计研究过程中可能遇到的困难和问题，以及解决的措施
blocks += [
    ('h1', '6. 预计研究过程中可能遇到的困难和问题，以及解决的措施'),
    ('h2', '6.1 困难及问题'),
    ('body', '（1）RAG检索效果调优难度大：知识切分策略、检索参数、提示词构造等环节均会影响导诊准确率，需要大量实验调参；'),
    ('body', '（2）大模型存在幻觉与科室名不规范问题：模型可能输出不存在的科室或置信度偏高，结构化解析可能失败；'),
    ('body', '（3）导诊准确率的客观评估依赖真实的挂号行为数据，本地开发环境缺乏真实患者数据；'),
    ('body', '（4）科室蓝本知识库语料构建工作量大，质量直接影响检索与推荐效果；'),
    ('body', '（5）多轮对话中患者信息不足时，追问策略不当会损害用户体验。'),
    ('h2', '6.2 解决措施'),
    ('body', '（1）采用消融实验方式对各环节进行量化对比，逐步调优切分与检索参数，对检索失败样本通过反馈闭环定位并持续改进；'),
    ('body', '（2）通过结构化输出约束与后置校验、科室名白名单映射、重试机制降低幻觉影响，导诊结论附知识溯源以增强可解释性；'),
    ('body', '（3）将模拟挂号设为导诊流程的必经环节，以患者实际选择科室作为客观基准采集数据，构建在线反馈数据集；'),
    ('body', '（4）提前规划并分批构建20个以上科室的蓝本语料，优先覆盖常见症状，保证演示与实验效果；'),
    ('body', '（5）采用“规则硬门槛+大模型合并判定”的追问策略，信息不足时针对性追问，并设置追问轮数上限，避免过度打扰。'),
]

# 7. 主要参考文献
refs = [
    '[1] GAO Y, XIONG Y, GAO X, et al. Retrieval-augmented generation for large language models: a survey[EB/OL]. (2023-12-18)[2026-09-03]. https://arxiv.org/abs/2312.10997.',
    '[2] SINGHAL K, AZIZI S, TU T, et al. Large language models encode clinical knowledge[J]. Nature, 2023, 620(7972): 172-180.',
    '[3] LI Y, LI Z, ZHANG K, et al. ChatDoctor: a medical chat model fine-tuned on a large language model meta-ai (LLaMA) using medical domain knowledge[J]. Cureus, 2023, 15(6): e40895.',
    '[4] HUANG L, YU W, MA W, et al. A survey on hallucination in large language models: principles, taxonomy, challenges, and open questions[J]. ACM Transactions on Information Systems, 2025, 43(2): 1-55.',
    '[5] WANG M, XU X, YUE Q, et al. A comprehensive survey and experimental comparison of graph-based approximate nearest neighbor search[J]. Proceedings of the VLDB Endowment, 2021, 14(11): 1964-1978.',
    '[6] ES S, JAMES J, ESPINOSA-ANKE L, et al. RAGAS: automated evaluation of retrieval augmented generation[C]//Proceedings of the 18th Conference of the European Chapter of the Association for Computational Linguistics: System Demonstrations. Stroudsburg: Association for Computational Linguistics, 2024: 150-158.',
    '[7] SINGHAL K, TU T, GOTTWEIS J, et al. Toward expert-level medical question answering with large language models[J]. Nature Medicine, 2025, 31(3): 943-950.',
    '[8] WALLS C. Spring实战:第6版[M]. 张卫滨, 吴国浩, 译. 北京: 人民邮电出版社, 2022.',
    '[9] 王珊, 萨师煊. 数据库系统概论:第5版[M]. 北京: 高等教育出版社, 2014.',
    '[10] 梁灏. Vue.js实战[M]. 北京: 清华大学出版社, 2017.',
    '[11] PGVECTOR. pgvector: open-source vector similarity search for postgres[EB/OL]. [2026-09-03]. https://github.com/pgvector/pgvector.',
    '[12] 罗杰, 艾山木, 辜锐, 等. 大语言模型在预检分诊运用对成人预检分诊正确率的影响[J]. 中华急诊医学杂志, 2025, 34(7): 987-991.',
]
blocks.append(('h1', '7. 主要参考文献'))
for r in refs:
    blocks.append(('ref', r))

# ---------- 5. 组装 ----------
def render(b):
    t, x = b
    if t == 'h1': return h1(x)
    if t == 'h2': return h2(x)
    if t == 'body': return body(x)
    if t == 'ref': return ref(x)
    if t == 'table': return x
    raise ValueError(t)

new_body = '\n          '.join(render(b) for b in blocks)

# 保留：index 0..1416（原1~1417行）；替换 index 1417..6347；保留 index 6348..end
head = ''.join(lines[0:1417])
tail = ''.join(lines[6348:])
doc = head + new_body + '\n        ' + tail

with io.open(OUT, 'w', encoding='utf-8') as f:
    f.write(doc)

print('done. new body blocks =', len(blocks))
