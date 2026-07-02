# 苍穹外卖智能化扩展项目完整版说明

## 一、项目介绍

本项目是在基础版苍穹外卖系统之上进行的二次开发与智能化扩展项目。原始苍穹外卖系统已经具备基础外卖业务能力，包括用户端点餐、购物车、下单、订单管理、菜品管理、套餐管理、员工管理和后台运营管理等功能。本项目在此基础上，对前端界面进行了重构优化，并在业务功能和智能化能力方面进行了进一步拓展。

本项目新增了人工客服、AI 智能助手、AI 智能推荐、AI 帮写评价、AI 回复评论、优惠券、菜品详情页、敏感词过滤、堂食订单、个人中心等功能模块，使系统从传统外卖点餐平台升级为一个具备智能客服、智能推荐、内容生成、用户互动和多场景点餐能力的综合性外卖服务平台。

在 AI 能力方面，系统集成 LangChain4j 框架和阿里云通义千问模型，使用 qwen3-max 作为对话与内容生成模型，使用 text-embedding-v4 作为文本嵌入模型，并通过 DashScope API 接入大模型服务。同时，系统结合 RAG 检索增强生成技术和 Redis 向量库，实现基于知识库的智能客服问答、菜品推荐和业务咨询能力。

本项目不仅保留了传统外卖系统的核心业务流程，还引入了智能化交互和自动化辅助能力，提升了用户点餐体验、商家运营效率和平台服务能力。后续可继续扩展骑手端、商家运营分析、更加复杂的推荐算法和配送调度能力，使系统更加完整。

---

## 二、项目定位

本项目定位为：

**基于 Spring Boot 3、Vue 3、uni-app、LangChain4j 和通义千问的智能外卖服务平台。**

项目核心目标包括：

1. 对原有苍穹外卖项目前端进行重构优化；
2. 补充评价、优惠券、堂食、人工客服、个人中心等业务模块；
3. 接入大语言模型能力，实现 AI 智能客服、AI 智能推荐和 AI 帮写评价；
4. 通过 RAG 技术提升 AI 客服对平台规则、配送范围、售后政策等问题的回答准确性；
5. 使用 Redis 向量库实现知识检索；
6. 通过工具调用能力，让 AI 助手能够结合真实业务数据进行回答；
7. 提升项目完整度、智能化程度和实际业务可用性。

---

## 三、项目主要改造内容

相比基础版苍穹外卖系统，本项目主要改造内容如下：

1. 对管理端前端进行重构，使用 Vue 3、TypeScript、Vite、Element Plus、Pinia、ECharts、UnoCSS 和 SCSS 进行开发；
2. 对小程序端进行重构，使用 uni-app、Vue 3、TypeScript、uView Plus、Pinia、unistorage、UnoCSS 和 SCSS 进行开发；
3. 新增 AI 智能助手，支持用户进行菜品咨询、订单查询、配送咨询和售后咨询；
4. 新增 AI 智能推荐，根据用户自然语言需求推荐菜品；
5. 新增 AI 帮写评价和 AI 回复评论功能；
6. 新增菜品评价功能，支持图文评价、评分、点赞和删除；
7. 新增人工客服功能，实现 AI 客服与人工客服结合；
8. 新增优惠券系统，支持商家发券、用户领券和订单使用；
9. 新增菜品详情页，展示菜品图片、描述、价格、规格、评价和推荐菜品；
10. 新增堂食功能，支持堂食订单和取餐号；
11. 新增敏感词过滤功能，对用户评价、评论回复和客服消息进行内容安全检测；
12. 新增个人中心，提升用户端功能完整度；
13. 通过 Redis 缓存提升菜品、热门数据和知识检索访问效率；
14. 通过 RAG 技术提升 AI 客服回答的准确性和可信度。

---

## 四、后端技术栈

后端主要技术栈如下：

- 开发框架：Spring Boot 3
- ORM 框架：MyBatis-Plus
- 缓存框架：Spring Cache
- 数据库：MySQL 8.0
- 缓存数据库：Redis
- AI 框架：LangChain4j
- 大语言模型：阿里云通义千问 qwen3-max
- 嵌入模型：text-embedding-v4
- AI 接口：DashScope API
- 检索增强生成：RAG
- 向量检索：Redis 向量库
- 接口文档：Knife4j / Swagger
- 身份认证：JWT
- 文件存储：本地存储或对象存储，可根据实际部署环境调整

---

## 五、前端技术栈

### 5.1 管理端技术栈

管理端采用 Vue 3 技术体系进行重构，主要技术如下：

- 前端框架：Vue 3
- 开发语言：TypeScript
- 构建工具：Vite
- UI 组件库：Element Plus
- 状态管理：Pinia
- 图表可视化：ECharts
- 原子化 CSS：UnoCSS
- 样式预处理：SCSS
- HTTP 请求：Axios
- 路由管理：Vue Router

### 5.2 小程序端技术栈

小程序端采用 uni-app 技术体系进行开发，主要技术如下：

- 跨端框架：uni-app
- 前端框架：Vue 3
- 开发语言：TypeScript
- UI 组件库：uView Plus
- 状态管理：Pinia
- 数据持久化：unistorage
- 原子化 CSS：UnoCSS
- 样式预处理：SCSS
- 平台支持：微信小程序，可根据需要扩展到 H5 或 App

---

## 六、AI 技术栈

本项目 AI 部分主要包括智能客服、智能推荐、AI 内容生成和知识库检索能力。

AI 技术栈如下：

- AI 框架：LangChain4j
- 对话模型：阿里云通义千问 qwen3-max
- 嵌入模型：text-embedding-v4
- API 接口：DashScope API
- 智能客服：多轮对话、工具调用、RAG 知识库检索
- 智能推荐：基于 qwen3-max 的个性化菜品推荐
- 内容生成：AI 辅助评价撰写、评论回复生成
- 向量检索：text-embedding-v4 嵌入模型 + Redis 向量库
- 响应方式：支持普通响应和 SSE 流式响应
- 会话管理：保留多轮上下文，最多保留 20 条历史消息

---

## 七、系统整体架构

系统整体架构可以分为以下几层：

1. 用户交互层  
   包括管理端前端、小程序端、AI 助手页面、AI 推荐页面、人工客服页面、评价页面、优惠券页面等。

2. 接口服务层  
   由 Spring Boot 后端提供统一 REST API，负责接收前端请求并进行业务处理。

3. 业务服务层  
   包括用户服务、菜品服务、订单服务、购物车服务、优惠券服务、评价服务、客服服务、堂食服务、敏感词服务等。

4. AI 服务层  
   包括 AI 智能客服、AI 菜品推荐、AI 帮写评价、RAG 知识库检索、向量化服务、会话记忆和工具调用能力。

5. 数据存储层  
   包括 MySQL 8.0、Redis 缓存、Redis 向量库和文件存储。

系统处理流程示意如下：

```text
用户端 / 管理端
        ↓
前端页面交互
        ↓
Spring Boot 后端接口
        ↓
业务服务层
        ↓
MySQL / Redis / 文件存储

AI 相关流程：
用户自然语言输入
        ↓
AI 智能助手
        ↓
LangChain4j
        ↓
Qwen 大语言模型
        ↓
RAG 知识库检索 / 业务工具调用
        ↓
返回智能回答或业务处理结果
```

---

## 八、核心功能模块

## 8.1 AI 智能推荐菜品

### 功能概述

AI 智能推荐菜品功能基于大语言模型技术，根据用户的口味偏好、用餐场景、预算要求、食材要求等自然语言描述，从当前可售菜品库中智能推荐最合适的菜品。

用户不需要按照分类逐个浏览菜品，而是可以直接输入类似以下内容：

```text
我想吃辣一点，一个人，预算 30 元以内。
```

或：

```text
今天想吃清淡一点，适合减肥的菜。
```

系统会结合当前可售菜品数据和用户需求，生成推荐菜品列表，并为每道菜生成个性化推荐理由。

### 核心实现

1. 集成阿里云通义千问 qwen3-max 模型；
2. 使用专门的美食推荐系统提示词；
3. 从数据库中实时查询当前可售菜品；
4. 将菜品名称、价格、分类、描述、销量、热度等信息传入推荐逻辑；
5. 根据用户描述筛选符合需求的菜品；
6. 返回推荐菜品 ID、菜品名称、价格、图片和推荐理由；
7. 前端以卡片形式展示推荐结果；
8. 支持用户直接进入菜品详情页或加入购物车。

