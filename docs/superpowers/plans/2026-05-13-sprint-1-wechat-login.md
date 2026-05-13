# Sprint 1: 微信登录认证 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建三端脚手架，实现微信小程序一键登录 + 管理端管理员登录，为后续 Sprint 提供身份基础。

**Architecture:** Spring Boot 3.5.14 后端 + MyBatis-Plus ORM + Sa-Token 鉴权，Vue 3 管理端，原生微信小程序。统一 Result<T> 响应体，CORS 全放行，Sa-Token 拦截 /api/** 并排除公开端点。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, Maven, Vue 3 + Vite + TS + Element Plus, 原生微信小程序, MySQL 8.0

---

### Task 1: 项目目录结构与 Maven 脚手架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/.mvn/maven.config`
- Create: `backend/src/main/java/com/shigui/BackendApplication.java`
- Create: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/resources/application-local.properties`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p /Users/cyrene/Dev/shigui/backend/src/main/java/com/shigui
mkdir -p /Users/cyrene/Dev/shigui/backend/src/main/resources
mkdir -p /Users/cyrene/Dev/shigui/backend/src/test/java/com/shigui
mkdir -p /Users/cyrene/Dev/shigui/backend/.mvn
mkdir -p /Users/cyrene/Dev/shigui/scripts
mkdir -p /Users/cyrene/Dev/shigui/admin-web
mkdir -p /Users/cyrene/Dev/shigui/miniapp
```

- [ ] **Step 2: 编写 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>
    <groupId>com.shigui</groupId>
    <artifactId>backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>backend</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.16</version>
        </dependency>
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>1.45.0</version>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Maven wrapper 配置**

```bash
# maven.config: 本地仓库重定向到项目外部
```

写入 `backend/.mvn/maven.config`:
```
-Dmaven.repo.local=../.maven/repository
```

- [ ] **Step 4: 编写 Spring Boot 主类**

`backend/src/main/java/com/shigui/BackendApplication.java`:
```java
package com.shigui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

- [ ] **Step 5: 编写应用配置**

`backend/src/main/resources/application.properties`:
```properties
spring.application.name=backend
spring.profiles.active=local

spring.datasource.url=jdbc:mysql://localhost:3306/shi_gui?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis-plus.configuration.map-underscore-to-camel-case=true
mybatis-plus.global-config.db-config.logic-delete-field=deleted
mybatis-plus.global-config.db-config.logic-delete-value=1
mybatis-plus.global-config.db-config.logic-not-delete-value=0

sa-token.token-name=satoken
sa-token.timeout=604800
sa-token.is-read-body=false
```

`backend/src/main/resources/application-local.properties`:
```properties
spring.datasource.password=Hang0611@
```

- [ ] **Step 6: 验证编译**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw compile
```

预期: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/
git commit -m "chore: init backend Maven scaffold (Spring Boot 3.5 + MyBatis-Plus + Sa-Token)"
```

---

### Task 2: 数据库初始化脚本

**Files:**
- Create: `scripts/init_schema.sql`
- Create: `scripts/seed_data.sql`

- [ ] **Step 1: 编写建表脚本**

`scripts/init_schema.sql`:
```sql
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
```

- [ ] **Step 2: 编写种子数据**

`scripts/seed_data.sql`:
```sql
USE shi_gui;

-- 测试管理员 (密码: admin123)
-- 哈希格式: salt:sha256(salt + password)
-- 生成方法见 AdminUserServiceImpl.verifyPassword
INSERT INTO admin_user (username, password_hash) VALUES
('admin', 'placeholder_run_Task11_Step2_to_generate');

-- 测试用户
INSERT INTO app_user (openid, nickname, role) VALUES
('dev_test_openid_001', '测试用户A', 'USER'),
('dev_test_openid_002', '测试用户B', 'USER');
```

- [ ] **Step 3: 执行建库建表**

```bash
mysql -u root -pHang0611@ < scripts/init_schema.sql
mysql -u root -pHang0611@ < scripts/seed_data.sql
```

- [ ] **Step 4: Commit**

```bash
git add scripts/
git commit -m "feat: database schema (10 tables) and seed data"
```

---

### Task 3: 后端公共基础设施

**Files:**
- Create: `backend/src/main/java/com/shigui/common/Result.java`
- Create: `backend/src/main/java/com/shigui/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/shigui/config/CorsConfig.java`
- Create: `backend/src/main/java/com/shigui/config/MybatisPlusConfig.java`
- Create: `backend/src/main/java/com/shigui/config/SaTokenConfig.java`

- [ ] **Step 1: 统一响应体**

`backend/src/main/java/com/shigui/common/Result.java`:
```java
package com.shigui.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
```

- [ ] **Step 2: 全局异常处理器**

`backend/src/main/java/com/shigui/common/GlobalExceptionHandler.java`:
```java
package com.shigui.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(401, "未登录或登录已过期");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadRequest(IllegalArgumentException e) {
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        return Result.fail(500, "服务器内部错误");
    }
}
```

- [ ] **Step 3: CORS 配置**

`backend/src/main/java/com/shigui/config/CorsConfig.java`:
```java
package com.shigui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 4: MyBatis-Plus 配置**

`backend/src/main/java/com/shigui/config/MybatisPlusConfig.java`:
```java
package com.shigui.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.shigui.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 5: Sa-Token 鉴权配置**

`backend/src/main/java/com/shigui/config/SaTokenConfig.java`:
```java
package com.shigui.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/wx-login",
                        "/api/admin/login",
                        "/api/posts/map"
                );
    }
}
```

- [ ] **Step 6: 验证编译**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw compile
```

预期: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/shigui/common/ backend/src/main/java/com/shigui/config/
git commit -m "feat: Result, GlobalExceptionHandler, CorsConfig, MybatisPlusConfig, SaTokenConfig"
```

---

### Task 4: AppUser 实体与 Mapper

**Files:**
- Create: `backend/src/main/java/com/shigui/entity/AppUser.java`
- Create: `backend/src/main/java/com/shigui/mapper/AppUserMapper.java`

- [ ] **Step 1: AppUser 实体**

`backend/src/main/java/com/shigui/entity/AppUser.java`:
```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: AppUserMapper**

`backend/src/main/java/com/shigui/mapper/AppUserMapper.java`:
```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
```

- [ ] **Step 3: 验证编译**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw compile
```

预期: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/shigui/entity/AppUser.java backend/src/main/java/com/shigui/mapper/AppUserMapper.java
git commit -m "feat: AppUser entity and mapper"
```

---

### Task 5: AppUserService（含测试）

**Files:**
- Create: `backend/src/main/java/com/shigui/service/AppUserService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/AppUserServiceImpl.java`
- Create: `backend/src/test/java/com/shigui/service/AppUserServiceTest.java`

- [ ] **Step 1: 编写 Service 接口**

`backend/src/main/java/com/shigui/service/AppUserService.java`:
```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AppUser;

public interface AppUserService extends IService<AppUser> {

    /**
     * 微信登录：根据 openid 查找或创建用户，返回用户 ID。
     * 新用户自动分配 nickname 和默认角色 USER。
     */
    Long loginByWechat(String openid);

    /**
     * 根据用户 ID 获取用户信息，id 不存在则抛 IllegalArgumentException。
     */
    AppUser getByIdOrThrow(Long userId);
}
```

- [ ] **Step 2: 编写测试（先写，确认失败再实现）**

`backend/src/test/java/com/shigui/service/AppUserServiceTest.java`:
```java
package com.shigui.service;

