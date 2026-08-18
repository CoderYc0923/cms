-- 002-add-admin-account.sql

-- 添加测试用户
INSERT INTO users (username, password, display_name, status) 
SELECT 'test', '$2a$10$8kUApsQZUm/ld0gbWCArIeUSwtnSgfOBT3KkZ7FB.xpF82iU30ooa', 'testUser', 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'test');