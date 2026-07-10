# 苍穹智膳 Vue 3 管理端

独立的 Takeout Guys AI 管理端工程，视觉取自根目录 `icons` 中的外卖袋机器人标识，统一使用橙红、海军蓝和暖白。包含经营总览、AI Agent 工作台、订单、菜品、优惠券、评价、人工客服、敏感词和员工管理入口。

```bash
npm install
npm run dev
```

开发地址为 `http://localhost:5173`，`/api` 由 Vite 默认代理到 `http://localhost:8080/admin`，本地联调不需要 Nginx。生产构建执行 `npm run build`，输出目录为 `dist`。

登录沿用现有 `/admin/employee/login`，请求头沿用 `token`。AI 流式对话使用 `POST /admin/ai/chat/stream`，请求体为 JSON，响应为 SSE。