import com.shigui.entity.AppUser;
import com.shigui.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserMapper appUserMapper;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new com.shigui.service.impl.AppUserServiceImpl();
        // 反射注入 mock mapper（ServiceImpl 的 baseMapper 是 protected）
        try {
            var field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(appUserService, appUserMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loginByWechat_existingUser_returnsId() {
        AppUser existing = new AppUser();
        existing.setId(1L);
        existing.setOpenid("wx_openid_123");

        when(appUserMapper.selectOne(any())).thenReturn(existing);

        Long id = appUserService.loginByWechat("wx_openid_123");
        assertThat(id).isEqualTo(1L);
    }

    @Test
    void loginByWechat_newUser_createsAndReturnsId() {
        when(appUserMapper.selectOne(any())).thenReturn(null);
        when(appUserMapper.insert(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(2L);
            return 1;
        });

        Long id = appUserService.loginByWechat("wx_new_openid");
        assertThat(id).isEqualTo(2L);
    }

    @Test
    void getByIdOrThrow_existingUser_returnsUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setNickname("test");

        when(appUserMapper.selectById(1L)).thenReturn(user);

        AppUser result = appUserService.getByIdOrThrow(1L);
        assertThat(result.getNickname()).isEqualTo("test");
    }

    @Test
    void getByIdOrThrow_notFound_throwsException() {
        when(appUserMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> appUserService.getByIdOrThrow(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户不存在");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AppUserServiceTest
```

预期: FAIL — AppUserServiceImpl 类不存在

- [ ] **Step 4: 实现 Service**

`backend/src/main/java/com/shigui/service/impl/AppUserServiceImpl.java`:
```java
package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.entity.AppUser;
import com.shigui.mapper.AppUserMapper;
import com.shigui.service.AppUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    @Override
    public Long loginByWechat(String openid) {
        AppUser existing = lambdaQuery().eq(AppUser::getOpenid, openid).one();
        if (existing != null) {
            return existing.getId();
        }
        AppUser newUser = new AppUser();
        newUser.setOpenid(openid);
        newUser.setNickname("微信用户");
        newUser.setRole("USER");
        newUser.setStatus("NORMAL");
        save(newUser);
        return newUser.getId();
    }

    @Override
    public AppUser getByIdOrThrow(Long userId) {
        AppUser user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        return user;
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AppUserServiceTest
```

预期: PASS — 4 tests passed

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/shigui/service/ backend/src/test/
git commit -m "feat: AppUserService (wechat login, getByIdOrThrow) with unit tests"
```

---

### Task 6: AppUserController（含测试）

**Files:**
- Create: `backend/src/main/java/com/shigui/controller/AppUserController.java`
- Create: `backend/src/test/java/com/shigui/controller/AppUserControllerTest.java`

- [ ] **Step 1: 编写 Controller**

`backend/src/main/java/com/shigui/controller/AppUserController.java`:
```java
package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.entity.AppUser;
import com.shigui.service.AppUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * 微信登录（公开接口）。
     * 开发阶段使用 openid 查询参数模拟微信 code 换 openid 的过程。
     */
    @PostMapping("/wx-login")
    public Result<String> wxLogin(@RequestBody Map<String, String> body) {
        String openid = body.getOrDefault("openid", "");
        if (openid.isBlank()) {
            return Result.fail(400, "openid 不能为空");
        }
        Long userId = appUserService.loginByWechat(openid);
        StpUtil.login(userId);
        return Result.ok(StpUtil.getTokenValue());
    }

    /**
     * 获取当前登录用户信息。
     */
    @GetMapping("/me")
    public Result<AppUser> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(appUserService.getByIdOrThrow(userId));
    }
}
```

- [ ] **Step 2: 编写 Controller 集成测试**

`backend/src/test/java/com/shigui/controller/AppUserControllerTest.java`:
```java
package com.shigui.controller;

import com.shigui.entity.AppUser;
import com.shigui.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppUserController.class)
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @Test
    void wxLogin_newUser_returnsToken() throws Exception {
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);

        mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"wx_test_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void wxLogin_emptyOpenid_returns400() throws Exception {
        mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void me_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }
}
```

注意：`@WebMvcTest` 会触发 Sa-Token 拦截器，`/api/user/me` 未登录时应返回 401。测试中不需要 mock SaToken 行为，验证拦截器生效即可。

- [ ] **Step 3: 运行 Controller 测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AppUserControllerTest
```

预期: PASS — 3 tests passed（wx-login 两个 + me 401）

- [ ] **Step 4: 运行全部后端测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```

预期: PASS — 7 tests passed

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/shigui/controller/AppUserController.java backend/src/test/java/com/shigui/controller/
git commit -m "feat: AppUserController (wx-login, /me) with integration tests"
```

---

### Task 7: 管理端脚手架（Vite + Vue 3）

**Files:**
- Create: `admin-web/package.json`
- Create: `admin-web/vite.config.ts`
- Create: `admin-web/tsconfig.json`
- Create: `admin-web/tsconfig.app.json`
- Create: `admin-web/tsconfig.node.json`
- Create: `admin-web/index.html`
- Create: `admin-web/.gitignore`
- Create: `admin-web/public/favicon.svg`

- [ ] **Step 1: 创建 package.json**

`admin-web/package.json`:
```json
{
  "name": "admin-web",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.2",
    "axios": "^1.16.0",
    "element-plus": "^2.13.7",
    "pinia": "^3.0.4",
    "vue": "^3.5.32",
    "vue-router": "^5.0.6"
  },
  "devDependencies": {
    "@types/node": "^24.12.2",
    "@vitejs/plugin-vue": "^6.0.6",
    "@vue/tsconfig": "^0.9.1",
    "typescript": "~6.0.2",
    "vite": "^8.0.10",
    "vue-tsc": "^3.2.7"
  }
}
```

- [ ] **Step 2: 创建配置文件**

`admin-web/vite.config.ts`:
```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
})
```

`admin-web/tsconfig.json`:
```json
{
  "files": [],
  "references": [
    { "path": "./tsconfig.app.json" },
    { "path": "./tsconfig.node.json" }
  ]
}
```

`admin-web/tsconfig.app.json`:
```json
{
  "extends": "@vue/tsconfig/tsconfig.dom.json",
  "compilerOptions": {
    "tsBuildInfoFile": "./node_modules/.tmp/tsconfig.app.tsbuildinfo",
    "types": ["vite/client"],
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "erasableSyntaxOnly": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue"]
}
```

`admin-web/tsconfig.node.json`:
```json
{
  "extends": "@vue/tsconfig/tsconfig.dom.json",
  "compilerOptions": {
    "tsBuildInfoFile": "./node_modules/.tmp/tsconfig.node.tsbuildinfo",
    "types": ["node"]
  },
  "include": ["vite.config.ts"]
}
```

`admin-web/index.html`:
```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>拾归 · 管理后台</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

`admin-web/.gitignore`:
```
node_modules/
dist/
```

`admin-web/public/favicon.svg`:
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#00573D"/>
  <text x="16" y="23" text-anchor="middle" font-size="20" fill="white">拾</text>
</svg>
```

- [ ] **Step 3: 安装依赖并验证启动**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm install && npm run dev
```

预期: Vite 启动在 `http://localhost:5173`

- [ ] **Step 4: Commit**

```bash
git add admin-web/
git commit -m "chore: scaffold admin-web with Vite + Vue 3 + TS + Element Plus"
```

---

### Task 8: 管理端登录功能

**Files:**
- Create: `admin-web/src/main.ts`
- Create: `admin-web/src/App.vue`
- Create: `admin-web/src/api/index.ts`
- Create: `admin-web/src/router/index.ts`
- Create: `admin-web/src/stores/auth.ts`
- Create: `admin-web/src/layouts/MainLayout.vue`
- Create: `admin-web/src/views/LoginView.vue`
- Create: `admin-web/src/views/DashboardView.vue`
- Create: `admin-web/src/style.css`

- [ ] **Step 1: main.ts**

`admin-web/src/main.ts`:
```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

- [ ] **Step 2: App.vue**

`admin-web/src/App.vue`:
```vue
<template>
  <router-view />
</template>
```

- [ ] **Step 3: Axios 封装**

`admin-web/src/api/index.ts`:
```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: 'http://127.0.0.1:8080' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.satoken = token
  return config
})

api.interceptors.response.use(
  (res) => {
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '请求失败')
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('adminToken')
      window.location.href = '/login'
      return Promise.reject(err)
    }
    ElMessage.error('网络错误')
    return Promise.reject(err)
  }
)

