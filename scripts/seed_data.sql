USE shi_gui;

-- 测试管理员 (密码: admin123)
-- 哈希格式: salt:sha256(salt + password)
-- 生成方法见 AdminUserServiceImpl.verifyPassword
INSERT INTO admin_user (username, password_hash) VALUES
('admin', '1Kgtp+nNHV4Nignq9aOyjw==:42cf36653d882acf983640003bf6ab9439ae1bb8ac7653970641d43e967b86fe');

-- 测试用户
INSERT INTO app_user (openid, nickname, role) VALUES
('dev_test_openid_001', '测试用户A', 'USER'),
('dev_test_openid_002', '测试用户B', 'USER');
