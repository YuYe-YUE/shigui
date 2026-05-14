CREATE DATABASE IF NOT EXISTS shi_gui DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shi_gui;

-- 小程序用户
CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(64) DEFAULT '',
    avatar_url VARCHAR(512) DEFAULT '',
    role VARCHAR(16) DEFAULT 'USER',
    status VARCHAR(16) DEFAULT 'NORMAL' COMMENT 'NORMAL/BANNED',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 后台管理员
CREATE TABLE admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 失物招领单据
CREATE TABLE lost_found_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_type VARCHAR(8) NOT NULL COMMENT 'LOST/FOUND',
    title VARCHAR(128) DEFAULT '',
    item_name VARCHAR(64) DEFAULT '',
    item_category VARCHAR(32) DEFAULT '',
    description TEXT,
    private_feature TEXT COMMENT '非公开特征，仅物主可知',
    campus_area VARCHAR(64) DEFAULT '',
    location_name VARCHAR(128) DEFAULT '',
    longitude DECIMAL(10,7) DEFAULT NULL,
    latitude DECIMAL(10,7) DEFAULT NULL,
    storage_location VARCHAR(256) DEFAULT '' COMMENT '暂存地点，仅招领单填写',
    event_time DATETIME DEFAULT NULL,
    status VARCHAR(32) DEFAULT 'PENDING_AUDIT' COMMENT 'PENDING_AUDIT/MATCHING/CLAIMING/RETURNING/COMPLETED',
    published_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_post_type (post_type),
    INDEX idx_location (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 内容审核操作日志
CREATE TABLE audit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL COMMENT 'APPROVE/DELETE',
    reason VARCHAR(512) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_id (admin_id),
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 认领申请与核验
CREATE TABLE claim_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    claimant_user_id BIGINT NOT NULL COMMENT '发起认领的用户',
    private_feature_answer TEXT COMMENT '失主填写的私密特征',
    status VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/VERIFIED/REJECTED/RETURNING/COMPLETED',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_claimant (claimant_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 智能匹配结果
CREATE TABLE match_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lost_post_id BIGINT NOT NULL,
    found_post_id BIGINT NOT NULL,
    score DECIMAL(5,4) DEFAULT 0 COMMENT '匹配得分 0-1',
    reason TEXT COMMENT 'AI 匹配理由',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lost_post (lost_post_id),
    INDEX idx_found_post (found_post_id),
    UNIQUE KEY uk_post_pair (lost_post_id, found_post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知（站内/微信）
CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) DEFAULT 'SYSTEM' COMMENT 'MATCH/CLAIM/AUDIT/SYSTEM',
    title VARCHAR(256) DEFAULT '',
    content TEXT,
    related_id BIGINT DEFAULT NULL COMMENT '关联业务ID',
    is_read TINYINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 匿名聊天会话
CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    lost_user_id BIGINT NOT NULL,
    found_user_id BIGINT NOT NULL,
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_users (lost_user_id, found_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 聊天消息
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    msg_type VARCHAR(8) DEFAULT 'TEXT' COMMENT 'TEXT/IMAGE',
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统配置
CREATE TABLE system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(256) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