export default api
```

- [ ] **Step 4: Vue Router**

`admin-web/src/router/index.ts`:
```typescript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
      ],
    },
  ],
})

export default router
```

- [ ] **Step 5: Pinia Auth Store**

`admin-web/src/stores/auth.ts`:
```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('adminToken') || '')

  async function login(username: string, password: string) {
    const res = await api.post('/api/admin/login', { username, password })
    token.value = res.data.data
    localStorage.setItem('adminToken', token.value)
    router.push('/dashboard')
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('adminToken')
    router.push('/login')
  }

  return { token, login, logout }
})
```

- [ ] **Step 6: MainLayout（左侧导航布局）**

`admin-web/src/layouts/MainLayout.vue`:
```vue
<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const menuItems = [
  { path: '/dashboard', title: '仪表盘' },
]
</script>

<template>
  <el-container style="height:100vh">
    <el-aside width="220px" style="background:#00573D">
      <div style="padding:20px;color:#fff;font-size:18px;font-weight:700;text-align:center">
        拾归 · 管理后台
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#00573D"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#fff"
        @select="(path: string) => router.push(path)"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;border-bottom:1px solid #eee;display:flex;align-items:center;justify-content:flex-end;padding:0 20px">
        <el-button text @click="auth.logout()">退出登录</el-button>
      </el-header>
      <el-main style="background:#f0f2f0">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
