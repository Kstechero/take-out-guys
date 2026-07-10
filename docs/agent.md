# Takeout Guys AI 项目维护文档

> 更新时间：2026-07-10  
> 依据：当前仓库代码结构、已落地功能、`git log`/`git status` 现状。  
> 目的：给开发者和后续 AI Agent 一份“与代码和 Git 同步”的项目事实说明。

## 1. 文档使用规则

- 本文档只记录“仓库中已经存在且基本可用”的能力，不把规划内容写成已完成。
- 每次新增模块、接口、页面、数据库表、环境变量或部署依赖后，应同步更新 `docs/agent.md`、`docs/PROJECT_DEVELOPMENT_LOG.md`、`docs/SKY_TAKE_OUT_FULL_PROJECT_README.md`。
- 日志型记录写入 `docs/PROJECT_DEVELOPMENT_LOG.md`；面向交接和维护的事实说明写入本文件；对外完整介绍写入 `docs/SKY_TAKE_OUT_FULL_PROJECT_README.md`。
- 涉及远端同步时，必须区分：
  - “代码已在本地实现但未提交/未推送”；
  - “代码已提交到本地 Git”；
  - “代码已推送到远端仓库”。
- 不在文档中记录真实密钥、令牌、数据库密码等敏感信息。

## 2. 当前仓库概况

当前仓库为统一单仓项目，目录职责如下：

```text
backend/        Spring Boot 后端、数据库脚本、AI/客服/评价等核心服务
admin-web/      Vue 3 + Vite 管理端
user-app/       uni-app 微信小程序用户端
docs/           项目文档、接口导出、维护记录
scripts/        辅助脚本
legacy/         历史资源与旧工程归档
```

当前根仓库远端：

