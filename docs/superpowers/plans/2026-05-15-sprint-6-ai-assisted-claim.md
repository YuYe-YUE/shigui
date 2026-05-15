# Sprint 6: AI 管理员辅助认领与归还闭环 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现“失主申请认领 -> AI 管理员预审 -> 人工管理员兜底 -> 失主查看暂存地点并确认收到”的认领归还闭环，并提供最小 HTTP 聊天能力。

**Architecture:** 后端以 `ClaimRecordService` 为认领状态机中心，`AiClaimReviewClient` 只负责 AI 预审，`AdminController`/`ClaimRecordController` 负责权限边界。小程序负责提交认领、查看认领记录、确认收到；管理端新增认领审核页面。聊天独立为 `ChatService`，不阻塞归还完成。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, Maven, OpenAI-compatible Chat Completions API, Vue 3 + Vite + TypeScript + Element Plus, 原生微信小程序。

---

## 执行前置条件

当前工作区可能仍有 S5.5 管理端匹配结果/仪表盘改动，以及 `backend/src/main/resources/application-local.properties` 的本地 API key 修改。执行 S6 前先做一次：

```bash
cd /Users/cyrene/Dev/shigui
git status --short
```

预期：

- S5.5 代码改动已经提交，或者执行者明确知道哪些文件属于 S5.5。
- `application-local.properties` 的敏感配置不要提交。

如果 S5.5 代码尚未提交，先让用户确认是否提交 S5.5，再执行本计划。不要把 S5.5 和 S6 混在同一个 commit 里。

---

## 文件结构

### 后端