### 技术特点

- 使用 LangChain4j 框架集成 AI 服务；
- 使用系统提示词模板化管理；
- 支持自然语言理解；
- 支持场景化推荐；
- 推荐结果来自真实数据库，避免模型编造菜品；
- 推荐理由由大语言模型生成，提高推荐结果的可解释性；
- 可与用户历史订单、收藏、浏览记录结合，后续扩展为更完整的个性化推荐系统。

### 推荐结果字段

推荐接口返回结果建议包含：

- dishId：菜品 ID
- name：菜品名称
- image：菜品图片
- price：菜品价格
- categoryName：菜品分类
- reason：推荐理由
- description：菜品描述
- hotSpot：菜品热度值

---

## 8.2 AI 智能客服

### 功能概述

AI 智能客服助手“小慧”提供 7×24 小时在线智能客服服务，支持菜品咨询、订单查询、配送范围咨询、售后处理、优惠券规则咨询、堂食取餐咨询和人工客服转接等功能。

AI 智能客服不是简单的问答功能，而是结合了多轮对话、知识库检索和工具调用能力。对于平台规则类问题，系统通过 RAG 检索知识库后回答；对于订单类问题，系统调用订单相关接口获取真实订单数据；对于复杂售后和投诉类问题，系统引导用户转接人工客服。

### 核心功能

1. 智能菜品推荐  
   AI 助手可以理解用户的口味偏好、预算要求和用餐场景，并推荐合适菜品。

2. 订单查询与管理  
   AI 助手可以查询用户订单状态，包括待付款、待接单、已接单、派送中、已完成、已取消和退款等状态。

3. 订单信息展示  
   AI 助手可以展示订单 ID、订单编号、下单时间、订单状态、订单金额、配送状态等信息。

4. 订单取消  
   在业务规则允许的情况下，AI 助手可以引导用户取消订单。仅待付款、待接单状态的订单允许取消。

5. 配送咨询  
   AI 助手可以回答配送范围、配送时间和配送规则等问题。例如配送范围为广州市天河区正佳广场周边 3 公里内。

6. 售后咨询  
   AI 助手可以根据知识库回答售后规则，但不能越权承诺退款或赔偿。

7. 人工客服转接  
   当用户问题复杂、涉及投诉或 AI 无法确定答案时，系统自动引导用户转人工客服。人工客服工作时间为每天 10:00–22:00。

### 技术特点

- 基于阿里云通义千问 qwen3-max 模型；
- 使用 LangChain4j 实现 AI 服务集成；
- 支持多轮对话；
- 每个会话最多保留 20 条历史消息；
- 使用 SSE 实现流式响应；
- 结合 RAG 技术进行知识库检索；
- 使用 text-embedding-v4 对知识文本进行向量化；
- 使用 Redis 向量库进行相似度检索；
- 支持订单查询、菜品推荐、人工客服转接等工具能力；
- 对订单、退款、赔偿等敏感业务场景进行安全约束。

### 安全机制

AI 智能客服必须遵守以下规则：

1. 不编造订单信息；
2. 不编造菜品信息；
3. 不编造优惠券；
4. 不承诺退款；
5. 不承诺赔偿；
6. 不索取用户身份证号、银行卡号等敏感信息；
7. 不展示其他用户订单；
8. 不允许越权取消订单；
9. 不绕过业务规则；
10. 复杂问题应引导转人工客服。

---

## 8.3 AI 智能回复评价

### 功能概述

AI 智能回复评价功能用于辅助用户撰写菜品评价，也可辅助商家生成评论回复内容。用户在完成订单后，可以基于订单中的菜品列表，让 AI 自动生成一段自然、友好、有表达力的评价内容。

该功能可以降低用户评价门槛，提高评价提交率，同时提升菜品详情页中的评价内容丰富度。

### 核心功能

1. 评价帮写  
   用户可以输入简单描述，例如“很好吃，分量足”，系统自动生成完整评价。

2. 内容优化  
   用户已经输入部分评价时，AI 可以进行润色、续写或精简。

3. 字数控制  
   生成评价最多 140 字，保证简洁明了。

4. 表情增强  
   生成内容可以适当添加可爱表情或图标，提升评价的亲和力。

5. 结合菜品信息  
   AI 会结合订单中的菜品名称和特点生成更有针对性的评价。

6. 敏感词过滤  
   AI 生成内容在提交前必须经过敏感词检测，避免违规内容进入平台。

### 技术实现

- 基于 qwen3-max 模型理解菜品信息和用户情绪；
- 使用专门的美食评价提示词；
- 结合订单菜品列表生成评价；
- 对生成内容进行字数限制；
- 对生成内容进行敏感词检测；
- 前端提供帮写、润色、续写和精简按钮。

---

## 8.4 菜品评价功能

### 功能概述

菜品评价功能支持用户对已完成订单中的菜品进行评价，包括文字评价、图片上传、评分、评价展示、评价点赞和评价管理等。评价内容会展示在菜品详情页，供其他用户参考。

### 核心功能

1. 提交评价  
   用户在订单完成后，可以对订单中的菜品进行评价。

2. 图文评价  
   支持文字描述和图片上传，增强评价真实性。

3. 评分功能  
   用户可以对菜品进行评分。

4. 评价展示  
   菜品详情页展示真实用户评价。

5. 评价点赞  
   用户可以对有价值的评价进行点赞。

6. 评价查询  
   支持分页查询菜品评价。

7. 订单评价状态检查  
   系统可以检查订单是否已经评价，避免重复评价。

8. 删除评价  
   用户可以删除自己的评价。

9. 内容安全  
   评价提交时自动进行敏感词检测。

### 技术特点

- 评价数据持久化存储；
- 评价与用户、订单、菜品进行关联；
- 支持分页查询；
- 支持图片上传；
- 支持点赞统计；
- 支持敏感词过滤；
- 菜品详情页关联展示评价内容。

### 建议数据表

```text
dish_review
dish_review_like
```

---

## 8.5 人工客服功能

### 功能概述

人工客服功能用于处理 AI 客服无法解决的复杂问题、投诉建议、特殊售后和个性化需求，实现 AI 客服与人工客服结合的混合客服模式。

用户可以在 AI 助手中选择转人工，也可以直接进入人工客服入口创建会话。客服人员可以在管理端查看会话列表，回复用户消息，并结束会话。

### 核心功能

1. 会话创建  
   用户可以创建或获取自己的人工客服会话。

2. 实时通讯  
   支持用户与人工客服进行文字沟通。

3. 消息管理  
   支持查看会话历史消息，完整记录沟通过程。

4. 会话状态管理  
   支持会话进行中、已结束等状态。

5. 管理端客服工作台  
   客服人员可以查看用户会话、回复消息、结束会话。

6. 服务类型区分  
   系统明确区分 AI 客服和人工客服。

7. 工作时间管理  
   人工客服工作时间为每天 10:00–22:00。

### 技术特点

- 每个用户会话独立管理；
- 支持用户端和管理端消息同步；
- 可以先使用轮询方式实现消息刷新；
- 后续可以扩展 WebSocket；
- 支持会话状态管理；
- 与 AI 客服形成互补。

### 建议数据表

```text
customer_service_session
customer_service_message
```

---

## 8.6 优惠券功能

### 功能概述

优惠券功能实现完整的优惠券业务流程，包括商家创建优惠券、用户领取优惠券、查看我的优惠券、下单时选择可用优惠券、自动计算优惠金额以及使用后更新优惠券状态。

该功能可以增强平台营销能力，提高用户下单转化率。

### 核心功能

1. 优惠券查询  
   用户可以查询可领取的优惠券列表，也可以查看优惠券详情，包括优惠类型、使用门槛、有效期、库存等信息。

2. 优惠券领取  
   用户可以领取指定优惠券，系统需要控制领取次数和库存数量。

3. 我的优惠券  
   用户可以查看自己已领取的优惠券，并按未使用、已使用、已过期等状态筛选。

4. 订单可用优惠券查询  
   用户提交订单时，系统根据订单金额查询可用优惠券。

5. 自动计算优惠金额  
   系统根据优惠券类型自动计算优惠金额。

6. 下单使用优惠券  
   用户选择优惠券后，下单金额需要扣减对应优惠金额。

