INSERT INTO sys_role (id, code, name, status)
VALUES
  (1001, 'ADMIN', '超级管理员', 'active'),
  (1002, 'BOSS', '老板/负责人', 'active'),
  (1003, 'FINANCE', '财务', 'active'),
  (1004, 'MANAGER', '部门负责人', 'active'),
  (1005, 'EMPLOYEE', '普通员工', 'active');

INSERT INTO sys_user (id, username, password, nickname, status)
VALUES
  (1001, 'admin', '$2y$10$0qX9HImzN8USewbUbq3pSee2Th3lfQyc4L94d4DFc.Rk4e9EpKWHa', '系统管理员', 'active');

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES
  (1001, 1001, 1001),
  (1002, 1001, 1002),
  (1003, 1001, 1003);

INSERT INTO salary_item (id, name, type, is_fixed, sort_order, status)
VALUES
  (2001, '基本工资', 'earning', 1, 10, 'active'),
  (2002, '岗位工资', 'earning', 1, 20, 'active'),
  (2003, '绩效工资', 'earning', 1, 30, 'active'),
  (2004, '餐补', 'earning', 1, 40, 'active'),
  (2005, '交通补贴', 'earning', 1, 50, 'active'),
  (2006, '通讯补贴', 'earning', 1, 60, 'active'),
  (2007, '加班费', 'earning', 0, 70, 'active'),
  (2008, '奖金', 'earning', 0, 80, 'active'),
  (2101, '请假扣款', 'deduction', 0, 110, 'active'),
  (2102, '社保扣款', 'deduction', 1, 120, 'active'),
  (2103, '借支扣款', 'deduction', 0, 130, 'active'),
  (2104, '罚款', 'deduction', 0, 140, 'active'),
  (2105, '其他扣款', 'deduction', 0, 150, 'active');