- Modify: `scripts/init_schema.sql`
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/java/com/shigui/entity/ClaimRecord.java`
- Create: `backend/src/main/java/com/shigui/entity/ChatSession.java`
- Create: `backend/src/main/java/com/shigui/entity/ChatMessage.java`
- Create: `backend/src/main/java/com/shigui/mapper/ClaimRecordMapper.java`
- Create: `backend/src/main/java/com/shigui/mapper/ChatSessionMapper.java`
- Create: `backend/src/main/java/com/shigui/mapper/ChatMessageMapper.java`
- Create: `backend/src/main/java/com/shigui/dto/CreateClaimRequest.java`
- Create: `backend/src/main/java/com/shigui/dto/ClaimResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/AdminClaimResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/RejectClaimRequest.java`
- Create: `backend/src/main/java/com/shigui/dto/AiClaimReviewResult.java`
- Create: `backend/src/main/java/com/shigui/dto/CreateChatSessionRequest.java`
- Create: `backend/src/main/java/com/shigui/dto/ChatSessionResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/ChatMessageResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/SendMessageRequest.java`
- Create: `backend/src/main/java/com/shigui/config/AiClaimReviewProperties.java`
- Create: `backend/src/main/java/com/shigui/service/AiClaimReviewClient.java`
- Create: `backend/src/main/java/com/shigui/service/ClaimRecordService.java`
- Create: `backend/src/main/java/com/shigui/service/ChatService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleClaimReviewClient.java`
- Create: `backend/src/main/java/com/shigui/service/impl/ClaimRecordServiceImpl.java`
- Create: `backend/src/main/java/com/shigui/service/impl/ChatServiceImpl.java`
- Create: `backend/src/main/java/com/shigui/controller/ClaimRecordController.java`
- Create: `backend/src/main/java/com/shigui/controller/ChatController.java`
- Modify: `backend/src/main/java/com/shigui/controller/AdminController.java`
- Test: `backend/src/test/java/com/shigui/service/OpenAiCompatibleClaimReviewClientTest.java`
- Test: `backend/src/test/java/com/shigui/service/ClaimRecordServiceTest.java`
- Test: `backend/src/test/java/com/shigui/service/ChatServiceTest.java`
- Test: `backend/src/test/java/com/shigui/controller/ClaimRecordControllerTest.java`
- Test: `backend/src/test/java/com/shigui/controller/AdminControllerTest.java`
- Test: `backend/src/test/java/com/shigui/controller/ChatControllerTest.java`

### 小程序

- Modify: `miniapp/app.json`
- Modify: `miniapp/pages/detail/detail.js`
- Modify: `miniapp/pages/detail/detail.wxml`
- Modify: `miniapp/pages/detail/detail.wxss`
- Modify: `miniapp/pages/mine/mine.js`
- Modify: `miniapp/pages/mine/mine.wxml`
- Modify: `miniapp/pages/chat/chat.js`
- Create: `miniapp/pages/claims/claims.js`
- Create: `miniapp/pages/claims/claims.wxml`
- Create: `miniapp/pages/claims/claims.wxss`
- Create: `miniapp/pages/claims/claims.json`

### 管理端

- Modify: `admin-web/src/layouts/MainLayout.vue`
- Modify: `admin-web/src/router/index.ts`
- Create: `admin-web/src/views/ClaimReviewView.vue`

---

### Task 1: 数据库、实体、Mapper 与 DTO

**Files:**
- Modify: `scripts/init_schema.sql`
- Create: `backend/src/main/java/com/shigui/entity/ClaimRecord.java`
- Create: `backend/src/main/java/com/shigui/entity/ChatSession.java`
- Create: `backend/src/main/java/com/shigui/entity/ChatMessage.java`
- Create: `backend/src/main/java/com/shigui/mapper/ClaimRecordMapper.java`
- Create: `backend/src/main/java/com/shigui/mapper/ChatSessionMapper.java`
- Create: `backend/src/main/java/com/shigui/mapper/ChatMessageMapper.java`
- Create: DTO files listed below

- [ ] **Step 1: 更新数据库初始化脚本**

在 `scripts/init_schema.sql` 中，把 `claim_record` 表改为：

```sql
CREATE TABLE claim_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    claimant_user_id BIGINT NOT NULL COMMENT '发起认领的用户',
    private_feature_answer TEXT COMMENT '失主填写的私密特征',
    status VARCHAR(32) DEFAULT 'PENDING_AI_REVIEW' COMMENT 'PENDING_AI_REVIEW/PENDING_ADMIN_REVIEW/VERIFIED/REJECTED/COMPLETED',
    ai_decision VARCHAR(32) DEFAULT '',
    ai_confidence DECIMAL(5,4) DEFAULT 0,
    ai_reason TEXT,
    admin_reason VARCHAR(512) DEFAULT '',
    verified_at DATETIME DEFAULT NULL,
    completed_at DATETIME DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_claimant (claimant_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

保留 `chat_session` 和 `chat_message` 表结构，但确认存在 `deleted` 字段和索引。

- [ ] **Step 2: 创建 ClaimRecord 实体**

创建 `backend/src/main/java/com/shigui/entity/ClaimRecord.java`：

```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("claim_record")
public class ClaimRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long claimantUserId;
    private String privateFeatureAnswer;
    private String status;
    private String aiDecision;
    private BigDecimal aiConfidence;
    private String aiReason;
    private String adminReason;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建聊天实体**

创建 `backend/src/main/java/com/shigui/entity/ChatSession.java`：

```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long lostUserId;
    private Long foundUserId;
    private String status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

创建 `backend/src/main/java/com/shigui/entity/ChatMessage.java`：

```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long senderUserId;
    private String content;
    private String msgType;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 Mapper**

创建 `backend/src/main/java/com/shigui/mapper/ClaimRecordMapper.java`：

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.ClaimRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClaimRecordMapper extends BaseMapper<ClaimRecord> {
}
```

创建 `backend/src/main/java/com/shigui/mapper/ChatSessionMapper.java`：

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
```

创建 `backend/src/main/java/com/shigui/mapper/ChatMessageMapper.java`：

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
```

- [ ] **Step 5: 创建认领 DTO**

创建 `backend/src/main/java/com/shigui/dto/CreateClaimRequest.java`：

```java
package com.shigui.dto;

import lombok.Data;

@Data
public class CreateClaimRequest {
    private Long postId;
    private String privateFeatureAnswer;
}
```

创建 `backend/src/main/java/com/shigui/dto/RejectClaimRequest.java`：

```java
package com.shigui.dto;

import lombok.Data;

@Data
public class RejectClaimRequest {
    private String reason;
}
```

创建 `backend/src/main/java/com/shigui/dto/AiClaimReviewResult.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiClaimReviewResult {
    private String decision;
    private BigDecimal confidence;
    private String reason;
}
```

创建 `backend/src/main/java/com/shigui/dto/ClaimResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClaimResponse {
    private Long id;
    private Long postId;
    private String postTitle;
    private String itemName;
    private String itemCategory;
    private String campusArea;
    private String locationName;
    private String storageLocation;
    private String status;
    private String aiDecision;
    private BigDecimal aiConfidence;
    private String aiReason;
    private String adminReason;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
}
```

创建 `backend/src/main/java/com/shigui/dto/AdminClaimResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminClaimResponse {
    private Long id;
    private Long postId;
    private String postTitle;
    private String itemName;
    private String itemCategory;
    private String campusArea;
    private String locationName;
    private String storageLocation;
    private String privateFeature;
    private Long claimantUserId;
    private String privateFeatureAnswer;
    private String status;
    private String aiDecision;
    private BigDecimal aiConfidence;
    private String aiReason;
    private String adminReason;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
}
```

- [ ] **Step 6: 创建聊天 DTO**

创建 `backend/src/main/java/com/shigui/dto/CreateChatSessionRequest.java`：

```java
package com.shigui.dto;

import lombok.Data;

@Data
public class CreateChatSessionRequest {
    private Long postId;
}
```

创建 `backend/src/main/java/com/shigui/dto/SendMessageRequest.java`：

```java
package com.shigui.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String content;
}
```

创建 `backend/src/main/java/com/shigui/dto/ChatSessionResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

@Data
public class ChatSessionResponse {
    private Long id;
    private Long postId;
    private Long lostUserId;
    private Long foundUserId;
    private String status;
}
```

创建 `backend/src/main/java/com/shigui/dto/ChatMessageResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long id;
    private Long sessionId;
    private Long senderUserId;
    private String content;
    private String msgType;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: 编译验证**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw compile
```

Expected: PASS，所有新增实体、Mapper、DTO 编译通过。

- [ ] **Step 8: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add scripts/init_schema.sql backend/src/main/java/com/shigui/entity/ClaimRecord.java backend/src/main/java/com/shigui/entity/ChatSession.java backend/src/main/java/com/shigui/entity/ChatMessage.java backend/src/main/java/com/shigui/mapper/ClaimRecordMapper.java backend/src/main/java/com/shigui/mapper/ChatSessionMapper.java backend/src/main/java/com/shigui/mapper/ChatMessageMapper.java backend/src/main/java/com/shigui/dto/CreateClaimRequest.java backend/src/main/java/com/shigui/dto/RejectClaimRequest.java backend/src/main/java/com/shigui/dto/AiClaimReviewResult.java backend/src/main/java/com/shigui/dto/ClaimResponse.java backend/src/main/java/com/shigui/dto/AdminClaimResponse.java backend/src/main/java/com/shigui/dto/CreateChatSessionRequest.java backend/src/main/java/com/shigui/dto/SendMessageRequest.java backend/src/main/java/com/shigui/dto/ChatSessionResponse.java backend/src/main/java/com/shigui/dto/ChatMessageResponse.java
git commit -m "feat: add claim and chat domain models"
```

---

### Task 2: AI 管理员预审 Client

**Files:**
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/java/com/shigui/config/AiClaimReviewProperties.java`
- Create: `backend/src/main/java/com/shigui/service/AiClaimReviewClient.java`
- Create: `backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleClaimReviewClient.java`
- Test: `backend/src/test/java/com/shigui/service/OpenAiCompatibleClaimReviewClientTest.java`

- [ ] **Step 1: 写真实 API 失败测试**

创建 `backend/src/test/java/com/shigui/service/OpenAiCompatibleClaimReviewClientTest.java`：

```java
package com.shigui.service;

import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.impl.OpenAiCompatibleClaimReviewClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class OpenAiCompatibleClaimReviewClientTest {
    private static String value(String name) {
        String v = System.getProperty(name);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(name);
        return (v != null && !v.isBlank()) ? v : null;
    }

    @Test
    void reviewClaim_realApi_approvesStrongPrivateFeatureMatch() {
        String baseUrl = value("AI_CLAIM_BASE_URL");
        String apiKey = value("AI_CLAIM_API_KEY");
        String model = value("AI_CLAIM_MODEL");
        if (baseUrl == null || apiKey == null || model == null) {
            fail("AI_CLAIM_BASE_URL, AI_CLAIM_API_KEY, AI_CLAIM_MODEL must be set via -D or env");
        }

        AiClaimReviewProperties properties = new AiClaimReviewProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(baseUrl);
        properties.setApiKey(apiKey);
        properties.setModel(model);
        properties.setTimeoutSeconds(30);
        properties.setAutoApproveThreshold("0.85");
        properties.setAutoRejectThreshold("0.85");

        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        post.setPostType("FOUND");
        post.setTitle("捡到绿色卡套校园卡");
        post.setItemName("校园卡");
        post.setItemCategory("证件");
        post.setDescription("在南校园逸夫楼门口捡到一张校园卡，绿色透明卡套");
        post.setPrivateFeature("卡号后四位9876，卡套里有蓝色星星贴纸");
        post.setCampusArea("南校园");
        post.setLocationName("逸夫楼");
        post.setEventTime(LocalDateTime.of(2026, 5, 15, 12, 0));

        OpenAiCompatibleClaimReviewClient client = new OpenAiCompatibleClaimReviewClient(properties);
        AiClaimReviewResult result = client.reviewClaim(post, "后四位9876，卡套里有蓝色星星贴纸");

        assertThat(result.getDecision()).isIn("APPROVE", "NEEDS_REVIEW");
        assertThat(result.getConfidence()).isNotNull();
        assertThat(result.getReason()).isNotBlank();
        assertThat(result.getReason()).doesNotContain("9876");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
AI_CLAIM_API_KEY=dummy AI_CLAIM_BASE_URL=http://127.0.0.1:1/v1 AI_CLAIM_MODEL=dummy ./mvnw test -Dtest=OpenAiCompatibleClaimReviewClientTest
```

Expected: FAIL，错误包含 `cannot find symbol`，因为 `AiClaimReviewProperties` 和 `OpenAiCompatibleClaimReviewClient` 还不存在。

- [ ] **Step 3: 添加配置**

在 `backend/src/main/resources/application.properties` 追加：

```properties
ai.claim.enabled=true
ai.claim.base-url=${AI_CLAIM_BASE_URL:${AI_MATCH_BASE_URL:https://api.deepseek.com}}
ai.claim.api-key=${AI_CLAIM_API_KEY:${AI_MATCH_API_KEY:}}
ai.claim.model=${AI_CLAIM_MODEL:${AI_MATCH_MODEL:deepseek-v4-flash}}
ai.claim.timeout-seconds=30
ai.claim.auto-approve-threshold=0.85
ai.claim.auto-reject-threshold=0.85
```

- [ ] **Step 4: 创建配置类**

创建 `backend/src/main/java/com/shigui/config/AiClaimReviewProperties.java`：

```java
package com.shigui.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
public class AiClaimReviewProperties {
    @Value("${ai.claim.enabled:true}")
    private boolean enabled;
    @Value("${ai.claim.base-url:}")
    private String baseUrl;
    @Value("${ai.claim.api-key:}")
    private String apiKey;
    @Value("${ai.claim.model:}")
    private String model;
    @Value("${ai.claim.timeout-seconds:30}")
    private int timeoutSeconds;
    @Value("${ai.claim.auto-approve-threshold:0.85}")
    private String autoApproveThreshold;
    @Value("${ai.claim.auto-reject-threshold:0.85}")
    private String autoRejectThreshold;

    public BigDecimal autoApproveThresholdValue() {
        return new BigDecimal(autoApproveThreshold);
    }

    public BigDecimal autoRejectThresholdValue() {
        return new BigDecimal(autoRejectThreshold);
    }
}
```

- [ ] **Step 5: 创建 Client 接口**

创建 `backend/src/main/java/com/shigui/service/AiClaimReviewClient.java`：

```java
package com.shigui.service;

import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;

public interface AiClaimReviewClient {
    AiClaimReviewResult reviewClaim(LostFoundPost foundPost, String privateFeatureAnswer);
}
```

- [ ] **Step 6: 实现 OpenAI-compatible Client**

创建 `backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleClaimReviewClient.java`：

```java
package com.shigui.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.AiClaimReviewClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiCompatibleClaimReviewClient implements AiClaimReviewClient {
    private final AiClaimReviewProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleClaimReviewClient(AiClaimReviewProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiClaimReviewResult reviewClaim(LostFoundPost foundPost, String privateFeatureAnswer) {
        validateConfig();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        RestClient client = RestClient.builder()
                .baseUrl(stripTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", userPrompt(foundPost, privateFeatureAnswer))
                )
        );

        String response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                .body(body)
                .retrieve()
                .body(String.class);
        return parseResponse(response);
    }

    private String systemPrompt() {
        return """
                你是校园失物招领系统的 AI 管理员，负责判断认领答案是否能证明申请人是物主。
                只返回 JSON: {"decision":"APPROVE|REJECT|NEEDS_REVIEW","confidence":0.0,"reason":"简短中文原因"}。
                confidence 必须在 0 到 1 之间。
                如果答案和私密特征高度一致，decision=APPROVE。
                如果答案明显矛盾，decision=REJECT。
                如果答案含糊、信息不足或可能误伤，decision=NEEDS_REVIEW。
                reason 不得复述私密特征原文、卡号、学号、编号或连续数字，只能使用“私密特征匹配/不匹配/信息不足”等概括表达。\
                """;
    }

    private String userPrompt(LostFoundPost post, String answer) {
        return """
                FOUND_POST:
                id=%s
                title=%s
                itemName=%s
                itemCategory=%s
                description=%s
                privateFeature=%s
                campusArea=%s
                locationName=%s
                eventTime=%s

                CLAIM_ANSWER:
                %s
                """.formatted(post.getId(), post.getTitle(), post.getItemName(), post.getItemCategory(),
                post.getDescription(), post.getPrivateFeature(), post.getCampusArea(), post.getLocationName(),
                post.getEventTime(), answer);
    }

    private AiClaimReviewResult parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String content = root.at("/choices/0/message/content").asText();
            String json = content.replace("```json", "").replace("```", "").trim();
            AiClaimReviewResult result = objectMapper.readValue(json, AiClaimReviewResult.class);
            if (!List.of("APPROVE", "REJECT", "NEEDS_REVIEW").contains(result.getDecision())) {
                throw new IllegalStateException("Unknown claim review decision: " + result.getDecision());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI claim review response: " + e.getMessage(), e);
        }
    }

    private void validateConfig() {
        if (!properties.isEnabled()) throw new IllegalStateException("AI claim review is disabled");
        if (isBlank(properties.getBaseUrl())) throw new IllegalStateException("AI_CLAIM_BASE_URL is required");
        if (isBlank(properties.getApiKey())) throw new IllegalStateException("AI_CLAIM_API_KEY is required");
        if (isBlank(properties.getModel())) throw new IllegalStateException("AI_CLAIM_MODEL is required");
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
```

- [ ] **Step 7: 运行真实 API 测试**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
AI_CLAIM_API_KEY="$AI_MATCH_API_KEY" AI_CLAIM_BASE_URL="${AI_MATCH_BASE_URL:-https://api.deepseek.com}" AI_CLAIM_MODEL="${AI_MATCH_MODEL:-deepseek-v4-flash}" ./mvnw test -Dtest=OpenAiCompatibleClaimReviewClientTest
```

Expected: PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

- [ ] **Step 8: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add backend/src/main/resources/application.properties backend/src/main/java/com/shigui/config/AiClaimReviewProperties.java backend/src/main/java/com/shigui/service/AiClaimReviewClient.java backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleClaimReviewClient.java backend/src/test/java/com/shigui/service/OpenAiCompatibleClaimReviewClientTest.java
git commit -m "feat: add AI claim review client"
```

---

### Task 3: ClaimRecordService 认领状态机

**Files:**
- Create: `backend/src/main/java/com/shigui/service/ClaimRecordService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/ClaimRecordServiceImpl.java`
- Test: `backend/src/test/java/com/shigui/service/ClaimRecordServiceTest.java`

- [ ] **Step 1: 写 Service 单元测试**

创建 `backend/src/test/java/com/shigui/service/ClaimRecordServiceTest.java`，覆盖核心状态机：

```java
package com.shigui.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.AiClaimReviewResult;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.entity.AppUser;
import com.shigui.entity.ClaimRecord;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.ClaimRecordMapper;
import com.shigui.service.impl.ClaimRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClaimRecordServiceTest {
    private ClaimRecordMapper claimRecordMapper;
    private LostFoundPostService lostFoundPostService;
    private AppUserService appUserService;
    private AiClaimReviewClient aiClaimReviewClient;
    private AiClaimReviewProperties properties;
    private ClaimRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        claimRecordMapper = mock(ClaimRecordMapper.class);
        lostFoundPostService = mock(LostFoundPostService.class);
        appUserService = mock(AppUserService.class);
        aiClaimReviewClient = mock(AiClaimReviewClient.class);
        properties = new AiClaimReviewProperties();
        properties.setAutoApproveThreshold("0.85");
        properties.setAutoRejectThreshold("0.85");
        service = new ClaimRecordServiceImpl(lostFoundPostService, appUserService, aiClaimReviewClient, properties);
        injectMapper(service, claimRecordMapper);
    }

    @Test
    void createClaim_aiApprove_movesPostToReturning() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser());
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        AiClaimReviewResult ai = new AiClaimReviewResult();
        ai.setDecision("APPROVE");
        ai.setConfidence(new BigDecimal("0.91"));
        ai.setReason("私密特征匹配");
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("蓝色贴纸"))).thenReturn(ai);

        CreateClaimRequest request = new CreateClaimRequest();
        request.setPostId(10L);
        request.setPrivateFeatureAnswer("蓝色贴纸");

        var response = service.createClaim(2L, request);

        assertThat(response.getStatus()).isEqualTo("VERIFIED");
        assertThat(post.getStatus()).isEqualTo("RETURNING");
        verify(lostFoundPostService).updateById(post);
    }

    @Test
    void createClaim_aiNeedsReview_keepsPostClaiming() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(2L)).thenReturn(normalUser());
        when(claimRecordMapper.selectCount(any())).thenReturn(0L);
        when(claimRecordMapper.insert(any(ClaimRecord.class))).thenAnswer(inv -> {
            ClaimRecord claim = inv.getArgument(0);
            claim.setId(99L);
            return 1;
        });
        AiClaimReviewResult ai = new AiClaimReviewResult();
        ai.setDecision("NEEDS_REVIEW");
        ai.setConfidence(new BigDecimal("0.50"));
        ai.setReason("信息不足");
        when(aiClaimReviewClient.reviewClaim(eq(post), eq("我的"))).thenReturn(ai);

        CreateClaimRequest request = new CreateClaimRequest();
        request.setPostId(10L);
        request.setPrivateFeatureAnswer("我的");

        var response = service.createClaim(2L, request);

        assertThat(response.getStatus()).isEqualTo("PENDING_ADMIN_REVIEW");
        assertThat(post.getStatus()).isEqualTo("CLAIMING");
    }

    @Test
    void createClaim_rejectsSelfClaim() {
        LostFoundPost post = foundPost();
        when(lostFoundPostService.getById(10L)).thenReturn(post);
        when(appUserService.getByIdOrThrow(1L)).thenReturn(normalUser());
        CreateClaimRequest request = new CreateClaimRequest();
        request.setPostId(10L);
        request.setPrivateFeatureAnswer("蓝色贴纸");

        assertThatThrownBy(() -> service.createClaim(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能认领自己发布的招领单");
    }

    private LostFoundPost foundPost() {
        LostFoundPost post = new LostFoundPost();
        post.setId(10L);
        post.setUserId(1L);
        post.setPostType("FOUND");
        post.setStatus("MATCHING");
        post.setTitle("捡到校园卡");
        post.setItemName("校园卡");
        post.setStorageLocation("保卫处");
        post.setDeleted(0);
        return post;
    }

    private AppUser normalUser() {
        AppUser user = new AppUser();
        user.setId(2L);
        user.setStatus("NORMAL");
        return user;
    }

    private void injectMapper(ClaimRecordServiceImpl service, ClaimRecordMapper mapper) {
        try {
            Field field = findField(service.getClass(), "baseMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=ClaimRecordServiceTest
```

Expected: FAIL，`ClaimRecordServiceImpl` 不存在。

- [ ] **Step 3: 创建 Service 接口**

创建 `backend/src/main/java/com/shigui/service/ClaimRecordService.java`：

```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.AdminClaimResponse;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.entity.ClaimRecord;

public interface ClaimRecordService extends IService<ClaimRecord> {
    ClaimResponse createClaim(Long claimantUserId, CreateClaimRequest request);
    Page<ClaimResponse> listMine(Long userId, int page, int size);
    ClaimResponse confirmReceive(Long userId, Long claimId);
    Page<AdminClaimResponse> listAdminClaims(int page, int size, String status);
    AdminClaimResponse approveByAdmin(Long claimId);
    AdminClaimResponse rejectByAdmin(Long claimId, String reason);
}
```

- [ ] **Step 4: 实现 ClaimRecordServiceImpl**

创建 `backend/src/main/java/com/shigui/service/impl/ClaimRecordServiceImpl.java`。核心方法必须包含以下逻辑：

```java
package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.config.AiClaimReviewProperties;
import com.shigui.dto.*;
import com.shigui.entity.AppUser;
import com.shigui.entity.ClaimRecord;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.ClaimRecordMapper;
import com.shigui.service.AiClaimReviewClient;
import com.shigui.service.AppUserService;
import com.shigui.service.ClaimRecordService;
import com.shigui.service.LostFoundPostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimRecordServiceImpl extends ServiceImpl<ClaimRecordMapper, ClaimRecord> implements ClaimRecordService {
    private final LostFoundPostService lostFoundPostService;
    private final AppUserService appUserService;
    private final AiClaimReviewClient aiClaimReviewClient;
    private final AiClaimReviewProperties properties;

    public ClaimRecordServiceImpl(LostFoundPostService lostFoundPostService,
                                  AppUserService appUserService,
                                  AiClaimReviewClient aiClaimReviewClient,
                                  AiClaimReviewProperties properties) {
        this.lostFoundPostService = lostFoundPostService;
        this.appUserService = appUserService;
        this.aiClaimReviewClient = aiClaimReviewClient;
        this.properties = properties;
    }

    @Override
    @Transactional
    public ClaimResponse createClaim(Long claimantUserId, CreateClaimRequest request) {
        AppUser user = appUserService.getByIdOrThrow(claimantUserId);
        if ("BANNED".equals(user.getStatus())) throw new IllegalArgumentException("封禁用户不能申请认领");
        if (request.getPostId() == null) throw new IllegalArgumentException("postId 不能为空");
        if (request.getPrivateFeatureAnswer() == null || request.getPrivateFeatureAnswer().isBlank()) {
            throw new IllegalArgumentException("私密特征答案不能为空");
        }
        LostFoundPost post = lostFoundPostService.getById(request.getPostId());
        validateClaimablePost(post, claimantUserId);
        long running = count(new LambdaQueryWrapper<ClaimRecord>()
                .eq(ClaimRecord::getPostId, post.getId())
                .in(ClaimRecord::getStatus, List.of("PENDING_AI_REVIEW", "PENDING_ADMIN_REVIEW", "VERIFIED")));
        if (running > 0) throw new IllegalArgumentException("该单据已有进行中的认领申请");

        ClaimRecord claim = new ClaimRecord();
        claim.setPostId(post.getId());
        claim.setClaimantUserId(claimantUserId);
        claim.setPrivateFeatureAnswer(request.getPrivateFeatureAnswer());
        claim.setStatus("PENDING_AI_REVIEW");
        claim.setDeleted(0);
        save(claim);

        post.setStatus("CLAIMING");
        lostFoundPostService.updateById(post);
        applyAiReview(post, claim);
        return toResponse(claim, post);
    }

    private void applyAiReview(LostFoundPost post, ClaimRecord claim) {
        try {
            AiClaimReviewResult result = aiClaimReviewClient.reviewClaim(post, claim.getPrivateFeatureAnswer());
            claim.setAiDecision(result.getDecision());
            claim.setAiConfidence(normalize(result.getConfidence()));
            claim.setAiReason(sanitize(result.getReason()));
            if ("APPROVE".equals(result.getDecision()) && claim.getAiConfidence().compareTo(properties.autoApproveThresholdValue()) >= 0) {
                claim.setStatus("VERIFIED");
                claim.setVerifiedAt(LocalDateTime.now());
                post.setStatus("RETURNING");
                lostFoundPostService.updateById(post);
            } else if ("REJECT".equals(result.getDecision()) && claim.getAiConfidence().compareTo(properties.autoRejectThresholdValue()) >= 0) {
                claim.setStatus("REJECTED");
                post.setStatus("MATCHING");
                lostFoundPostService.updateById(post);
            } else {
                claim.setStatus("PENDING_ADMIN_REVIEW");
            }
        } catch (Exception e) {
            claim.setAiDecision("NEEDS_REVIEW");
            claim.setAiConfidence(BigDecimal.ZERO);
            claim.setAiReason("AI 预审失败，等待管理员审核");
            claim.setStatus("PENDING_ADMIN_REVIEW");
        }
        updateById(claim);
    }

    private void validateClaimablePost(LostFoundPost post, Long claimantUserId) {
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) throw new IllegalArgumentException("单据不存在");
        if (!"FOUND".equals(post.getPostType())) throw new IllegalArgumentException("只能认领招领单");
        if (!"MATCHING".equals(post.getStatus())) throw new IllegalArgumentException("只能认领匹配中的招领单");
        if (post.getUserId().equals(claimantUserId)) throw new IllegalArgumentException("不能认领自己发布的招领单");
    }

    @Override
    public Page<ClaimResponse> listMine(Long userId, int page, int size) {
        Page<ClaimRecord> entityPage = page(new Page<>(page, size), new LambdaQueryWrapper<ClaimRecord>()
                .eq(ClaimRecord::getClaimantUserId, userId)
                .eq(ClaimRecord::getDeleted, 0)
                .orderByDesc(ClaimRecord::getCreatedAt));
        Page<ClaimResponse> result = new Page<>(page, size);
        result.setRecords(entityPage.getRecords().stream().map(this::toResponse).toList());
        result.setTotal(entityPage.getTotal());
        return result;
    }

    @Override
    @Transactional
    public ClaimResponse confirmReceive(Long userId, Long claimId) {
        ClaimRecord claim = getById(claimId);
        if (claim == null || Integer.valueOf(1).equals(claim.getDeleted())) throw new IllegalArgumentException("认领申请不存在");
        if (!claim.getClaimantUserId().equals(userId)) throw new IllegalArgumentException("只能确认自己的认领申请");
        if (!"VERIFIED".equals(claim.getStatus())) throw new IllegalArgumentException("只有已通过的认领申请可以确认收到");
        LostFoundPost post = lostFoundPostService.getById(claim.getPostId());
        claim.setStatus("COMPLETED");
        claim.setCompletedAt(LocalDateTime.now());
        updateById(claim);
        post.setStatus("COMPLETED");
        lostFoundPostService.updateById(post);
        return toResponse(claim, post);
    }

    @Override
    public Page<AdminClaimResponse> listAdminClaims(int page, int size, String status) {
        LambdaQueryWrapper<ClaimRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimRecord::getDeleted, 0);
        wrapper.eq(status != null && !status.isBlank(), ClaimRecord::getStatus, status);
        wrapper.orderByDesc(ClaimRecord::getCreatedAt);
        Page<ClaimRecord> entityPage = page(new Page<>(page, size), wrapper);
        Page<AdminClaimResponse> result = new Page<>(page, size);
        result.setRecords(entityPage.getRecords().stream().map(this::toAdminResponse).toList());
        result.setTotal(entityPage.getTotal());
        return result;
    }

    @Override
    @Transactional
    public AdminClaimResponse approveByAdmin(Long claimId) {
        ClaimRecord claim = getById(claimId);
        ensureAdminReviewable(claim);
        LostFoundPost post = lostFoundPostService.getById(claim.getPostId());
        claim.setStatus("VERIFIED");
        claim.setVerifiedAt(LocalDateTime.now());
        updateById(claim);
        post.setStatus("RETURNING");
        lostFoundPostService.updateById(post);
        return toAdminResponse(claim);
    }

    @Override
    @Transactional
    public AdminClaimResponse rejectByAdmin(Long claimId, String reason) {
        ClaimRecord claim = getById(claimId);
        ensureAdminReviewable(claim);
        LostFoundPost post = lostFoundPostService.getById(claim.getPostId());
        claim.setStatus("REJECTED");
        claim.setAdminReason(reason == null ? "" : reason);
        updateById(claim);
        post.setStatus("MATCHING");
        lostFoundPostService.updateById(post);
        return toAdminResponse(claim);
    }

    private void ensureAdminReviewable(ClaimRecord claim) {
        if (claim == null || Integer.valueOf(1).equals(claim.getDeleted())) throw new IllegalArgumentException("认领申请不存在");
        if (!List.of("PENDING_AI_REVIEW", "PENDING_ADMIN_REVIEW").contains(claim.getStatus())) {
            throw new IllegalArgumentException("当前认领申请不可审核");
        }
    }

    private ClaimResponse toResponse(ClaimRecord claim) {
        return toResponse(claim, lostFoundPostService.getById(claim.getPostId()));
    }

    private ClaimResponse toResponse(ClaimRecord claim, LostFoundPost post) {
        ClaimResponse response = new ClaimResponse();
        response.setId(claim.getId());
        response.setPostId(claim.getPostId());
        if (post != null) {
            response.setPostTitle(post.getTitle());
            response.setItemName(post.getItemName());
            response.setItemCategory(post.getItemCategory());
            response.setCampusArea(post.getCampusArea());
            response.setLocationName(post.getLocationName());
            if (List.of("VERIFIED", "COMPLETED").contains(claim.getStatus())) {
                response.setStorageLocation(post.getStorageLocation());
            }
        }
        response.setStatus(claim.getStatus());
        response.setAiDecision(claim.getAiDecision());
        response.setAiConfidence(claim.getAiConfidence());
        response.setAiReason(claim.getAiReason());
        response.setAdminReason(claim.getAdminReason());
        response.setCreatedAt(claim.getCreatedAt());
        response.setVerifiedAt(claim.getVerifiedAt());
        response.setCompletedAt(claim.getCompletedAt());
        return response;
    }

    private AdminClaimResponse toAdminResponse(ClaimRecord claim) {
        LostFoundPost post = lostFoundPostService.getById(claim.getPostId());
        AdminClaimResponse response = new AdminClaimResponse();
        response.setId(claim.getId());
        response.setPostId(claim.getPostId());
        response.setClaimantUserId(claim.getClaimantUserId());
        response.setPrivateFeatureAnswer(claim.getPrivateFeatureAnswer());
        response.setStatus(claim.getStatus());
        response.setAiDecision(claim.getAiDecision());
        response.setAiConfidence(claim.getAiConfidence());
        response.setAiReason(claim.getAiReason());
        response.setAdminReason(claim.getAdminReason());
        response.setCreatedAt(claim.getCreatedAt());
        response.setVerifiedAt(claim.getVerifiedAt());
        response.setCompletedAt(claim.getCompletedAt());
        if (post != null) {
            response.setPostTitle(post.getTitle());
            response.setItemName(post.getItemName());
            response.setItemCategory(post.getItemCategory());
            response.setCampusArea(post.getCampusArea());
            response.setLocationName(post.getLocationName());
            response.setStorageLocation(post.getStorageLocation());
            response.setPrivateFeature(post.getPrivateFeature());
        }
        return response;
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("\\d{4,}", "***")
                .replaceAll("卡号|学号|工号|编号|尾号", "")
                .trim();
    }
}
```

- [ ] **Step 5: 运行 Service 测试**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=ClaimRecordServiceTest
```

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add backend/src/main/java/com/shigui/service/ClaimRecordService.java backend/src/main/java/com/shigui/service/impl/ClaimRecordServiceImpl.java backend/src/test/java/com/shigui/service/ClaimRecordServiceTest.java
git commit -m "feat: add claim state machine service"
```

---

### Task 4: 用户端认领 API

**Files:**
- Create: `backend/src/main/java/com/shigui/controller/ClaimRecordController.java`
- Test: `backend/src/test/java/com/shigui/controller/ClaimRecordControllerTest.java`

- [ ] **Step 1: 写 Controller 测试**

创建 `backend/src/test/java/com/shigui/controller/ClaimRecordControllerTest.java`：

```java
package com.shigui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.service.AppUserService;
import com.shigui.service.ClaimRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimRecordControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimRecordService claimRecordService;

    @MockitoBean
    private AppUserService appUserService;

    private String getUserToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.setContext(request, response);
            cn.dev33.satoken.stp.StpUtil.login(2L);
            return cn.dev33.satoken.stp.StpUtil.getTokenValue();
        } finally {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.clearContext();
        }
    }

    @Test
    void createClaim_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":1,\"privateFeatureAnswer\":\"蓝色贴纸\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createClaim_loggedIn_returnsClaim() throws Exception {
        ClaimResponse response = new ClaimResponse();
        response.setId(10L);
        response.setPostId(1L);
        response.setStatus("PENDING_ADMIN_REVIEW");
        when(claimRecordService.createClaim(eq(2L), any(CreateClaimRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/claims")
                        .header("satoken", getUserToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":1,\"privateFeatureAnswer\":\"蓝色贴纸\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING_ADMIN_REVIEW"));
    }

    @Test
    void listMine_loggedIn_returnsPage() throws Exception {
        Page<ClaimResponse> page = new Page<>(1, 10);
        page.setTotal(0);
        when(claimRecordService.listMine(2L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/claims/mine").header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void confirmReceive_loggedIn_returnsCompletedClaim() throws Exception {
        ClaimResponse response = new ClaimResponse();
        response.setId(10L);
        response.setStatus("COMPLETED");
        when(claimRecordService.confirmReceive(2L, 10L)).thenReturn(response);

        mockMvc.perform(put("/api/claims/10/confirm-receive").header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=ClaimRecordControllerTest
```

Expected: FAIL，`ClaimRecordController` 不存在。

- [ ] **Step 3: 创建 Controller**

创建 `backend/src/main/java/com/shigui/controller/ClaimRecordController.java`：

```java
package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.ClaimResponse;
import com.shigui.dto.CreateClaimRequest;
import com.shigui.service.ClaimRecordService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
public class ClaimRecordController {
    private final ClaimRecordService claimRecordService;

    public ClaimRecordController(ClaimRecordService claimRecordService) {
        this.claimRecordService = claimRecordService;
    }

    @PostMapping
    public Result<ClaimResponse> create(@RequestBody CreateClaimRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.createClaim(userId, request));
    }

    @GetMapping("/mine")
    public Result<Page<ClaimResponse>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.listMine(userId, page, size));
    }

    @PutMapping("/{id}/confirm-receive")
    public Result<ClaimResponse> confirmReceive(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(claimRecordService.confirmReceive(userId, id));
    }
}
```

- [ ] **Step 4: 运行 Controller 测试**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=ClaimRecordControllerTest
```

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add backend/src/main/java/com/shigui/controller/ClaimRecordController.java backend/src/test/java/com/shigui/controller/ClaimRecordControllerTest.java
git commit -m "feat: add user claim APIs"
```

---

### Task 5: 管理端认领审核 API

**Files:**
- Modify: `backend/src/main/java/com/shigui/controller/AdminController.java`
- Modify: `backend/src/test/java/com/shigui/controller/AdminControllerTest.java`

- [ ] **Step 1: 增加 AdminController 测试**

在 `backend/src/test/java/com/shigui/controller/AdminControllerTest.java` 中增加 `@MockitoBean`：

```java
@MockitoBean
private ClaimRecordService claimRecordService;
```

追加测试：

```java
@Test
void listClaims_loggedIn_returnsClaims() throws Exception {
    String token = getAdminToken();
    AdminClaimResponse row = new AdminClaimResponse();
    row.setId(1L);
    row.setPostTitle("捡到校园卡");
    row.setPrivateFeatureAnswer("蓝色贴纸");
    row.setStatus("PENDING_ADMIN_REVIEW");
    Page<AdminClaimResponse> page = new Page<>(1, 10);
    page.setRecords(java.util.List.of(row));
    page.setTotal(1);
    when(claimRecordService.listAdminClaims(1, 10, "PENDING_ADMIN_REVIEW")).thenReturn(page);

    mockMvc.perform(get("/api/admin/claims")
                    .param("status", "PENDING_ADMIN_REVIEW")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].postTitle").value("捡到校园卡"));
}

@Test
void approveClaim_loggedIn_returnsVerified() throws Exception {
    String token = getAdminToken();
    AdminClaimResponse response = new AdminClaimResponse();
    response.setId(1L);
    response.setStatus("VERIFIED");
    when(claimRecordService.approveByAdmin(1L)).thenReturn(response);

    mockMvc.perform(put("/api/admin/claims/1/approve").header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("VERIFIED"));
}

@Test
void rejectClaim_loggedIn_returnsRejected() throws Exception {
    String token = getAdminToken();
    AdminClaimResponse response = new AdminClaimResponse();
    response.setId(1L);
    response.setStatus("REJECTED");
    when(claimRecordService.rejectByAdmin(1L, "答案不匹配")).thenReturn(response);

    mockMvc.perform(put("/api/admin/claims/1/reject")
                    .header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"答案不匹配\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=AdminControllerTest
```

Expected: FAIL，`/api/admin/claims` 相关接口不存在。

- [ ] **Step 3: 修改 AdminController**

在 `AdminController` 构造函数中注入 `ClaimRecordService`：

```java
private final ClaimRecordService claimRecordService;
```

并追加接口：

```java
@GetMapping("/claims")
public Result<Page<AdminClaimResponse>> listClaims(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status) {
    requireAdmin();
    return Result.ok(claimRecordService.listAdminClaims(page, size, status));
}

@PutMapping("/claims/{id}/approve")
public Result<AdminClaimResponse> approveClaim(@PathVariable Long id) {
    requireAdmin();
    return Result.ok(claimRecordService.approveByAdmin(id));
}

@PutMapping("/claims/{id}/reject")
public Result<AdminClaimResponse> rejectClaim(@PathVariable Long id, @RequestBody RejectClaimRequest request) {
    requireAdmin();
    return Result.ok(claimRecordService.rejectByAdmin(id, request.getReason()));
}
```

需要 import：

```java
import com.shigui.dto.AdminClaimResponse;
import com.shigui.dto.RejectClaimRequest;
import com.shigui.service.ClaimRecordService;
```

- [ ] **Step 4: 运行 AdminControllerTest**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=AdminControllerTest
```

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add backend/src/main/java/com/shigui/controller/AdminController.java backend/src/test/java/com/shigui/controller/AdminControllerTest.java
git commit -m "feat: add admin claim review APIs"
```

---

### Task 6: 最小匿名聊天 API

**Files:**
- Create: `backend/src/main/java/com/shigui/service/ChatService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/ChatServiceImpl.java`
- Create: `backend/src/main/java/com/shigui/controller/ChatController.java`
- Test: `backend/src/test/java/com/shigui/service/ChatServiceTest.java`
- Test: `backend/src/test/java/com/shigui/controller/ChatControllerTest.java`

- [ ] **Step 1: 写 ChatServiceTest**

创建 `backend/src/test/java/com/shigui/service/ChatServiceTest.java`，至少覆盖：

```java
@Test
void createOrGetSession_postOwnerAndClaimant_getsSession() {
    // 构造 FOUND post userId=1，当前用户=2
    // mock chatSessionMapper.selectOne 返回 null
    // 调用 createOrGetSession(2L, 10L)
    // 断言 foundUserId=1, lostUserId=2, status=ACTIVE
}

@Test
void sendMessage_participant_savesTextMessage() {
    // mock session lostUserId=2, foundUserId=1
    // 调用 sendMessage(2L, sessionId, "你好")
    // 断言 msgType=TEXT, content=你好
}
```

实现时不要允许第三方访问会话。

- [ ] **Step 2: 写 Controller 测试**

创建 `backend/src/test/java/com/shigui/controller/ChatControllerTest.java`，覆盖：

```java
@Test
void createSession_loggedIn_returnsSession() throws Exception {
    // POST /api/chat/sessions
}

@Test
void messages_loggedIn_returnsList() throws Exception {
    // GET /api/chat/sessions/{id}/messages
}

@Test
void sendMessage_loggedIn_returnsMessage() throws Exception {
    // POST /api/chat/sessions/{id}/messages
}
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=ChatServiceTest,ChatControllerTest
```

Expected: FAIL，Chat service/controller 不存在。

- [ ] **Step 4: 创建 ChatService 接口**

```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.ChatMessageResponse;
import com.shigui.dto.ChatSessionResponse;
import com.shigui.entity.ChatSession;

import java.util.List;

public interface ChatService extends IService<ChatSession> {
    ChatSessionResponse createOrGetSession(Long userId, Long postId);
    List<ChatMessageResponse> listMessages(Long userId, Long sessionId);
    ChatMessageResponse sendMessage(Long userId, Long sessionId, String content);
}
```

- [ ] **Step 5: 实现 ChatServiceImpl**

核心规则：

- `createOrGetSession`: 当前用户不能是第三方；对 `FOUND` 单，`foundUserId=post.userId`，`lostUserId=currentUserId`；对 `LOST` 单反过来。
- `listMessages` 和 `sendMessage`: 只有 `lostUserId` 或 `foundUserId` 可访问。
- 封禁用户不能创建或发送消息。

实现文件：`backend/src/main/java/com/shigui/service/impl/ChatServiceImpl.java`。

- [ ] **Step 6: 创建 ChatController**

创建 `backend/src/main/java/com/shigui/controller/ChatController.java`：

```java
package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shigui.common.Result;
import com.shigui.dto.*;
import com.shigui.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public Result<ChatSessionResponse> createSession(@RequestBody CreateChatSessionRequest request) {
        return Result.ok(chatService.createOrGetSession(StpUtil.getLoginIdAsLong(), request.getPostId()));
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageResponse>> messages(@PathVariable Long id) {
        return Result.ok(chatService.listMessages(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping("/sessions/{id}/messages")
    public Result<ChatMessageResponse> send(@PathVariable Long id, @RequestBody SendMessageRequest request) {
        return Result.ok(chatService.sendMessage(StpUtil.getLoginIdAsLong(), id, request.getContent()));
    }
}
```

- [ ] **Step 7: 运行聊天测试**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
./mvnw test -Dtest=ChatServiceTest,ChatControllerTest
```

Expected: PASS。

- [ ] **Step 8: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add backend/src/main/java/com/shigui/service/ChatService.java backend/src/main/java/com/shigui/service/impl/ChatServiceImpl.java backend/src/main/java/com/shigui/controller/ChatController.java backend/src/test/java/com/shigui/service/ChatServiceTest.java backend/src/test/java/com/shigui/controller/ChatControllerTest.java
git commit -m "feat: add minimal chat APIs"
```

---

### Task 7: 小程序认领申请与认领记录

**Files:**
- Modify: `miniapp/app.json`
- Modify: `miniapp/pages/detail/detail.js`
- Modify: `miniapp/pages/detail/detail.wxml`
- Modify: `miniapp/pages/detail/detail.wxss`
- Modify: `miniapp/pages/mine/mine.js`
- Modify: `miniapp/pages/mine/mine.wxml`
- Create: `miniapp/pages/claims/claims.js`
- Create: `miniapp/pages/claims/claims.wxml`
- Create: `miniapp/pages/claims/claims.wxss`
- Create: `miniapp/pages/claims/claims.json`

- [ ] **Step 1: 注册页面**

在 `miniapp/app.json` 的 `pages` 数组加入：

```json
"pages/claims/claims"
```

- [ ] **Step 2: 详情页提交认领**

把 `miniapp/pages/detail/detail.js` 的 `applyClaim` 替换为：

```js
applyClaim() {
  if (!app.globalData.token) {
    wx.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  if (!this.data.post || this.data.post.postType !== 'FOUND' || this.data.post.status !== 'MATCHING') {
    wx.showToast({ title: '当前单据不可认领', icon: 'none' })
    return
  }
  wx.showModal({
    title: '申请认领',
    editable: true,
    placeholderText: '请填写只有物主知道的特征',
    success: (modal) => {
      if (!modal.confirm) return
      const answer = (modal.content || '').trim()
      if (!answer) {
        wx.showToast({ title: '请填写私密特征', icon: 'none' })
        return
      }
      wx.request({
        url: `${app.globalData.baseUrl}/api/claims`,
        method: 'POST',
        header: { satoken: app.globalData.token },
        data: { postId: Number(this.data.id), privateFeatureAnswer: answer },
        success: (res) => {
          if (res.data.code === 200) {
            wx.showToast({ title: '已提交认领', icon: 'success' })
            wx.navigateTo({ url: '/pages/claims/claims' })
          } else {
            wx.showToast({ title: res.data.message || '提交失败', icon: 'none' })
          }
        },
        fail: () => wx.showToast({ title: '网络错误', icon: 'none' })
      })
    }
  })
}
```

在 `detail.wxml` 中只对可认领 FOUND 单显示按钮：

```xml
<button wx:if="{{post.postType === 'FOUND' && post.status === 'MATCHING'}}" class="btn-primary" bindtap="applyClaim">申请认领</button>
```

- [ ] **Step 3: 我的页接认领记录入口**

在 `miniapp/pages/mine/mine.js` 添加：

```js
goClaims() {
  wx.navigateTo({ url: '/pages/claims/claims' })
}
```

在 `mine.wxml` 中把“认领记录”菜单项改为：

```xml
<view class="menu-item" bindtap="goClaims">
  <image class="menu-icon" src="/assets/icons/doc.svg" />
  <text class="menu-text">认领记录</text>
  <image class="arrow-icon" src="/assets/icons/chevron-right.svg" />
</view>
```

- [ ] **Step 4: 创建认领记录页**

创建 `miniapp/pages/claims/claims.js`：

```js
const app = getApp()

Page({
  data: { claims: [], page: 1 },
  onShow() {
    this.setData({ page: 1 })
    this.loadClaims()
  },
  onReachBottom() {
    this.setData({ page: this.data.page + 1 })
    this.loadClaims()
  },
  loadClaims() {
    wx.request({
      url: `${app.globalData.baseUrl}/api/claims/mine?page=${this.data.page}&size=10`,
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({ claims: this.data.page === 1 ? records : [...this.data.claims, ...records] })
        } else {
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '网络错误', icon: 'none' })
    })
  },
  confirmReceive(e) {
    const id = e.currentTarget.dataset.id
    wx.request({
      url: `${app.globalData.baseUrl}/api/claims/${id}/confirm-receive`,
      method: 'PUT',
      header: { satoken: app.globalData.token },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '已确认收到', icon: 'success' })
          this.setData({ page: 1 })
          this.loadClaims()
        } else {
          wx.showToast({ title: res.data.message || '操作失败', icon: 'none' })
        }
      }
    })
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.postId}` })
  }
})
```

创建 `miniapp/pages/claims/claims.wxml`：

```xml
<view class="container">
  <view class="claim-card" wx:for="{{claims}}" wx:key="id">
    <view class="claim-title">{{item.postTitle}}</view>
    <view class="claim-meta">{{item.itemName}} · {{item.campusArea}} {{item.locationName}}</view>
    <view class="claim-status">{{item.status}}</view>
    <view class="claim-reason" wx:if="{{item.aiReason}}">{{item.aiReason}}</view>
    <view class="storage" wx:if="{{item.storageLocation}}">暂存地点：{{item.storageLocation}}</view>
    <view class="actions">
      <button size="mini" bindtap="goDetail" data-post-id="{{item.postId}}">查看单据</button>
      <button wx:if="{{item.status === 'VERIFIED'}}" size="mini" type="primary" bindtap="confirmReceive" data-id="{{item.id}}">确认收到</button>
    </view>
  </view>
  <view wx:if="{{claims.length === 0}}" class="empty">暂无认领记录</view>
