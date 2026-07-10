# Takeout Guys 小程序接口对接说明

本文档记录 uni-app + Vue 3 用户端与后端的接口映射。项目使用 Vue 3 的 `createSSRApp` 入口与 Vuex 4 `createStore`，接口封装统一位于 `pages/api/api.js`，请求配置位于 `utils/request.js` 和 `utils/env.js`。

## 运行配置

- 当前小程序开发地址：`http://192.168.31.107:8080`
- 用户 JWT 请求头：`authentication`
- 登录令牌本地键名：`user_token`
- 后端统一响应：`{ "code": 1, "msg": null, "data": {} }`

小程序中的 `localhost` 指向小程序运行环境而不是电脑。开发机 IP 变化后，需要把 `utils/env.js` 中的 `baseUrl` 改为 `ipconfig` 显示的局域网 IPv4 地址，并确保手机和电脑在同一网络。当前项目的微信开发者工具配置已在开发环境关闭合法域名校验；正式发布必须使用已备案的 HTTPS 域名并加入小程序服务器域名白名单。

## 已接通的后端模块

| 模块 | 接口 |
| --- | --- |
| 登录 | `POST /user/user/login`、`POST /user/user/logout` |
| 店铺 | `GET /user/shop/status` |
| 菜单 | `GET /user/category/list`、`GET /user/dish/list` |
| 套餐 | `GET /user/setmeal/list`、`GET /user/setmeal/dish/{id}` |
| 购物车 | `POST /user/shoppingCart/add`、`POST /user/shoppingCart/sub`、`GET /user/shoppingCart/list`、`DELETE /user/shoppingCart/clean` |
| 地址 | `/user/addressBook` 下的查询、新增、修改、删除及默认地址接口 |
| 订单 | 提交、支付、历史订单、详情、取消、再来一单、催单 |
| 优惠券 | `GET /user/coupon/available`、`POST /user/coupon/receive/{couponId}`、`GET /user/coupon/my`、`GET /user/coupon/order/available` |

金额直接使用后端 `BigDecimal` 的元单位，前端不再除以 100。订单明细字段使用 `orderDetailList`。

## AI Agent 接口

前端已加入 AI 对话页、智能推荐页及以下请求封装：

- `POST /user/ai/chat`
- `GET /user/ai/chat/stream`
- `POST /user/ai/recommend`
- `POST /user/ai/review/write`
- `GET /user/ai/session/list`
- `DELETE /user/ai/session/{sessionId}`

当前 uni-app 已按正式 `USER_API_APIFOX.json` 对齐以下契约细节：

- AI 对话请求体使用 `{ sessionId?, message }`
- AI 推荐请求体使用 `{ requirement, budget?, peopleCount? }`
- AI 评价帮写请求体使用 `{ orderId, dishId?, rating?, keywords?, draft?, style? }`
- 用户评价提交流程已改为 `{ orderId, dishId, rating, content, images }`

这些用户端 AI 接口目前尚未由后端完整实现，因此页面在失败时会明确提示，不会伪造成功结果，也不会把 GX10 密钥放进小程序；模型密钥只能由后端保存和调用。

## 优惠券页面接入说明

- 个人中心已新增“优惠券中心”入口，对接可领取优惠券和我的优惠券查询；
- 订单页已新增“优惠券”入口，对接当前订单可用券查询与选择；
- 由于当前真实后端 `OrdersSubmitDTO` 还没有 `couponId` 字段，下单时暂不能真正完成优惠券核销；
- 前端会回填已选择的优惠券，并明确提示“当前下单接口暂未完成优惠券核销”，避免把前端预选误当成已抵扣成功。