7. 状态更新  
   优惠券使用后标记为已使用，过期后标记为已过期。

8. 并发控制  
   防止优惠券超发和重复使用。

### 优惠券类型

1. 满减券  
   满指定金额减指定金额。

2. 折扣券  
   按指定折扣比例进行优惠。

3. 新人券  
   新用户专享优惠券。

### 技术特点

- 优惠券数据持久化存储；
- 用户优惠券单独记录；
- 支持领取限制；
- 支持库存控制；
- 支持订单金额匹配；
- 支持状态自动更新；
- 支持并发控制，避免超发。

### 建议数据表

```text
coupon
user_coupon
```

---

## 8.7 菜品详情页

### 功能概述

菜品详情页用于展示菜品完整信息，帮助用户全面了解菜品并做出点餐决策。详情页不仅展示基础菜品信息，还展示规格、口味、用户评价和推荐菜品。

### 核心功能

1. 菜品信息展示  
   展示菜品高清图片、名称、描述、价格、分类等信息。

2. 价格信息展示  
   支持原价、优惠价等信息展示。

3. 规格和口味选择  
   支持用户选择辣度、甜度、份量等规格或口味。

4. 评价展示  
   展示真实用户评价、评分和图文评价。

5. 推荐菜品展示  
   根据热度值、分类或 AI 推荐结果展示相关菜品。

6. 加入购物车  
   用户可以在详情页选择规格后加入购物车。

7. 图片懒加载  
   对菜品图片进行懒加载，优化页面性能。

8. 缓存优化  
   菜品数据可以缓存到 Redis，提高访问速度。

### 技术特点

- Redis 缓存菜品详情；
- 支持按 hotSpot 热度值排序；
- 支持图片懒加载；
- 支持规格选择；
- 与评价模块关联；
- 与购物车模块关联；
- 与 AI 推荐模块关联。

---

## 8.8 堂食功能

### 功能概述

堂食功能用于支持餐厅堂食点餐场景。堂食订单与外卖订单共用部分订单流程，但在订单类型、状态、配送逻辑和取餐方式上有所区别。

堂食订单不需要配送地址和配送流程，用户下单支付后，系统生成取餐号，商家制作完成后，用户凭取餐号取餐。

### 核心功能

1. 堂食订单类型  
   使用订单类型字段区分外卖订单和堂食订单。

2. 取餐号生成  
   用户下单后自动生成唯一取餐号。

3. 堂食订单状态  
   堂食订单使用独立状态进行标识，例如状态 8 表示已制作完成，等待取餐。

4. 扫码点餐  
   用户可以通过扫码进入堂食点餐页面。

5. 堂食订单提交  
   用户选择菜品后提交堂食订单。

6. 支付后生成取餐号  
   支付完成后生成取餐号并展示给用户。

7. 后厨制作完成  
   商家制作完成后更新订单状态。

8. 用户凭号取餐  
   用户凭取餐号到店取餐。

### 订单类型说明

```text
1：外卖订单
2：堂食订单
```

### 订单状态说明

```text
1：待付款
2：待接单
3：已接单
4：派送中
5：已完成
6：已取消
7：退款
8：堂食订单，已制作完成，等待取餐
```

### 技术特点

- 订单类型字段区分外卖和堂食；
- 堂食订单不进入配送流程；
- 使用雪花算法生成唯一取餐号；
- 堂食订单状态独立管理；
- 堂食订单仅在特定状态下可以标记完成。

---

## 8.9 敏感词过滤

### 功能概述

敏感词过滤功能用于维护平台内容安全。后台可以管理敏感词库，系统对用户评价、评论回复、人工客服消息和 AI 生成内容进行实时检测，防止违规内容发布。

### 核心功能

1. 敏感词管理  
   支持新增敏感词、修改敏感词、分页查询敏感词和批量删除敏感词。

2. 敏感词检测  
   用户提交评价、评论回复或客服消息时，系统自动检测是否包含敏感词。

3. AI 内容检测  
   AI 生成评价或评论回复后，也需要经过敏感词检测。

4. 内容拦截  
   如果检测到敏感词，系统阻止提交并提示用户修改。

5. 词库动态更新  
   管理端更新敏感词后，系统应实时生效，无需重启服务。

### 技术实现

- 使用 DFA 确定有限状态自动机算法进行敏感词匹配；
- 敏感词存储在数据库中；
- 系统启动或词库更新后加载敏感词；
- 支持精确匹配；
- 支持一定程度的模糊匹配；
- 支持评价、评论回复、客服消息和 AI 内容生成等多场景集成。

### 建议数据表

```text
sensitive_word
```

---

## 九、AI 智能助手设计

AI 智能助手是本项目智能化能力的核心。它主要服务于用户端，负责回答用户问题、推荐菜品、查询订单、解释平台规则和引导转人工客服。

### 9.1 助手名称

建议 AI 助手命名为：

```text
小慧
```

### 9.2 助手能力

AI 智能助手具备以下能力：

1. 多轮对话；
2. 菜品咨询；
3. 菜品推荐；
4. 订单查询；
5. 订单取消引导；
6. 配送范围咨询；
7. 售后规则咨询；
8. 优惠券规则咨询；
9. 堂食取餐咨询；
10. 人工客服转接；
11. 平台 FAQ 问答；
12. 基于知识库回答问题；
13. 基于真实业务数据回答问题。

### 9.3 会话管理

系统对 AI 对话进行会话管理：

- 每个用户可以有独立会话；
- 每个会话保存最近 20 条历史消息；
- 支持上下文连续理解；
- 支持清空会话；
- 支持会话超时；
- 支持普通响应和流式响应。

### 9.4 工具能力

AI 助手可以调用以下业务工具：

1. 菜品查询工具；
2. 菜品推荐工具；
3. 订单查询工具；
4. 订单取消工具；
5. 优惠券查询工具；
6. 人工客服转接工具；
7. 敏感词检测工具；
8. 知识库检索工具。

AI 助手不能直接操作数据库，必须通过封装好的后端服务进行业务调用。

---

## 十、RAG 知识库设计

RAG 知识库用于解决 AI 客服回答平台规则类问题时容易编造的问题。系统将平台规则、配送范围、售后说明、优惠券规则、堂食规则等内容整理成知识库文档，经过向量化后存入 Redis 向量库。

### 10.1 知识库内容

知识库建议包含以下内容：

1. 平台介绍；
2. 配送范围；
3. 配送时间；
4. 人工客服工作时间；
5. 订单取消规则；
6. 退款规则；
7. 优惠券使用规则；
8. 堂食取餐规则；
9. 用户评价规则；
10. 常见问题；
11. 售后处理说明；
12. 隐私保护说明。

### 10.2 RAG 流程

```text
知识库文本
        ↓
文本切分
        ↓
text-embedding-v4 向量化
        ↓
存入 Redis 向量库
        ↓
用户提出问题
        ↓
向量相似度检索
        ↓
获取相关知识片段
        ↓
拼接提示词上下文
        ↓
qwen3-max 生成回答
        ↓
返回用户
```

### 10.3 技术特点

- 使用 text-embedding-v4 进行文本向量化；
- 使用 Redis 存储向量数据；
- 支持相似度检索；
- 支持知识库动态更新；
- 提高回答准确性；
- 降低模型幻觉；
- 适合配送、售后、优惠券和平台规则类问答。

---

## 十一、前端页面设计

## 11.1 小程序端页面

小程序端面向普通用户，主要用于点餐、下单、查询订单、使用 AI 助手和查看个人信息。

小程序端主要页面包括：

1. 首页；
2. 菜品分类页；
3. 菜品列表页；
4. 菜品详情页；
5. 购物车页；
6. 订单确认页；
7. 支付结果页；
8. 订单列表页；
9. 订单详情页；
10. AI 助手页；
11. AI 推荐页；
12. 评价撰写页；
13. 优惠券中心；
14. 我的优惠券；
15. 人工客服页；
16. 堂食点餐页；
17. 个人中心。

### 小程序端重点体验

1. 首页展示热门菜品、推荐菜品和优惠券入口；
2. 菜品详情页展示图片、价格、描述、规格和评价；
3. AI 推荐页支持自然语言输入并返回菜品卡片；
4. AI 助手页支持聊天气泡、流式输出和快捷问题；
5. 订单页支持订单状态查看；
6. 评价页支持 AI 帮写；
7. 个人中心展示用户信息、订单、优惠券和客服入口。

