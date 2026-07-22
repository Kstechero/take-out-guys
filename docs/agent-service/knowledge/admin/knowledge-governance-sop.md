---
title: RAG 知识库治理与发布 SOP
maintainer: Agent Platform 组
updated_at: 2026-07-21
domain: knowledge_governance
visibility: admin
status: approved
source_refs:
  - docs/agent-service/knowledge/admin/knowledge-governance-sop.md
---

# RAG 知识库治理与发布 SOP

## 进入 RAG 的条件

文档必须有业务负责人、适用角色、有效日期、版本号、来源、审核状态和冲突处理方式。规则类文档进入 RAG 前必须完成内容审核、敏感信息检查和最小检索测试。

## 不进入 RAG 的内容

订单、库存、实时菜单、实时优惠资格、支付流水、退款金额、用户地址、手机号、内部 token、数据库导出、未脱敏客服对话和未审核草稿。

## 发布流程

```text
起草 -> 业务负责人审核 -> 风险/隐私检查 -> 生成版本号
  -> 小规模检索评测 -> 发布 approved 版本 -> 观察引用与投诉
  -> 发现冲突时下线旧版本或明确优先级
```

## 冲突优先级

实时业务 Tool > 当前有效且角色可见的正式政策 > 历史政策 > FAQ 示例。若两个有效政策冲突，Agent 不自行选择，应返回不确定并转人工或请求管理员处理。

## 文档格式

Markdown/HTML 适合版本控制和段落切分；PDF/DOCX 适合正式制度、培训手册和审批矩阵；XLSX 适合结构化规则矩阵和字段字典；JSON 适合 schema 与接口样例。格式不同不改变权限过滤和审核要求。
