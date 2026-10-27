# PROJECT_BRIEF.md

# 建筑劳务经营与薪酬管理平台

## 文档状态

本文保留项目最初的需求、技术约定和 v0.1 设计基线。它不再定义当前正式版的
上线边界：当前交付范围、延期事项和上线前验收条件以
[`LAUNCH_SCOPE.md`](LAUNCH_SCOPE.md) 为准，实际验收与 Gate 状态以
[`UAT_SIGNOFF.md`](UAT_SIGNOFF.md) 和 [`LAUNCH_READINESS_REVIEW.md`](LAUNCH_READINESS_REVIEW.md)
为准。

正式版操作与治理资料不写在本历史基线中：桌面管理端路径见
[`USER_GUIDE_DESKTOP.md`](USER_GUIDE_DESKTOP.md)，小程序现场端路径见
[`USER_GUIDE_MINIAPP.md`](USER_GUIDE_MINIAPP.md)，缺陷阻断与延期口径见
[`LAUNCH_DEFECT_POLICY.md`](LAUNCH_DEFECT_POLICY.md)。这些文档的结论以实际 UAT 和
Gate 记录为准，不因功能已进入代码范围而自动成立。

## 1. 项目定位

本项目面向建筑劳务公司，业务以 **市政电缆线隧道工程** 为主。

平台分阶段建设：

```text
v0.1：员工信息管理 + 员工工资管理
v1.0：项目经营管理
v2.0：薪酬与绩效管理
v3.0：微信小程序端
```

当前代码已覆盖员工、工资、项目经营、薪酬绩效和微信小程序现场端。v0.1 的内容
保留为最初核心目标：

```text
1. 把员工信息录入清楚。
2. 把员工银行卡、岗位、部门、入职状态维护清楚。
3. 把每个月哪些人发了多少工资记录清楚。
4. 工资是否审核、是否发放、是否上传凭证可以追溯。
```

---

## 2. 技术栈

### Web 端

```text
React 19
React Router
TypeScript
Vite
Tailwind CSS
shadcn/ui
React Hook Form
Zod
TanStack Table
TanStack Query
ECharts
Axios
dayjs
lucide-react
```

### 服务端

```text
Spring Boot 3
Java 21
MyBatis Plus
Spring Security
JWT
PostgreSQL 18
Flyway
Spring Validation
Knife4j / OpenAPI
```

### 小程序端

微信小程序现场端已纳入当前产品范围；其正式版体验与弱网验收按
[`UAT_CHECKLIST.md`](UAT_CHECKLIST.md) 执行，并在 [`UAT_SIGNOFF.md`](UAT_SIGNOFF.md)
记录结果。

```text
微信原生小程序
Vant Weapp
```

---

## 3. 强制技术约定

```text
1. 数据库使用 PostgreSQL 18。
2. 主键统一使用 Long 类型。
3. 主键由 MyBatis Plus 雪花算法生成。
4. 实体主键使用 @TableId(type = IdType.ASSIGN_ID)。
5. 数据库主键字段只写 BIGINT PRIMARY KEY，不使用自增。
6. 金额字段使用 NUMERIC(12,2)，Java 使用 BigDecimal。
7. 时间字段 Java 使用 LocalDate / LocalDateTime。
8. 核心表必须包含 deleted、created_by、created_at、updated_by、updated_at。
9. 逻辑删除字段 deleted：0 正常，1 删除。
10. 所有接口统一返回 Result<T>。
11. Controller 不写业务逻辑。
12. Service 处理业务逻辑和事务。
13. Mapper 只负责数据库访问。
14. 历史工资明细必须冗余工资项名称和类型。
```

MyBatis Plus 配置：

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

实体主键示例：

```java
@TableId(type = IdType.ASSIGN_ID)
private Long id;
```

---

## 4. v0.1 功能范围

### 4.1 员工信息管理，重点功能

员工信息录入是 v0.1 的重点。

功能包括：

```text
员工列表
新增员工
编辑员工
员工详情
停用员工
离职员工
员工搜索
员工状态筛选
部门筛选
岗位筛选
员工银行卡信息维护
员工紧急联系人维护
员工备注信息维护
```

员工核心字段：

```text
姓名
手机号
身份证号
性别
出生日期
部门
岗位
员工类型
入职日期
离职日期
银行卡号
开户行
紧急联系人
紧急联系人电话
员工状态
备注
```