---

## 11.2 管理端页面

管理端面向商家或平台管理员，主要用于菜品、订单、优惠券、评价、敏感词和客服管理。

管理端主要页面包括：

1. 登录页；
2. 后台首页；
3. 数据看板；
4. 菜品管理；
5. 分类管理；
6. 套餐管理；
7. 订单管理；
8. 优惠券管理；
9. 评价管理；
10. 敏感词管理；
11. 人工客服工作台；
12. 用户管理；
13. 系统设置。

### 管理端重点体验

1. 后台首页展示营业额、订单数、热门菜品等统计数据；
2. 菜品管理支持新增、修改、上下架；
3. 优惠券管理支持发放和状态查看；
4. 敏感词管理支持新增、修改、删除；
5. 客服工作台支持查看会话和回复用户；
6. 评价管理支持查看用户评价和处理违规内容。

---

## 十二、数据库设计建议

在原有苍穹外卖数据库基础上，本项目建议新增或扩展以下数据表。

### 12.1 菜品评价表

```text
dish_review
```

主要字段：

- id
- user_id
- order_id
- dish_id
- rating
- content
- images
- like_count
- status
- create_time
- update_time

### 12.2 评价点赞表

```text
dish_review_like
```

主要字段：

- id
- review_id
- user_id
- create_time

### 12.3 优惠券表

```text
coupon
```

主要字段：

- id
- name
- type
- threshold_amount
- discount_amount
- discount_rate
- total_stock
- remaining_stock
- receive_limit
- start_time
- end_time
- status
- create_time
- update_time

### 12.4 用户优惠券表

```text
user_coupon
```

主要字段：

- id
- user_id
- coupon_id
- status
- receive_time
- use_time
- order_id
- expire_time

### 12.5 人工客服会话表

```text
customer_service_session
```

主要字段：

- id
- user_id
- admin_id
- type
- status
- last_message
- last_message_time
- create_time
- end_time

### 12.6 人工客服消息表

```text
customer_service_message
```

主要字段：

- id
- session_id
- sender_id
- sender_type
- content
- message_type
- read_status
- create_time

### 12.7 敏感词表

```text
sensitive_word
```

主要字段：

- id
- word
- category
- status
- create_time
- update_time

### 12.8 AI 会话表

```text
ai_chat_session
```

主要字段：

- id
- user_id
- title
- status
- create_time
- update_time

### 12.9 AI 消息表

```text
ai_chat_message
```

主要字段：

- id
- session_id
- user_id
- role
- content
- message_type
- create_time

### 12.10 订单表扩展字段

在原订单表中建议增加：

- order_type：订单类型，1 表示外卖，2 表示堂食；
- pickup_number：堂食取餐号；
- coupon_id：使用的优惠券 ID；
- discount_amount：优惠金额；
- original_amount：原始金额；
- actual_amount：实付金额。

---

## 十三、接口设计建议

## 13.1 AI 相关接口

```text
POST /user/ai/chat
GET  /user/ai/chat/stream
POST /user/ai/recommend
POST /user/ai/review/write
GET  /user/ai/session/list
DELETE /user/ai/session/{sessionId}
```

## 13.2 菜品详情和评价接口

```text
GET  /user/dish/{id}
GET  /user/dish/{id}/reviews
POST /user/review
POST /user/review/{id}/like
DELETE /user/review/{id}
GET  /user/order/{orderId}/review/status
```

## 13.3 优惠券接口

```text
GET  /user/coupon/available
POST /user/coupon/receive/{couponId}
GET  /user/coupon/my
GET  /user/coupon/order/available
POST /admin/coupon
GET  /admin/coupon/page
PUT  /admin/coupon/{id}
DELETE /admin/coupon/{id}
```

## 13.4 人工客服接口

```text
POST /user/service/session
GET  /user/service/session/current
POST /user/service/message
GET  /user/service/message/list
POST /user/service/session/end

GET  /admin/service/session/page
GET  /admin/service/message/list
POST /admin/service/message/reply
POST /admin/service/session/end
```

## 13.5 敏感词接口

```text
POST /admin/sensitive-word
GET  /admin/sensitive-word/page
PUT  /admin/sensitive-word/{id}
DELETE /admin/sensitive-word/batch
POST /admin/sensitive-word/check
```

## 13.6 堂食接口

```text
POST /user/dine-in/order/submit
GET  /user/dine-in/order/{id}
GET  /user/dine-in/order/list
POST /admin/dine-in/order/complete
```

---

## 十四、业务流程设计

### 14.1 AI 推荐流程

```text
用户输入用餐需求
        ↓
后端查询当前可售菜品
        ↓
构造推荐提示词
        ↓
调用 qwen3-max
        ↓
返回菜品 ID 和推荐理由
        ↓
后端根据 ID 查询真实菜品详情
        ↓
前端展示推荐菜品卡片
```

### 14.2 AI 客服订单查询流程

```text
用户询问订单状态
        ↓
AI 判断需要查询订单
        ↓
调用订单查询工具
        ↓
后端校验当前用户身份
        ↓
查询用户自己的订单
        ↓
返回订单状态和订单信息
        ↓
AI 组织自然语言回复
```

### 14.3 AI 帮写评价流程

```text
用户选择已完成订单
        ↓
读取订单菜品列表
        ↓
用户输入简单评价要求
        ↓
调用 qwen3-max 生成评价
        ↓
限制 140 字以内
        ↓
敏感词过滤
        ↓
用户确认提交评价
```

### 14.4 优惠券使用流程

```text
用户进入优惠券中心
        ↓
领取优惠券
        ↓
进入订单确认页
        ↓
系统查询可用优惠券
        ↓
用户选择优惠券
        ↓
系统计算优惠金额
        ↓
提交订单
        ↓
优惠券状态更新为已使用
```

### 14.5 堂食订单流程

```text
用户扫码进入堂食点餐页
        ↓
选择菜品
        ↓
提交堂食订单
        ↓
支付成功
        ↓
系统生成取餐号
        ↓
商家制作
        ↓
制作完成后更新状态
        ↓
用户凭取餐号取餐
```

### 14.6 人工客服流程

```text
用户发起人工客服
        ↓
创建或获取客服会话
        ↓
用户发送消息
        ↓
客服后台查看消息
        ↓
客服回复
        ↓
双方查看历史记录
        ↓
问题解决后结束会话
```

---

## 十五、项目亮点

本项目主要亮点包括：

1. 在传统苍穹外卖系统基础上进行智能化重构；
2. 接入 LangChain4j 和通义千问大模型；
3. 实现 AI 智能客服，支持多轮对话和业务咨询；
4. 使用 RAG 技术增强知识库问答能力；
5. 使用 Redis 向量库实现知识检索；
6. 实现 AI 智能推荐菜品，根据自然语言需求推荐真实菜品；
7. 实现 AI 帮写评价，提升用户评价体验；
8. 新增完整菜品评价系统，支持图文、评分和点赞；
9. 新增优惠券系统，增强营销能力；
10. 新增人工客服系统，实现 AI 与人工结合；
11. 新增堂食功能，覆盖到店点餐场景；
12. 新增敏感词过滤，提高内容安全；
13. 重构管理端和小程序端，提升前端体验；
14. 通过缓存优化提高访问效率；
15. 功能覆盖用户端、管理端、AI 服务和运营管理多个场景。

---

## 十六、后续扩展方向

本项目后续可以继续拓展以下方向：

1. 增加骑手端，实现骑手接单、配送状态更新和配送轨迹展示；
2. 增加更复杂的推荐算法，如协同过滤、用户画像推荐和混合推荐；
3. 增加商家运营分析功能，让 AI 分析营业额、订单量和菜品销量；
4. 增加库存管理和备菜预测；
5. 增加会员体系和积分系统；
6. 增加秒杀、满减活动和组合套餐营销；
7. 增加 WebSocket，实现更实时的人工客服通信；
8. 增加订单异常检测和自动处理；
9. 增加多门店管理；
10. 增加移动端 App 适配；
11. 增加自动化测试和接口压力测试；
12. 增加日志监控和系统告警。

---

## 十七、项目总结

