# Takeout Guys AI 项目开发记录

> 当前统一仓库结构：`backend/` 为 Spring Boot 后端，`admin-web/` 为管理端，`user-app/` 为小程序，接口与维护文档统一放在 `docs/`。

> 本文档是项目的长期维护记录，面向开发者和 AI Agent。每次完成功能、修改架构、增加依赖或发现重要问题后，应更新对应章节，并在“变更记录”末尾追加条目。

## 1. 文档维护规则

### 1.1 更新原则

- 记录已经落地并验证的事实，不把规划写成已完成功能；
- 未完成内容统一放入“待办与已知限制”；
- 新增接口后同步更新 `AI_AGENT_API_REQUIREMENTS.json` 或相应 API 文档；
- 新增依赖、中间件、环境变量、数据表时，必须在本文档登记；
- 不在文档中记录真实 API Key、数据库密码或其他敏感凭据；
- 每条变更记录应包含日期、范围、改动、验证方式和后续事项。

### 1.2 变更记录模板

```markdown
### YYYY-MM-DD · 变更标题

- 范围：前端 / 后端 / 数据库 / AI / 部署
- 问题：修改前存在的问题
- 改动：实际完成的内容
- 方法：关键实现方式或架构决策
- 文件：新增或修改的主要文件
- 验证：执行过的测试或构建命令
- 后续：仍需完成的事项；没有则写“无”
```

## 2. 项目概况

项目是在基础版苍穹外卖系统上进行的智能化扩展，当前包含：

- Spring Boot 多模块后端；
- Vue 3 Takeout Guys AI 管理端；
- uni-app Vue 3 小程序端；
- 旧工程与原型归档；
- GX10 vLLM 模型服务配置和 AI Agent 接口契约。

主要目录：

```text
backend/                          Spring Boot 后端与数据库脚本
admin-web/                        Vue 3 管理端
user-app/                         uni-app Vue 3 小程序
docs/                             API、日志和品牌资源
scripts/                          工具脚本
legacy/                           旧工程与原型（不纳入新仓库提交）
```

## 3. 当前技术架构

### 3.1 后端

- Java；
- Spring Boot 2.7.3；
- Spring MVC；
- MyBatis；
- MySQL；
- Redis / Spring Cache；
- JWT；
- Knife4j / Swagger；
- Maven 多模块工程。

后端模块：

```text
sky-common
sky-pojo
sky-server
```

三个模块的父工程位于 `backend/pom.xml`。

### 3.2 新管理端

- Vue 3；
- TypeScript；
- Vite；
- Vue Router；
- Pinia；
- Axios；
- Element Plus；
- Element Plus Icons。

### 3.3 AI 服务

- 推理服务：GX10 上运行的 vLLM；
- 协议：OpenAI Compatible API；
- 模型：`ornith`；
- 模型发现：`GET /v1/models`；
- 对话补全：`POST /v1/chat/completions`；
- 最大上下文：262144；
- 认证方式：`Authorization: Bearer <api-key>`。

当前后端同时使用原生 Java HTTP 调用和 LangChain4j 文档抽象。管理端和用户端 GX10 对话、健康检查与 Tool Calling 继续通过统一的 `AiToolCallingClient` 实现；当前已补齐面向管理端与用户端共享的业务知识检索链路，并提供只读资源目录桥接；AI 会话持久化、Embedding 和向量检索增强仍待实现。

## 4. 重要架构决策

### 4.1 使用独立 Vue 3 管理端

原管理端是 Vue 2 + Vue CLI 工程。为承载新的 AI Agent 和品牌设计，新建并演进为当前的 `admin-web` Vue 3 管理端，不直接覆盖旧管理端，便于回退和对照功能。

### 4.2 开发环境不依赖 Nginx

Vite 开发服务器负责接口代理：

```text
浏览器 /api/**
    → Vite :5173
    → http://localhost:8080/admin/**
```

配置文件：`admin-web/vite.config.ts`。

这样本地联调不需要 Nginx，也不需要额外配置后端 CORS。生产环境仍可选择 Nginx、容器网关或其他反向代理。

### 4.3 沿用现有认证协议

- 管理端登录：`POST /admin/employee/login`；
- 管理端 Token 请求头：`token`；
- 用户端 Token 请求头：`authentication`；
- 统一响应：`{ code, msg, data }`。

### 4.4 GX10 密钥通过环境变量注入

后端配置位于：

```text
backend/sky-server/src/main/resources/application.yml
```

支持的环境变量：

```text
GX10_AI_BASE_URL
GX10_AI_API_KEY
GX10_AI_MODEL
GX10_AI_CONNECT_TIMEOUT
GX10_AI_READ_TIMEOUT
GX10_AI_MAX_TOKENS
GX10_AI_TEMPERATURE
GX10_AI_RAG_ENABLED
GX10_AI_RAG_TOP_K
GX10_AI_MCP_ENABLED
```

