# 部署说明 (DEPLOYMENT)

## 系统环境要求
- **JDK**：17+
- **MySQL**：8.0+
- **Node.js**：18+
- **Maven**：3.8+

---

## 1. 数据库初始化

1. 登录 MySQL 并创建数据库：
   ```sql
   CREATE DATABASE IF NOT EXISTS campus_material DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
2. 导入结构与数据：
   ```bash
   mysql -u root -p campus_material < sql/schema.sql
   mysql -u root -p campus_material < sql/seed.sql
   ```
> 注：`seed.sql` 中的预设用户密码统一为 **`Abc@123456`**。

---

## 2. 后端部署 (Spring Boot)

1. 进入 `backend` 目录：
   ```bash
   cd backend
   ```
2. 开发环境可直接使用 `backend/src/main/resources/application-dev.yml` 中的 MySQL 配置。
   如果需要切换账号、密码或域名，请修改：
   - `spring.datasource.url`
   - `spring.datasource.username`
   - `spring.datasource.password`
   - `security.jwt.secret`
   - `security.cors.allowed-origins`

3. 编译并启动：
   ```bash
   mvn clean package -DskipTests
   java -jar target/campus-backend-1.0.0.jar
   ```
   > 开发环境下也可以使用 `mvn spring-boot:run`。后端默认运行在 `8080` 端口。

---

## 3. 前端部署 (Vue 3)

1. 进入 `frontend` 目录：
   ```bash
   cd frontend
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
   > 如需部署生产环境，请执行 `npm run build`，并将 `dist` 目录下的静态文件部署到 Nginx 或其他 Web 服务器。

---

## 4. 演示与测试账号

系统内置以下测试账号（密码均为 **`Abc@123456`**）：
- `admin` (系统管理员，拥有所有权限)
- `warehouse` (仓库管理员，负责出入库、调拨执行与预警处理)
- `approver` (审批负责人，负责处理申领与调拨申请)
- `dept` (普通部门用户，可发起申领)

---

## 常见问题排查

1. **登录提示“用户名或密码错误”或“未认证”？**
   - 检查启动前是否导入了最新的 `seed.sql` (密码已由明文改为 BCrypt)。
   - 检查后端的 `JWT_SECRET` 环境变量是否遗漏或长度不足 32 字节。
2. **前后端接口跨域 (CORS) 报错？**
   - 后端通过 `CORS_ALLOWED_ORIGINS` 环境变量控制允许的跨域源，默认允许 `http://localhost:5173`。如果您通过其他域名访问前端，请修改此配置。
3. **数据库连接失败？**
   - 请检查 `application-dev.yml` 中的 MySQL 账号、密码是否正确，MySQL 服务是否开启，端口是否为 `3306`。