```

- [ ] **Step 7: 登录页**

`admin-web/src/views/LoginView.vue`:
```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const username = ref('')
const password = ref('')

function handleLogin() {
  auth.login(username.value, password.value)
}
</script>

<template>
  <div style="display:flex;align-items:center;justify-content:center;min-height:100vh;background:linear-gradient(135deg,#00573D 0%,#2E7D32 100%)">
    <el-card style="width:400px;border-radius:12px">
      <h2 style="text-align:center;margin-bottom:24px;color:#00573D">拾归管理后台</h2>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%;background:#00573D;border-color:#00573D" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
```

- [ ] **Step 8: 仪表盘占位页**

`admin-web/src/views/DashboardView.vue`:
```vue
<template>
  <div>
    <h2 style="margin-bottom:16px">仪表盘</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover">
          <div style="font-size:14px;color:#999">{{ card.title }}</div>
          <div style="font-size:28px;font-weight:700;margin-top:8px">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
const cards = [
  { title: '注册用户', value: '--' },
  { title: '匹配中单据', value: '--' },
  { title: '今日发布', value: '--' },
  { title: '成功认领', value: '--' },
]
</script>
```

- [ ] **Step 9: 全局样式**

`admin-web/src/style.css`:
```css
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
```

- [ ] **Step 10: 验证管理端启动**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm run dev
```