密钥禁止写入前端、接口响应或版本库。`gx10.txt` 当前含明文凭据，应该迁移为本机环境变量并从版本管理中排除。

### 4.5 AI 工具必须复用业务 Service

AI Agent 不直接操作 Mapper 或数据库。订单查询、订单取消、菜品查询、经营统计等能力应通过已有 Service 实现，同时执行用户身份、订单归属和状态校验。

## 5. 当前功能状态

### 5.1 已接通的管理端能力

- 管理员登录和退出；
- JWT Token 自动携带；
- 工作台经营数据；
- 订单概览；
- 菜品概览；
- 订单分页查询；
- 菜品分页查询；
- 员工分页查询；
- 分类分页查询；
- 套餐分页查询；
- 订单接单、拒单、取消、派送和完成；
- 菜品、套餐、分类、员工修改和状态切换；
- 菜品、套餐和分类删除；
- 分类新增；
- 菜品新增和修改，支持分类、价格、图片、描述及多组口味；
- 员工新增和修改，支持账号、姓名、手机号、性别及身份证号；
- 订单详情查看，所有订单状态均保留详情入口；
- 营业额、订单、用户和销量统计入口；
- 店铺营业状态读取和切换。

### 5.2 新管理端页面

- 登录页；
- 经营总览；
- 数据统计；
- AI Agent 工作台；
- 订单管理；
- 菜品管理；
- 分类管理；
- 套餐管理；
- 员工管理；
- 优惠券入口；
- 评价管理入口；
- 敏感词管理入口；
- 人工客服工作台入口。

### 5.3 已接通的用户端 AI 能力

- `POST /user/ai/chat` 非流式对话；
- `GET /user/ai/chat/stream` SSE 流式对话；
- `GET /user/ai/session/list` 会话列表；
- `DELETE /user/ai/session/{sessionId}` 删除会话；
- 店铺营业状态查询；
- 地址列表、地址详情、默认地址查询；
- 地址新增、修改、删除和设为默认；
- 我的优惠券、可领取优惠券、订单可用优惠券查询；
- 领券；
- 购物车查询、加购、减购和清空；
- 历史订单查询、订单详情查询；
- 取消订单、再来一单和催单；
- 菜品与套餐搜索。

### 5.4 仅有前端入口、后端尚未完成

- 优惠券；
- 菜品评价；
- 敏感词；
- 人工客服；
- AI 推荐；
- AI 评价帮写；
- RAG 知识库。

这些页面不得使用模拟数据表示已完成，应展示明确的待实现状态。

## 6. 已接入的后端接口

```text
POST /user/ai/chat
GET  /user/ai/chat/stream
GET  /user/ai/session/list
DELETE /user/ai/session/{sessionId}

POST /admin/employee/login
POST /admin/employee/logout
GET  /admin/employee/page

GET  /admin/workspace/businessData
GET  /admin/workspace/overviewOrders
GET  /admin/workspace/overviewDishes
GET  /admin/workspace/overviewSetmeals

GET  /admin/order/conditionSearch
GET  /admin/order/statistics
PUT  /admin/order/confirm
PUT  /admin/order/rejection
PUT  /admin/order/cancel
PUT  /admin/order/delivery/{id}
PUT  /admin/order/complete/{id}

GET  /admin/dish/page
GET  /admin/category/page
GET  /admin/setmeal/page

GET  /admin/report/turnoverStatistics
GET  /admin/report/ordersStatistics
GET  /admin/report/userStatistics
GET  /admin/report/top10

GET  /admin/shop/status
PUT  /admin/shop/{status}
```

AI 新增接口以 `AI_AGENT_API_REQUIREMENTS.json` 为唯一契约来源。实现接口时需同步更新契约状态。

## 7. 品牌与视觉规范

品牌名称：Takeout Guys AI。

品牌资源：

```text
icons/icon (1).png    完整品牌字标
icons/icon (2).png    外卖袋机器人图标
```

视觉关键词：

- 橙红色；
- 深海军蓝；
- 暖白背景；
- 圆角卡片；
- 外卖速度线；
- AI 电路节点。

新管理端通过 Vite `publicDir: '../icons'` 复用根目录资源。新增页面应继续使用当前 CSS 变量和视觉语言，避免混入旧后台的黄色主题。

## 8. 新增和修改的主要文件

### 8.1 根目录

- `AI_AGENT_API_REQUIREMENTS.json`：AI Agent 接口和安全需求；
- `PROJECT_DEVELOPMENT_LOG.md`：长期开发记录；
- `admin-web/`：新 Vue 3 管理端。

### 8.2 新管理端