- `origin`: [https://github.com/Kstechero/take-out-guys.git](https://github.com/Kstechero/take-out-guys.git)

当前分支状态（2026-07-10 检查时）：

- 当前分支：`main`
- 已关联远端：`origin/main`
- 存在未提交修改：
  - `backend/sky-server/src/main/java/com/sky/service/impl/AddressBookServiceImpl.java`
  - `user-app/pages/order/index.js`

这意味着：文档中凡是涉及这两个文件的“最新变化”，只能描述为“工作区已有未提交改动”，不能写成“已提交并推送”。

## 3. 当前已落地功能范围

以下内容均以当前代码目录、控制器、页面和最近提交记录为准。

### 3.1 后端基础业务能力

后端当前已具备的传统外卖业务能力包括：

- 员工登录、分页、状态启停、信息维护；
- 分类管理；
- 菜品管理；
- 套餐管理；
- 店铺营业状态管理；
- 用户微信登录；
- 用户端菜品/套餐浏览；
- 购物车增删改查；
- 地址簿 CRUD；
- 下单、订单详情、订单状态流转；
- 历史订单与订单相关统计；
- 运营报表与工作台概览；
- Redis 缓存（店铺状态、菜品/套餐等）。

从 Git 历史看，这部分能力对应的关键提交包括：

- `80e7873` 分类管理
- `917b6fd` / `36206d1` / `97f07cc` 菜品管理
- `297b095` 套餐管理
- `f92e74c` 店铺状态 Redis 支持
- `2c67aeb` 微信登录与用户端商品浏览
- `45ec21c` 购物车 CRUD
- `63fdbb2` 地址簿 CRUD
- `fd8dc59` / `12ceb85` / `d4137ff` 下单、订单处理、报表、定时任务等

### 3.2 管理端当前页面能力

`admin-web/src/views` 当前实际存在的页面：

- `LoginView.vue`：登录页
- `DashboardView.vue`：经营总览
- `ReportsView.vue`：统计报表
- `AgentView.vue`：AI Agent 工作台
- `BusinessView.vue`：订单、菜品、分类、套餐、敏感词、员工等复用业务页
- `CouponView.vue`：优惠券管理
- `ReviewsView.vue`：评价管理
- `ServiceView.vue`：人工客服工作台

`admin-web/src/router/index.ts` 中当前已注册的管理端菜单/路由：

- `/dashboard` 经营总览
- `/reports` 数据统计
- `/agent` AI Agent
- `/orders` 订单管理
- `/dishes` 菜品管理
- `/categories` 分类管理
- `/setmeals` 套餐管理
- `/coupons` 优惠券管理
- `/reviews` 评价管理
- `/service` 人工客服
- `/sensitive` 敏感词库
- `/employees` 员工管理

说明：文档以后应以路由和页面文件为准，不再沿用旧版“管理端只有基础 CRUD”描述。

### 3.3 用户端当前页面能力

`user-app/pages.json` 与 `user-app/pages/` 当前显示用户端已落地页面包括：

- 首页 `pages/index/index`
- 提交订单 `pages/order/index`
- 下单成功 `pages/order/success`
- 地址管理 `pages/address/address`
- 新增/编辑地址 `pages/addOrEditAddress/addOrEditAddress`
- 历史订单 `pages/historyOrder/historyOrder`
- 个人中心 `pages/my/my`
- AI 聊天助手 `pages/ai/chat`
- AI 智能推荐 `pages/ai/recommend`
- 人工客服 `pages/service/index`
- 用户评价提交 `pages/review/index`
- 菜品评价列表 `pages/dish-reviews/index`
- 优惠券中心 `pages/coupon/index`

说明：用户端已经不再只是“点餐 + 地址 + 历史订单”，还包含 AI、评价、客服、优惠券、个人中心等扩展能力。

### 3.4 当前 AI 能力

当前代码显示，AI 能力已经进入“业务集成阶段”，不是单独 Demo：

后端控制器已存在：

- `backend/sky-server/src/main/java/com/sky/controller/admin/AdminAiController.java`
- `backend/sky-server/src/main/java/com/sky/controller/user/UserAiController.java`

结合近期提交，当前已落地能力包括：

- 管理端 AI 对话与健康检查；
- 用户端 AI 聊天；
- 用户端 AI 推荐；
- 管理端与用户端 Tool Calling 接入；
- 后端 AI 调用循环与工具执行注册；
- 管理端本地知识检索/知识问答方向的基础设施；
- SSE 流式响应。

与 AI 相关的关键提交：

- `c50b930` add GX10 AI streaming API
- `fae8fa3` Add AI service backend loop
- `af82a46` refactor admin/user ai tool calling, update frontend, and sync docs
- `6553c72` Replace admin mock data with live API states

### 3.5 当前评价、敏感词、人工客服能力

这是当前项目近几次迭代中最重要的新增能力之一。

#### 评价系统

当前已存在：

- 用户端评价提交：`UserReviewController.java`
- 管理端评价管理：`AdminReviewController.java`
- 用户端菜品评价列表页：`user-app/pages/dish-reviews/index.vue`
- 管理端评价管理页：`admin-web/src/views/ReviewsView.vue`
- 点赞/分页/管理端筛选相关 Mapper 与 VO/DTO

对应关键提交：

- `a7ca576 add review moderation and sensitive tools`

#### 敏感词能力

当前已存在：

- 管理端敏感词接口：`AdminSensitiveWordController.java`
- 敏感词 DTO / Mapper / Service 实现
- 与 AI/客服/评价场景联动的内容检测能力

说明：敏感词库已经是现有代码的一部分，不应再写成“预留规划”。

#### 人工客服

当前已存在：

- 用户端人工客服会话创建、查询、发消息、结束会话；
- 管理端客服会话分页、消息列表、回复、结束；
- 会话与消息表、Schema 迁移初始化；
- 用户端 `pages/service/index.vue` 与管理端 `ServiceView.vue` 联动。

对应关键提交：

- `9013394 feat: deliver end-to-end human customer service across app, admin, and backend`

### 3.6 优惠券能力

当前代码中已存在：

- 管理端优惠券控制器：`backend/sky-server/src/main/java/com/sky/controller/admin/CouponController.java`
- 用户端优惠券控制器：`backend/sky-server/src/main/java/com/sky/controller/user/CouponController.java`
- 用户端优惠券页面：`user-app/pages/coupon/index.vue`
- 管理端优惠券页面：`admin-web/src/views/CouponView.vue`
- `UserCouponMapper` 等配套数据访问代码

说明：优惠券功能至少已完成基础的前后端与接口串联，后续若有更细节的规则扩展，再增量补充文档。

## 4. 当前后端接口范围

从控制器文件来看，当前后端接口分为以下几类。

### 4.1 管理端控制器

- `AdminAiController`
- `AdminCustomerServiceController`
- `AdminReviewController`
- `AdminSensitiveWordController`
- `CategoryController`
- `CommonController`
- `CouponController`
- `DishController`
- `EmployeeController`
- `OrderController`
- `ReportController`
- `SetmealController`
- `ShopController`
- `WorkspaceController`

### 4.2 用户端控制器

- `AddressBookController`
- `CategoryController`
- `CouponController`
- `DishController`
- `OrderController`
- `SetmealController`
- `ShopController`
- `ShoppingCartController`
- `UserAiController`
- `UserController`
- `UserCustomerServiceController`
- `UserReviewController`

### 4.3 通知与其他

- `PayNotifyController`

因此，`docs/USER_API_APIFOX.json` 与 `docs/ADMIN_API_APIFOX.json` 理论上应覆盖以上接口族；若有缺漏，应后续按控制器补齐导出。

## 5. Git 状态与上传情况说明

截至 2026-07-10，本仓库 Git 现状可归纳为：

### 5.1 已提交并已在远端可见的主要阶段

远端 `origin/main` 已包含以下关键演进：

1. 后端基础外卖系统能力；
2. Vue 3 管理端重构；
3. uni-app 用户端并入统一仓库；
4. AI 流式接口与 AI 服务循环；
5. 管理端从 mock 数据切换到真实接口；
6. 人工客服全链路；
7. 评价管理与敏感词工具。

最近关键提交顺序：

- `6553c72` 管理端改为真实 API
- `fae8fa3` AI 服务后端闭环
- `9013394` 人工客服全链路
- `a7ca576` 评价审核与敏感词工具

### 5.2 当前未提交工作区改动

当前 `git status` 显示仍有 2 个文件未提交：

- `backend/sky-server/src/main/java/com/sky/service/impl/AddressBookServiceImpl.java`
- `user-app/pages/order/index.js`

结论：

- 文档应承认“仓库当前存在未提交本地变更”；
- 这两处变更如果影响功能描述，需要等提交后再把它们写入开发日志中的“已提交记录”；
- 若只是小修复，可以在开发日志中写成“2026-07-10 工作区存在待提交修正”。

## 6. 当前明确未完成或不应夸大描述的部分

根据代码与提交现状，以下内容不建议写成“已经完整闭环上线”：

- 向量化知识库、Embedding、RAG 增强检索的完整生产化闭环；
- AI 会话持久化、长期记忆、多租户知识库管理的完整产品能力；
- 推荐系统的强个性化训练或实时反馈闭环；
- 全量自动化测试、完整 CI/CD、正式生产部署方案；
- 小程序所有页面都经过正式联调验收。

更准确的表述应是：

- 相关能力已有基础实现或局部接入；
- 生产级完善度仍取决于后续联调、数据、部署与验收。

## 7. 文档维护建议

后续每次更新文档时，建议固定执行以下检查：

1. 先看 `git status --short --branch`，区分未提交与已推送；
2. 再看 `git log --oneline -n 20`，提炼最近阶段成果；
3. 核对 `admin-web/src/router/index.ts`、`user-app/pages.json`、后端 `controller/`，确认实际功能面；
4. 最后同步更新三份文档，确保“功能说明、开发日志、完整 README”三者口径一致。

## 8. 本次整理结论

本次文档整理后的统一口径应为：

- 项目已经从“基础外卖系统”演进为“外卖业务 + AI 助手 + 人工客服 + 评价治理 + 敏感词能力”的统一单仓项目；
- 管理端、用户端、后端三侧均已存在对应代码与页面，不再是概念设计；
- Git 远端已经包含人工客服、评价、敏感词等主要增量；
- 当前工作区仍有 2 个文件待提交，因此文档中不把这部分描述为“已上传完成”。