员工类型：

```text
company_employee：公司员工
project_staff：项目人员
temporary_worker：临时人员
team_leader：班组负责人
```

员工状态：

```text
active：在职
inactive：停用
left：离职
```

---

### 4.2 部门岗位管理

功能包括：

```text
部门列表
新增部门
编辑部门
停用部门
岗位列表
新增岗位
编辑岗位
停用岗位
```

---

### 4.3 工资项设置

工资项必须可配置，不能写死。

默认应发项：

```text
基本工资
岗位工资
绩效工资
餐补
交通补贴
通讯补贴
加班费
奖金
```

默认扣减项：

```text
请假扣款
社保扣款
借支扣款
罚款
其他扣款
```

工资项类型：

```text
earning：应发项
deduction：扣减项
```

---

### 4.4 员工工资档案

用于维护员工默认工资结构。

功能包括：

```text
为员工设置默认工资项
修改员工工资档案
启用/停用工资档案
按员工姓名、直属部门筛选工资档案
按直属部门查看档案数量、启用数量和金额小计
```

说明：

```text
工资档案用于生成月度工资表。
启用档案前，员工必须属于一个启用的直属部门。
生成后的月度工资明细可以单独调整，不影响工资档案。
```

---

### 4.5 月度工资表

核心流程：

```text
选择工资月份和直属部门，创建一张部门工资表
  ↓
读取该直属部门内的在职员工工资档案
  ↓
根据工资档案生成工资明细
  ↓
财务调整工资明细
  ↓
提交审核
  ↓
老板审核
  ↓
财务登记发放
  ↓
上传发放凭证
```

同一月份允许不同部门分别创建、生成、审核和发放工资表；同一部门同一月份只能存在一张
正常工资表。工资明细保存生成时的直属部门快照，员工后续调部门不会改写历史。服务端禁止
同一员工进入同月份两张正常工资表；发生月内调动时，应先重算原部门草稿工资表移除员工，
原表已提交或锁定时不得在新部门重复生成。

月度列表以月份为一级分组、部门工资表为组内核算单元。工作台、工资统计和月度趋势默认
按月份汇总全部部门，工资统计可再按直属部门筛选。

工资表状态：

```text
draft：草稿
pending_approval：待审核
approved：已审核
paid：已发放
archived：已归档
cancelled：已作废
```

---

### 4.6 工资发放记录

功能包括：

```text
单人标记发放
批量标记发放
记录发放日期
记录发放方式
上传发放凭证
查看未发放人员
查看已发放人员
查看员工历史工资
```

发放方式：

```text
bank_transfer：银行转账
cash：现金
wechat：微信
alipay：支付宝
other：其他
```

---

### 4.7 工资统计

v0.1 至少实现：

```text
本月应发工资总额
本月扣款总额
本月实发工资总额
本月已发金额
本月未发金额
本月发放人数
本月未发人数
部门工资汇总
近 12 个月工资趋势
员工历史工资记录
```

---

## 5. Web 页面路由

```text
/login

/dashboard

/employees
/employees/create
/employees/:id
/employees/:id/edit

/departments
/positions

/salary/items
/salary/profiles
/salary/monthly
/salary/monthly/create
/salary/monthly/:id
/salary/monthly/:id/edit
/salary/monthly/:id/payment

/statistics/salary

/system/users
/system/roles
/system/logs
```

---

## 6. Web 目录结构

```text
clients/desktop/
├── src/
│   ├── app/
│   │   ├── router.tsx
│   │   └── providers.tsx
│   ├── components/
│   │   ├── ui/
│   │   ├── layout/
│   │   └── common/
│   ├── features/
│   │   ├── auth/
│   │   ├── dashboard/
│   │   ├── employee/
│   │   ├── department/
│   │   ├── position/
│   │   ├── salary-item/
│   │   ├── salary-profile/
│   │   ├── salary-monthly/
│   │   ├── statistics/
│   │   └── system/
│   ├── hooks/
│   ├── lib/
│   │   ├── axios.ts
│   │   ├── auth.ts
│   │   └── utils.ts
│   ├── types/
│   └── main.tsx
```

每个 feature 结构：

```text
features/employee/
├── api.ts
├── types.ts
├── schema.ts
├── hooks.ts
├── pages/
└── components/
```

---

## 7. Spring Boot 后端结构

