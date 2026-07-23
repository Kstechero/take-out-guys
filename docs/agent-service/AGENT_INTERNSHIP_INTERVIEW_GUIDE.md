# Agent 开发实习面试与项目学习手册

> 更新时间：2026-07-23
>
> 适用方向：大模型应用开发、AI Agent 开发、RAG 应用工程、LLM 后端实习
>
> 使用建议：先遮住“回答要点”口述 1～2 分钟，再结合本项目补充证据；不要背框架 API，要讲清问题、选择、约束、验证和改进。

## 阅读地图

本文分为两部分：

- 第一部分是 Agent 开发实习常见面试题及回答要点。
- 第二部分介绍 Takeout Guys Agent 项目、当前完成度、讲解模板和项目追问题库。

## 第一部分：Agent 开发实习面试题

### 一、岗位画像与准备优先级

近期 Agent 实习岗位反复出现的能力要求包括：Python 与后端基础、LangChain/LangGraph 等编排框架、Tool Calling、RAG、向量检索与 Rerank、Prompt、记忆、评测、部署和工程稳定性。部分岗位还会问 MCP、多智能体、PyTorch、模型微调与推理部署。传音的 Agent 实习岗位明确提到感知、规划、记忆、执行、Tool Calling、RAG、Rerank 和 ReAct；百度岗位同时要求计算机基础、RAG、Prompt、Agent 编排和代码工程能力。[传音岗位](https://www.nowcoder.com/jobs/detail/444729) · [百度岗位](https://talent.baidu.com/jobs/detail/INTERN/e3cec5b8-b7a3-4946-99fc-b292b749cd53)

建议按以下顺序准备：

1. 能把自己的项目讲清楚，并能解释每个关键设计的取舍。
2. Python、网络、数据库、并发、测试等软件工程基础。
3. LLM、Prompt、Tool Calling、Agent loop 与 LangGraph。
4. RAG 的召回、排序、生成、评测和故障定位。
5. 安全、确认、幂等、可观测性、成本与部署。
6. MCP、多 Agent、微调等扩展知识。

---

### 二、LLM 与 Prompt 基础

#### 1. Transformer 和 Attention 的核心思想是什么？

回答要点：

- Self-Attention 让每个 token 根据 Query 与其他 token 的 Key 相似度，对 Value 加权聚合。
- 常见形式为 `softmax(QK^T / sqrt(d_k))V`；除以 `sqrt(d_k)` 是为了控制点积方差，避免 softmax 过度饱和。
- Multi-Head Attention 让模型在不同表示子空间学习不同关系。
- 还应能说出位置编码、残差连接、归一化、前馈网络和因果掩码的作用。

#### 2. temperature、top-p 和 max tokens 分别影响什么？

回答要点：

- temperature 调整 logits 分布的尖锐程度，低温更稳定，高温更多样。
- top-p 只在累计概率达到阈值的候选集合中采样。
- max tokens 限制最大输出长度，直接影响截断、时延和成本。
- Tool Calling、分类、抽取等任务通常偏低温；创意生成可适度提高，但仍需用评测决定。

#### 3. Prompt 应该怎样设计才更稳定？

回答要点：

- 明确角色、任务、输入边界、允许能力、禁止事项和完成条件。
- 用结构化输出 schema 代替只靠自然语言约束。
- Few-shot 适合提供边界样例和格式样例，但会占用上下文。
- 把实时事实交给工具，把稳定规则放在提示或审核知识库中。
- Prompt 不是安全边界；权限、参数和业务前置条件必须由代码验证。

#### 4. Prompt Engineering、RAG 和微调怎样选择？

回答要点：

- Prompt：成本低、迭代快，适合改变任务说明、格式与少量行为。
- RAG：适合外部知识频繁变化、需要来源引用、不能重新训练的场景。
- 微调：适合稳定地学习风格、格式、领域行为或提升特定任务能力，不适合充当实时数据库。
- 常见顺序是先建立基线与评测，再做 Prompt/RAG，最后根据误差类型决定是否微调。

#### 5. 什么是幻觉？如何降低？

回答要点：

- 幻觉是模型生成了看似合理但缺少事实依据或与上下文冲突的内容。
- 可通过权威检索、引用、结构化工具结果、阈值拒答、事实一致性校验和人工确认降低。
- “降低”不等于“消除”；必须用数据集测 groundedness、引用正确率和拒答质量。

---

### 三、Agent 与 Tool Calling

#### 6. Agent 与普通聊天机器人、固定工作流有什么区别？

回答要点：

- 普通聊天通常是输入到输出的一次生成。
- 固定工作流由代码预先确定步骤和分支。
- Agent 由模型根据目标和当前观察动态决定下一步行动、选择工具、读取结果，并循环到完成或移交人工。
- OpenAI 将 Agent 的基础概括为模型、工具、指令；不是所有接入 LLM 的应用都需要 Agent，确定性流程应优先使用普通代码。[OpenAI Agent 指南](https://openai.com/business/guides-and-resources/a-practical-guide-to-building-ai-agents/)

#### 7. ReAct 是什么？

回答要点：

- ReAct 将推理与行动交替：根据任务形成下一步决策，执行工具，观察结果，再继续决策。
- 优点是能使用外部事实并对失败进行调整。
- 风险是循环失控、错误累积、时延与成本增加，因此要限制步数、超时、工具范围并设置终止条件。

#### 8. Tool Calling 的完整链路是什么？

回答要点：

1. 向模型提供工具名、描述和 JSON Schema。
2. 模型返回工具名及结构化参数。
3. 应用校验参数、权限和业务前置条件。
4. 应用执行真实函数或 API。
5. 将工具结果作为 Tool Message 交回模型。
6. 模型生成最终回答或继续选择工具。

#### 9. 怎样设计一个高质量 Tool？

回答要点：

- 一个工具只负责清晰、稳定、可测试的业务能力，名称和描述避免歧义。
- 输入用严格 schema：类型、范围、枚举、必填项和跨字段校验。
- 输出保持机器可读，错误使用稳定错误码，不把异常堆栈交给模型。
- 区分只读与写入；写入要考虑授权、确认、幂等、审计和补偿。
- 工具数量过多会降低选择准确率，可按角色、场景动态注册或先路由再暴露子集。

#### 10. 模型生成的工具参数能直接执行吗？

不能。模型输出、用户输入和 RAG 文档都属于不可信输入。必须经过 schema、权限、资源归属、业务状态和风险校验。高风险写操作还应暂停并请求人工确认。

#### 11. 如何避免 Agent 无限循环或重复调用？

回答要点：

- 设置最大步数、总超时、单工具超时和预算。
- 明确定义完成、拒答、降级和人工接管条件。
- 记录已执行动作或状态摘要，识别相同参数的重复调用。
- 写操作用幂等键；自动重试只用于安全的只读或明确幂等操作。

#### 12. 单 Agent 和多 Agent 如何选择？

回答要点：

- 优先从单 Agent 加清晰工具开始，易调试、低时延、低成本。
- 当上下文、权限或专业能力明显分离，或单 Agent 工具过多导致路由下降时，再拆分。
- 常见模式包括 manager 调用专业 Agent、handoff、并行专家和 evaluator-optimizer。
- 多 Agent 会增加状态同步、归因、可观测性和故障恢复复杂度，不是“越多越智能”。

#### 13. LangChain 与 LangGraph 的区别是什么？

回答要点：

- LangChain 提供模型、Prompt、Tool、Retriever 等高层组件与通用 Agent 接口。
- LangGraph 是面向长期、状态化 Agent 的低层编排与运行时，强调节点、边、状态、持久化、流式和 Human-in-the-loop。
- 需要明确状态机、持久恢复、复杂分支和可控中断时适合 LangGraph。[LangChain 产品概念](https://docs.langchain.com/oss/python/concepts/products)

#### 14. LangGraph 的 state、node、edge、conditional edge 是什么？

回答要点：

- state 是图执行过程共享的数据模型，如消息、用户上下文、确认状态和检索结果。
- node 是读取 state 并返回更新的计算单元。
- edge 定义固定转移；conditional edge 根据路由函数选择下一节点。
- reducer 决定并发或多次更新如何合并，消息列表常使用追加型 reducer。

#### 15. interrupt、checkpointer 和 thread_id 怎样配合？

回答要点：

- `interrupt()` 在运行时暂停图并把可序列化请求暴露给调用方。
- checkpointer 在步骤间保存图状态，使服务重启或跨请求后仍可恢复。
- `thread_id` 是定位同一执行线程状态的游标；恢复时使用同一 ID 和 `Command(resume=...)`。
- 中断前发生的副作用必须幂等，因为节点恢复时可能从头执行。[LangGraph Interrupts](https://docs.langchain.com/oss/python/langgraph/interrupts) · [Persistence](https://docs.langchain.com/oss/python/langgraph/persistence)

#### 16. 短期记忆、长期记忆和业务数据有什么区别？

回答要点：

- 短期记忆是 thread 内对话与执行状态，通常由 checkpointer 保存。
- 长期记忆是跨 thread 的用户偏好或可复用信息，需要明确写入、更新和遗忘策略。
- 订单、库存、价格等是权威业务数据，应实时从业务服务查询，不能把 Agent memory 当事实源。
- 长对话还要处理上下文窗口：裁剪、摘要、按需检索和隐私保留期限。

#### 17. MCP 是什么，和普通 Tool API 有什么区别？

回答要点：

- MCP 是 Host—Client—Server 架构的标准协议，数据层基于 JSON-RPC，传输可用 stdio 或 Streamable HTTP。
- Server 可暴露 Tools、Resources 和 Prompts；Client 负责发现能力和路由调用。
- 普通函数调用解决“模型怎样调用本应用定义的函数”，MCP 更关注不同 AI Host 与外部能力之间的标准化发现和互操作。
- MCP 并不会自动解决权限与安全，Host 仍需审批、最小权限、来源信任和输出校验。[MCP 架构](https://modelcontextprotocol.io/docs/learn/architecture)

---

### 四、RAG 与检索

#### 18. 请描述 RAG 的完整流程。

回答要点：

离线侧：文档解析与清洗 → 分块 → 元数据与权限标注 → embedding → 索引。

在线侧：查询理解/改写 → 召回 → 权限过滤 → Rerank → 上下文构造 → 生成 → 引用与拒答 → 评测和反馈。

#### 19. Chunk 怎样切？越小越好吗？

回答要点：

- 小 chunk 定位精确但语义可能不完整；大 chunk 上下文完整但噪声多且成本高。
- overlap 可缓解边界信息丢失，但会产生重复召回。
- 应优先按标题、段落、表格、代码函数等语义结构切分，再用长度兜底。
- 需要在真实问答集上联合调节 chunk size、overlap、top-k 和 rerank，而不是只看经验值。

#### 20. Embedding 是什么？余弦相似度、点积和欧氏距离有何区别？

回答要点：

- Embedding 将文本映射为稠密向量，使语义相近内容在向量空间更接近。
- 余弦相似度关注方向；归一化后与点积排序等价。
- 点积同时受方向和模长影响；欧氏距离衡量绝对距离。
- 必须与所选 embedding 模型和索引配置一致，不能随意混用度量。

#### 21. Dense、Sparse、Hybrid Retrieval 有什么区别？

回答要点：

- Dense 向量检索擅长语义相似。
- Sparse/BM25 擅长关键词、专有名词、编号和精确匹配。
- Hybrid 合并两路结果，常用加权或 RRF，再做 Rerank。
- LangChain 文档也提醒：单一 dense embedding 对产品编码、实体名等精确查询不一定好，需在自有数据上评测。[Embedding 选型](https://docs.langchain.com/oss/python/integrations/embeddings)

#### 22. Rerank 为什么有用？

回答要点：

- 首阶段召回要快，通常只做近似相似度，目标是高 Recall。
- Reranker 对 query—document 做更精细的相关性判断，提高前几条精度。
- 常见做法是向量/BM25 召回较多候选，再由 cross-encoder 或模型排序，最后取少量上下文。
- 代价是额外时延和费用，需要比较无 rerank 与有 rerank 的端到端收益。

#### 23. RAG 没答对，怎样定位是检索问题还是生成问题？

回答要点：

1. 查看正确证据是否进入 top-k；没进入是召回问题。
2. 正确证据进入但排序靠后或被截断，是排序/上下文构造问题。
3. 正确证据已清晰进入 Prompt 但回答错，是生成或指令遵循问题。
4. 同时检查语料质量、权限过滤、query rewrite、引用映射和评测标注。

#### 24. 如何评测 RAG？

回答要点：

- 检索：Recall@k、Precision@k、MRR、nDCG。
- 生成：答案正确性、faithfulness/groundedness、引用正确性、完整性和拒答质量。
- 系统：P50/P95 时延、token/费用、索引新鲜度、错误率。
- 指标解释：Recall@k 看正确证据是否在前 k；MRR 更重视第一个正确结果排得多靠前。

#### 25. 什么是 Agentic RAG？

回答要点：

- 普通 RAG 往往每次都按固定流程检索；Agentic RAG 允许模型判断是否检索、选择数据源、改写问题、检查证据并决定是否重试。
- 灵活性更强，但分支、时延和不可预测性也更高。
- LangGraph 官方示例将 retriever 做成 Tool，让 Agent 决定何时使用。[LangGraph Agentic RAG](https://docs.langchain.com/oss/python/langgraph/agentic-rag)

---

### 五、评测、可靠性与安全

#### 26. Agent 应该怎样评测？

回答要点：

- 不只比较最终文本，还要评测执行轨迹：工具选择、参数、顺序、重试、终止和副作用。
- 确定性行为用规则断言；开放回答可用人工或 LLM-as-a-Judge，但应校准 Judge 偏差。
- 数据集至少覆盖正常、边界、权限、工具错误、注入、超时、无依据和写操作确认。
- 离线评测用于发布门禁，线上监控与失败样本回流形成闭环。[LangChain Agent Evals](https://docs.langchain.com/oss/python/langchain/test/evals) · [Anthropic Agent Evals](https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents)

#### 27. 为什么只测最终答案不够？

同一个正确答案可能通过错误或危险路径得到，例如查询了越权数据、重复执行写操作或碰巧猜对。Agent 是“过程产生副作用”的系统，因此轨迹与状态变化和结果同等重要。

#### 28. LLM-as-a-Judge 有什么风险？

回答要点：

- 可能存在位置偏差、长度偏差、自我偏好、提示敏感和不稳定。
- 应使用明确 rubric、参考答案、交换候选顺序、多次评审或多 Judge，并抽样人工复核。
- 权限、幂等、schema 等确定性条件不要交给 LLM Judge。

#### 29. Prompt Injection 是什么？RAG 能解决吗？

回答要点：

- 直接注入来自用户输入；间接注入藏在网页、文件、邮件或检索内容中。
- RAG 和微调都不能根治 Prompt Injection。
- 防御要分层：区分指令与数据、最小工具权限、工具参数校验、敏感工具确认、内容来源信任、输出检查、审计和沙箱。
- OWASP 将 Prompt Injection、敏感信息泄露、不当输出处理和过度代理权列为 LLM 应用核心风险。[OWASP LLM Top 10](https://genai.owasp.org/llm-top-10/) · [Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/)

#### 30. 为什么“在 system prompt 里禁止越权”不够？

Prompt 是概率性约束，可能被注入、上下文冲突或模型错误绕过。真正权限边界必须落在 API、身份认证、资源归属校验、字段白名单和数据库事务中。

#### 31. Human-in-the-loop 应用在哪些操作？

回答要点：

- 不可逆、高金额、影响他人、权限敏感或模型低置信的操作。
- 确认卡应展示主体、目标、旧值、新值、影响范围、风险、有效期和审计理由。
- 确认后仍需服务端二次鉴权和状态校验，不能把“用户点了确认”当作绕过业务规则的许可。

#### 32. 什么是幂等？为什么写 Tool 特别需要？

回答要点：

- 同一逻辑请求执行一次或多次，产生的最终业务效果相同。
- 网络超时可能让调用方不知道服务端是否已成功，盲目重试会重复扣款、领券或创建资源。
- 可用 Idempotency-Key、唯一约束、状态机和结果缓存；写调用默认不自动重试。

#### 33. Agent 服务需要哪些可观测信息？

回答要点：

- request/trace/thread/actor 标识，节点与工具轨迹，耗时、错误码、重试、模型与版本。
- token、费用、首 token 时延、端到端 P95、工具 P95、检索命中与确认转化。
- Prompt、日志和 Tool 结果要脱敏，避免记录密钥、JWT、完整地址和个人数据。
- 线上要能从异常回答追溯到模型、提示、检索证据、工具和代码版本。

#### 34. 如何控制 Agent 延迟和成本？

回答要点：

- 先分解端到端耗时：模型、检索、Rerank、Tool、排队和网络。
- 缩短上下文、缓存稳定结果、并行无依赖只读调用、减少无意义 Agent 步数。
- 按难度路由模型，在评测基线上逐步用小模型替换简单任务。
- 不能只优化 token 单价，还要看成功任务成本和失败重试成本。

---

### 六、Python 与后端工程高频题

#### 35. Python async/await 适合什么场景？

回答要点：

- 适合大量 I/O 等待，如模型 API、HTTP Tool、数据库和流式连接。
- 不会自动加速 CPU 密集任务；CPU 密集需进程池、原生库或独立任务服务。
- async 函数内调用阻塞 I/O 会阻塞事件循环，应使用异步客户端或线程池隔离。

#### 36. 进程、线程、协程有什么区别？

回答要点：

- 进程内存隔离、开销较高，适合 CPU 并行与故障隔离。
- 线程共享内存，切换成本较低；CPython GIL 限制纯 Python CPU 并行，但 I/O 可受益。
- 协程由事件循环协作调度，适合高并发 I/O，但必须避免阻塞调用。

#### 37. FastAPI 的依赖注入有什么价值？

回答要点：

- 路由声明自己需要的认证、配置、客户端和服务，框架负责构造与复用。
- 便于统一认证、资源生命周期管理和测试替换。
- 测试中可用 `app.dependency_overrides` 替换真实模型或业务客户端。[FastAPI 依赖测试](https://fastapi.tiangolo.com/advanced/testing-dependencies/)

#### 38. Pydantic 在 Agent 项目中的作用是什么？

回答要点：

- 校验 API、Tool 和跨服务合同，生成 JSON Schema/OpenAPI。
- 对范围、枚举和跨字段条件做确定性检查，阻止错误参数进入业务层。
- 输入与输出模型应分开，避免泄露内部或敏感字段。

#### 39. SSE 和 WebSocket 如何选择？

回答要点：

- SSE 是基于 HTTP 的服务端到客户端单向事件流，浏览器支持自动重连，实现简单，适合 token 流式输出。
- WebSocket 是全双工长连接，适合双方高频实时通信，但连接管理更复杂。
- Agent 聊天常用普通 POST 提交消息、SSE 返回 delta/tool/citation/done 事件。

#### 40. HTTP 超时和重试怎样设计？

回答要点：

- 区分连接、读取和总超时。
- 只读请求可对短暂网络错误做有限次数指数退避并加 jitter。
- 非幂等写请求不要盲目重试，应依赖幂等键与状态查询。
- 设置熔断、限流和降级，错误需要可观测且对用户可理解。

#### 41. Redis、PostgreSQL 和向量数据库在 Agent 系统中分别做什么？

回答要点：

- Redis：缓存、限流、短期状态、分布式锁或幂等记录。
- PostgreSQL：可靠业务数据、审计、配置与持久 checkpoint；pgvector 也可承担中小规模向量检索。
- 专用向量库：面向向量索引、过滤、分片和混合检索，但会增加运维复杂度。

#### 42. Docker 化 Agent 服务要注意什么？

回答要点：

- 固定依赖与镜像版本，使用非 root、健康检查、资源限制和小镜像。
- 密钥通过安全配置注入，不写进镜像或仓库。
- 状态放到外部 PostgreSQL/对象存储/向量库，容器本身尽量无状态。
- 明确 readiness、graceful shutdown、日志、指标和回滚版本。

#### 43. 现场编码可能考什么？

建议练习：

- 用 Pydantic 定义一个有枚举、范围和跨字段校验的 Tool schema。
- 写一个 async HTTP Tool，包含超时、错误映射和 request_id 透传。
- 实现 LRU/TTL Cache、限流器或幂等处理。
- 给定文档实现简单切块、余弦检索和 Recall@k。
- 用 FastAPI 写聊天接口和 SSE 事件生成器。
- 实现一个有最大步数和终止条件的简化 ReAct loop。

---

## 第二部分：Takeout Guys Agent 项目

### 一、30 秒项目介绍

这是一个把传统外卖系统的 AI 能力迁移为独立 Agent 微服务的项目。系统使用 FastAPI、LangChain 和 LangGraph 实现用户客服 Agent 与管理运营 Agent，通过 RAG 回答规则与运营知识，通过 Spring Boot Internal API 获取实时订单、菜品、购物车等业务事实。所有写操作都经过确认卡、身份与资源校验、幂等和审计；本地使用 SQLite checkpoint，生产 Compose 使用 PostgreSQL checkpoint，并通过 SSE 将模型增量、工具状态、引用、确认和完成事件传给前端。

### 二、为什么这个项目有面试价值

它不是单纯的“聊天页面 + 模型 API”，而是覆盖了 Agent 落地的关键难点：

- 模型、工具、业务服务之间的清晰边界。
- 用户 Agent 与管理 Agent 的状态和工具隔离。
- RAG 的多格式语料、可见性、引用、拒答与评测。
- 写操作的 Human-in-the-loop、持久恢复和幂等执行。
- Spring Boot、Python Agent、Vue 管理端和 uni-app 用户端的跨栈集成。
- 服务认证、最小化 actor、字段脱敏、限流、熔断、指标和回滚手册。

### 三、架构与一次请求链路

```text
用户端 / 管理端
      ↓ 既有业务 API、SSE
Spring Boot（认证、业务规则、真实读写、审计）
      ↓ Agent API                         ↑ Internal Agent API
FastAPI + LangGraph（规划、工具选择、RAG、确认与恢复）
      ↓                 ↓                 ↓
    LLM            RAG Knowledge      Checkpointer
                                      SQLite / PostgreSQL
```

一次只读请求：

1. 前端请求 Spring Boot，Spring 构造最小化 actor 上下文。
2. Spring 用服务 token 调用 Python `/v1/user/chat` 或 `/v1/admin/chat`。
3. LangGraph 根据角色注册有限 Tool，模型选择工具。
4. Python 使用服务认证调用 Spring Internal API，不直连业务 MySQL/Redis。
5. 工具结果回到模型，最终通过普通响应或 SSE 返回。

一次写请求：

1. 模型选择写 Tool，Tool 只生成带摘要与风险信息的确认卡。
2. 图通过 `interrupt()` 暂停，checkpoint 保存状态。
3. 用户批准、编辑或拒绝；恢复接口校验 token、actor、session、有效期和参数。
4. 批准后 Python 携带 confirmation token 与 Idempotency-Key 调用 Spring。
5. Spring 二次鉴权、校验资源状态和字段白名单，在业务服务内执行并审计。
6. LangGraph 用相同 thread 恢复，返回成功、拒绝或冲突结果。

### 四、功能完成度判断（2026-07-23 实测）

结论：**核心 P0～P4 和大部分 P5 能力已基本打通，适合演示和面试讲解；当前仍不是零缺口的生产发布态。**

| 规划阶段 | 当前证据 | 判断 |
| --- | --- | --- |
| P0 合同与骨架 | FastAPI 分层、Pydantic schema、Spring Internal API、服务认证、文档齐全 | 已完成 |
| P1 用户只读 | 菜单、营业状态、订单、购物车、地址、优惠券、敏感词等 Tool | 已完成 |
| P2 用户写与确认 | 加购、改数量、删除/清空购物车、领券、评价草稿；确认、编辑、拒绝、过期、恢复 | 已完成 |
| P3 管理只读 | 经营概览、订单、菜品、套餐、分类、优惠券、评价和运营知识 | 已完成 |
| P4 管理受控写 | 门店状态、订单流转、优惠券、菜品和分类的白名单写操作 | 已完成受控范围；高风险与批量动作按设计不开放 |
| P5 运行能力 | 指标、trace、限流、熔断、PostgreSQL checkpoint、Compose、回滚手册 | 代码和文档已具备；真实生产配置与演练待完成 |

本次验证结果：

- Python：`70 passed`；Ruff 与 `compileall` 通过。
- 管理端：TypeScript 检查和 Vite production build 通过；存在大 chunk 警告，不阻塞构建。
- Java：29 项测试中 27 项通过，2 项失败。失败值本身正确，是测试期望 `Integer`、实际分页 `total` 为 `Long` 的类型断言差异。
- 用户端：已补充 DCloud Vue 3/Vite CLI 构建，H5 与微信小程序 production build 均通过；浏览器实测 Agent 聊天页、移动端布局、快捷提问和后端不可用降级正常。微信登录、支付和真机网络仍需在微信开发者工具及真实账号环境验证。
- 历史真实模型评测报告：工具路由 13/13，RAG Recall@1/3/5 为 95%/100%/100%，引用可见性和无依据拒答为 100%，安全自动化用例通过，Tool P95 28.75ms，端到端 P95 约 9.5s。

发布前仍需：

- 修正 Java 两项类型断言并恢复全绿。
- 配置真实密钥、PostgreSQL 密码、镜像 tag 和 Prometheus 告警接收端。
- 执行部署级回滚演练和真实依赖端到端冒烟。
- 在微信开发者工具和真机环境验证登录、合法域名、SSE/确认卡与支付等平台能力。
- 知识规模扩大前，评测并考虑将离线字符 n-gram embedding 切到生产语义 embedding 与向量库。

### 五、项目关键设计取舍

#### 1. 为什么 Python Agent 不直连业务数据库？

参考回答：

Spring Boot 已经承载订单归属、状态机、事务和权限规则。Agent 如果直连数据库，会复制业务逻辑并绕过审计，也让模型产生的参数更接近真实数据层，风险很高。因此 Python 只负责编排、检索和工具选择，所有业务事实与写入都经过 Spring Internal API。代价是多一次网络调用，但换来单一事实源、事务复用和权限边界。

#### 2. 为什么用户和管理 Agent 分开？

参考回答：

两者的身份、工具、知识可见性和风险完全不同。分开 graph、state namespace、prompt 和 registry，可以从结构上避免用户会话拿到管理工具，也减少单 Agent 工具过多导致的选择干扰。代价是部分公共 Tool 需要复用封装，而不能直接共用完整状态。

#### 3. 为什么实时业务数据用 Tool，规则文档用 RAG？

参考回答：

订单状态、库存、价格会频繁变化，必须读取权威业务服务；取消规则、客服 SOP 等经过审核且变化相对慢，适合进入知识库。这样可以避免把交易数据写进向量库，也能给规则回答附来源。

#### 4. 为什么确认后 Spring 还要二次校验？

参考回答：

确认卡生成到执行之间，价格、资源版本、权限或订单状态都可能变化；确认 token 也可能被换用户重放。因此 Python 校验确认流程，Spring 再校验真实业务前置条件和资源归属，两层分别解决编排安全与业务一致性。

#### 5. 为什么只读请求可以重试，写请求不自动重试？

参考回答：

只读重试通常不会产生副作用；写请求超时后无法判断服务端是否已经提交，自动重试可能重复写入。项目为写入携带 Idempotency-Key，但客户端仍保持单次尝试，让服务端去重和状态查询负责恢复。

#### 6. 为什么本地 SQLite、生产 PostgreSQL？

参考回答：

SQLite 配置简单，适合单机开发和测试；多实例部署需要共享 checkpoint 和更好的并发、持久性，因此生产使用 PostgreSQL。代码通过配置选择 checkpointer，保持图逻辑不变。

### 六、面试官可能围绕项目追问的问题

#### 1. 你个人具体做了什么？

不要回答“整个项目都是我做的”。按可验证模块说明：

- 我设计/实现了什么接口、graph 节点或 Tool。
- 当时遇到的具体问题是什么。
- 为什么选这个方案，放弃了哪些方案。
- 用什么测试或指标证明有效。
- 仍有哪些限制。

#### 2. 画出用户 Agent 的状态图。

应能画出并解释：入口 → domain/actor guard → agent → tools → agent → confirmation interrupt 或 done；异常时进入降级。恢复时用 thread_id 找 checkpoint，以 `Command(resume=...)` 继续。

#### 3. 如何保证用户看不到别人的订单？

参考回答：

- Spring 从登录上下文生成短期最小 actor，不把原 JWT 交给模型。
- Python 只给 user actor 注册个人工具。
- Spring Internal API 再用当前 actor_id 查询资源归属。
- 返回 DTO 删除用户 ID、完整电话和详细地址等非必要字段。

#### 4. 模型伪造 category_id 会怎样？

参考回答：

模型不能直接执行写入。Python 在生成创建菜品确认卡前先查真实分类并验证；确认后 Spring 再校验 category_id 存在、类型正确且启用。错误 ID 会返回稳定的校验错误，不进入数据库写入。

#### 5. 确认 token 被盗或重放怎么办？

参考回答：

- 存储 token hash 而不是明文。
- token 绑定 agent、actor、session、动作和参数摘要，并设置 TTL。
- 参数编辑会生成新预览，旧 token 不能执行新参数。
- 执行状态原子地从 pending 切换到 executing/completed。
- 下游用相同 hash 作为幂等键，重复请求返回原结果而不是再次写入。

#### 6. 服务重启后确认流程怎样恢复？

参考回答：

确认记录存持久化数据库，LangGraph 使用 checkpointer 保存 thread 状态。客户端携带同一 thread_id 和确认 token 调用恢复接口，服务验证后使用 `Command(resume=...)` 继续。在多实例生产环境中 checkpoint 放 PostgreSQL，确保任一实例都能加载。

#### 7. RAG 为什么 Recall@1 不是 100%？你会怎么改？

参考回答：

当前小型中文语料使用离线字符 n-gram hash embedding，优点是无外部依赖，但语义表达有限。Recall@3 已是 100%，说明正确证据能召回但首位排序仍有提升空间。我会做错误样本分析，再比较中文语义 embedding、BM25+dense hybrid、metadata filter 和 reranker，并以 Recall、MRR、延迟和费用共同决定。

#### 8. 端到端 P95 约 9.5 秒，怎样优化？

参考回答：

Tool P95 只有约 28.75ms，主要瓶颈在模型而不是业务 API。优先分析首 token 与各 Agent step，减少无效循环和上下文，能并行的只读查询并行执行，对简单路由使用小模型，并用 SSE 提前展示 tool_status 与 delta。不能为了低时延牺牲写入校验。

#### 9. 为什么不用一个大 Prompt 完成所有事情？

参考回答：

Prompt 不能提供实时事实、事务、权限或确定性参数校验。项目把模型放在“理解与决策”位置，把事实、授权和写入放在工具与业务服务中，这样更可测试、可审计、可恢复。

#### 10. 如何测试 Tool 路由？

参考回答：

建立自然语言问题与期望 Tool/参数的数据集，运行真实模型或固定模型，对首个工具、完整轨迹、参数 schema 和最终状态评分。除成功用例，还覆盖无工具、错误工具、越权工具和多步工具链；发布时固定模型、Prompt、知识库 hash 和阈值。

#### 11. 测试为什么要 mock Spring 或模型？

参考回答：

单元测试要稳定、快速并定位边界，因此用依赖注入替换模型和 HTTP client；合同测试验证双方 schema；少量真实模型与端到端评测捕捉 mock 无法覆盖的路由波动。三者互补，不能只靠其中一种。

#### 12. 管理写 Tool 为什么只开放一部分？

参考回答：

按风险、可逆性、影响范围和权限分级。项目只注册经过评审的单资源动作，删除、批量修改、员工权限等高风险能力保持不可见。安全不是“工具已实现就全部开放”，而是最小代理权和逐项评审。

#### 13. 项目怎样防 Prompt Injection？

参考回答：

- 管理 Agent 有领域 guard，明显无关问题在模型前拒绝。
- 用户、管理工具注册表隔离，工具按 actor 动态暴露。
- Tool 输入通过 Pydantic 和 Spring 业务规则二次校验。
- RAG 文档带可见性和来源，知识内容只作为数据而不是系统指令。
- 写操作需要确认、幂等和审计。
- 仍应补充恶意文档、间接注入和工具输出注入的专项红队评测。

#### 14. 如果模型不可用，系统怎么办？

参考回答：

设置模型与 HTTP 超时、熔断和结构化错误映射，向用户返回可理解的降级信息，不伪造业务结果。关键业务仍可通过原有确定性页面完成；生产故障通过部署版本回滚，而不是维护两套不断漂移的 Agent 业务逻辑。

#### 15. 如果知识文档更新，怎样避免全量重建？

参考回答：

对规范化文档内容计算 hash，未变化 chunk 复用 embedding，新增或修改内容增量写入，删除内容清理对应 chunk。记录知识库版本和来源，使线上回答可追溯到具体 corpus hash。

#### 16. 为什么当前项目还不能直接说“生产就绪”？

参考回答：

核心功能和评测门槛已经满足，用户端 H5/微信小程序也已有可重复构建；但生产就绪还要求全绿构建、真实密钥管理、告警接收端、固定镜像 tag、多实例与真实依赖冒烟、微信开发者工具/真机验证和回滚演练。目前 Java 还有两项类型断言失败，生产配置和演练也需要在实际环境完成。

#### 17. 如果再给你两周，你优先做什么？

推荐回答顺序：

1. 修正 Java 测试断言并建立一键 CI，保证 Python/Java/管理端全绿。
2. 用 Compose 做真实依赖端到端冒烟和回滚演练。
3. 补 Prompt Injection、恶意 RAG 文档和并发确认的安全测试。
4. 分析 Recall@1 失败样本，做 hybrid + rerank 对照实验。
5. 完成微信开发者工具/真机验证、线上 token/成本统计和告警。

### 七、项目介绍的 STAR 模板

**Situation：** 原系统的 AI 编排写在 Java 内，工具选择、RAG、状态恢复和用户/管理权限边界难以独立演进。

**Task：** 在不绕过现有业务服务的前提下，建设独立 Python Agent 微服务，并兼容用户端和管理端现有入口。

**Action：** 使用 FastAPI 暴露 API，LangGraph 编排双 Agent；通过 Spring Internal API 封装业务 Tool；RAG 只存审核知识；写操作采用 interrupt、持久 checkpoint、确认 token、幂等键与 Spring 二次校验；补充 pytest、Java 测试、RAG/路由评测、Prometheus 和 Compose。

**Result：** 核心 P0～P4 打通；当前 Python 70 项测试通过，历史真实模型工具路由 13/13、RAG Recall@3 100%、安全用例通过。仍有 2 项 Java 类型断言和生产演练需要收尾。

### 八、面试前自检清单

- [ ] 1 分钟内讲清项目目标、用户、架构和结果。
- [ ] 能不看代码画出只读链路与确认写链路。
- [ ] 能解释为什么 Agent 不直连业务数据库。
- [ ] 能解释 Tool schema、权限、确认、幂等和重试策略。
- [ ] 能计算并解释 Recall@k、MRR、P95。
- [ ] 能讲一个真实问题、排查过程、修复和验证。
- [ ] 能诚实说明尚未完成的生产事项。
- [ ] 能现场写一个 Pydantic Tool schema 和 async HTTP 调用。
- [ ] 能回答 LangGraph state、interrupt、checkpoint、thread_id。
- [ ] 能提出下一阶段的安全、检索和工程改进计划。

### 九、建议继续阅读

- [OpenAI：A practical guide to building agents](https://openai.com/business/guides-and-resources/a-practical-guide-to-building-ai-agents/)
- [LangGraph：Interrupts](https://docs.langchain.com/oss/python/langgraph/interrupts)
- [LangGraph：Persistence](https://docs.langchain.com/oss/python/langgraph/persistence)
- [LangGraph：Agentic RAG](https://docs.langchain.com/oss/python/langgraph/agentic-rag)
- [LangChain：Agent Evals](https://docs.langchain.com/oss/python/langchain/test/evals)
- [Anthropic：Demystifying evals for AI agents](https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents)
- [OWASP：Top 10 for LLM Applications](https://genai.owasp.org/llm-top-10/)
- [MCP：Architecture overview](https://modelcontextprotocol.io/docs/learn/architecture)