浏览器访问 `http://localhost:5173/login`，确认登录页展示正常。

- [ ] **Step 11: Commit**

```bash
git add admin-web/src/ admin-web/index.html admin-web/public/
git commit -m "feat: admin-web login page, router, auth store, MainLayout, dashboard"
```

---

### Task 9: Admin 后端（管理员登录 API + 测试）

**Files:**
- Create: `backend/src/main/java/com/shigui/entity/AdminUser.java`
- Create: `backend/src/main/java/com/shigui/mapper/AdminUserMapper.java`
- Create: `backend/src/main/java/com/shigui/service/AdminUserService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/AdminUserServiceImpl.java`
- Create: `backend/src/main/java/com/shigui/controller/AdminController.java`
- Create: `backend/src/test/java/com/shigui/service/AdminUserServiceTest.java`
- Create: `backend/src/test/java/com/shigui/controller/AdminControllerTest.java`

- [ ] **Step 1: AdminUser 实体**

`backend/src/main/java/com/shigui/entity/AdminUser.java`:
```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin_user")
public class AdminUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: AdminUserMapper**

`backend/src/main/java/com/shigui/mapper/AdminUserMapper.java`:
```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
```

- [ ] **Step 3: AdminUserService 接口**

`backend/src/main/java/com/shigui/service/AdminUserService.java`:
```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.entity.AdminUser;

public interface AdminUserService extends IService<AdminUser> {