本项目在基础苍穹外卖系统之上进行了完整的智能化扩展和前端重构。系统不仅保留了传统外卖平台的核心点餐、下单和管理能力，还新增了 AI 智能助手、AI 智能推荐、AI 帮写评价、人工客服、优惠券、菜品详情页、堂食订单、敏感词过滤和个人中心等功能。

通过 LangChain4j、通义千问 qwen3-max、text-embedding-v4、DashScope API、RAG 和 Redis 向量库等技术，项目实现了从普通外卖系统到智能外卖服务平台的升级。该系统具备较强的业务完整性、技术综合性和展示价值，适合作为 Java 后端开发、全栈开发或 AI 应用开发方向的项目作品。

---

# Complete Project Description of Sky Take-Out Intelligent Extension System

## 1. Project Introduction

This project is a secondary development and intelligent extension of the basic Sky Take-Out system. The original Sky Take-Out system already provides core food delivery business capabilities, including user ordering, shopping cart, order placement, order management, dish management, set meal management, employee management, and backend operation management.

Based on the original system, this project refactors and optimizes the frontend and further extends both business functions and AI capabilities.

The project adds human customer service, AI assistant, AI dish recommendation, AI review writing, AI comment reply generation, coupon system, dish detail page, sensitive word filtering, dine-in ordering, personal center, and other modules. As a result, the system is upgraded from a traditional food delivery platform into a comprehensive intelligent food delivery service platform with intelligent customer service, intelligent recommendation, content generation, user interaction, and multi-scenario ordering capabilities.

In terms of AI capabilities, the system integrates LangChain4j and Alibaba Cloud Tongyi Qianwen. It uses qwen3-max as the dialogue and content generation model, text-embedding-v4 as the text embedding model, and DashScope API to access large language model services. In addition, the system combines RAG technology and Redis vector store to implement knowledge-base-driven intelligent customer service, dish recommendation, and business consultation.

This project not only preserves the core business process of a traditional food delivery system, but also introduces intelligent interaction and automated assistance, improving user ordering experience, merchant operation efficiency, and platform service capability. In the future, the system can be further extended with a rider-side application, merchant operation analysis, more advanced recommendation algorithms, and delivery scheduling.

---

## 2. Project Positioning

This project is positioned as:

**An intelligent food delivery service platform based on Spring Boot 3, Vue 3, uni-app, LangChain4j, and Tongyi Qianwen.**

The core goals of the project include:

1. Refactor and optimize the frontend of the original Sky Take-Out project;
2. Add business modules such as review, coupon, dine-in ordering, human customer service, and personal center;
3. Integrate large language model capabilities to implement AI customer service, AI dish recommendation, and AI review writing;
4. Use RAG technology to improve the accuracy of AI customer service answers about platform rules, delivery scope, and after-sales policies;
5. Use Redis vector store for knowledge retrieval;
6. Enable the AI assistant to answer based on real business data through tool calling;
7. Improve project completeness, intelligence, and practical business usability.

---

## 3. Main Refactoring and Extension Content

Compared with the basic Sky Take-Out system, this project mainly includes the following improvements:

1. Refactor the admin frontend using Vue 3, TypeScript, Vite, Element Plus, Pinia, ECharts, UnoCSS, and SCSS;
2. Refactor the mini program frontend using uni-app, Vue 3, TypeScript, uView Plus, Pinia, unistorage, UnoCSS, and SCSS;
3. Add an AI assistant for dish consultation, order query, delivery consultation, and after-sales consultation;
4. Add AI dish recommendation based on natural language user requirements;
5. Add AI review writing and AI comment reply generation;
6. Add a dish review system with text review, image review, rating, like, and delete features;
7. Add human customer service to combine AI service with human service;
8. Add a coupon system supporting coupon issuing, claiming, and order usage;
9. Add a dish detail page displaying dish images, description, price, specifications, reviews, and recommended dishes;
10. Add dine-in ordering with dine-in orders and pickup numbers;
11. Add sensitive word filtering for reviews, comment replies, and customer service messages;
12. Add personal center to improve user-side functionality;
13. Use Redis cache to improve access efficiency for dishes, hot data, and knowledge retrieval;
14. Use RAG technology to improve the accuracy and reliability of AI customer service answers.

---

## 4. Backend Technology Stack

The backend technology stack includes:

- Framework: Spring Boot 3
- ORM framework: MyBatis-Plus
- Cache framework: Spring Cache
- Database: MySQL 8.0
- Cache database: Redis
- AI framework: LangChain4j
- Large language model: Alibaba Cloud Tongyi Qianwen qwen3-max
- Embedding model: text-embedding-v4
- AI API: DashScope API
- Retrieval augmented generation: RAG
- Vector retrieval: Redis vector store
- API documentation: Knife4j / Swagger
- Authentication: JWT
- File storage: local storage or object storage depending on the deployment environment

---

## 5. Frontend Technology Stack

### 5.1 Admin Frontend

The admin frontend is refactored using the Vue 3 ecosystem:

- Frontend framework: Vue 3
- Programming language: TypeScript
- Build tool: Vite
- UI library: Element Plus
- State management: Pinia
- Chart visualization: ECharts
- Atomic CSS: UnoCSS
- CSS preprocessor: SCSS
- HTTP request: Axios
- Routing: Vue Router

### 5.2 Mini Program Frontend

The mini program frontend is developed using uni-app:

- Cross-platform framework: uni-app
- Frontend framework: Vue 3
- Programming language: TypeScript
- UI library: uView Plus
- State management: Pinia
- Data persistence: unistorage
- Atomic CSS: UnoCSS
- CSS preprocessor: SCSS
- Platform support: WeChat Mini Program, with possible extension to H5 or App

---

## 6. AI Technology Stack

The AI part of this project mainly includes intelligent customer service, intelligent recommendation, AI content generation, and knowledge base retrieval.

The AI technology stack includes:

- AI framework: LangChain4j
- Dialogue model: Alibaba Cloud Tongyi Qianwen qwen3-max
- Embedding model: text-embedding-v4
- API service: DashScope API
- Intelligent customer service: multi-turn dialogue, tool calling, and RAG knowledge retrieval
- Intelligent recommendation: personalized dish recommendation based on qwen3-max
- Content generation: AI-assisted review writing and comment reply generation
- Vector retrieval: text-embedding-v4 embedding model and Redis vector store
- Response method: normal response and SSE streaming response
- Session management: multi-turn context with up to 20 historical messages

---

## 7. Overall System Architecture

The system architecture can be divided into the following layers:

1. User interaction layer  
   This includes the admin frontend, mini program frontend, AI assistant page, AI recommendation page, human customer service page, review page, and coupon page.

2. API service layer  
   The Spring Boot backend provides REST APIs to receive frontend requests and process business logic.

3. Business service layer  
   This includes user service, dish service, order service, shopping cart service, coupon service, review service, customer service, dine-in service, and sensitive word service.

4. AI service layer  
   This includes AI customer service, AI dish recommendation, AI review writing, RAG knowledge retrieval, embedding service, conversation memory, and tool calling.

5. Data storage layer  
   This includes MySQL 8.0, Redis cache, Redis vector store, and file storage.

System process:

```text
User frontend / Admin frontend
        ↓
Frontend interaction
        ↓
Spring Boot backend API
        ↓
Business service layer
        ↓
MySQL / Redis / File storage

AI-related process:
User natural language input
        ↓
AI assistant
        ↓
LangChain4j
        ↓
Qwen large language model
        ↓
RAG knowledge retrieval / business tool calling
        ↓
Intelligent answer or business result
```

---

## 8. Core Functional Modules

## 8.1 AI Dish Recommendation

### Function Overview

The AI dish recommendation function uses large language model technology to recommend suitable dishes from the current available dish database based on the user's taste preference, dining scenario, budget, and ingredient requirements expressed in natural language.

Users do not need to browse dishes category by category. They can directly enter requirements such as:

```text
I want something spicy, for one person, within 30 yuan.
```

Or:

```text
I want something light today, suitable for weight control.
```

The system will combine current available dish data and user requirements to generate a recommended dish list, along with personalized recommendation reasons for each dish.

### Core Implementation

1. Integrate Alibaba Cloud Tongyi Qianwen qwen3-max;
2. Use a professional food recommendation prompt;
3. Query current available dishes from the database in real time;
4. Pass dish name, price, category, description, sales volume, and hot value into the recommendation logic;
5. Filter dishes according to the user's description;
6. Return dish ID, dish name, price, image, and recommendation reason;
7. Display recommendation results as cards on the frontend;
8. Support entering dish detail pages or adding dishes to the shopping cart.