</view>
```

创建 `miniapp/pages/claims/claims.json`：

```json
{
  "navigationBarTitleText": "认领记录"
}
```

创建 `miniapp/pages/claims/claims.wxss`：

```css
.container { min-height: 100vh; background: #f0f2f0; padding: 24rpx; box-sizing: border-box; }
.claim-card { background: #fff; border-radius: 8rpx; padding: 24rpx; margin-bottom: 20rpx; }
.claim-title { font-size: 32rpx; font-weight: 700; color: #1f2937; }
.claim-meta { margin-top: 8rpx; color: #6b7280; font-size: 24rpx; }
.claim-status { margin-top: 12rpx; color: #00573D; font-size: 26rpx; font-weight: 600; }
.claim-reason, .storage { margin-top: 12rpx; font-size: 26rpx; color: #374151; }
.actions { display: flex; gap: 16rpx; margin-top: 20rpx; }
.empty { text-align: center; color: #999; margin-top: 120rpx; }
```

- [ ] **Step 5: 手动检查小程序文件**

Run:

```bash
cd /Users/cyrene/Dev/shigui
rg -n "pages/claims/claims|goClaims|confirmReceive|/api/claims" miniapp
```

Expected: 输出包含 `app.json`、`mine.js`、`detail.js`、`claims.js`。

- [ ] **Step 6: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add miniapp/app.json miniapp/pages/detail/detail.js miniapp/pages/detail/detail.wxml miniapp/pages/detail/detail.wxss miniapp/pages/mine/mine.js miniapp/pages/mine/mine.wxml miniapp/pages/claims/claims.js miniapp/pages/claims/claims.wxml miniapp/pages/claims/claims.wxss miniapp/pages/claims/claims.json
git commit -m "feat: add miniapp claim flow"
```

---

### Task 8: 小程序聊天页接真实接口

**Files:**
- Modify: `miniapp/pages/detail/detail.js`
- Modify: `miniapp/pages/detail/detail.wxml`
- Modify: `miniapp/pages/chat/chat.js`

- [ ] **Step 1: 详情页打开聊天**

在 `miniapp/pages/detail/detail.js` 中替换 `openChat`：

```js
openChat() {
  if (!app.globalData.token) {
    wx.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  wx.navigateTo({ url: `/pages/chat/chat?postId=${this.data.id}` })
}
```

在 `detail.wxml` 中保留联系按钮：

```xml
<button class="btn-secondary" bindtap="openChat">联系对方</button>
```

- [ ] **Step 2: 加强聊天页错误提示**

在 `miniapp/pages/chat/chat.js` 的每个 `wx.request` success 中，如果 `res.data.code !== 200`，添加：

```js
wx.showToast({ title: res.data.message || '请求失败', icon: 'none' })
```

在 `send()` 成功后，保留：

```js
this.setData({ inputText: '' })
this.loadMessages()
```

- [ ] **Step 3: 手动检查小程序文件**

Run:

```bash
cd /Users/cyrene/Dev/shigui
rg -n "/api/chat/sessions|openChat|请求失败" miniapp/pages/detail miniapp/pages/chat
```

Expected: detail 和 chat 文件都能搜到真实接口路径和错误提示。

- [ ] **Step 4: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add miniapp/pages/detail/detail.js miniapp/pages/detail/detail.wxml miniapp/pages/chat/chat.js
git commit -m "feat: wire miniapp chat page"
```

---

### Task 9: 管理端认领审核页面

**Files:**
- Modify: `admin-web/src/layouts/MainLayout.vue`
- Modify: `admin-web/src/router/index.ts`
- Create: `admin-web/src/views/ClaimReviewView.vue`

- [ ] **Step 1: 添加菜单**

在 `admin-web/src/layouts/MainLayout.vue` 的 `menuItems` 加：

```ts
{ path: '/claims', title: '认领审核' },
```

- [ ] **Step 2: 添加路由**

在 `admin-web/src/router/index.ts` 的 children 加：

```ts
{ path: 'claims', name: 'claims', component: () => import('../views/ClaimReviewView.vue') },
```

- [ ] **Step 3: 创建 ClaimReviewView**

创建 `admin-web/src/views/ClaimReviewView.vue`：

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const claims = ref<any[]>([])
const activeStatus = ref('PENDING_ADMIN_REVIEW')
const page = ref(1)
const total = ref(0)
const loading = ref(false)

onMounted(() => loadClaims())

function switchStatus(status: string) {
  activeStatus.value = status
  page.value = 1
  loadClaims()
}

async function loadClaims() {
  loading.value = true
  try {
    const status = activeStatus.value === 'all' ? undefined : activeStatus.value
    const res = await api.get('/api/admin/claims', { params: { page: page.value, size: 10, status } })
    if (res.data.code === 200) {
      claims.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function approve(id: number) {
  await ElMessageBox.confirm('确认通过该认领申请？通过后失主将看到暂存地点。', '通过认领', { type: 'success' })
  const res = await api.put(`/api/admin/claims/${id}/approve`)
  if (res.data.code === 200) {
    ElMessage.success('已通过')
    loadClaims()
  }
}

async function reject(id: number) {
  const { value } = await ElMessageBox.prompt('请填写拒绝原因', '拒绝认领', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消',
    inputType: 'textarea',
  })
  const res = await api.put(`/api/admin/claims/${id}/reject`, { reason: value || '' })
  if (res.data.code === 200) {
    ElMessage.success('已拒绝')
    loadClaims()
  }
}
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">认领审核</h2>
    <el-tabs v-model="activeStatus" @tab-click="(t: any) => switchStatus(t.paneName as string)">
      <el-tab-pane label="待人工审核" name="PENDING_ADMIN_REVIEW" />
      <el-tab-pane label="已通过" name="VERIFIED" />
      <el-tab-pane label="已拒绝" name="REJECTED" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>
    <el-table v-loading="loading" :data="claims" stripe>
      <el-table-column prop="postTitle" label="招领单" min-width="160" />
      <el-table-column prop="itemName" label="物品" width="100" />
      <el-table-column prop="privateFeature" label="私密特征" min-width="180" show-overflow-tooltip />
      <el-table-column prop="privateFeatureAnswer" label="认领答案" min-width="180" show-overflow-tooltip />
      <el-table-column prop="aiDecision" label="AI 判断" width="110" />
      <el-table-column prop="aiConfidence" label="置信度" width="90" />
      <el-table-column prop="aiReason" label="AI 原因" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="140" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING_ADMIN_REVIEW' || row.status === 'PENDING_AI_REVIEW'" size="small" type="success" @click="approve(row.id)">通过</el-button>
          <el-button v-if="row.status === 'PENDING_ADMIN_REVIEW' || row.status === 'PENDING_AI_REVIEW'" size="small" type="danger" @click="reject(row.id)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" :page-size="10" @current-change="loadClaims" background layout="prev, pager, next" />
  </div>
</template>
```

- [ ] **Step 4: 构建验证**

Run:

```bash
cd /Users/cyrene/Dev/shigui/admin-web
npm run build
```

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add admin-web/src/layouts/MainLayout.vue admin-web/src/router/index.ts admin-web/src/views/ClaimReviewView.vue
git commit -m "feat: add admin claim review page"
```

---

### Task 10: 全量验证、冒烟脚本与文档同步

**Files:**
- Modify: `docs/第19组-拾归系统-用户手册.md` if present
- Modify: `docs/第19组-拾归系统-整合.md` if present

- [ ] **Step 1: 后端全量测试**

Run:

```bash
cd /Users/cyrene/Dev/shigui/backend
AI_CLAIM_API_KEY="$AI_MATCH_API_KEY" AI_CLAIM_BASE_URL="${AI_MATCH_BASE_URL:-https://api.deepseek.com}" AI_CLAIM_MODEL="${AI_MATCH_MODEL:-deepseek-v4-flash}" ./mvnw test
```

Expected: PASS，新增 AI claim review 测试不 skip。

- [ ] **Step 2: 管理端构建**

Run:

```bash
cd /Users/cyrene/Dev/shigui/admin-web
npm run build
```

Expected: PASS。

- [ ] **Step 3: HTTP 冒烟验证**

启动后端后执行一组手动流程：

```bash
cd /Users/cyrene/Dev/shigui/backend
AI_CLAIM_API_KEY="$AI_MATCH_API_KEY" AI_CLAIM_BASE_URL="${AI_MATCH_BASE_URL:-https://api.deepseek.com}" AI_CLAIM_MODEL="${AI_MATCH_MODEL:-deepseek-v4-flash}" ./mvnw spring-boot:run
```

另一个终端验证：

```bash
BASE=http://localhost:8080
ADMIN_TOKEN=$(curl -sS -X POST "$BASE/api/admin/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | jq -r '.data')
USER_TOKEN=$(curl -sS -X POST "$BASE/api/user/wx-login" -H 'Content-Type: application/json' -d '{"openid":"s6_claim_user"}' | jq -r '.data')
curl -sS "$BASE/api/admin/claims?page=1&size=10" -H "satoken: $ADMIN_TOKEN" | jq '.code'
curl -sS "$BASE/api/claims/mine?page=1&size=10" -H "satoken: $USER_TOKEN" | jq '.code'
```

Expected: 两个命令都输出 `200`。完整发布/审核/认领流程可用小程序和管理端手动跑。

- [ ] **Step 4: 文档同步**

如果存在 `docs/第19组-拾归系统-用户手册.md`，把功能状态补充为：

```markdown
| AI 辅助认领 | 已支持失主申请认领、AI 管理员预审、人工管理员复核、失主确认收到 |
| 匿名聊天 | 已支持基础 HTTP 消息发送与读取，不支持实时推送 |
```

如果存在 `docs/第19组-拾归系统-整合.md`，补充 S6 接口清单：

```markdown
- `POST /api/claims`
- `GET /api/claims/mine`
- `PUT /api/claims/{id}/confirm-receive`
- `GET /api/admin/claims`
- `PUT /api/admin/claims/{id}/approve`
- `PUT /api/admin/claims/{id}/reject`
- `POST /api/chat/sessions`
- `GET /api/chat/sessions/{id}/messages`
- `POST /api/chat/sessions/{id}/messages`
```

- [ ] **Step 5: 最终状态检查**

Run:

```bash
cd /Users/cyrene/Dev/shigui
git status --short
```

Expected: 只剩不应提交的本地敏感配置，例如 `backend/src/main/resources/application-local.properties`。

- [ ] **Step 6: Commit**

```bash
cd /Users/cyrene/Dev/shigui
git add docs
git commit -m "docs: update docs for sprint 6 claim flow"
```

如果 `docs` 没有变化，跳过此 commit，并在最终汇报中说明“没有需要同步的 docs 文件”。

---

## 验证清单

- [ ] `claim_record` schema 包含 `ai_decision`, `ai_confidence`, `ai_reason`, `admin_reason`, `verified_at`, `completed_at`。
- [ ] `POST /api/claims` 只允许登录用户认领 `FOUND + MATCHING` 单。
- [ ] 发布者本人不能认领自己的招领单。
- [ ] 封禁用户不能申请认领或发送聊天消息。
- [ ] 同一单据不能同时存在多个进行中的 claim。
- [ ] AI 高置信 `APPROVE` 自动进入 `VERIFIED/RETURNING`。
- [ ] AI 高置信 `REJECT` 自动进入 `REJECTED/MATCHING`。
- [ ] AI 低置信或异常进入 `PENDING_ADMIN_REVIEW`。
- [ ] 管理员能通过/拒绝认领申请。
- [ ] 失主确认收到后 claim 和 post 都进入 `COMPLETED`。
- [ ] 小程序详情页能提交认领申请。
- [ ] 小程序认领记录页能显示状态和暂存地点。
- [ ] 管理端认领审核页面能显示 AI 决策、置信度、原因。
- [ ] 聊天 API 能创建会话、读取消息、发送文本消息。
- [ ] `cd backend && ./mvnw test` 通过。
- [ ] `cd admin-web && npm run build` 通过。

## 自检结果

- Spec coverage: 覆盖了 S6 spec 中的 AI 预审、人工审核、失主确认收到、小程序、管理端、聊天和权限规则。
- Placeholder scan: 未使用 TBD/TODO/待定占位。
- Type consistency: 计划统一使用 `PENDING_AI_REVIEW`, `PENDING_ADMIN_REVIEW`, `VERIFIED`, `REJECTED`, `COMPLETED`；API 路径与 spec 一致。