    /**
     * 管理员登录：校验用户名和密码，返回 Sa-Token token。
     * 用户名不存在或密码错误抛 IllegalArgumentException。
     */
    String login(String username, String password);
}
```

- [ ] **Step 4: AdminUserServiceImpl（含 BCrypt 密码校验）**

`backend/src/main/java/com/shigui/service/impl/AdminUserServiceImpl.java`:
```java
package com.shigui.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.entity.AdminUser;
import com.shigui.mapper.AdminUserMapper;
import com.shigui.service.AdminUserService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

    @Override
    public String login(String username, String password) {
        AdminUser admin = lambdaQuery().eq(AdminUser::getUsername, username).one();
        if (admin == null || !verifyPassword(password, admin.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        StpUtil.login(admin.getId());
        return StpUtil.getTokenValue();
    }

    private boolean verifyPassword(String rawPassword, String storedHash) {
        // 简易 SHA-256 + salt 校验（生产环境应使用 BCrypt）
        if (storedHash == null || !storedHash.contains(":")) return false;
        String[] parts = storedHash.split(":", 2);
        String salt = parts[0];
        String hash = parts[1];
        String computed = sha256(salt + rawPassword);
        return hash.equals(computed);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

原因：不引入 Spring Security 或额外 BCrypt 库，用简单 SHA-256 + salt 实现密码验证，减少依赖。种子数据中的密码哈希需对应生成。

- [ ] **Step 5: 编写 Service 测试**

`backend/src/test/java/com/shigui/service/AdminUserServiceTest.java`:
```java
package com.shigui.service;

import com.shigui.entity.AdminUser;
import com.shigui.mapper.AdminUserMapper;
import com.shigui.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    private AdminUserServiceImpl adminUserService;

    private static String hashPassword(String raw) {
        SecureRandom rng = new SecureRandom();
        byte[] saltBytes = new byte[16];
        rng.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((salt + raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return salt + ":" + sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl();
        try {
            var field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(adminUserService, adminUserMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void login_correctCredentials_returnsToken() {
        String passwordHash = hashPassword("admin123");
        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordHash);

        when(adminUserMapper.selectOne(any())).thenReturn(admin);

        String token = adminUserService.login("admin", "admin123");
        assertThat(token).isNotBlank();
    }

    @Test
    void login_wrongPassword_throwsException() {
        String passwordHash = hashPassword("correct_password");
        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordHash);

        when(adminUserMapper.selectOne(any())).thenReturn(admin);

        assertThatThrownBy(() -> adminUserService.login("admin", "wrong_password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void login_userNotFound_throwsException() {
        when(adminUserMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> adminUserService.login("nobody", "any"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
    }
}
```

- [ ] **Step 6: 运行 Service 测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AdminUserServiceTest
```

预期: PASS — 3 tests passed

- [ ] **Step 7: AdminController**

`backend/src/main/java/com/shigui/controller/AdminController.java`:
```java
package com.shigui.controller;

import com.shigui.common.Result;
import com.shigui.service.AdminUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 管理员登录（公开接口）。
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if (username.isBlank() || password.isBlank()) {
            return Result.fail(400, "用户名和密码不能为空");
        }
        String token = adminUserService.login(username, password);
        return Result.ok(token);
    }
}
```

- [ ] **Step 8: Controller 测试**

`backend/src/test/java/com/shigui/controller/AdminControllerTest.java`:
```java
package com.shigui.controller;

import com.shigui.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void login_success_returnsToken() throws Exception {
        when(adminUserService.login(anyString(), anyString())).thenReturn("test-token-123");

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("test-token-123"));
    }

    @Test
    void login_emptyUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
```

- [ ] **Step 9: 运行全部后端测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```

预期: PASS — 12 tests passed（4 AppUserService + 3 AppUserController + 3 AdminUserService + 2 AdminController）

- [ ] **Step 10: 更新种子数据（用正确密码哈希）**

