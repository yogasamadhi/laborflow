# LaborFlow

建筑劳务经营与薪酬管理平台 v0.1 MVP，当前聚焦员工信息管理与工资发放管理。

## 技术栈

- Web：React 19、React Router、TypeScript、Vite、Tailwind CSS 风格组件、TanStack Query、TanStack Table、React Hook Form、Zod、ECharts、Axios、dayjs、lucide-react
- Server：Spring Boot 3、Java 21、MyBatis Plus、Spring Security、JWT、Flyway、PostgreSQL 18、OpenAPI/Knife4j

## 本地启动

```bash
cp .env.example .env
docker compose up -d
```

后端需要 Java 21 和 Maven：

```bash
cd server
mvn spring-boot:run
```

前端：

```bash
cd web
npm install
npm run dev
```

默认账号：

```text
admin / admin123456
```

## 目录

```text
web/     React 管理后台
server/  Spring Boot API 服务
```

