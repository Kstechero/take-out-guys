# Takeout Guys AI 项目开发记录

> 当前统一仓库结构：`backend/` 为 Spring Boot 后端，`admin-web/` 为管理端，`user-app/` 为小程序，接口与维护文档统一放在 `docs/`。

> 本文档是项目的长期维护说明，面向开发者和 AI Agent。每次完成功能、修改架构、增加依赖或发现重要问题后，应更新对应章节。每日按时间顺序的开发记录请见 `PROJECT_DEVELOPMENT_LOG.md`。

## 1. 文档维护规则

### 1.1 更新原则

- 记录已经落地并验证的事实，不把规划写成已完成功能；
- 未完成内容统一放入“待办与已知限制”；
- 新增接口后按调用端同步更新 `USER_API_APIFOX.json` 或 `ADMIN_API_APIFOX.json`；
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

当前没有在 Java 工程中加入 LangChain4j、Spring AI 或其他 AI SDK。管理端和用户端 GX10 对话、健康检查与 Tool Calling 均基于原生 Java HTTP 调用实现。`/admin/ai/**` 与 `/user/ai/**` 当前都只保留 LLM 对话与 tool calling 主链路，旧的固定回答、关键词触发和直连流式分支已移除。两端工具统一返回结构化 JSON，管理端列表工具支持 `all=true` 以便后端自动分页拉取全量结果。AI 服务端代码已按 `ServiceImpl 编排层 + ToolRegistry + ToolExecutor + SessionManager` 的结构拆分；会话持久化和 RAG 仍待实现。

## 4. 重要架构决策

### 4.1 使用独立 Vue 3 管理端

原管理端是 Vue 2 + Vue CLI 工程。为承载新的 AI Agent 和品牌设计，新建 `sky-agent-admin-vue3`，不直接覆盖旧管理端，便于回退和对照功能。

### 4.2 开发环境不依赖 Nginx

Vite 开发服务器负责接口代理：

```text
浏览器 /api/**
    → Vite :5173
    → http://localhost:8080/admin/**
```

配置文件：`sky-agent-admin-vue3/vite.config.ts`。

这样本地联调不需要 Nginx，也不需要额外配置后端 CORS。生产环境仍可选择 Nginx、容器网关或其他反向代理。

### 4.3 沿用现有认证协议

- 管理端登录：`POST /admin/employee/login`；
- 管理端 Token 请求头：`token`；
- 用户端 Token 请求头：`authentication`；
- 统一响应：`{ code, msg, data }`。

### 4.4 GX10 密钥通过环境变量注入

后端配置位于：

```text
backend/sky-take-out/sky-server/src/main/resources/application.yml
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

AI 接口与普通业务接口使用同一套契约：用户侧归入 `USER_API_APIFOX.json`，管理侧归入 `ADMIN_API_APIFOX.json`；未实现的规划接口不进入正式 Apifox 文档。

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

- `USER_API_APIFOX.json`：用户端及支付回调接口；
- `ADMIN_API_APIFOX.json`：管理端接口（包括已实现的 AI 接口）；
- `PROJECT_DEVELOPMENT_LOG.md`：每日开发日志；
- `sky-agent-admin-vue3/`：新 Vue 3 管理端。

### 8.2 新管理端

```text
sky-agent-admin-vue3/
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
| GX10 vLLM | 已验证，待后端接入 | 大模型推理 |
| LangChain4j | 未加入 | 可选 Agent/RAG 框架 |
| Spring AI | 未加入 | 可选 AI 客户端框架 |

## 10. 验证基线

完成变更后至少执行：

### 10.1 后端

```powershell
cd backend\sky-take-out
mvn compile -DskipTests
```

### 10.2 新管理端

```powershell
cd sky-agent-admin-vue3
npm.cmd exec vue-tsc -- --noEmit -p tsconfig.app.json
```

开发运行：

```powershell
npm.cmd run dev
```

访问：`http://localhost:5173`。

### 10.3 AI 契约

```powershell
Get-Content -Raw -Encoding UTF8 USER_API_APIFOX.json | ConvertFrom-Json | Out-Null
Get-Content -Raw -Encoding UTF8 ADMIN_API_APIFOX.json | ConvertFrom-Json | Out-Null
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

- 管理端 AI 页面已有前端协议，但后端接口未实现；
- 用户端与管理端 AI 会话当前仅保存在服务进程内存中，多实例部署下不会共享；
- GX10 推理模型可能返回 `reasoning_content`，最终用户响应只应展示 `content`；
- 推理模型使用过小的 `max_tokens` 可能在正文生成前以 `length` 结束；
- `gx10.txt` 含明文凭据，存在安全风险；
- 菜品新增表单已经支持图片上传、分类和口味；套餐新增仍需实现套餐菜品组合表单；
- 优惠券、评价、敏感词和客服页面目前没有对应后端 Controller。