重新生成 `scripts/seed_data.sql`，用 `AdminUserServiceTest` 中的 `hashPassword("admin123")` 的哈希值。为此先写一个 main 方法生成哈希，或直接用已知哈希替换。

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/shigui/entity/AdminUser.java backend/src/main/java/com/shigui/mapper/AdminUserMapper.java backend/src/main/java/com/shigui/service/AdminUserService.java backend/src/main/java/com/shigui/service/impl/AdminUserServiceImpl.java backend/src/main/java/com/shigui/controller/AdminController.java backend/src/test/java/com/shigui/service/AdminUserServiceTest.java backend/src/test/java/com/shigui/controller/AdminControllerTest.java
git commit -m "feat: Admin login API with SHA-256 password verification and tests"
```

---

### Task 10: 小程序脚手架

**Files:**
- Create: `miniapp/app.js`
- Create: `miniapp/app.json`
- Create: `miniapp/app.wxss`
- Create: `miniapp/project.config.json`
- Create: `miniapp/pages/index/index.js`
- Create: `miniapp/pages/index/index.json`
- Create: `miniapp/pages/index/index.wxml`
- Create: `miniapp/pages/index/index.wxss`
- Create: `miniapp/pages/mine/mine.js`
- Create: `miniapp/pages/mine/mine.json`
- Create: `miniapp/pages/mine/mine.wxml`
- Create: `miniapp/pages/mine/mine.wxss`

- [ ] **Step 1: app.js**

`miniapp/app.js`:
```javascript
App({
  globalData: {
    token: '',
    userInfo: null,
    baseUrl: 'http://127.0.0.1:8080'
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  }
})
```

- [ ] **Step 2: app.json**

`miniapp/app.json`:
```json
{
  "pages": [
    "pages/index/index",
    "pages/mine/mine"
  ],
  "window": {
    "navigationBarBackgroundColor": "#00573D",
    "navigationBarTitleText": "拾归",
    "navigationBarTextStyle": "white",
    "backgroundColor": "#f0f2f0"
  },
  "tabBar": {
    "color": "#999",
    "selectedColor": "#00573D",
    "backgroundColor": "#fff",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页"
      },
      {
        "pagePath": "pages/mine/mine",
        "text": "我的"
      }
    ]
  }
}
```

- [ ] **Step 3: app.wxss**

`miniapp/app.wxss`:
```css
page {
  --primary-color: #2E7D32;
  --primary-light: #E8F5E9;
  --lost-color: #FF9800;
  --lost-light: #FFF3E0;
  --found-color: #4CAF50;
  --found-light: #E8F5E9;
  --bg-color: #F8F9FA;
  --text-main: #1A1A1A;
  --text-second: #666666;
  --text-light: #999999;
  --white: #FFFFFF;
  --shadow: 0 4px 12px rgba(0,0,0,0.05);
  --border-radius: 16rpx;

  font-size: 28rpx;
  color: var(--text-main);
  background: var(--bg-color);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
```

- [ ] **Step 4: project.config.json**

`miniapp/project.config.json`:
```json
{
  "description": "拾归 - 校园失物招领",
  "appid": "wx58ca83eb30d907e7",
  "projectname": "拾归",
  "compileType": "miniprogram",
  "setting": {
    "urlCheck": false,
    "es6": true,
    "postcss": true,
    "minified": true
  }
}
```

- [ ] **Step 5: 首页占位**

`miniapp/pages/index/index.json`:
```json
{}
```

`miniapp/pages/index/index.js`:
```javascript
Page({
  data: {
    title: '拾归'
  },
  onLoad() {
    console.log('首页加载')
  }
})
```

`miniapp/pages/index/index.wxml`:
```html
<view class="container" style="display:flex;align-items:center;justify-content:center;height:100vh">
  <text style="font-size:36rpx;color:var(--text-second)">首页 — 即将上线</text>
</view>
```

`miniapp/pages/index/index.wxss`:
```css
/* 首页样式 — S3 实现 */
```

- [ ] **Step 6: "我的"页面（含微信登录）**

`miniapp/pages/mine/mine.json`:
```json
{}
```

`miniapp/pages/mine/mine.js`:
```javascript
const app = getApp()

Page({
  data: {
    userInfo: null
  },

  onShow() {
    this.loadUserInfo()
  },

  loadUserInfo() {
    const token = app.globalData.token
    if (!token) {
      this.setData({ userInfo: null })
      return
    }
    wx.request({
      url: `${app.globalData.baseUrl}/api/user/me`,
      header: { satoken: token },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ userInfo: res.data.data })
        } else {
          this.setData({ userInfo: null })
        }
      },
      fail: () => this.setData({ userInfo: null })
    })
  },

  wxLogin() {
    wx.login({
      success: (res) => {
        const openid = 'dev_' + res.code
        wx.request({
          url: `${app.globalData.baseUrl}/api/user/wx-login`,
          method: 'POST',
          data: { openid },
          success: (resp) => {
            if (resp.data.code === 200) {
              const token = resp.data.data
              app.globalData.token = token
              wx.setStorageSync('token', token)
              this.loadUserInfo()
              wx.showToast({ title: '登录成功', icon: 'success' })
            }
          }
        })
      }
    })
  }
})
```

`miniapp/pages/mine/mine.wxml`:
```html
<view class="container">
  <view class="header-section">
    <view class="user-card">
      <view class="avatar">{{userInfo.nickname ? userInfo.nickname[0] : '拾'}}</view>
      <view class="user-info" wx:if="{{userInfo}}">
        <view class="name">{{userInfo.nickname}}</view>
        <view class="tag">校园认证用户</view>
      </view>
      <view class="user-info" wx:else>
        <view class="name">未登录</view>
        <view class="tag">点击登录体验完整功能</view>
      </view>
      <view class="login-action" wx:if="{{!userInfo}}">
        <button class="btn-login" bindtap="wxLogin">微信一键登录</button>
      </view>
    </view>
  </view>
