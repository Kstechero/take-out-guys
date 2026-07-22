# Agent RAG 知识库说明

本目录保存经过审核、适合解释业务规则的知识。它不是订单、菜单、库存、支付或退款金额数据库。

## 推荐边界

- 菜品名称、价格、规格、库存：使用数据库和 `menu_search` Tool。
- 订单状态、配送轨迹、退款状态：使用订单/配送/退款 Internal API。
- 优惠券实时资格和最终金额：使用优惠业务 Service。
- 客服 SOP、敏感内容、升级条件、退款解释、审批规则：进入 RAG。
- 用户隐私、完整对话、支付凭证、token 和未审核草稿：禁止进入 RAG。

## 文档格式

Markdown/HTML 适合 FAQ、规则和 SOP；PDF/DOCX 适合正式制度、培训手册和审批文件；Unicode XLSX 适合敏感词矩阵、升级矩阵、回复模板、字段字典和来源登记；JSON 适合 Tool schema 和事件样例。

## 当前语料

规则文档必须带 YAML front matter：

```yaml
title: 客服响应与工单处理 SOP
maintainer: 客服运营组
updated_at: 2026-07-21
domain: customer_service
visibility: user
status: approved
source_refs:
  - docs/agent-service/knowledge/user/customer-service-response-sop.md
```

Markdown 使用 front matter 审核；PDF、XLSX、CSV、JSON 使用 `structured-sources.json` 审核白名单。只有 `status: approved` 的来源会被索引，检索前必须按 actor、角色、门店/租户和可见性过滤。

推荐直接打开：

- `data/takeout-rag-business-dataset.xlsx`：规则型 Unicode Excel 工作簿，避免 CSV 中文编码问题；
- `admin/customer-service-and-risk-handbook.pdf`：客服培训和风控手册；
- `GENERATED_CORPUS.md`：语料类型、实时 API 边界和评测重点。

旧 CSV 兼容样例已转换为 UTF-8 with BOM，Excel/OfficePLUS 应能正常显示中文。CSV 不保存列宽，因此日期或长文本在窄列中可能显示为 `######`；需要正常列宽、筛选和表格展示时请打开 `.xlsx`。这些 CSV 仅用于兼容性测试，不作为菜品查询或 RAG 的事实来源。