### Technical Features

- Uses LangChain4j to integrate AI services;
- Uses template-based system prompts;
- Supports natural language understanding;
- Supports scenario-based recommendation;
- Recommended dishes must come from the real database;
- Recommendation reasons are generated by the large language model;
- Can be further extended with user order history, favorites, and browsing history for personalized recommendation.

### Recommended Result Fields

The recommendation API should return:

- dishId: dish ID
- name: dish name
- image: dish image
- price: dish price
- categoryName: dish category
- reason: recommendation reason
- description: dish description
- hotSpot: dish hot value

---

## 8.2 AI Intelligent Customer Service

### Function Overview

The AI customer service assistant "Xiaohui" provides 24/7 online intelligent customer service. It supports dish consultation, order query, delivery range consultation, after-sales guidance, coupon rule consultation, dine-in pickup consultation, and human service transfer.

The AI customer service is not a simple Q&A feature. It combines multi-turn dialogue, knowledge base retrieval, and tool calling. For platform rule questions, the system answers based on RAG retrieval. For order-related questions, the system calls order APIs to obtain real order data. For complex after-sales or complaint issues, the system guides the user to human customer service.

### Core Functions

1. Intelligent dish recommendation  
   The AI assistant can understand user taste preferences, budget, and dining scenarios, then recommend suitable dishes.

2. Order query and management  
   The AI assistant can query order statuses, including pending payment, pending acceptance, accepted, delivering, completed, cancelled, and refunded.

3. Order information display  
   The AI assistant can display order ID, order number, order time, order status, order amount, and delivery status.

4. Order cancellation  
   When allowed by business rules, the AI assistant can guide the user to cancel an order. Only orders in pending payment or pending acceptance status can be cancelled.

5. Delivery consultation  
   The AI assistant can answer questions about delivery range, delivery time, and delivery rules. For example, the delivery range is within 3 kilometers around Grandview Mall in Tianhe District, Guangzhou.

6. After-sales consultation  
   The AI assistant can answer after-sales rules based on the knowledge base, but cannot promise refunds or compensation without authorization.

7. Human service transfer  
   When a user issue is complex, involves complaints, or cannot be determined by AI, the system guides the user to human customer service. Human service working hours are 10:00–22:00 every day.

### Technical Features

- Based on Alibaba Cloud Tongyi Qianwen qwen3-max;
- Uses LangChain4j to integrate AI services;
- Supports multi-turn dialogue;
- Each session keeps up to 20 historical messages;
- Uses SSE for streaming response;
- Uses RAG for knowledge base retrieval;
- Uses text-embedding-v4 for knowledge text embedding;
- Uses Redis vector store for similarity retrieval;
- Supports tools such as order query, dish recommendation, and human service transfer;
- Applies safety constraints to sensitive business scenarios such as orders, refunds, and compensation.

### Safety Mechanisms

The AI customer service must follow these rules:

1. Do not fabricate order information;
2. Do not fabricate dish information;
3. Do not fabricate coupons;
4. Do not promise refunds;
5. Do not promise compensation;
6. Do not ask for sensitive information such as ID card numbers or bank card numbers;
7. Do not display other users' orders;
8. Do not allow unauthorized order cancellation;
9. Do not bypass business rules;
10. Guide complex issues to human customer service.

---

## 8.3 AI Review and Comment Reply Generation

### Function Overview

The AI review and comment reply function assists users in writing dish reviews and can also assist merchants in generating comment replies. After completing an order, users can use AI to generate a natural, friendly, and expressive review based on the dishes in the order.

This feature reduces the difficulty of writing reviews, increases review submission rate, and enriches the review content displayed on dish detail pages.

### Core Functions

1. Review writing  
   Users can input simple descriptions such as "delicious and generous portion", and the system generates a complete review.

2. Content optimization  
   If the user has already written partial content, AI can polish, continue, or shorten it.

3. Word count control  
   The generated review should be no more than 140 characters.

4. Emoji enhancement  
   The generated content can include appropriate emojis to improve friendliness.

5. Dish information integration  
   AI generates more targeted reviews based on dish names and features from the order.

6. Sensitive word filtering  
   AI-generated content must pass sensitive word detection before submission.

### Technical Implementation

- Uses qwen3-max to understand dish information and user sentiment;
- Uses a dedicated food review prompt;
- Generates reviews based on order dish lists;
- Applies character length limitation;
- Performs sensitive word detection;
- Provides buttons such as write, polish, continue, and shorten on the frontend.

---

## 8.4 Dish Review System

### Function Overview

The dish review system allows users to review dishes from completed orders. It supports text review, image upload, rating, review display, review likes, and review management. Reviews are displayed on the dish detail page for other users to reference.

### Core Functions

1. Submit review  
   Users can review dishes after the order is completed.

2. Text and image review  
   Supports both text descriptions and image uploads.

3. Rating  
   Users can rate dishes.

4. Review display  
   Dish detail pages display real user reviews.

5. Review like  
   Users can like valuable reviews.

6. Review query  
   Supports paginated query of dish reviews.

7. Order review status check  
   The system can check whether an order has already been reviewed to avoid duplicate reviews.

8. Delete review  
   Users can delete their own reviews.

9. Content safety  
   Sensitive word detection is performed when submitting reviews.

### Technical Features

- Persistent storage of review data;
- Reviews are associated with users, orders, and dishes;
- Supports paginated query;
- Supports image upload;
- Supports like count;
- Supports sensitive word filtering;
- Reviews are displayed on the dish detail page.

### Suggested Tables

```text
dish_review
dish_review_like
```

---

## 8.5 Human Customer Service

### Function Overview

The human customer service function is used to handle complex issues, complaints, special after-sales requests, and personalized needs that AI customer service cannot solve. It implements a hybrid customer service model combining AI and human support.

Users can choose to transfer to human service from the AI assistant or directly create a human service session. Customer service staff can view session lists, reply to user messages, and end sessions in the admin frontend.

### Core Functions

1. Session creation  
   Users can create or retrieve their own human customer service session.

2. Real-time communication  
   Supports text communication between users and human customer service.

3. Message management  
   Supports viewing historical messages and keeping complete communication records.

4. Session status management  
   Supports statuses such as ongoing and ended.

5. Admin customer service workbench  
   Customer service staff can view user sessions, reply to messages, and end sessions.

6. Service type distinction  
   The system clearly distinguishes AI customer service and human customer service.

7. Working hour management  
   Human customer service working hours are 10:00–22:00 every day.

### Technical Features

- Each user session is managed independently;
- Supports message synchronization between user side and admin side;
- Polling can be used first for message refresh;
- WebSocket can be added later;
- Supports session status management;
- Works as a complement to AI customer service.

### Suggested Tables

```text
customer_service_session
customer_service_message
```

---

## 8.6 Coupon System

### Function Overview

The coupon system implements a complete coupon business process, including coupon creation by merchants, coupon claiming by users, viewing my coupons, selecting available coupons during order placement, automatically calculating discount amounts, and updating coupon status after usage.

This feature improves platform marketing capability and increases order conversion rate.

### Core Functions

1. Coupon query  
   Users can query claimable coupons and view coupon details, including coupon type, threshold, validity period, and stock.

2. Coupon claiming  
   Users can claim specific coupons. The system must control claim limits and stock.

3. My coupons  
   Users can view their claimed coupons and filter them by unused, used, and expired status.

4. Query available coupons for orders  
   When users submit an order, the system queries available coupons based on the order amount.

5. Automatic discount calculation  
   The system automatically calculates the discount amount based on coupon type.

6. Use coupon during order placement  
   After users select a coupon, the order amount should be reduced accordingly.

7. Status update  
   After a coupon is used, it is marked as used. Expired coupons are marked as expired.

8. Concurrency control  
   Prevent coupon over-issuing and repeated usage.

### Coupon Types

1. Full reduction coupon  
   Reduce a specified amount after reaching a specified threshold.

2. Discount coupon  
   Apply a specified discount rate.

3. New user coupon  
   Exclusive coupon for new users.

### Technical Features

- Persistent storage of coupon data;
- Separate records for user coupons;
- Supports claim limits;
- Supports stock control;
- Supports order amount matching;
- Supports automatic status update;
- Supports concurrency control to prevent over-issuing.

