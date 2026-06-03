# JavaDiet 开发启动指南

## 项目结构

```
JavaDiet/
├── src/                  # Spring Boot 后端
│   └── main/
│       ├── java/         # Java 源码
│       └── resources/
│           └── application.yaml  # 后端配置（通过环境变量读取敏感信息）
├── frontend/             # Next.js 前端
│   ├── pages/            # 页面路由
│   ├── components/       # UI 组件
│   └── package.json
├── admin/                # Vue 后台管理端
│   ├── src/              # 管理端源码
│   └── package.json
├── mvnw / mvnw.cmd       # Maven Wrapper
└── pom.xml
```

---

## 环境要求

| 工具    | 版本要求                               |
| ------- | -------------------------------------- |
| Java    | 17                                     |
| Node.js | 18 及以上                              |
| MySQL   | 8.0                                    |
| Maven   | 由 mvnw wrapper 自动管理，无需单独安装 |

---

## 第一步：准备数据库配置

项目需要连接 MySQL。公开仓库中不保存真实数据库地址、账号、密码或 API Key。

推荐复制本地配置模板：

```powershell
Copy-Item src\main\resources\application-local.example.yaml src\main\resources\application-local.yaml
```

然后在 `src/main/resources/application-local.yaml` 中填写你自己的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/JavaDiet?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true
    username: your_db_user
    password: your_db_password
```

`application-local.yaml` 已加入 `.gitignore`，不要提交该文件。

也可以直接通过环境变量配置：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/JavaDiet?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true'
$env:DB_USERNAME='your_db_user'
$env:DB_PASSWORD='your_db_password'
$env:LLM_GATEWAY_API_KEY='your_llm_api_key'
$env:FDC_API_KEY='your_fdc_api_key'
```

> Hibernate 设置了 `ddl-auto: update`，启动时会自动建表，**无需手动建表**。

---

## 第二步：启动后端（Spring Boot）

在项目根目录 `JavaDiet/` 下执行：

**Windows（PowerShell / CMD）：**

```powershell
.\mvnw.cmd spring-boot:run
```

**macOS / Linux：**

```bash
./mvnw spring-boot:run
```

**启动成功标志：**

```
Tomcat started on port 8080 (http)
Started JavaDietApplication in X.XX seconds
```

后端运行地址：`http://localhost:8080`

> 如果需要跳过测试加速启动：
>
> ```powershell
> .\mvnw.cmd spring-boot:run -DskipTests
> ```

---

## 第三步：启动前端（Next.js）

进入 `frontend/` 目录：

```powershell
cd frontend
```

**首次运行** — 先安装依赖（只需执行一次）：

```powershell
pm run dev

```

**启动开发服务器：**

```powershell
npm run dev
```

**启动成功标志：**

```
▲ Next.js 13.x.x
- Local: http://localhost:3000
```

前端运行地址：`http://localhost:3000`

---

## 第四步：启动后台管理端（Vue）

进入 `admin/` 目录：

```powershell
cd admin
```

**首次运行** — 先安装依赖（只需执行一次）：

```powershell
npm install
```

**启动开发服务器：**

```powershell
npm run dev
```

后台管理端运行地址通常为：`http://localhost:3001`

---

## 前后端联调说明

前端通过 Next.js 内置代理将 `/api/*` 请求转发到后端：

```
浏览器请求 → http://localhost:3000/api/...
              ↓ (next.config.js 代理)
         → http://localhost:8080/api/...
```

**因此启动顺序不影响功能，但前端和后端都要运行才能正常使用。**

---

## 常用页面路由

| 页面             | 地址                                     |
| ---------------- | ---------------------------------------- |
| 首页             | http://localhost:3000/                   |
| 登录             | http://localhost:3000/login              |
| 注册             | http://localhost:3000/register           |
| 个人资料         | http://localhost:3000/profile            |
| 健康报告         | http://localhost:3000/reports/health     |
| 附近餐厅         | http://localhost:3000/restaurants/nearby |
| 膳食记录（早餐） | http://localhost:3000/meals/breakfast    |
| 打卡             | http://localhost:3000/checkin            |
| 意见反馈         | http://localhost:3000/contact/feedback   |

---

## 关闭服务

- **后端**：在运行 `mvnw spring-boot:run` 的终端按 `Ctrl + C`
- **前端**：在运行 `npm run dev` 的终端按 `Ctrl + C`
- **后台管理端**：在运行 `npm run dev` 的终端按 `Ctrl + C`

---

## 常见问题

**Q：后端启动报错 `Communications link failure`**  
A：MySQL 服务未启动，或本地数据库地址、端口、账号、密码配置有误。

**Q：前端访问 `/api/...` 返回 502 / 连接失败**  
A：后端没有运行，先启动后端再刷新页面。

**Q：`npm run dev` 报错 `Cannot find module`**  
A：依赖未安装，在对应目录执行 `npm install` 后重试。

**Q：登录后首页数据为空**  
A：属于正常情况，需要先通过注册流程完善健康档案，再进行膳食记录后才有数据展示。

---

## 公开仓库注意事项

- 不要提交真实数据库密码、API Key、Cookie 或浏览器登录状态。
- 不要提交 `node_modules/`、日志文件、`.env`、`application-local.yaml`。
- 如果敏感信息曾经进入 Git 历史，需要更换对应密码或密钥；必要时清理 Git 历史后再公开仓库。