后端按照阿里 Java 开发规约风格组织。

核心原则：

```text
1. 包名小写。
2. 类名使用 UpperCamelCase。
3. DO / DTO / BO / Query / VO 分清楚。
4. Service 接口和实现类分离。
5. Controller 不直接调用 Mapper。
6. Mapper 不写业务逻辑。
```

目录结构：

```text
server/
├── pom.xml
└── src/main/
    ├── java/com/company/labor/
    │   ├── LaborApplication.java
    │   │
    │   ├── common/
    │   │   ├── constant/
    │   │   ├── enums/
    │   │   ├── exception/
    │   │   ├── result/
    │   │   └── util/
    │   │
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   ├── MybatisPlusConfig.java
    │   │   ├── WebMvcConfig.java
    │   │   └── OpenApiConfig.java
    │   │
    │   ├── module/
    │   │   ├── system/
    │   │   │   ├── controller/
    │   │   │   ├── service/
    │   │   │   ├── service/impl/
    │   │   │   ├── mapper/
    │   │   │   ├── converter/
    │   │   │   └── domain/
    │   │   │       ├── entity/
    │   │   │       ├── dto/
    │   │   │       ├── bo/
    │   │   │       ├── query/
    │   │   │       └── vo/
    │   │   │
    │   │   ├── employee/
    │   │   │   ├── controller/
    │   │   │   ├── service/
    │   │   │   ├── service/impl/
    │   │   │   ├── mapper/
    │   │   │   ├── converter/
    │   │   │   └── domain/
    │   │   │       ├── entity/
    │   │   │       ├── dto/
    │   │   │       ├── bo/
    │   │   │       ├── query/
    │   │   │       └── vo/
    │   │   │
    │   │   ├── salary/
    │   │   │   ├── controller/
    │   │   │   ├── service/
    │   │   │   ├── service/impl/
    │   │   │   ├── mapper/
    │   │   │   ├── converter/
    │   │   │   └── domain/
    │   │   │       ├── entity/
    │   │   │       ├── dto/
    │   │   │       ├── bo/
    │   │   │       ├── query/
    │   │   │       └── vo/
    │   │   │
    │   │   └── file/
    │   │       ├── controller/
    │   │       ├── service/
    │   │       ├── service/impl/
    │   │       ├── mapper/
    │   │       ├── converter/
    │   │       └── domain/
    │   │           ├── entity/
    │   │           ├── dto/
    │   │           ├── bo/
    │   │           ├── query/
    │   │           └── vo/
    │   │
    │   └── security/
    │       ├── JwtAuthenticationFilter.java
    │       ├── JwtTokenProvider.java
    │       └── LoginUser.java
    │
    └── resources/
        ├── application.yml
        ├── mapper/
        │   ├── system/
        │   ├── employee/
        │   ├── salary/
        │   └── file/
        └── db/migration/
            └── V1__init_schema.sql
```

命名规范：

```text
数据库实体：XxxDO
创建参数：XxxCreateDTO
更新参数：XxxUpdateDTO
查询参数：XxxQuery
业务对象：XxxBO
返回对象：XxxVO
转换类：XxxConverter
枚举类：XxxEnum
常量类：XxxConstants
```

示例：

```text
EmployeeDO
EmployeeCreateDTO
EmployeeUpdateDTO
EmployeeQuery
EmployeeBO
EmployeeVO
EmployeeConverter
EmployeeController
EmployeeService
EmployeeServiceImpl
EmployeeMapper
```

---

## 8. 核心数据库表

### 8.1 系统表

```sql
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  employee_id BIGINT,
  nickname VARCHAR(64),
  phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
);
```

---

### 8.2 员工基础表

```sql
CREATE TABLE department (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  parent_id BIGINT,
  sort_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE position (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  department_id BIGINT,
  sort_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee (
  id BIGINT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  id_card VARCHAR(32),
  gender VARCHAR(16),
  birth_date DATE,
  department_id BIGINT,
  position_id BIGINT,
  employee_type VARCHAR(32) NOT NULL DEFAULT 'company_employee',
  entry_date DATE,
  leave_date DATE,
  bank_account VARCHAR(64),
  bank_name VARCHAR(128),
  emergency_contact VARCHAR(64),
  emergency_phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_name ON employee(name);
CREATE INDEX idx_employee_phone ON employee(phone);
CREATE INDEX idx_employee_department_id ON employee(department_id);
CREATE INDEX idx_employee_position_id ON employee(position_id);
```