### Suggested Tables

```text
coupon
user_coupon
```

---

## 8.7 Dish Detail Page

### Function Overview

The dish detail page displays complete dish information and helps users make ordering decisions. It shows not only basic dish information, but also specifications, flavors, user reviews, and recommended dishes.

### Core Functions

1. Dish information display  
   Displays high-quality dish images, name, description, price, and category.

2. Price display  
   Supports original price and discounted price.

3. Specification and flavor selection  
   Supports selecting spiciness, sweetness, portion size, and other specifications or flavors.

4. Review display  
   Displays real user reviews, ratings, and image reviews.

5. Recommended dishes display  
   Displays related dishes based on hot value, category, or AI recommendation results.

6. Add to cart  
   Users can select specifications and add dishes to the shopping cart.

7. Image lazy loading  
   Uses lazy loading for dish images to optimize page performance.

8. Cache optimization  
   Dish data can be cached in Redis to improve access speed.

### Technical Features

- Redis cache for dish details;
- Supports sorting by hotSpot value;
- Supports image lazy loading;
- Supports specification selection;
- Integrated with review module;
- Integrated with shopping cart module;
- Integrated with AI recommendation module.

---

## 8.8 Dine-In Ordering

### Function Overview

The dine-in ordering function supports restaurant dine-in scenarios. Dine-in orders share part of the order process with delivery orders, but differ in order type, status, delivery logic, and pickup method.

Dine-in orders do not require delivery address or delivery process. After ordering and payment, the system generates a pickup number. After the merchant finishes preparing the food, the user picks up the meal using the pickup number.

### Core Functions

1. Dine-in order type  
   Use an order type field to distinguish delivery orders and dine-in orders.

2. Pickup number generation  
   Automatically generate a unique pickup number after the user places an order.

3. Dine-in order status  
   Use an independent status for dine-in orders. For example, status 8 indicates that the food is prepared and waiting for pickup.

4. QR code ordering  
   Users can scan a QR code to enter the dine-in ordering page.

5. Dine-in order submission  
   Users select dishes and submit dine-in orders.

6. Generate pickup number after payment  
   Generate and display pickup number after payment is completed.

7. Kitchen preparation completion  
   The merchant updates order status after preparation is completed.

8. Pickup by number  
   Users pick up food at the restaurant using the pickup number.

### Order Type Description

```text
1: Delivery order
2: Dine-in order
```

### Order Status Description

```text
1: Pending payment
2: Pending acceptance
3: Accepted
4: Delivering
5: Completed
6: Cancelled
7: Refunded
8: Dine-in order, prepared and waiting for pickup
```

### Technical Features

- Uses order type field to distinguish delivery and dine-in orders;
- Dine-in orders do not enter delivery process;
- Uses Snowflake algorithm to generate unique pickup numbers;
- Dine-in order status is managed independently;
- Dine-in orders can only be completed under specific status.

---

## 8.9 Sensitive Word Filtering

### Function Overview

The sensitive word filtering function maintains platform content safety. Admin users can manage a sensitive word library, and the system detects reviews, comment replies, human customer service messages, and AI-generated content in real time to prevent illegal or inappropriate content from being published.

### Core Functions

1. Sensitive word management  
   Supports adding, updating, paginated querying, and batch deleting sensitive words.

2. Sensitive word detection  
   When users submit reviews, comment replies, or customer service messages, the system automatically detects whether sensitive words are included.

3. AI content detection  
   AI-generated reviews or comment replies also need to pass sensitive word detection.

4. Content blocking  
   If sensitive words are detected, the system blocks submission and prompts the user to modify the content.

5. Dynamic word library update  
   After the sensitive word library is updated in the admin frontend, it should take effect in real time without restarting the system.

### Technical Implementation

- Uses DFA algorithm for sensitive word matching;
- Sensitive words are stored in the database;
- Sensitive words are loaded when the system starts or when the word library is updated;
- Supports exact matching;
- Supports a certain degree of fuzzy matching;
- Supports integration across reviews, comment replies, customer service messages, and AI content generation.

### Suggested Table

```text
sensitive_word
```

---

## 9. AI Assistant Design

The AI assistant is the core of the intelligent capabilities in this project. It mainly serves users and is responsible for answering questions, recommending dishes, querying orders, explaining platform rules, and guiding users to human customer service.

### 9.1 Assistant Name

The recommended AI assistant name is:

```text
Xiaohui
```

### 9.2 Assistant Capabilities

The AI assistant has the following capabilities:

1. Multi-turn dialogue;
2. Dish consultation;
3. Dish recommendation;
4. Order query;
5. Order cancellation guidance;
6. Delivery range consultation;
7. After-sales rule consultation;
8. Coupon rule consultation;
9. Dine-in pickup consultation;
10. Human customer service transfer;
11. Platform FAQ answering;
12. Knowledge-base-based answering;
13. Answering based on real business data.

### 9.3 Session Management

The system manages AI conversations as follows:

- Each user can have an independent session;
- Each session keeps the latest 20 historical messages;
- Supports continuous context understanding;
- Supports clearing sessions;
- Supports session timeout;
- Supports normal response and streaming response.

### 9.4 Tool Capabilities

The AI assistant can call the following business tools:

1. Dish query tool;
2. Dish recommendation tool;
3. Order query tool;
4. Order cancellation tool;
5. Coupon query tool;
6. Human customer service transfer tool;
7. Sensitive word detection tool;
8. Knowledge base retrieval tool.

The AI assistant must not directly operate the database. It must call backend services through well-defined service methods.

---

## 10. RAG Knowledge Base Design

The RAG knowledge base is used to reduce hallucination when the AI customer service answers platform rule questions. The system organizes platform rules, delivery range, after-sales instructions, coupon rules, and dine-in rules into knowledge base documents, vectorizes them, and stores them in Redis vector store.

### 10.1 Knowledge Base Content

The knowledge base should include:

1. Platform introduction;
2. Delivery range;
3. Delivery time;
4. Human customer service working hours;
5. Order cancellation rules;
6. Refund rules;
7. Coupon usage rules;
8. Dine-in pickup rules;
9. User review rules;
10. FAQ;
11. After-sales handling instructions;
12. Privacy protection instructions.

### 10.2 RAG Process

```text
Knowledge base text
        ↓
Text splitting
        ↓
text-embedding-v4 embedding
        ↓
Stored in Redis vector store
        ↓
User asks a question
        ↓
Vector similarity retrieval
        ↓
Relevant knowledge chunks
        ↓
Prompt context construction
        ↓
qwen3-max generates response
        ↓
Return answer to user
```

### 10.3 Technical Features

- Uses text-embedding-v4 for text embedding;
- Uses Redis to store vector data;
- Supports similarity retrieval;
- Supports dynamic knowledge base update;
- Improves answer accuracy;
- Reduces model hallucination;
- Suitable for delivery, after-sales, coupon, and platform rule Q&A.

---

## 11. Frontend Page Design

## 11.1 Mini Program Pages

The mini program frontend is used by normal users for ordering, placing orders, querying orders, using AI assistant, and viewing personal information.

Main mini program pages include:

1. Home page;
2. Dish category page;
3. Dish list page;
4. Dish detail page;
5. Shopping cart page;
6. Order confirmation page;
7. Payment result page;
8. Order list page;
9. Order detail page;
10. AI assistant page;
11. AI recommendation page;
12. Review writing page;
13. Coupon center;
14. My coupons;
15. Human customer service page;
16. Dine-in ordering page;
17. Personal center.

### Key Mini Program Experience

1. The home page displays popular dishes, recommended dishes, and coupon entry;
2. The dish detail page displays images, price, description, specifications, and reviews;
3. The AI recommendation page supports natural language input and returns dish cards;
4. The AI assistant page supports chat bubbles, streaming output, and quick questions;
5. The order page supports order status viewing;
6. The review page supports AI review writing;
7. The personal center displays user information, orders, coupons, and customer service entry.

---

## 11.2 Admin Frontend Pages

The admin frontend is used by merchants or platform administrators to manage dishes, orders, coupons, reviews, sensitive words, and customer service.

Main admin pages include:

1. Login page;
2. Dashboard;
3. Data dashboard;
4. Dish management;
5. Category management;
6. Set meal management;
7. Order management;
8. Coupon management;
9. Review management;
10. Sensitive word management;
11. Human customer service workbench;
12. User management;
13. System settings.

