# 提交清单（稳定优先版）

## 一、必须提交（建议纳入本次里程碑）

### 1) 安全与配置收口
- backend/src/main/resources/application.yml
- backend/src/main/resources/application-dev.yml
- .env.example
- DEPLOY.md
- README.md

### 2) 接口兼容性修复（remark 双通道兼容）
- backend/src/main/java/com/campus/material/modules/apply/controller/ApplyController.java
- backend/src/main/java/com/campus/material/modules/transfer/controller/TransferController.java
- backend/src/main/java/com/campus/material/modules/warning/controller/WarningController.java
- backend/src/main/java/com/campus/material/common/RemarkRequest.java

### 3) 错误码与异常处理统一
- backend/src/main/java/com/campus/material/common/ErrorCode.java
- backend/src/main/java/com/campus/material/common/BizException.java
- backend/src/main/java/com/campus/material/common/GlobalExceptionHandler.java
- backend/src/main/java/com/campus/material/security/JwtTokenProvider.java
- backend/src/main/java/com/campus/material/modules/analytics/service/AnalyticsService.java

### 3.1) 认证与文案收敛
- backend/src/main/java/com/campus/material/modules/auth/AuthService.java
- backend/src/main/java/com/campus/material/modules/notification/service/NotificationService.java

### 4) 测试补齐（建议与上面改动一并提交）
- backend/src/test/java/com/campus/material/common/ApiResponseTest.java
- backend/src/test/java/com/campus/material/common/BizExceptionTest.java
- backend/src/test/java/com/campus/material/common/GlobalExceptionHandlerTest.java
- backend/src/test/java/com/campus/material/security/JwtTokenProviderTest.java
- backend/src/test/java/com/campus/material/modules/auth/AuthServiceTest.java
- backend/src/test/java/com/campus/material/modules/auth/AuthRefreshTokenCleanupTaskTest.java
- backend/src/test/java/com/campus/material/modules/apply/controller/ApplyControllerTest.java
- backend/src/test/java/com/campus/material/modules/transfer/controller/TransferControllerTest.java
- backend/src/test/java/com/campus/material/modules/warning/controller/WarningControllerTest.java
- backend/src/test/java/com/campus/material/modules/transfer/service/TransferServiceTest.java

### 5) 工程规范
- .editorconfig
- .gitignore

## 二、可选提交（视答辩侧重点决定）

### 前端构建分包优化
- frontend/vite.config.js

### 数据脚本与迁移辅助（如本次答辩需要统一口径可一并提交）
- sql/schema.sql
- sql/seed.sql
- sql/update_pw.sql

说明：该项用于降低构建告警与后续维护成本，不影响后端功能正确性。若本次里程碑重点是“后端稳定性与安全”，可单独放到下一次提交。

## 三、不建议纳入本次提交
- backend/target/**
- frontend/dist/**
- node_modules/**
- 仅本地调试输出文件

## 四、提交前最终检查（手工）
1. 后端：mvn -f backend/pom.xml test
2. 前端：cd frontend && npm run build
3. 清理构建产物后确认 git status 干净（仅保留预期源码改动）

## 五、推荐分批提交信息
1. feat(compat): approve/reject/handle 支持 remark query+body 双兼容
2. sec(config): JWT_SECRET 必配与 CORS 默认来源收口
3. refactor(error): 引入 ErrorCode 并统一全局异常返回
4. test(core): 补齐 auth/exception/controller/service 关键路径测试
5. chore(build): editorconfig/gitignore/前端分包优化

## 六、当前工作区改动与提交批次映射（最终版）

### Commit A — 接口兼容
- backend/src/main/java/com/campus/material/modules/apply/controller/ApplyController.java
- backend/src/main/java/com/campus/material/modules/transfer/controller/TransferController.java
- backend/src/main/java/com/campus/material/modules/warning/controller/WarningController.java
- backend/src/main/java/com/campus/material/common/RemarkRequest.java

### Commit B — 安全与配置
- backend/src/main/resources/application.yml
- backend/src/main/resources/application-dev.yml
- .env.example
- DEPLOY.md
- README.md
- sql/seed.sql
- sql/update_pw.sql

### Commit C — 错误处理与认证稳定性
- backend/src/main/java/com/campus/material/common/ErrorCode.java
- backend/src/main/java/com/campus/material/common/BizException.java
- backend/src/main/java/com/campus/material/common/GlobalExceptionHandler.java
- backend/src/main/java/com/campus/material/security/JwtTokenProvider.java
- backend/src/main/java/com/campus/material/modules/auth/AuthService.java
- backend/src/main/java/com/campus/material/modules/notification/service/NotificationService.java
- backend/src/main/java/com/campus/material/modules/analytics/service/AnalyticsService.java

### Commit D — 测试补齐
- backend/src/test/java/com/campus/material/BcryptGen.java
- backend/src/test/java/com/campus/material/common/ApiResponseTest.java
- backend/src/test/java/com/campus/material/common/BizExceptionTest.java
- backend/src/test/java/com/campus/material/common/GlobalExceptionHandlerTest.java
- backend/src/test/java/com/campus/material/security/JwtTokenProviderTest.java
- backend/src/test/java/com/campus/material/modules/auth/AuthServiceTest.java
- backend/src/test/java/com/campus/material/modules/auth/AuthRefreshTokenCleanupTaskTest.java
- backend/src/test/java/com/campus/material/modules/apply/controller/ApplyControllerTest.java
- backend/src/test/java/com/campus/material/modules/apply/service/ApplyServiceTest.java
- backend/src/test/java/com/campus/material/modules/transfer/controller/TransferControllerTest.java
- backend/src/test/java/com/campus/material/modules/transfer/service/TransferServiceTest.java
- backend/src/test/java/com/campus/material/modules/warning/controller/WarningControllerTest.java
- backend/src/test/resources/application-test.yml
- backend/src/test/resources/schema.sql

### Commit E — 工程与构建优化
- .editorconfig
- .gitignore
- frontend/vite.config.js
- backend/pom.xml
- sql/schema.sql
- SUBMIT_CHECKLIST.md
