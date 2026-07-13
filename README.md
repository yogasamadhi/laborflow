# LaborFlow

建筑劳务经营与薪酬管理平台 v0.1 MVP，当前聚焦员工信息管理与工资发放管理。

## 技术栈

- Web：React 19、React Router、TypeScript、Vite、Tailwind CSS 风格组件、TanStack Query、TanStack Table、React Hook Form、Zod、ECharts、Axios、dayjs、lucide-react
- Miniapp：微信原生小程序、Vant Weapp 依赖预置
- Server：Spring Boot 3、Java 21、MyBatis Plus、Spring Security、JWT、Flyway、PostgreSQL 18、OpenAPI/Knife4j

## 本地启动

```bash
cp .env.example .env
docker compose up -d
```

PostgreSQL 默认映射到宿主机 `5434` 端口，后端默认连接 `jdbc:postgresql://localhost:5434/laborflow`。

默认服务端口：前端 `5175`，后端 `8089`。

后端需要 Java 21 和 Maven：

```bash
cd server
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn spring-boot:run
```

前端：

```bash
cd web
pnpm install
pnpm dev
```

小程序：

```bash
微信开发者工具导入 miniapp/
```

生产环境不提供可直接登录的默认账号。首次管理员必须通过下方受控的启动期初始化流程
创建，不应通过自助注册、公开接口或手工修改前端来绕过权限边界。

## 正式版验收资料

当前正式上线范围、明确延期项和非部署 Gate 条件见
[`docs/LAUNCH_SCOPE.md`](docs/LAUNCH_SCOPE.md)。实际执行进度与完成记录见
[`docs/LOOPMAP2.md`](docs/LOOPMAP2.md)，它不应被代码存在与自动化通过所替代。

- Web 管理端角色路径见 [`docs/USER_GUIDE_WEB.md`](docs/USER_GUIDE_WEB.md)。
- 小程序现场端角色路径见 [`docs/USER_GUIDE_MINIAPP.md`](docs/USER_GUIDE_MINIAPP.md)。
- 缺陷分级、阻断与延期批准要求见
  [`docs/LAUNCH_DEFECT_POLICY.md`](docs/LAUNCH_DEFECT_POLICY.md)。
- 业务 UAT 场景与证据要求见 [`docs/UAT_CHECKLIST.md`](docs/UAT_CHECKLIST.md) 和
  [`docs/REGRESSION_EVIDENCE_POLICY.md`](docs/REGRESSION_EVIDENCE_POLICY.md)。
- 当前非部署 Gate 结论和阻断项见
  [`docs/LAUNCH_READINESS_REVIEW.md`](docs/LAUNCH_READINESS_REVIEW.md) 与
  [`docs/UAT_SIGNOFF.md`](docs/UAT_SIGNOFF.md)；当前结论为“不批准”，不是上线通过声明。

这些资料只使用测试或脱敏数据；不得在仓库、截图或验收记录中写入密码、token、真实
个人信息或生产数据。

## 首次管理员初始化

Flyway 会创建默认角色和一个已停用的历史管理员账号，但不会提供可登录的默认密码。
当数据库没有启用的 `ADMIN` 用户时，服务启动会拒绝继续运行，直到通过运行环境同时提供
下列一次性配置：

```text
BOOTSTRAP_ADMIN_USERNAME=<3-64 个字符的管理员账号>
BOOTSTRAP_ADMIN_PASSWORD=<12-64 个字符的管理员密码>
```

启动期只会在不存在启用的 `ADMIN` 时创建管理员，或恢复同名且已有 `ADMIN` 角色的停用
账号；它不会提升同名普通账号。密码只会 BCrypt 加密入库，且不会写入日志。首次管理员
登录并创建日常账号后，应从运行环境移除这两个配置并重启一次确认不会再次执行。

完整的角色、工资项、工序库、测试账号和微信绑定准备步骤见
[`docs/INITIALIZATION_CHECKLIST.md`](docs/INITIALIZATION_CHECKLIST.md)。

## 后端集成测试

后端的 `mvn test` 会运行单元测试以及 PostgreSQL 集成测试。集成测试使用 Testcontainers
启动一次短生命周期的 PostgreSQL 18，自动执行 Flyway，并验证管理员登录和受保护接口；
运行该命令的本机或 CI runner 需要可访问 Docker。

```bash
cd server
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
```

Gitee pull-request gate configuration and its Docker/Testcontainers runner
requirement are documented in [`docs/CI_BACKEND_GATE.md`](docs/CI_BACKEND_GATE.md).
The Web typecheck, test, and build gate is documented in
[`docs/CI_WEB_GATE.md`](docs/CI_WEB_GATE.md).
The miniapp syntax gate and manual smoke evidence template are documented in
[`docs/CI_MINIAPP_GATE.md`](docs/CI_MINIAPP_GATE.md).
The documentation, Flyway migration, and whitespace gate is documented in
[`docs/CI_DOCS_GATE.md`](docs/CI_DOCS_GATE.md).
The required pull-request regression evidence and review rule for payroll,
projects, permissions, and migrations is documented in
[`docs/REGRESSION_EVIDENCE_POLICY.md`](docs/REGRESSION_EVIDENCE_POLICY.md).
The automated payroll lifecycle regression and accompanying Web smoke record
are documented in [`docs/PAYROLL_E2E_REGRESSION.md`](docs/PAYROLL_E2E_REGRESSION.md).
The project daily-report, team-settlement, payment, and dashboard regression is
documented in [`docs/PROJECT_SETTLEMENT_E2E_REGRESSION.md`](docs/PROJECT_SETTLEMENT_E2E_REGRESSION.md).
The variation-order attachment, commercial confirmation, and project-income
regression is documented in
[`docs/VARIATION_INCOME_E2E_REGRESSION.md`](docs/VARIATION_INCOME_E2E_REGRESSION.md).
The attendance, statutory-deduction, performance, and project-bonus payroll
source regression is documented in
[`docs/SALARY_SOURCES_E2E_REGRESSION.md`](docs/SALARY_SOURCES_E2E_REGRESSION.md).
The authorization, salary-scope, project-scope, sensitive-user-field, and Web
no-permission regression is documented in
[`docs/ACCESS_CONTROL_E2E_REGRESSION.md`](docs/ACCESS_CONTROL_E2E_REGRESSION.md).
The cross-module operation-log regression and administrator smoke record are
documented in [`docs/AUDIT_LOG_E2E_REGRESSION.md`](docs/AUDIT_LOG_E2E_REGRESSION.md).
The private-file download and cross-business attachment-boundary regression is
documented in [`docs/FILE_ACCESS_E2E_REGRESSION.md`](docs/FILE_ACCESS_E2E_REGRESSION.md).


## 目录

```text
web/     React 管理后台
server/  Spring Boot API 服务
miniapp/ 微信小程序端
```