```text
admin-web/
├── package.json
├── package-lock.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── .env.development
├── .env.production
├── README.md
└── src/
    ├── api/admin.ts
    ├── layout/AppLayout.vue
    ├── router/index.ts
    ├── stores/auth.ts
    ├── styles/main.css
    ├── utils/request.ts
    └── views/
        ├── LoginView.vue
        ├── DashboardView.vue
        ├── ReportsView.vue
        ├── AgentView.vue
        ├── BusinessView.vue
        └── ServiceView.vue
```

### 8.3 后端配置

- `backend/sky-take-out/sky-server/src/main/resources/application.yml`
  - 新增 `sky.ai` 配置；
  - 支持 GX10 URL、Key、模型、超时、温度和最大输出 Token。

## 9. 依赖与中间件登记

### 9.1 新管理端依赖

| 依赖 | 用途 |
|---|---|
| Vue 3 | 前端框架 |
| Vite | 开发服务器与构建 |
| TypeScript / vue-tsc | 类型检查 |
| Vue Router | 页面路由 |
| Pinia | 登录和全局状态 |
| Axios | HTTP 请求与 Token 拦截 |
| Element Plus | 管理端 UI |
| Element Plus Icons | 页面图标 |

### 9.2 基础设施与中间件

| 名称 | 状态 | 用途 |
|---|---|---|
| MySQL | 已使用 | 业务数据持久化 |
| Redis | 已使用 | 缓存；未来用于会话或向量检索 |
| Vite Proxy | 已使用 | 本地替代 Nginx 转发管理端请求 |
| Nginx | 开发环境不需要 | 可用于生产静态部署和反向代理 |
| GX10 vLLM | 已接入 | 大模型推理 |
| LangChain4j | 已加入 | 企业知识文档抽象与轻量 RAG |
| Spring AI | 未加入 | 可选 AI 客户端框架 |
| MCP Bridge | 已加入 | 管理端只读能力目录与资源读取桥接 |

## 10. 验证基线

完成变更后至少执行：

### 10.1 后端

```powershell
cd backend
mvn compile -DskipTests
```

### 10.2 新管理端

```powershell
cd admin-web
npm.cmd exec vue-tsc -- --noEmit -p tsconfig.app.json
```

开发运行：

```powershell
npm.cmd run dev
```

访问：`http://localhost:5173`。

### 10.3 AI 契约

```powershell
Get-Content -Raw -Encoding UTF8 AI_AGENT_API_REQUIREMENTS.json | ConvertFrom-Json | Out-Null
```

### 10.4 已验证事实

- Spring Boot 可在 8080 端口启动；
- Vue 3 开发服务器可在 5173 端口启动；
- `admin` 登录可签发 JWT；
- 工作台、订单、菜品、员工、分类和套餐接口可携带 Token 调用；
- GX10 `/v1/models` 可访问并返回 `ornith`；
- Maven 多模块编译通过；
- 新管理端 TypeScript 检查通过；
- AI Agent JSON 契约解析通过。

## 11. 待办与已知限制

### P0

1. AI 会话和消息持久化；
2. RAG 知识库；
3. AI 推荐结果回查与正式业务闭环；
4. 同步更新 `USER_API_APIFOX.json` 与 `ADMIN_API_APIFOX.json` 中新增 AI tool calling 契约；
5. 完善套餐新增表单和套餐菜品组合表单。

### P1

1. AI 会话和消息表；
2. AI 菜品推荐；
3. AI 评价帮写；
4. RAG 知识库；
5. 菜品评价模块；
6. 优惠券模块；
7. 敏感词模块；
8. 人工客服模块；
9. 完整前端错误处理和权限反馈。

### 已知限制

- 用户端与管理端 AI 会话当前仅保存在服务进程内存中，多实例部署下不会共享；
- GX10 推理模型可能返回 `reasoning_content`，最终用户响应只应展示 `content`；
- 推理模型使用过小的 `max_tokens` 可能在正文生成前以 `length` 结束；
- `gx10.txt` 含明文凭据，存在安全风险；
- 菜品新增表单已经支持图片上传、分类和口味；套餐新增仍需实现套餐菜品组合表单；
- 优惠券、评价、敏感词和客服页面目前没有对应后端 Controller。

## 12. 变更记录

### 2026-07-01 · Vue 3 管理端、真实业务接口与 GX10 契约

- 范围：前端、后端配置、AI、开发环境；
- 问题：旧管理端技术栈较旧，新管理端初版只有登录真实接入，业务页面存在骨架和模拟内容，AI 服务没有明确配置和接口契约；
- 改动：创建 Vue 3 管理端，完成 Takeout Guys AI 品牌设计，接通现有管理端业务接口，补回套餐、分类、订单操作、统计和店铺状态，验证 GX10 vLLM，增加 AI 配置和接口需求文档；
- 方法：使用 Vite Proxy 替代开发环境 Nginx；复用现有 JWT 和 Service 业务边界；GX10 密钥通过环境变量注入；AI API 使用独立 JSON 契约；
- 文件：`sky-agent-admin-vue3/`、`AI_AGENT_API_REQUIREMENTS.json`、后端 `application.yml`；
- 验证：真实 Token 接口联调、GX10 模型发现、Vue TypeScript 检查、JSON 解析、Maven 编译；
- 后续：实现后端 AI 客户端、Controller、Service、Tool Calling、会话存储和 RAG。