### Key Admin Frontend Experience

1. Dashboard displays revenue, order count, popular dishes, and other statistics;
2. Dish management supports adding, editing, enabling, and disabling dishes;
3. Coupon management supports issuing coupons and checking statuses;
4. Sensitive word management supports adding, editing, and deleting words;
5. Customer service workbench supports viewing sessions and replying to users;
6. Review management supports viewing user reviews and handling illegal content.

---

## 12. Database Design Suggestions

Based on the original Sky Take-Out database, this project suggests adding or extending the following tables.

### 12.1 Dish Review Table

```text
dish_review
```

Main fields:

- id
- user_id
- order_id
- dish_id
- rating
- content
- images
- like_count
- status
- create_time
- update_time

### 12.2 Review Like Table

```text
dish_review_like
```

Main fields:

- id
- review_id
- user_id
- create_time

### 12.3 Coupon Table

```text
coupon
```

Main fields:

- id
- name
- type
- threshold_amount
- discount_amount
- discount_rate
- total_stock
- remaining_stock
- receive_limit
- start_time
- end_time
- status
- create_time
- update_time

### 12.4 User Coupon Table

```text
user_coupon
```

Main fields:

- id
- user_id
- coupon_id
- status
- receive_time
- use_time
- order_id
- expire_time

### 12.5 Human Customer Service Session Table

```text
customer_service_session
```

Main fields:

- id
- user_id
- admin_id
- type
- status
- last_message
- last_message_time
- create_time
- end_time

### 12.6 Human Customer Service Message Table

```text
customer_service_message
```

Main fields:

- id
- session_id
- sender_id
- sender_type
- content
- message_type
- read_status
- create_time

### 12.7 Sensitive Word Table

```text
sensitive_word
```

Main fields:

- id
- word
- category
- status
- create_time
- update_time

### 12.8 AI Chat Session Table

```text
ai_chat_session
```

Main fields:

- id
- user_id
- title
- status
- create_time
- update_time

### 12.9 AI Chat Message Table

```text
ai_chat_message
```

Main fields:

- id
- session_id
- user_id
- role
- content
- message_type
- create_time

### 12.10 Order Table Extension Fields

The original order table should be extended with:

- order_type: order type, 1 for delivery and 2 for dine-in;
- pickup_number: dine-in pickup number;
- coupon_id: used coupon ID;
- discount_amount: discount amount;
- original_amount: original amount;
- actual_amount: actual paid amount.

---

## 13. API Design Suggestions

## 13.1 AI APIs

```text
POST /user/ai/chat
GET  /user/ai/chat/stream
POST /user/ai/recommend
POST /user/ai/review/write
GET  /user/ai/session/list
DELETE /user/ai/session/{sessionId}
```

## 13.2 Dish Detail and Review APIs

```text
GET  /user/dish/{id}
GET  /user/dish/{id}/reviews
POST /user/review
POST /user/review/{id}/like
DELETE /user/review/{id}
GET  /user/order/{orderId}/review/status
```

## 13.3 Coupon APIs

```text
GET  /user/coupon/available
POST /user/coupon/receive/{couponId}
GET  /user/coupon/my
GET  /user/coupon/order/available
POST /admin/coupon
GET  /admin/coupon/page
PUT  /admin/coupon/{id}
DELETE /admin/coupon/{id}
```

## 13.4 Human Customer Service APIs

```text
POST /user/service/session
GET  /user/service/session/current
POST /user/service/message
GET  /user/service/message/list
POST /user/service/session/end

GET  /admin/service/session/page
GET  /admin/service/message/list
POST /admin/service/message/reply
POST /admin/service/session/end
```

## 13.5 Sensitive Word APIs

```text
POST /admin/sensitive-word
GET  /admin/sensitive-word/page
PUT  /admin/sensitive-word/{id}
DELETE /admin/sensitive-word/batch
POST /admin/sensitive-word/check
```

## 13.6 Dine-In APIs

```text
POST /user/dine-in/order/submit
GET  /user/dine-in/order/{id}
GET  /user/dine-in/order/list
POST /admin/dine-in/order/complete
```

---

## 14. Business Process Design

### 14.1 AI Recommendation Process

```text
User enters dining requirements
        ↓
Backend queries current available dishes
        ↓
Recommendation prompt is constructed
        ↓
qwen3-max is called
        ↓
Dish IDs and recommendation reasons are returned
        ↓
Backend queries real dish details by ID
        ↓
Frontend displays recommended dish cards
```

### 14.2 AI Customer Service Order Query Process

```text
User asks about order status
        ↓
AI determines that order query is needed
        ↓
Order query tool is called
        ↓
Backend verifies current user identity
        ↓
User's own order is queried
        ↓
Order status and order information are returned
        ↓
AI generates a natural language response
```

### 14.3 AI Review Writing Process

```text
User selects a completed order
        ↓
Order dish list is retrieved
        ↓
User enters simple review requirements
        ↓
qwen3-max generates review
        ↓
The review is limited to 140 characters
        ↓
Sensitive word filtering is performed
        ↓
User confirms and submits the review
```

### 14.4 Coupon Usage Process

```text
User enters coupon center
        ↓
User claims coupon
        ↓
User enters order confirmation page
        ↓
System queries available coupons
        ↓
User selects coupon
        ↓
System calculates discount amount
        ↓
Order is submitted
        ↓
Coupon status is updated to used
```

### 14.5 Dine-In Order Process

```text
User scans QR code to enter dine-in ordering page
        ↓
User selects dishes
        ↓
Dine-in order is submitted
        ↓
Payment is completed
        ↓
System generates pickup number
        ↓
Merchant prepares food
        ↓
Order status is updated after preparation
        ↓
User picks up food with pickup number
```

### 14.6 Human Customer Service Process

```text
User starts human customer service
        ↓
Customer service session is created or retrieved
        ↓
User sends message
        ↓
Admin staff views message
        ↓
Admin staff replies
        ↓
Both sides view message history
        ↓
Session is ended after issue is resolved
```

---

## 15. Project Highlights

The main highlights of this project include:

1. Intelligent refactoring based on the traditional Sky Take-Out system;
2. Integration of LangChain4j and Tongyi Qianwen large language model;
3. AI customer service with multi-turn dialogue and business consultation;
4. RAG technology for enhanced knowledge base Q&A;
5. Redis vector store for knowledge retrieval;
6. AI dish recommendation based on natural language requirements and real dishes;
7. AI review writing to improve user review experience;
8. Complete dish review system with text, images, ratings, and likes;
9. Coupon system to enhance marketing capability;
10. Human customer service system combining AI and human service;
11. Dine-in ordering covering in-store ordering scenarios;
12. Sensitive word filtering to improve content safety;
13. Refactored admin frontend and mini program frontend for better user experience;
14. Cache optimization for better access performance;
15. Functional coverage across user side, admin side, AI service, and operation management.

---

## 16. Future Extensions

This project can be further extended in the following directions:

1. Add rider-side application with rider order acceptance, delivery status update, and delivery route display;
2. Add more complex recommendation algorithms, such as collaborative filtering, user profile recommendation, and hybrid recommendation;
3. Add merchant operation analysis so that AI can analyze revenue, order volume, and dish sales;
4. Add inventory management and food preparation prediction;
5. Add membership and points system;
6. Add flash sale, full reduction campaign, and set meal marketing;
7. Add WebSocket for more real-time human customer service communication;
8. Add order anomaly detection and automatic processing;
9. Add multi-store management;
10. Add mobile App adaptation;
11. Add automated testing and API stress testing;
12. Add log monitoring and system alerts.

---

## 17. Project Summary

This project implements a complete intelligent extension and frontend refactoring based on the basic Sky Take-Out system. It not only preserves the core ordering, order placement, and management capabilities of a traditional food delivery platform, but also adds AI assistant, AI dish recommendation, AI review writing, human customer service, coupon system, dish detail page, dine-in orders, sensitive word filtering, and personal center.

Through LangChain4j, Tongyi Qianwen qwen3-max, text-embedding-v4, DashScope API, RAG, and Redis vector store, the project upgrades a normal food delivery system into an intelligent food delivery service platform. The system has strong business completeness, technical comprehensiveness, and demonstration value, making it suitable as a project for Java backend development, full-stack development, or AI application development.
