# RAG 知识源与入库规则

## 可入库知识

首批白名单：

- `docs/agent.md` 中已确认的业务能力和维护事实；
- `docs/SKY_TAKE_OUT_FULL_PROJECT_README.md` 的项目说明；
- 人工维护的营业、配送、退款、优惠券、客服 SOP、菜品与套餐规则文档；
- 经管理员审核后放入 `docs/agent-service/knowledge/` 的 Markdown、HTML、PDF、XLSX、CSV 或 JSON 结构化资料。

当前首批审核知识清单维护在 `docs/agent-service/knowledge/README.md` 和 `structured-sources.json`。Loader 支持带 `status: approved` front matter 的 Markdown，以及审核清单中的 PDF、XLSX、CSV、JSON；所有格式统一转换为 `KnowledgeChunk`，未审核或明确排除的结构化资料不得入库。

知识文档必须标明标题、维护人、更新时间、适用范围和可见级别。业务实时数据（订单状态、库存、店铺状态、优惠券资格）必须通过 Tool 查询，不能依赖向量库。

Markdown 文档统一使用 YAML front matter，字段为 `title`、`maintainer`、`updated_at`、`domain`、`visibility`、`status` 和 `source_refs`。`source_refs` 用于审核追溯，正式引用仍展示文档标题、相对路径和更新时间。

## 禁止入库内容

- 用户订单明细、地址、电话、支付信息、会话原文；
- 数据库备份、日志、密钥、token、`application-dev.yml`；
- `docs/private/`、个人简历、未审核的聊天记录；
- 可直接执行的管理操作口令或敏感词库全文。

## 文档元数据

每个 chunk 至少包含：

```json
{
  "source": "docs/agent-service/knowledge/user/coupon-policy.md",
  "title": "优惠券使用规则",
  "domain": "coupon",
  "visibility": "user",
  "updated_at": "2026-07-10",
  "content_hash": "sha256",
  "chunk_index": 3
}
```

PDF 保留页码/表名，XLSX/CSV 保留工作表、表头和行来源，JSON 保留 schema 或样例路径。统一要求 `status`、`visibility`、actor/角色、门店/租户和更新时间可过滤。

`visibility` 仅使用 `public`、`user`、`admin`。检索时必须用 actor 类型和角色做元数据过滤，不能只在回答生成后再过滤。

## 切分、索引与检索

- 优先按 Markdown 标题和段落切分，保留标题层级；当前本地索引按约 320 字符上限合并相邻标题段落。
- 全量入库命令生成清单与 hash；增量入库只处理内容 hash 变化的源文件。
- 源文件被删除、降级可见范围或更新时，同步删除或更新对应向量。
- 检索顺序：可见性/域过滤 → 向量 Top-K → 可选重排 → 置信度阈值判断。
- 回答引用展示标题、路径和更新时间；置信度不足时说明“未在当前知识库找到确认依据”。

当前默认实现使用 2048 维本地字符 n-gram 哈希向量、余弦相似度和二元组覆盖率轻量重排，索引持久化到 `agent-service/data/rag-index.json`。该后端无需外部 embedding 服务，接口已与 Loader、Store 和 Retriever 解耦，后续可替换为生产 embedding 与向量数据库。

执行入库：

```powershell
cd agent-service
python -m scripts.ingest_knowledge
```

命令输出文档数、chunk 数、新生成/复用/删除向量数和知识库 hash。服务启动时 `RAG_AUTO_INDEX=true` 会执行同一增量同步；初始化失败时不注册知识 Tool，其他实时 Tool 继续服务。

## 评测集要求

- 首个切片至少 10 条 RAG eval，覆盖营业、配送、优惠券、售后、菜品规则和无依据拒答；
- 与 `06-test-cases.md` 合计至少 30 条 pytest，覆盖知识权限、旧文档过滤和索引回滚；
- 每条记录包括 query、actor、预期 source、允许/禁止答案要点和最低相似度阈值；
- 文档更新后必须重新执行检索评测，确认旧内容不会被继续引用。