### 2026-07-01 · 菜品口味关联、员工表单与订单操作完善

- 范围：后端事务、Vue 3 管理端、接口联调；
- 问题：新管理端的菜品表单没有提交 `categoryId`、图片和 `flavors`，导致菜品与口味功能看起来不连通；员工只有列表，没有新增表单；已完成或已取消订单的操作列为空，订单详情无法查看；菜品修改时删除旧口味和写入新口味不在事务中；
- 改动：完善新增/修改分类；新增完整菜品表单，支持菜品分类、价格、图片上传或 URL、描述、多组口味和多个口味选项；口味选项按 `dish_flavor.value` 要求序列化为 JSON 数组；新增员工表单和字段校验；订单增加固定详情按钮，并根据状态显示接单、拒单、取消、派送和完成按钮；菜品修改业务增加事务；
- 方法：前端按 `DishDTO`、`DishFlavor`、`EmployeeDTO` 构造请求；使用 `GET /admin/category/list?type=1` 加载启用的菜品分类；使用 `POST /admin/common/upload` 上传菜品图片；使用 `GET /admin/order/details/{id}` 展示订单详情；后端在 `DishServiceImpl.updateWithFlavor` 增加 `@Transactional`，保证菜品表和口味表同时成功或同时回滚；
- 文件：`sky-agent-admin-vue3/src/views/BusinessView.vue`、`sky-agent-admin-vue3/src/api/admin.ts`、`sky-agent-admin-vue3/src/styles/main.css`、`backend/sky-take-out/sky-server/src/main/java/com/sky/service/impl/DishServiceImpl.java`；
- 验证：Vue TypeScript 检查通过；Maven 多模块编译通过；真实 Token 调用返回 7 个启用菜品分类；查询菜品 ID 65 时成功返回 1 组口味；订单详情接口返回 `code=1`；
- 后续：实现完整套餐新增表单和套餐菜品组合；补充新增菜品、员工、分类的自动化集成测试，避免在开发数据库中生成测试脏数据。

### 2026-07-01 · 固定口味下拉与自定义扩展

- 范围：Vue 3 菜品表单；
- 问题：菜品口味名称和选项只能自由输入，没有复用原管理端定义的固定口味，下拉列表为空；
- 改动：恢复甜味、温度、忌口、辣度四组固定口味；选择口味名称后自动带出对应的固定选项；已选择的固定口味不会在其他行重复出现；口味名称和口味选项均保留自定义创建能力；编辑菜品时继续回显数据库中的固定或自定义口味；
- 方法：在前端维护与原管理端一致的 `flavorPresets`，使用 Element Plus 可创建下拉框实现预设选择和扩展输入；
- 文件：`sky-agent-admin-vue3/src/views/BusinessView.vue`；
- 验证：Vue TypeScript 检查；
- 后续：如果口味需要后台统一维护，应新增独立口味模板表和管理接口，替代前端常量。

### 2026-07-01 · 订单状态工作流完善

- 范围：Vue 3 订单管理；
- 问题：订单操作只按状态显示在表格行中，数据库现有订单多为已完成或已取消，页面看起来没有操作；缺少状态分类、待处理数量、二次确认和详情内操作；
- 改动：增加全部、待接单、待派送、派送中、已完成和已取消状态栏；接入订单统计数量；按状态查询订单；所有订单固定显示详情入口；待接单支持接单、拒单和取消，已接单支持派送和取消，派送中支持完成和取消；详情弹窗提供相同操作；
- 方法：使用 `GET /admin/order/statistics` 展示待处理数量，使用 `status` 参数调用条件分页接口；状态变更前使用确认弹窗，拒单和取消必须填写原因；操作成功后关闭详情并刷新列表；
- 文件：`sky-agent-admin-vue3/src/views/BusinessView.vue`、`sky-agent-admin-vue3/src/api/admin.ts`、`sky-agent-admin-vue3/src/styles/main.css`；
- 验证：Vue TypeScript 检查通过；订单统计字段与后端 `OrderStatisticsVO` 对齐；
- 后续：增加订单操作端到端测试，以及 WebSocket 新订单提醒。

### 2026-07-01 · GX10 管理端流式 AI 接口

