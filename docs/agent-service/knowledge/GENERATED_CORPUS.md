---
title: 客服 RAG 语料清单与数据边界
maintainer: Agent Service
updated_at: 2026-07-21
domain: rag_corpus_catalog
visibility: public
status: approved
source_refs:
  - docs/agent-service/knowledge/GENERATED_CORPUS.md
---

# 客服 RAG 语料清单与数据边界

## 1. RAG 应该解决什么问题

本项目的 RAG 主要回答“规则是什么、应该如何处理、何时转人工、回复应该怎么说、哪些操作需要确认”。它不是菜品搜索、库存查询、订单查询或退款计算系统。

### 适合进入 RAG

| 类型 | 示例 | 典型格式 |
| --- | --- | --- |
| 客服规则 | 催单、取消、退款解释、售后流程 | Markdown/HTML/PDF |
| 风控规则 | 敏感内容、隐私越权、食品安全、支付风险 | Markdown/XLSX/PDF |
| 操作 SOP | 建单、升级、人工转接、管理员审批 | Markdown/DOCX/PDF |
| 回复模板 | 共情、拒绝、澄清、转人工、确认卡片 | Markdown/XLSX |
| 规则矩阵 | 风险等级、升级队列、退款例外、审批条件 | XLSX/PDF |
| 字段字典 | 意图、风险、政策版本、审计号、Tool 参数 | XLSX/JSON |
| 版本与来源 | 生效时间、维护人、适用角色、来源引用 | YAML front matter/Excel |

### 不适合进入 RAG

| 数据 | 原因 | 正确来源 |
| --- | --- | --- |
| 菜品名称、价格、规格、库存 | 高频变化且需要精确结果 | 菜品数据库 + `menu_search` Tool |
| 订单当前状态、配送轨迹 | 每分钟变化，不能使用旧 chunk | 订单/配送 Internal API |
| 优惠券实时资格和最终金额 | 需要用户、门店、时间和活动计算 | 优惠业务 Service |
| 退款金额、支付流水 | 涉及资金和权限 | 退款/支付业务 Service |
| 用户地址、手机号、完整对话 | 隐私和数据泄露风险 | 受控业务系统，最小化日志 |
| 未审核草稿、导出数据库、运行日志 | 来源不可信或包含敏感数据 | 审核队列/内部存储 |

## 2. 企业常见文档格式

- Markdown/HTML：适合 Git 版本控制、FAQ、规则和 SOP，当前 loader 优先支持。
- PDF/DOCX：适合正式制度、培训手册、退款矩阵和审批文件；入库时要保留章节、页码和表格标题。
- XLSX：适合敏感内容矩阵、升级矩阵、回复模板、字段字典和来源登记；使用 Unicode `.xlsx`，不依赖 CSV 编码推断。
- JSON：适合 Tool schema、事件结构和脱敏 API 样例；不应把样例当成实时业务事实。
- API/DB：适合实时订单、菜品、库存、优惠资格和退款状态；不进入 RAG，运行时调用。

## 3. 本次生成的真实化语料

### 用户客服 RAG

- `user/customer-service-response-sop.md`：客服响应步骤、回复边界、人工升级条件。
- `user/sensitive-content-handling-policy.md`：敏感内容风险分类与处理动作。
- `user/complaint-classification-and-escalation.md`：投诉分类、服务恢复和升级规则。
- `user/cancellation-and-refund-policy.md`：取消退款政策解释。
- `user/delivery-and-support-faq.md`：配送和客服 FAQ。

### 管理端 RAG

- `admin/knowledge-governance-sop.md`：知识审核、版本、冲突和发布流程。
- `admin/menu-maintenance-sop.md`：管理操作 SOP；不作为菜品主数据。
- `admin/refund-and-order-operation-matrix.md`：退款和订单操作边界。
- `admin/customer-service-and-risk-handbook.pdf`：正式客服培训/风控手册，包含多张规则表。

### 结构化规则工作簿

`data/takeout-rag-business-dataset.xlsx` 使用 Unicode XLSX，包含：

- `Sensitive_Words`：风险分类、触发语义、等级、动作和回复防护；
- `Intent_Routing`：用户/管理端意图到 RAG 或 Tool 的路由；
- `Escalation_Matrix`：客服升级队列和触发条件；
- `Refund_Rules`：退款/补发流程和确认要求；
- `Response_Templates`：回复模板与使用条件；
- `Field_Dictionary`：RAG 字段、Tool 字段和存储边界；
- `Source_Register`：来源格式、维护人、状态和入库方式；
- `README`：RAG 与实时 API 的边界说明。

`data/order-event-samples.json` 只用于事件 schema 和 Tool 测试。旧 CSV 文件仅保留为兼容性样例，不作为主要交付格式，也不应被用户直接用来查询菜品。

## 4. 入库规则

Markdown 文档必须有 `title`、`maintainer`、`updated_at`、`domain`、`visibility`、`status` 和 `source_refs`。规则检索必须先过滤 `visibility`、actor 类型、角色、门店/租户范围和有效时间，再做关键词/向量检索。

当前 `agent-service/app/rag/loader.py` 自动索引审核通过的 Markdown、PDF、XLSX、CSV 和 JSON，并统一保留 `source`、标题/页码/工作表、`domain`、`visibility`、`updated_at`、`chunk_index` 和 `content_hash`。`structured-sources.json` 是非 Markdown 资料的审核白名单。

## 5. 评测重点

- 能否拒绝使用 RAG 猜测实时订单、菜品库存和退款金额；
- 敏感内容是否正确分类、脱敏并升级；
- 退款/补发/管理修改是否先查询、预览、确认，再调用写 Tool；
- 回复是否引用有效政策版本，遇到冲突是否转人工；
- 用户 Agent 和管理 Agent 是否严格使用各自的知识可见范围。
