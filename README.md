# Takeout Guys AI

统一仓库包含 Spring Boot 后端、Vue 3 管理端和 uni-app Vue 3 用户端。

## 项目结构

```text
sky-takeout-agent/
├─ backend/                 # Spring Boot 多模块后端及数据库脚本
├─ admin-web/               # Vue 3 + Vite 管理端
├─ user-app/                # uni-app + Vue 3 微信小程序
├─ docs/                    # API、开发日志和品牌资源
├─ scripts/                 # 辅助脚本
└─ legacy/                  # 原始前端、原型和旧部署资源
```

## 启动

后端：

```powershell
cd backend
mvn spring-boot:run -pl sky-server -am
```

也可以直接运行 `backend/sky-server/src/main/java/com/sky/SkyApplication.java`。

管理端：

```powershell
cd admin-web
npm install
npm run dev
```

访问 `http://localhost:5173`。

用户端：使用 HBuilderX 打开 `user-app`，运行到微信开发者工具。

## 文档

- [开发日志](docs/PROJECT_DEVELOPMENT_LOG.md)
- [完整项目说明](docs/SKY_TAKE_OUT_FULL_PROJECT_README.md)
- [用户端 Apifox](docs/USER_API_APIFOX.json)
- [管理端 Apifox](docs/ADMIN_API_APIFOX.json)