- 范围：后端 AI、管理端 Agent、配置与接口契约；
- 问题：GX10 只有连接参数和外部协议验证，Spring Boot 没有实际 AI 客户端，`/admin/ai/chat/stream` 不存在，前端无法对话；
- 改动：实现 `POST /admin/ai/chat/stream`，将管理端消息以 OpenAI-compatible 请求发送到 GX10 vLLM，并以 `meta/delta/done/error` SSE 事件向前端增量转发；实现 `GET /admin/ai/health`，实际检查 `/models` 中是否存在目标模型；前端改为正确解析 SSE 事件并动态显示 GX10 连接状态；
- 方法：基于 Java 8 `HttpURLConnection` 实现上游流式客户端，不引入额外 AI SDK；使用 `SseEmitter` 向浏览器推送；通过 `GX10_AI_API_KEY` 环境变量注入密钥；客户端断开、超时或错误时主动断开上游连接；系统提示词禁止在业务 Tool 尚未接入时编造经营数据；
- 文件：`AiProperties.java`、`AdminAiChatRequestDTO.java`、`AdminAiChatService.java`、`AdminAiChatServiceImpl.java`、`AdminAiController.java`、`AgentView.vue`、`AI_AGENT_API_REQUIREMENTS.json`；
- 验证：GX10 `/v1/models` 已验证；Maven 编译通过；Vue TypeScript 检查通过；完成后端重启后需执行健康检查和流式端到端请求；
- 后续：实现会话持久化、管理端业务只读 Tool、RAG，并用专用线程池替代默认 `CompletableFuture` 公共线程池。

### 2026-07-01 · GX10 本地开发配置

- 范围：Spring Boot 开发环境配置；
- 问题：从 IntelliJ 直接启动时没有注入 `GX10_AI_API_KEY`，管理端流式接口返回“未配置 GX10_AI_API_KEY”；
- 改动：在 `application-dev.yml` 的 `sky.ai` 下增加 GX10 默认 URL、开发密钥和模型名；环境变量仍具有更高优先级；
- 方法：使用 `${ENV_VAR:development-default}` 形式，使本地开发可以直接启动，同时允许部署环境覆盖；
- 文件：`backend/sky-take-out/sky-server/src/main/resources/application-dev.yml`；
- 验证：需要重启 Spring Boot 后调用 `/admin/ai/health`；
- 后续：提交公开仓库或部署生产环境前，删除开发密钥默认值并轮换密钥。

### 2026-07-02 · Takeout Guys uni-app 用户端重构

- 范围：微信小程序用户端、品牌视觉、现有业务接口与 AI Agent 前端契约；
- 问题：旧 uni-app 仍使用瑞吉外卖视觉和旧接口字段，JWT 请求头、订单明细字段、金额单位及部分订单路径与当前后端不一致，用户端也没有 AI 入口；
- 改动：使用 Takeout Guys 图标和橙色/深蓝色体系重构首页、导航栏及个人中心；统一请求封装和用户 Token；对齐登录、店铺、分类、菜品、套餐、购物车、地址和订单接口；修正套餐加入购物车、订单状态、金额单位和模拟支付流程；新增 AI 对话与智能推荐页面及用户 AI API 封装；
- 方法：直接以 `controller/user`、DTO 和 VO 为接口事实来源；已有接口立即接通，尚未实现的 `/user/ai/**` 按 `AI_AGENT_API_REQUIREMENTS.json` 建立前端契约并在失败时明确提示；GX10 密钥仅由后端保存；
- 文件：`project-rjwm-weixin-uniapp/pages/`、`project-rjwm-weixin-uniapp/utils/`、`project-rjwm-weixin-uniapp/store/`、`project-rjwm-weixin-uniapp/static/takeout-guys-*.png`、`project-rjwm-weixin-uniapp/API_INTEGRATION.md`；
- 验证：核心 JavaScript 语法检查、`pages.json` 解析、页面 API 导入导出检查、后端 Controller 路径对照；
- 后续：在 HBuilderX 和微信开发者工具完成真机联调；实现用户 AI Controller、会话存储、推荐结果回查和流式接口。

### 2026-07-02 · uni-app 迁移至 Vue 3

- 范围：小程序运行时、应用入口、状态管理和模板语法；
- 问题：用户端仍使用 Vue 2 的 `new Vue`、`Vue.use`、原型注入和模板过滤器，无法作为 Vue 3 项目编译；
- 改动：应用入口迁移为 `createSSRApp`；Vuex 状态管理迁移为 `createStore` 并通过 `app.use(store)` 注入；在 `manifest.json` 启用 `vueVersion: 3`；将手机号过滤器改为普通方法；清理组件中的 Vue 2 `filters` 配置；
- 文件：`project-rjwm-weixin-uniapp/main.js`、`store/index.js`、`manifest.json`、`pages/my/my.vue`、`components/empty/empty.vue`；
- 验证：检查 Vue 2 专属入口和模板过滤器已清零，并完成 JavaScript、页面配置及组件结构静态检查；
- 后续：使用支持 Vue 3 的 HBuilderX 重新编译 `unpackage`，再导入微信开发者工具进行运行时回归测试。