</view>
```

`miniapp/pages/mine/mine.wxss`:
```css
.container {
  min-height: 100vh;
  background: var(--bg-color);
}

.header-section {
  padding: 80rpx 40rpx 40rpx;
}

.user-card {
  background: var(--white);
  border-radius: 24rpx;
  padding: 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: var(--shadow);
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  background: linear-gradient(135deg, #00573D, #2E7D32);
  border-radius: 50%;
  color: #fff;
  font-size: 48rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.name {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text-main);
}

.tag {
  font-size: 24rpx;
  color: var(--text-light);
  margin-top: 8rpx;
}

.login-action {
  margin-top: 32rpx;
}

.btn-login {
  background: #00573D;
  color: #fff;
  border-radius: 44rpx;
  font-size: 28rpx;
  padding: 16rpx 48rpx;
  border: none;
}
```

- [ ] **Step 7: Commit**

```bash
git add miniapp/
git commit -m "feat: miniapp scaffold (app + index placeholder + mine with wx-login)"
```

---

### Task 11: 端到端集成验证

- [ ] **Step 1: 初始化数据库**

```bash
mysql -u root -pHang0611@ < scripts/init_schema.sql
```

- [ ] **Step 2: 生成管理员密码哈希并写入种子数据**

先写一个临时 main 方法生成 `admin123` 的 SHA-256 + salt 哈希，然后更新 seed_data.sql。

创建 `backend/src/test/java/com/shigui/GenPasswordHash.java`:
```java
package com.shigui;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class GenPasswordHash {
    public static void main(String[] args) {
        SecureRandom rng = new SecureRandom();
        byte[] saltBytes = new byte[16];
        rng.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((salt + "admin123").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            System.out.println(salt + ":" + sb.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

```bash
cd /Users/cyrene/Dev/shigui/backend
javac src/test/java/com/shigui/GenPasswordHash.java -d /tmp
java -cp /tmp GenPasswordHash
# 输出版本: <salt>:<hash> 例如: abc123def456:7890abcd...
```

将输出的哈希值替换到 `scripts/seed_data.sql` 中的 `placeholder_run_Task11_Step2_to_generate`，然后：

```bash
mysql -u root -pHang0611@ < scripts/seed_data.sql
```

执行完删除临时文件：
```bash
rm backend/src/test/java/com/shigui/GenPasswordHash.java
```

- [ ] **Step 3: 启动后端**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw spring-boot:run
```

- [ ] **Step 4: 验证后端 API（curl 测试）**

```bash
# 测试小程序登录
curl -X POST http://localhost:8080/api/user/wx-login \
  -H "Content-Type: application/json" \
  -d '{"openid":"dev_test_001"}'

# 预期: {"code":200,"message":"success","data":"<token>"}

# 测试获取用户信息（用上一步返回的 token）
curl http://localhost:8080/api/user/me \
  -H "satoken: <token>"

# 预期: {"code":200,"data":{"id":...,"nickname":"微信用户"...}}

# 测试未登录 401
curl http://localhost:8080/api/user/me
# 预期: 401

# 测试管理员登录
curl -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 预期: {"code":200,"data":"<token>"}
```

- [ ] **Step 5: 启动管理端验证登录**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm run dev
```

浏览器访问 `http://localhost:5173/login`，用 admin/admin123 登录，验证跳转到仪表盘。

- [ ] **Step 6: 微信开发者工具验证**

用微信开发者工具打开 `miniapp/` 目录，切换到"我的"tab，点击"微信一键登录"，确认登录成功后显示昵称。

- [ ] **Step 7: 运行全部测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```

预期: 全部 12 tests PASS

- [ ] **Step 8: 最终 Commit**

```bash
git add .
git commit -m "feat: Sprint 1 complete - WeChat login + Admin login + full integration verified"
```

---

### 验证清单

- [ ] 后端编译通过 (`./mvnw compile`)
- [ ] 全部测试通过 (`./mvnw test`) — 12 tests
- [ ] `POST /api/user/wx-login` 返回 token
- [ ] `GET /api/user/me` 登录后返回用户信息
- [ ] `GET /api/user/me` 未登录返回 401
- [ ] `POST /api/admin/login` 返回 token
- [ ] 管理端登录页 -> 登录 -> 跳转仪表盘 -> 退出登录
- [ ] 小程序"我的"页点击登录 -> 登录成功 -> 显示昵称