---

### 8.3 工资表

```sql
CREATE TABLE salary_item (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(32) NOT NULL,
  is_fixed SMALLINT NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE salary_profile (
  id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  effective_date DATE,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE salary_profile_item (
  id BIGINT PRIMARY KEY,
  salary_profile_id BIGINT NOT NULL,
  salary_item_id BIGINT NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255)
);

CREATE TABLE salary_monthly (
  id BIGINT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL,
  title VARCHAR(100) NOT NULL,
  total_employee_count INTEGER NOT NULL DEFAULT 0,
  total_payable_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  total_deduction_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  total_actual_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  created_by BIGINT,
  approved_by BIGINT,
  approved_at TIMESTAMP WITHOUT TIME ZONE,
  paid_at TIMESTAMP WITHOUT TIME ZONE,
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_salary_monthly_month
ON salary_monthly(salary_month)
WHERE deleted = 0;

CREATE TABLE salary_monthly_detail (
  id BIGINT PRIMARY KEY,
  salary_monthly_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  department_id BIGINT,
  position_id BIGINT,
  project_id BIGINT,
  payable_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  deduction_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  actual_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  payment_status VARCHAR(32) NOT NULL DEFAULT 'unpaid',
  payment_date DATE,
  payment_method VARCHAR(32),
  payment_proof_url VARCHAR(255),
  remark VARCHAR(255),
  deleted SMALLINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE salary_detail_item (
  id BIGINT PRIMARY KEY,
  salary_detail_id BIGINT NOT NULL,
  salary_item_id BIGINT,
  salary_item_name VARCHAR(100) NOT NULL,
  salary_item_type VARCHAR(32) NOT NULL,
  amount NUMERIC(12,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255)
);
```

---

### 8.4 文件与日志

```sql
CREATE TABLE file_record (
  id BIGINT PRIMARY KEY,
  original_name VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  content_type VARCHAR(100),
  created_by BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY,
  module VARCHAR(64) NOT NULL,
  business_id BIGINT,
  action VARCHAR(64) NOT NULL,
  operator_id BIGINT,
  operator_name VARCHAR(64),
  content TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 9. 后端核心接口

### 认证

```text
POST /api/auth/login
GET /api/auth/me
POST /api/auth/logout
```

### 员工

```text
GET /api/employees
POST /api/employees
GET /api/employees/{id}
PUT /api/employees/{id}
DELETE /api/employees/{id}
PUT /api/employees/{id}/status
```

### 部门岗位

```text
GET /api/departments
POST /api/departments
PUT /api/departments/{id}
DELETE /api/departments/{id}

GET /api/positions
POST /api/positions
PUT /api/positions/{id}
DELETE /api/positions/{id}
```

### 工资

```text
GET /api/salary/items
POST /api/salary/items
PUT /api/salary/items/{id}
DELETE /api/salary/items/{id}

GET /api/salary/profiles
POST /api/salary/profiles
GET /api/salary/profiles/{id}
PUT /api/salary/profiles/{id}

GET /api/salary/monthly
POST /api/salary/monthly
GET /api/salary/monthly/{id}
PUT /api/salary/monthly/{id}
POST /api/salary/monthly/{id}/generate
POST /api/salary/monthly/{id}/submit
POST /api/salary/monthly/{id}/approve
POST /api/salary/monthly/{id}/reject
POST /api/salary/monthly/{id}/mark-paid

GET /api/salary/monthly/{monthlyId}/details
PUT /api/salary/details/{detailId}
POST /api/salary/details/{detailId}/mark-paid
POST /api/salary/details/batch-mark-paid
```

### 文件与统计

```text
POST /api/files/upload
GET /api/files/{id}

GET /api/statistics/salary/overview
GET /api/statistics/salary/monthly-trend
GET /api/statistics/salary/department-summary
GET /api/statistics/salary/employee-history/{employeeId}
```

---

## 10. 统一返回结构

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}
```

---

## 11. 核心业务规则

### 员工信息规则

```text
1. 员工姓名必填。
2. 手机号建议唯一。
3. 员工离职后，不参与后续工资表自动生成。
4. 停用员工不允许登录。
5. 员工历史工资记录不因员工离职而删除。
```

### 工资表生成规则