### 2026-07-02 · 微信基础库与本地网络兼容

- 范围：HarmonyOS 兼容、微信开发者工具网络配置和本地联调；
- 问题：微信基础库提示聚合系统信息 API 已废弃，小程序无法请求 `localhost`，开发日志 WebSocket 受到合法域名校验限制；
- 改动：改用 `getDeviceInfo` 和 `getWindowInfo`；开发接口地址切换为电脑有效局域网地址 `192.168.31.107:8080`；开发工具项目配置关闭合法域名校验；
- 文件：`utils/system.js`、`utils/env.js`、相关页面和状态栏组件、`project.config.json`、`project.private.config.json`；
- 验证：Vue 3 共 17 个组件模板编译检查和 JSON 配置解析通过；当前检测时 8080 未运行，启动后端后才能进行接口请求验证；
- 后续：电脑 DHCP 地址变化时同步更新 `baseUrl`；正式发布改用已备案 HTTPS 域名并开启域名校验。

### 2026-07-02 · 小程序视觉收口与完整 Apifox 文档

- 范围：小程序首页、订单相关页面、后端 Swagger 与接口交付；
- 问题：首页顶部橙色区域过高，订单和地址页面仍残留旧黄色按钮；已有 Apifox 文件只覆盖 AI 契约；评价菜单容易被误认为已完成；
- 改动：首页导航缩短并改为深蓝；小程序按钮和状态色统一为 Takeout Guys 橙色；新增完整后端 Swagger 分组和 `BACKEND_API_APIFOX.json`；增加可重复执行的 Apifox 文档生成脚本；
- 文件：小程序 Navbar 与页面样式、`WebMvcConfiguration.java`、`scripts/generate-apifox.js`、`BACKEND_API_APIFOX.json`；
- 验证：完整文档包含 66 条路径、76 个操作和 71 个模型；Maven 多模块编译通过；Vue 3 模板检查通过；
- 已知限制：评价/点评当前只有菜单占位与 AI 帮写契约，尚无评价数据表、Controller、Service、Mapper 和真实前端内容。

### 2026-07-02 · 规格购物车加减修复

- 范围：小程序菜单、规格选择和购物车；
- 问题：旧黄色加减位图不符合品牌色；减少带规格菜品时前端遗漏 `dishFlavor`，后端按用户、菜品和口味精确查询后返回“购物车中不存在该商品”；
- 改动：加减按钮改为 CSS 绘制的橙色/深蓝色按钮；购物车加减请求携带当前记录的准确口味；带规格菜品始终进入规格选择；规格弹窗按所选口味匹配购物车数量；
- 文件：`project-rjwm-weixin-uniapp/pages/index/index.vue`、`index.js`、`style.scss`；
- 验证：JavaScript 语法、Vue 3 模板编译和 Git 空白检查通过。

### 2026-07-02 · 购物车合并、浏览区减少与客户评价入口

- 范围：购物车 SQL、小程序菜单交互、规格展示和客户评价页面；
- 问题：无口味商品首次写入空字符串、后续却按 NULL 查询，重复添加会产生两行；浏览区带规格商品无法减少；购物车不展示所选口味；评价页面缺失；
- 改动：空字符串与 NULL 统一匹配并在入库前标准化；浏览区单一规格可直接减少，多规格引导在购物车精确选择；每个口味组可选零项或一项；购物车展示 `dishFlavor`；按钮缩小至 44rpx；个人中心增加客户评价入口及评价/AI 帮写页面；
- 验证：Maven clean compile、Vue 3 模板和 JavaScript 语法检查通过；
- 已知限制：评价提交与 AI 帮写后端仍待实现，页面不会伪造提交成功。

### 2026-07-02 · Monorepo 根目录重构

- 范围：Git 仓库、后端、管理端、小程序、文档和旧工程归档；
- 问题：GitHub 仓库嵌套在 `backend/sky-take-out`，用户端另有独立 Git，根目录存在多个新旧前端，启动和提交边界不清晰；
- 改动：将 GitHub 仓库提升到项目根目录；后端统一到 `backend/`，管理端移动到 `admin-web/`，用户端移动到 `user-app/`，文档集中到 `docs/`，旧项目归档到 `legacy/`；用户端原 Git 历史备份在主仓库 `.git/user-app-history`；
- 方法：保留当前 GitHub remote 和 main 分支，通过 Git rename 检测保留后端历史；统一根 `.gitignore`，排除构建产物、本地密钥、旧工程与迁移锁定残留；
- 后续：关闭旧 Vite/HBuilderX 进程后，可删除空的 `backend/sky-take-out/` 与 `project-rjwm-weixin-uniapp/` 残留目录。

