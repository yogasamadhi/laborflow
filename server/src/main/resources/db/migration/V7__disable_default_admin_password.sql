UPDATE sys_user
SET password = '{disabled-default-admin}',
    status = 'inactive',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin'
  AND password = '$2y$10$0qX9HImzN8USewbUbq3pSee2Th3lfQyc4L94d4DFc.Rk4e9EpKWHa';
