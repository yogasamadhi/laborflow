# 时区策略

## 背景

历史数据库迁移（V1–V22）的审计与业务时间列统一使用
`TIMESTAMP WITHOUT TIME ZONE`，Java 实体使用 `LocalDateTime`。这意味着时间
语义依赖读取方的时区：同一行数据在不同时区的 JVM 里会渲染成不同的本地时间。
对跨日结算、考勤、工资月份边界等场景，时区漂移会引发隐蔽 bug。

本策略在**不改动历史列类型**的前提下，把系统时间语义固定到一个已知时区，
消除跨服务器/跨本地的漂移。

## 约定

- **业务时区**：`Asia/Shanghai`（可通过 `LABORFLOW_TIMEZONE` 覆盖，但所有
  实例必须一致）。
- **JSON 序列化**：`spring.jackson.time-zone` 在 `application.yml` 固定为
  `${LABORFLOW_TIMEZONE:Asia/Shanghai}`，保证 API 输出的日期/时间字段不随
  JVM 默认时区变化。
- **JVM 默认时区**：`run.py` 启动服务时注入 `TZ=Asia/Shanghai`（除非已设置）。
  生产部署须在容器/进程环境显式设置 `TZ=Asia/Shanghai`。
- **逻辑时区来源**：`LoginSecurityService` 等使用 `Clock.systemDefaultZone()`，
  因此固定 JVM 时区即固定了账号锁定/登录记录等的时间基准。

## 新表/新列规范

- 新增的时间列一律使用 `TIMESTAMP WITH TIME ZONE`，避免再引入无时区列。
- 对应 Java 字段使用 `Instant` 或 `OffsetDateTime`，而非 `LocalDateTime`。
- 该规范由评审把关（无自动化守卫）；写迁移时在 PR 中显式说明。

## 现有列的迁移（未执行，评估后再立项）

把历史 `TIMESTAMP WITHOUT TIME ZONE` 列改为 `WITH TIME ZONE` 是**有损操作**：
PostgreSQL 会把已存储的本地时间原样解释为目标时区，若列中混入了不同写入时区
的历史数据，转换结果可能错误。因此本策略**不包含**批量迁移，仅在发现具体业务
受时区影响（例如跨日结算偏差）时，按列逐项评估并立项迁移。

## 相关配置

| 配置 | 位置 | 默认 |
|---|---|---|
| `LABORFLOW_TIMEZONE` | `.env` / 运行环境 | `Asia/Shanghai` |
| `TZ` | 运行环境（run.py / 容器）| `Asia/Shanghai` |
| `spring.jackson.time-zone` | `application.yml` | `${LABORFLOW_TIMEZONE:Asia/Shanghai}` |