### 2026-07-05 · 管理端 AI 仅保留 LLM 对话与 Tool Calling 主链路

- 范围：后端 AI、文档；
- 问题：`AdminAiChatServiceImpl` 仍混有固定回答、关键词触发分支和旧的直连流式路径；部分文档仍把管理端 Tool Calling 写成“未完成”，且工具输出依赖格式化字符串，列表查询也容易被默认分页限制；
- 改动：移除管理端 AI 中遗留的固定回答、关键词触发和旧流式分支，只保留基于 `AiToolCallingClient` 的 LLM 对话与 Tool Calling 主链路；工具返回值统一调整为结构化 JSON；`query_orders`、`query_coupons`、`query_goods` 增加 `all=true`，后端自动分页抓取全量结果，避免模型只能看到默认第一页；同步更新 `docs/agent.md` 与 `docs/SKY_TAKE_OUT_FULL_PROJECT_README.md` 的能力边界说明；
- 方法：`/admin/ai/chat/stream` 统一经由 `completeWithTools` 执行，由模型从 `get_shop_status`、`set_shop_status`、`query_orders`、`update_order`、`query_coupons`、`query_goods`、`get_business_overview`、`get_business_trend` 中自主选用；经营概览、趋势、菜品、优惠券、订单等工具均直接向 LLM 返回原始结构化数据，减少格式化摘要耦合；
- 文件：`backend/sky-server/src/main/java/com/sky/service/impl/AdminAiChatServiceImpl.java`、`docs/PROJECT_DEVELOPMENT_LOG.md`、`docs/agent.md`、`docs/SKY_TAKE_OUT_FULL_PROJECT_README.md`；
- 验证：`mvn compile -DskipTests` 通过；
- 后续：会话持久化与 RAG 仍未实现。

### 2026-07-05 · 管理端与用户端 AI Tool Calling 统一重构

- 范围：后端 AI、文档；
- 问题：`AdminAiChatServiceImpl` 与 `UserAiChatServiceImpl` 都承载了会话管理、工具定义、参数解析、工具执行和 SSE 编排，单类体积过大且难以维护；用户端 AI 虽有对话入口，但仍混有关键词分支、自然语言格式化和旧实现路径；
- 改动：将管理端和用户端 AI 服务都重构为统一的四层结构：`ServiceImpl` 只保留对话编排，`ToolRegistry` 维护工具 schema，`ToolExecutor` 负责业务工具执行，`SessionManager` 负责内存会话；用户端正式接入结构化 JSON Tool Calling，覆盖店铺状态、地址、优惠券、购物车、订单和菜单搜索能力；所有变更类工具统一要求 `confirmed=true`；
- 方法：两端统一复用 `AiToolCallingClient` 与现有业务 Controller/Service，不直接访问 Mapper；管理端工具输出继续保持结构化 JSON 和 `all=true` 全量拉取能力；用户端移除旧的关键词分支和旧流式直连逻辑，仅保留 LLM + tool calling 主链路；
- 文件：`backend/sky-server/src/main/java/com/sky/service/impl/AdminAiChatServiceImpl.java`、`backend/sky-server/src/main/java/com/sky/service/impl/UserAiChatServiceImpl.java`、`backend/sky-server/src/main/java/com/sky/service/ai/admin/`、`backend/sky-server/src/main/java/com/sky/service/ai/user/`、`docs/agent.md`、`docs/PROJECT_DEVELOPMENT_LOG.md`；
- 验证：`mvn clean compile -DskipTests` 通过；
- 后续：同步更新 `USER_API_APIFOX.json` 与 `ADMIN_API_APIFOX.json` 的 AI tool 契约；将内存会话迁移到可持久化存储。

### 2026-07-05 · 企业化 AI 升级：LangChain4j 文档检索与 MCP 风格能力目录

