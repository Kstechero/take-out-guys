# Takeout Guys AI 完整项目说明

## 一、项目定位

Takeout Guys AI 是一个在经典外卖业务系统基础上扩展的多端统一项目。当前仓库已经不只是“点餐系统”，而是一个同时覆盖：

- 外卖基础业务；
- 管理端运营后台；
- 用户端微信小程序；
- AI 助手能力；
- 人工客服；
- 用户评价与评价治理；
- 敏感词管理；
- 优惠券能力；

的统一单仓工程。

当前统一仓库目录：

```text
backend/        Spring Boot 后端与数据库脚本
admin-web/      Vue 3 管理端
user-app/       uni-app 微信小程序用户端
docs/           项目文档与接口导出
scripts/        辅助脚本
legacy/         历史资源归档
```

## 二、当前实际完成情况

### 2.1 后端

后端位于 `backend/`，当前为 Spring Boot 多模块工程，已实际存在以下模块：

- `sky-common`
- `sky-pojo`
- `sky-server`

当前后端已覆盖的能力：

- 员工管理
- 分类管理
- 菜品管理
- 套餐管理
- 店铺状态控制
- 微信用户登录
- 商品浏览
- 购物车
- 地址簿
- 下单与订单流转
- 订单统计与经营报表
- AI 对话与 AI 推荐相关接口
- 人工客服会话与消息
- 用户评价与管理端评价治理
- 敏感词管理与内容检测
- 优惠券相关接口

当前能直接从控制器确认的后端接口族包括：

#### 管理端

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

#### 用户端

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

#### 其他

- `PayNotifyController`

### 2.2 管理端

管理端位于 `admin-web/`，当前采用 Vue 3 + Vite 技术栈，实际存在并启用的页面包括：

- 登录
- 经营总览
- 数据统计
- AI Agent
- 订单管理
- 菜品管理
- 分类管理
- 套餐管理
- 优惠券管理
- 评价管理
- 人工客服
- 敏感词库
- 员工管理

这些页面已经在 `admin-web/src/router/index.ts` 注册，说明它们不是占位目录，而是项目当前功能面的一部分。

### 2.3 用户端

用户端位于 `user-app/`，当前采用 uni-app，代码中已存在以下页面：

- 首页
- 提交订单
- 下单成功
- 地址管理
- 新增/编辑地址
- 历史订单
- 个人中心
- AI 聊天助手
- AI 智能推荐
- 人工客服
- 用户评价
- 菜品评价列表
- 优惠券中心

这意味着项目用户侧已经从“基础点餐小程序”扩展为“点餐 + AI + 客服 + 评价 + 优惠券”的综合用户端。

## 三、技术栈

### 3.1 后端技术栈

- Java
- Spring Boot 2.7.3
- Spring MVC
- MyBatis
- MySQL 8.0
- Redis
- Spring Cache
- JWT
- Knife4j / Swagger
- Maven 多模块

### 3.2 管理端技术栈

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Axios
- Vue Router
- ECharts
- SCSS

### 3.3 用户端技术栈

- uni-app
- Vue 3
- JavaScript / 部分 TypeScript 风格组织
- Pinia
- SCSS
- 微信小程序运行环境

### 3.4 AI 相关技术方向

从现有代码与提交记录看，项目当前已采用或接入：

- OpenAI 兼容接口风格
- SSE 流式响应
- AI Tool Calling
- 管理端与用户端 AI 对话集成
- 基于本地业务知识的检索增强方向探索
- GX10 / vLLM 相关模型服务配置

需要注意的是：

- AI 基础链路已经接入代码；
- 但完整生产级 RAG、Embedding、长期记忆、强个性化推荐闭环，当前不宜写成“全部成熟完成”。

## 四、近期关键迭代

根据当前 Git 历史，项目近期关键里程碑如下：

- `6553c72`：管理端由 mock 数据切换到真实后端接口
- `fae8fa3`：AI 服务后端闭环成型
- `9013394`：人工客服全链路打通
- `a7ca576`：评价审核与敏感词工具落地

这说明当前仓库的主要增量，已经从“基础业务功能开发”进入到“AI 与运营治理能力集成”阶段。

## 五、Git 与上传现状

截至 2026-07-10：

- 当前分支为 `main`
- 已连接远端仓库：`origin`
- 最近关键功能提交已存在于 `origin/main`
- 当前工作区仍有未提交修改：
  - `backend/sky-server/src/main/java/com/sky/service/impl/AddressBookServiceImpl.java`
  - `user-app/pages/order/index.js`

因此，准确说法是：

- 大部分核心新功能已经提交并推送；
- 当前仍有局部本地修正尚未上传。

## 六、当前项目边界

为了避免文档失真，当前项目应这样理解：

### 已经可以明确认定为“代码中存在”的部分

- 外卖基础业务闭环
- 管理端运营后台
- 用户端小程序
- AI 对话与推荐入口
- 人工客服双端联动
- 评价系统与管理端评价治理
- 敏感词管理
- 优惠券基础能力

### 暂不宜夸大为“完整成熟平台”的部分

- 生产级向量知识库平台
- 完整 AI 会话长期记忆
- 全链路自动化测试与 CI/CD
- 所有页面完成正式发布级验收
- 强个性化推荐闭环

## 七、启动方式

### 7.1 后端

```powershell
cd backend
mvn spring-boot:run -pl sky-server -am
```

或直接运行：

- `backend/sky-server/src/main/java/com/sky/SkyApplication.java`

### 7.2 管理端

```powershell
cd admin-web
npm install
npm run dev
```

默认访问：

- [http://localhost:5173](http://localhost:5173)

### 7.3 用户端

- 使用 HBuilderX 打开 `user-app/`
- 运行到微信开发者工具进行调试

## 八、文档清单

当前 `docs/` 下建议重点参考：

- `docs/agent.md`：维护和交接视角的事实说明
- `docs/PROJECT_DEVELOPMENT_LOG.md`：按时间记录的开发日志与 Git 状态说明
- `docs/SKY_TAKE_OUT_FULL_PROJECT_README.md`：项目完整介绍
- `docs/USER_API_APIFOX.json`：用户端接口导出
- `docs/ADMIN_API_APIFOX.json`：管理端接口导出

## 九、当前结论

截至当前代码状态，Takeout Guys AI 已经是一个“基础外卖系统 + AI 能力 + 客服体系 + 内容治理能力”的综合项目，而不是仅有部分原型页面的演示仓库。

同时，文档需要始终区分：

- 已实现；
- 已提交并推送；
- 本地仍在修改。

本次整理后的内容，已经按这一原则与当前代码和 Git 状态重新对齐。
