# Takeout Guys AI 项目开发记录

> 本文档按时间记录“已经发生”的开发事实，并补充 Git 上传状态。  
> 当前校准日期：2026-07-10。

## 2026-07-10 · 文档与项目状态重新校准

- 范围：`docs/agent.md`、`docs/PROJECT_DEVELOPMENT_LOG.md`、`docs/SKY_TAKE_OUT_FULL_PROJECT_README.md`
- 背景：`docs` 下三份 Markdown 与当前代码、Git 提交历史已不完全同步，需要按仓库实际状态重写。
- 核对结果：
  - 当前仓库为统一单仓，包含 `backend/`、`admin-web/`、`user-app/`、`docs/` 等目录；
  - 管理端已具备经营总览、报表、AI Agent、订单、菜品、分类、套餐、优惠券、评价、人工客服、敏感词、员工等页面或路由；
  - 用户端已具备首页、下单、地址、历史订单、个人中心、AI 聊天、AI 推荐、人工客服、评价、菜品评价、优惠券中心等页面；
  - 后端已存在 AI、人工客服、评价、敏感词、优惠券等控制器与配套服务代码；
  - 远端 `origin/main` 最近关键提交已包含人工客服全链路、评价治理、敏感词工具、管理端真实 API 替换等成果；
  - 当前工作区仍有 2 个未提交文件：
    - `backend/sky-server/src/main/java/com/sky/service/impl/AddressBookServiceImpl.java`
    - `user-app/pages/order/index.js`
- 处理：
  - 重新统一三份文档口径；
  - 明确区分“已实现”“已提交/已推送”“本地未提交修改”；
  - 删除或弱化对未完全落地能力的夸大描述。
- 验证：
  - `git status --short --branch`
  - `git log --oneline --decorate -n 25`
  - 核对 `admin-web/src/router/index.ts`
  - 核对 `user-app/pages.json`
  - 核对 `backend/sky-server/src/main/java/com/sky/controller/`
- 后续：待当前未提交文件完成提交后，再补一条“2026-07-10 本地修正已提交”的记录。

## 2026-07-07 · 评价治理与敏感词工具落地

- Git 提交：`a7ca576 add review moderation and sensitive tools`
- 范围：后端、管理端、用户端、文档
- 已完成：
  - 新增用户端评价提交与菜品评价浏览能力；
  - 新增管理端评价管理页；
  - 新增管理端敏感词工具接入；
  - 补充评价相关 DTO、VO、Mapper、Service；
  - 同步更新部分项目文档。
- 影响文件示例：
  - `admin-web/src/views/ReviewsView.vue`
  - `backend/sky-server/src/main/java/com/sky/controller/admin/AdminReviewController.java`
  - `backend/sky-server/src/main/java/com/sky/controller/user/UserReviewController.java`
  - `backend/sky-server/src/main/java/com/sky/service/impl/ReviewServiceImpl.java`
  - `user-app/pages/dish-reviews/index.vue`
- 状态：已提交，且已在 `origin/main`。

## 2026-07-06 · 人工客服全链路打通

- Git 提交：`9013394 feat: deliver end-to-end human customer service across app, admin, and backend`
- 范围：后端、管理端、用户端、数据库、文档
- 已完成：
  - 新增用户端人工客服页面；
  - 新增管理端客服工作台；
  - 打通会话创建、会话分页、消息收发、结束会话；
  - 新增客服相关数据表、Mapper、VO、DTO 与服务实现；
  - 增加 Schema 迁移初始化代码。
- 影响文件示例：
  - `admin-web/src/views/ServiceView.vue`
  - `user-app/pages/service/index.vue`
  - `backend/sky-server/src/main/java/com/sky/service/impl/CustomerServiceServiceImpl.java`
  - `backend/database/ai_review_service.sql`
- 状态：已提交，且已在 `origin/main`。

## 2026-07-06 · 管理端真实接口替换 Mock 数据

- Git 提交：`6553c72 Replace admin mock data with live API states`
- 范围：管理端、文档
- 已完成：
  - `DashboardView`、`ReportsView`、`AgentView`、`ServiceView` 等页面对接真实后端接口；
  - 优化管理端布局与样式；
  - 文档同步修订。
- 状态：已提交，且已在 `origin/main`。

## 2026-07-06 · AI 服务后端闭环形成

- Git 提交：`fae8fa3 Add AI service backend loop`
- 范围：后端、数据库
- 已完成：
  - 新增 AI 推荐、AI 写评价、敏感词检测、客服会话等 DTO/VO/Entity；
  - 新增 AI 聊天会话与消息基础结构；
  - 补齐 AI 服务调用链基础设施。
- 状态：已提交，且已在 `origin/main`。

## 2026-07-06 · 管理端 / 用户端 AI Tool Calling 重构

- Git 提交：`af82a46 refactor admin/user ai tool calling, update frontend, and sync docs`
- 范围：前后端、文档
- 已完成：
  - 重构管理端和用户端 AI Tool Calling；
  - 同步前端交互与文档。
- 状态：已提交，且已在 `origin/main`。

## 2026-07-02 ~ 2026-07-05 · 统一仓库与多端并入阶段

- 已发生的关键演进：
  - `432b02d refactor: reorganize project and add uni-app client`
  - `eba303c feat: add Vue 3 admin panel`
- 结果：
  - 形成 `backend/ + admin-web/ + user-app/ + docs/` 的统一仓库结构；
  - 管理端切换为 Vue 3 + Vite 技术栈；
  - 小程序端并入统一仓库，后续 AI、客服、评价等能力在此基础上扩展。
- 状态：已提交，且已在 `origin/main`。

## 更早阶段 · 基础外卖业务能力

以下能力来自更早的连续提交，现已作为项目基础能力存在：

- 员工管理：`3d8d869`、`631c9e2`
- 分类管理：`80e7873`
- 菜品管理：`917b6fd`、`36206d1`、`97f07cc`
- 套餐管理：`297b095`
- 店铺状态 Redis 支持：`f92e74c`
- 用户登录与商品浏览：`2c67aeb`
- Redis 菜品/套餐缓存：`1b1685b`
- 购物车 CRUD：`45ec21c`
- 地址簿 CRUD：`63fdbb2`
- 下单流程：`fd8dc59`
- 双端订单详情与操作：`12ceb85`
- 报表、调度、通知等：`d4137ff`

## 当前 Git 上传状态摘要

截至 2026-07-10：

- 当前分支：`main`
- 远端跟踪：`origin/main`
- 远端已包含：AI 主链路、人工客服、评价治理、敏感词工具等最近关键能力
- 本地未提交：
  - `backend/sky-server/src/main/java/com/sky/service/impl/AddressBookServiceImpl.java`
  - `user-app/pages/order/index.js`

结论：

- 可以说“主要功能增量已经上传到远端”；
- 不能说“当前工作区全部改动都已经上传完成”。