- 范围：后端 AI、知识检索、文档归档；
- 问题：`README`、`agent.md` 与开发记录对 AI 当前能力的描述不一致；后端缺少企业化知识检索层，平台规则、架构说明和交付边界无法被 AI 稳定引用；仓库也没有 MCP 形态的能力目录；
- 改动：在 `sky-server` 中加入 LangChain4j 依赖，并基于 `Document` / `TextSegment` 落地本地文档分段检索；管理端新增 `list_knowledge_sources`、`search_knowledge_base`、`list_mcp_capabilities`、`read_mcp_resource` 四个只读工具；`/admin/ai/health` 额外返回 RAG 与 MCP 状态；同步修正 `docs/agent.md`、`docs/SKY_TAKE_OUT_FULL_PROJECT_README.md` 与本文档中关于用户端 Tool Calling、RAG、LangChain4j 与 MCP 的口径；
- 方法：保持原有 `AiToolCallingClient + ToolRegistry + ToolExecutor` 主链路不变，仅在管理端追加“企业知识层”；RAG 采用本地文档切片与关键词排序，避免在未完成 embedding / 向量库选型前引入高耦合实现；MCP 先以只读、进程内 bridge 方式提供能力目录与资源读取，避免过早开放写操作；
- 文件：`backend/sky-server/pom.xml`、`backend/sky-common/src/main/java/com/sky/properties/AiProperties.java`、`backend/sky-server/src/main/java/com/sky/service/ai/knowledge/EnterpriseKnowledgeBaseService.java`、`backend/sky-server/src/main/java/com/sky/service/ai/mcp/McpGatewayService.java`、`backend/sky-server/src/main/java/com/sky/service/ai/admin/AdminAiToolRegistry.java`、`backend/sky-server/src/main/java/com/sky/service/ai/admin/AdminAiToolExecutor.java`、`backend/sky-server/src/main/java/com/sky/service/impl/AdminAiChatServiceImpl.java`、`docs/agent.md`、`docs/SKY_TAKE_OUT_FULL_PROJECT_README.md`、`docs/PROJECT_DEVELOPMENT_LOG.md`；
- 验证：`mvn compile -DskipTests` 通过；
- 后续：将当前本地关键词检索升级为 embedding + 向量检索 + 重排；为用户侧整理正式客服知识语料；评估是否需要提供独立外部 MCP Server。

### 2026-07-05 · 业务知识中心落地与 RAG 命名收口

- 范围：后端 AI、知识检索、文档口径；
- 问题：现有知识检索仍偏“技术演示”命名，类名和工具名带有 `Enterprise`、`KnowledgeBase`、`McpGateway` 等实现痕迹；用户端虽然具备 Tool Calling，但平台规则、配送说明、售后规则等问题没有正式接入共享知识检索链路；
- 改动：将知识层重构为 `OperationsKnowledgeService` 与 `OperationsResourceCatalogService`，移除旧的 `EnterpriseKnowledgeBaseService`、`McpGatewayService` 文件；将管理端知识工具调整为 `list_operational_documents`、`search_operational_knowledge`、`list_resource_catalog`、`read_resource_detail`；新增用户端 `search_service_knowledge` 与 `read_service_resource`；在管理端和用户端对话主链路中自动注入知识检索上下文，形成“知识检索 + Tool Calling + SSE/非流式对话”的完整业务流程；
- 方法：基于 LangChain4j `Document` / `TextSegment` 保留文档抽象层，引入按段落优先切片、多信号排序、文档域分类、术语提取和上下文拼装，不再把知识检索暴露为实验性质的文件命名；资源目录继续保持只读桥接，避免越权写操作；
- 文件：`backend/sky-server/src/main/java/com/sky/service/ai/knowledge/OperationsKnowledgeService.java`、`backend/sky-server/src/main/java/com/sky/service/ai/mcp/OperationsResourceCatalogService.java`、`backend/sky-server/src/main/java/com/sky/service/ai/admin/AdminAiToolRegistry.java`、`backend/sky-server/src/main/java/com/sky/service/ai/admin/AdminAiToolExecutor.java`、`backend/sky-server/src/main/java/com/sky/service/ai/user/UserAiToolRegistry.java`、`backend/sky-server/src/main/java/com/sky/service/ai/user/UserAiToolExecutor.java`、`backend/sky-server/src/main/java/com/sky/service/impl/AdminAiChatServiceImpl.java`、`backend/sky-server/src/main/java/com/sky/service/impl/UserAiChatServiceImpl.java`、`docs/PROJECT_DEVELOPMENT_LOG.md`、`docs/SKY_TAKE_OUT_FULL_PROJECT_README.md`；
- 验证：待执行 `mvn compile -DskipTests`；
- 后续：继续升级为 embedding + 向量检索 + 重排，并补充知识源权限分层与持久化会话。
### 2026-07-06 · User AI / Review / Moderation / Service Backend Closure

- Scope: backend AI, review flow, sensitive-word moderation, customer service, database, and docs.
- Added `backend/database/ai_review_service.sql` and implemented the corresponding entities, mappers, services, controllers, and MyBatis mappings.
- Migrated user/admin AI sessions to database persistence with `ai_chat_session` and `ai_chat_message`.
- Implemented `/user/ai/recommend`, `/user/ai/review/write`, `/user/dish/{id}/reviews`, `/user/review`, `/user/review/{id}/like`, `/user/review/{id}`, `/user/order/{orderId}/review/status`, `/user/service/**`, `/admin/service/**`, and `/admin/sensitive-word/**`.
- Routed AI review drafting, review submission, and customer-service messages through sensitive-word checks.
- Added session-title and last-message truncation guards to avoid MySQL data truncation when long model outputs are persisted, and widened `ai_chat_session.title` / `last_message` in SQL scripts.
- Validation: `mvn compile` passed in `backend/`.