```text
1. 选择工资月份。
2. 查询所有在职员工。
3. 根据员工工资档案生成工资明细。
4. 汇总应发项为 payable_amount。
5. 汇总扣减项为 deduction_amount。
6. actual_amount = payable_amount - deduction_amount。
7. 工资明细生成后允许在草稿状态下手动调整。
```

### 工资表状态规则

```text
draft：允许编辑。
pending_approval：不允许编辑。
approved：允许发放，不允许编辑。
paid：不允许编辑。
archived：不允许编辑。
cancelled：不允许编辑。
```

### 审核发放规则

```text
1. 只有 BOSS 或 ADMIN 可以审核。
2. 只有 approved 状态可以发放。
3. 支持单人发放和批量发放。
4. 发放时记录发放日期、发放方式、发放凭证。
5. 全部明细发放后，工资表状态变为 paid。
```

---

## 12. 初始化数据

默认角色：

```text
ADMIN：超级管理员
BOSS：老板/负责人
FINANCE：财务
MANAGER：部门负责人
EMPLOYEE：普通员工
```

默认管理员：

```text
username：admin
password：生产环境不提供固定默认密码，首次部署必须通过安全初始化流程设置管理员密码。
```

密码必须 BCrypt 加密入库。

---

## 13. v0.1 历史开发顺序

```text
1. 后端基础工程、PostgreSQL、Flyway、MyBatis Plus
2. Spring Security + JWT 登录认证
3. 前端基础工程、登录页、后台布局
4. 部门管理
5. 岗位管理
6. 员工信息管理
7. 工资项设置
8. 员工工资档案
9. 月度工资表生成
10. 工资明细编辑
11. 工资审核
12. 工资发放登记
13. 文件上传凭证
14. 工资统计
15. 操作日志
```

注意：本节用于保留 v0.1 的建设顺序，不作为当前版本的待办列表。

---

## 14. v0.1 原始验收标准

```text
1. 可以登录系统。
2. 可以维护部门和岗位。
3. 可以新增、编辑、停用、离职员工。
4. 可以完整录入员工基础信息、银行卡信息、紧急联系人。
5. 可以按姓名、手机号、部门、岗位、状态筛选员工。
6. 可以配置工资项。
7. 可以为员工设置工资档案。
8. 可以创建月度工资表。
9. 可以根据工资档案生成工资明细。
10. 可以编辑草稿工资明细。
11. 可以提交审核。
12. BOSS 可以审核通过或驳回。
13. 财务可以登记发放。
14. 可以上传发放凭证。
15. 可以查看工资统计。
16. 可以查看员工历史工资。
17. 已发工资不能随意修改。
18. 主键全部由 MyBatis Plus 雪花算法生成。
```

---

## 15. v0.1 历史范围限制

```text
1. 不开发项目经营模块。
2. 不开发绩效模块。
3. 不开发小程序端。
4. 不开发复杂考勤。
5. 不开发社保个税自动计算。
6. 不开发复杂 OA 审批流。
7. 不开发班组结算。
8. 不开发工程量日报。
```

上述限制仅适用于最初的 v0.1 MVP 阶段。项目经营、薪酬绩效和小程序现场端已进入
当前代码范围；正式上线的纳入和延期事项以 `docs/LAUNCH_SCOPE.md` 为准。

---

## 16. v0.1 历史启动指令

```text
请严格按照 PROJECT_BRIEF.md 初始化并开发项目。

以下内容是项目初始化时期的约束示例，不再表示当前只实现 v0.1。

强制要求：
1. Web 端使用 React 19、React Router、TypeScript、Tailwind CSS、shadcn/ui、React Hook Form、Zod、TanStack Table、TanStack Query、ECharts。
2. 服务端使用 Spring Boot 3、Java 21、MyBatis Plus、Spring Security、JWT。
3. 数据库使用 PostgreSQL 18。
4. 主键统一为 Long 类型。
5. 主键由 MyBatis Plus 雪花算法生成，使用 IdType.ASSIGN_ID。
6. PostgreSQL 表主键只使用 BIGINT PRIMARY KEY，不使用自增。
7. 后端项目结构按照本文档中的阿里 Java 开发规约风格组织。
8. v0.1 必须优先完成员工信息录入、员工列表、员工详情、部门岗位、工资项、工资档案、月度工资表、工资审核、工资发放和工资统计。
```
