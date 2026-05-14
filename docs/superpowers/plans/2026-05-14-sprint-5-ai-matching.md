# Sprint 5: AI 智能匹配 + 通知提醒 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现审核通过后自动调用 OpenAI-compatible API 做失物招领智能匹配，生成匹配记录和双方通知，并在小程序展示“我的匹配”和“匹配提醒”。

**Architecture:** 后端新增 `MatchRecordService` 作为匹配主流程，先规则预筛候选，再调用 `AiMatchClient` 获取语义匹配结果，最后写入 `match_record` 和 `notification`。`AdminPostService.approvePost()` 在单据进入 `MATCHING` 后触发匹配。小程序新增两个轻量页面读取 `/api/matches/mine` 和 `/api/notifications`。

**Tech Stack:** Spring Boot 3.5.14, MyBatis-Plus 3.5.16, Sa-Token 1.45.0, Java 21, Maven, OpenAI-compatible Chat Completions API, 原生微信小程序。

---

## 文件结构

- Modify: `scripts/init_schema.sql`
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/java/com/shigui/entity/MatchRecord.java`
- Create: `backend/src/main/java/com/shigui/entity/Notification.java`
- Create: `backend/src/main/java/com/shigui/mapper/MatchRecordMapper.java`
- Create: `backend/src/main/java/com/shigui/mapper/NotificationMapper.java`
- Create: `backend/src/main/java/com/shigui/dto/MatchResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/NotificationResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/AiMatchRequest.java`
- Create: `backend/src/main/java/com/shigui/dto/AiMatchResult.java`
- Create: `backend/src/main/java/com/shigui/config/AiMatchProperties.java`
- Create: `backend/src/main/java/com/shigui/service/AiMatchClient.java`
- Create: `backend/src/main/java/com/shigui/service/MatchRecordService.java`
- Create: `backend/src/main/java/com/shigui/service/NotificationService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleMatchClient.java`
- Create: `backend/src/main/java/com/shigui/service/impl/MatchRecordServiceImpl.java`
- Create: `backend/src/main/java/com/shigui/service/impl/NotificationServiceImpl.java`
- Modify: `backend/src/main/java/com/shigui/service/AdminPostService.java`
- Modify: `backend/src/main/java/com/shigui/service/impl/AdminPostServiceImpl.java`
- Create: `backend/src/main/java/com/shigui/controller/MatchRecordController.java`
- Create: `backend/src/main/java/com/shigui/controller/NotificationController.java`
- Create: `backend/src/test/java/com/shigui/service/OpenAiCompatibleMatchClientTest.java`
- Create: `backend/src/test/java/com/shigui/service/MatchRecordServiceTest.java`
- Create: `backend/src/test/java/com/shigui/controller/MatchRecordControllerTest.java`
- Create: `backend/src/test/java/com/shigui/controller/NotificationControllerTest.java`
- Modify: `backend/src/test/java/com/shigui/service/AdminPostServiceTest.java`
- Modify: `miniapp/app.json`
- Modify: `miniapp/pages/mine/mine.js`
- Modify: `miniapp/pages/mine/mine.wxml`
- Create: `miniapp/pages/matches/matches.js`
- Create: `miniapp/pages/matches/matches.wxml`
- Create: `miniapp/pages/matches/matches.wxss`
- Create: `miniapp/pages/matches/matches.json`
- Create: `miniapp/pages/notifications/notifications.js`
- Create: `miniapp/pages/notifications/notifications.wxml`
- Create: `miniapp/pages/notifications/notifications.wxss`
- Create: `miniapp/pages/notifications/notifications.json`

---

### Task 1: 数据库字段、实体、Mapper、DTO

**Files:**
- Modify: `scripts/init_schema.sql`
- Create: `backend/src/main/java/com/shigui/entity/MatchRecord.java`
- Create: `backend/src/main/java/com/shigui/entity/Notification.java`
- Create: `backend/src/main/java/com/shigui/mapper/MatchRecordMapper.java`
- Create: `backend/src/main/java/com/shigui/mapper/NotificationMapper.java`
- Create: `backend/src/main/java/com/shigui/dto/MatchResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/NotificationResponse.java`
- Create: `backend/src/main/java/com/shigui/dto/AiMatchRequest.java`
- Create: `backend/src/main/java/com/shigui/dto/AiMatchResult.java`

- [ ] **Step 1: 更新 init_schema**

在 `scripts/init_schema.sql` 的 `match_record` 表中，把 `score` 后面改成：

```sql
    score DECIMAL(5,4) DEFAULT 0 COMMENT '匹配得分 0-1',
    reason TEXT COMMENT 'AI 匹配理由',
    deleted TINYINT DEFAULT 0,
```

- [ ] **Step 2: 创建 MatchRecord 实体**

创建 `backend/src/main/java/com/shigui/entity/MatchRecord.java`：

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
@TableName("match_record")
public class MatchRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lostPostId;
    private Long foundPostId;
    private BigDecimal score;
    private String reason;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 Notification 实体**

创建 `backend/src/main/java/com/shigui/entity/Notification.java`：

```java
package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 Mapper**

创建 `backend/src/main/java/com/shigui/mapper/MatchRecordMapper.java`：

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.MatchRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MatchRecordMapper extends BaseMapper<MatchRecord> {
}
```

创建 `backend/src/main/java/com/shigui/mapper/NotificationMapper.java`：

```java
package com.shigui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shigui.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
```

- [ ] **Step 5: 创建响应 DTO**

创建 `backend/src/main/java/com/shigui/dto/MatchResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MatchResponse {
    private Long id;
    private BigDecimal score;
    private String reason;
    private PostResponse myPost;
    private PostResponse matchedPost;
    private LocalDateTime createdAt;
}
```

创建 `backend/src/main/java/com/shigui/dto/NotificationResponse.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: 创建 AI DTO**

创建 `backend/src/main/java/com/shigui/dto/AiMatchRequest.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiMatchRequest {
    private Candidate target;
    private List<Candidate> candidates;

    @Data
    public static class Candidate {
        private Long id;
        private String postType;
        private String title;
        private String itemName;
        private String itemCategory;
        private String description;
        private String privateFeature;
        private String campusArea;
        private String locationName;
        private LocalDateTime eventTime;
    }
}
```

创建 `backend/src/main/java/com/shigui/dto/AiMatchResult.java`：

```java
package com.shigui.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiMatchResult {
    private List<Decision> matches = new ArrayList<>();

    @Data
    public static class Decision {
        private Long candidatePostId;
        private Boolean matched;
        private BigDecimal score;
        private String reason;
    }
}
```

- [ ] **Step 7: 验证编译**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw compile
```

预期：`BUILD SUCCESS`。

- [ ] **Step 8: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add scripts/init_schema.sql backend/src/main/java/com/shigui/entity/MatchRecord.java backend/src/main/java/com/shigui/entity/Notification.java backend/src/main/java/com/shigui/mapper/MatchRecordMapper.java backend/src/main/java/com/shigui/mapper/NotificationMapper.java backend/src/main/java/com/shigui/dto/MatchResponse.java backend/src/main/java/com/shigui/dto/NotificationResponse.java backend/src/main/java/com/shigui/dto/AiMatchRequest.java backend/src/main/java/com/shigui/dto/AiMatchResult.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: add match and notification domain models"
```

---

### Task 2: OpenAI-compatible AiMatchClient（真实 API 测试）

**Files:**
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/java/com/shigui/config/AiMatchProperties.java`
- Create: `backend/src/main/java/com/shigui/service/AiMatchClient.java`
- Create: `backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleMatchClient.java`
- Create: `backend/src/test/java/com/shigui/service/OpenAiCompatibleMatchClientTest.java`

- [ ] **Step 1: 写真实 API 测试**

创建 `backend/src/test/java/com/shigui/service/OpenAiCompatibleMatchClientTest.java`：

```java
package com.shigui.service;

import com.shigui.config.AiMatchProperties;
import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.impl.OpenAiCompatibleMatchClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleMatchClientTest {

    @Test
    void rankMatches_realApi_returnsStrongMatchAndRejectsNoise() {
        AiMatchProperties properties = new AiMatchProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(requiredEnv("AI_MATCH_BASE_URL"));
        properties.setApiKey(requiredEnv("AI_MATCH_API_KEY"));
        properties.setModel(requiredEnv("AI_MATCH_MODEL"));
        properties.setTimeoutSeconds(30);
        properties.setIncludePrivateFeature(true);
        properties.setMaxCandidates(20);
        properties.setMaxResults(5);
        properties.setThreshold("0.70");

        OpenAiCompatibleMatchClient client = new OpenAiCompatibleMatchClient(properties);
        LostFoundPost target = post(100L, 21L, "FOUND", "捡到绿色卡套校园卡", "校园卡", "证件",
                "在南校园逸夫楼门口捡到一张校园卡，绿色透明卡套", "卡号后四位1234，卡套里有蓝色贴纸",
                "南校园", "逸夫楼", LocalDateTime.of(2026, 5, 14, 12, 0));

        List<LostFoundPost> candidates = List.of(
                post(1L, 11L, "LOST", "丢失绿色卡套校园卡", "校园卡", "证件", "可能在逸夫楼附近丢失，绿色卡套", "后四位1234，蓝色贴纸", "南校园", "逸夫楼", LocalDateTime.of(2026, 5, 14, 9, 0)),
                post(2L, 12L, "LOST", "丢了校园卡", "校园卡", "证件", "在南校园教学楼附近丢失", "卡号不记得", "南校园", "第三教学楼", LocalDateTime.of(2026, 5, 13, 18, 0)),
                post(3L, 13L, "LOST", "黑色雨伞丢失", "雨伞", "生活用品", "黑色长柄伞", "伞柄有划痕", "南校园", "图书馆", LocalDateTime.of(2026, 5, 14, 10, 0)),
                post(4L, 14L, "LOST", "耳机不见了", "耳机", "电子产品", "白色无线耳机", "左耳有小贴纸", "东校园", "食堂", LocalDateTime.of(2026, 5, 12, 10, 0)),
                post(5L, 15L, "LOST", "学生证丢失", "学生证", "证件", "学生证可能在操场附近掉了", "姓名首字母 C", "南校园", "操场", LocalDateTime.of(2026, 5, 10, 10, 0)),
                post(6L, 16L, "LOST", "水杯遗失", "水杯", "生活用品", "银色保温杯", "杯底贴纸", "北校园", "实验楼", LocalDateTime.of(2026, 5, 14, 10, 0)),
                post(7L, 17L, "LOST", "钥匙串丢了", "钥匙", "生活用品", "三把钥匙一个挂件", "红色挂件", "南校园", "宿舍区", LocalDateTime.of(2026, 5, 14, 10, 0)),
                post(8L, 18L, "LOST", "绿色卡套证件丢失", "校园卡", "证件", "绿色卡套，但地点在东校园", "后四位9999", "东校园", "行政楼", LocalDateTime.of(2026, 5, 14, 10, 0)),
                post(9L, 19L, "LOST", "书包丢失", "书包", "其他", "蓝色双肩包", "里面有一本英语书", "珠海校区", "教学楼", LocalDateTime.of(2026, 5, 14, 10, 0)),
                post(10L, 20L, "LOST", "银行卡遗失", "银行卡", "证件", "银行卡一张", "尾号8888", "南校园", "饭堂", LocalDateTime.of(2026, 5, 14, 10, 0))
        );

        AiMatchResult result = client.rankMatches(target, candidates);

        assertThat(result.getMatches()).isNotEmpty();
        AiMatchResult.Decision strong = result.getMatches().stream()
                .filter(item -> item.getCandidatePostId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(strong.getMatched()).isTrue();
        assertThat(strong.getScore()).isGreaterThanOrEqualTo(new java.math.BigDecimal("0.70"));
        assertThat(strong.getReason()).isNotBlank();
        assertThat(strong.getReason()).doesNotContain("1234");
        assertThat(result.getMatches()).allSatisfy(item ->
                assertThat(candidates.stream().map(LostFoundPost::getId).toList()).contains(item.getCandidatePostId()));
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for ./mvnw test");
        }
        return value;
    }

    private static LostFoundPost post(Long id, Long userId, String postType, String title, String itemName,
                                      String itemCategory, String description, String privateFeature,
                                      String campusArea, String locationName, LocalDateTime eventTime) {
        LostFoundPost post = new LostFoundPost();
        post.setId(id);
        post.setUserId(userId);
        post.setPostType(postType);
        post.setTitle(title);
        post.setItemName(itemName);
        post.setItemCategory(itemCategory);
        post.setDescription(description);
        post.setPrivateFeature(privateFeature);
        post.setCampusArea(campusArea);
        post.setLocationName(locationName);
        post.setEventTime(eventTime);
        post.setStatus("MATCHING");
        post.setDeleted(0);
        return post;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && AI_MATCH_API_KEY=dummy AI_MATCH_BASE_URL=http://127.0.0.1:1/v1 AI_MATCH_MODEL=dummy ./mvnw test -Dtest=OpenAiCompatibleMatchClientTest
```

预期：FAIL，`OpenAiCompatibleMatchClient` 和配置类还不存在。

- [ ] **Step 3: 添加配置项**

在 `backend/src/main/resources/application.properties` 追加：

```properties
ai.match.enabled=true
ai.match.base-url=${AI_MATCH_BASE_URL:}
ai.match.api-key=${AI_MATCH_API_KEY:}
ai.match.model=${AI_MATCH_MODEL:}
ai.match.timeout-seconds=30
ai.match.include-private-feature=true
ai.match.max-candidates=20
ai.match.max-results=5
ai.match.threshold=0.70
```

创建 `backend/src/main/java/com/shigui/config/AiMatchProperties.java`：

```java
package com.shigui.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
public class AiMatchProperties {
    @Value("${ai.match.enabled:true}")
    private boolean enabled;
    @Value("${ai.match.base-url:}")
    private String baseUrl;
    @Value("${ai.match.api-key:}")
    private String apiKey;
    @Value("${ai.match.model:}")
    private String model;
    @Value("${ai.match.timeout-seconds:30}")
    private int timeoutSeconds;
    @Value("${ai.match.include-private-feature:true}")
    private boolean includePrivateFeature;
    @Value("${ai.match.max-candidates:20}")
    private int maxCandidates;
    @Value("${ai.match.max-results:5}")
    private int maxResults;
    @Value("${ai.match.threshold:0.70}")
    private String threshold;

    public BigDecimal thresholdValue() {
        return new BigDecimal(threshold);
    }
}
```

- [ ] **Step 4: 创建 AiMatchClient 接口**

创建 `backend/src/main/java/com/shigui/service/AiMatchClient.java`：

```java
package com.shigui.service;

import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;

import java.util.List;

public interface AiMatchClient {
    AiMatchResult rankMatches(LostFoundPost targetPost, List<LostFoundPost> candidates);
}
```

- [ ] **Step 5: 实现 OpenAI-compatible Client**

创建 `backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleMatchClient.java`：

```java
package com.shigui.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shigui.config.AiMatchProperties;
import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.AiMatchClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiCompatibleMatchClient implements AiMatchClient {
    private final AiMatchProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleMatchClient(AiMatchProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiMatchResult rankMatches(LostFoundPost targetPost, List<LostFoundPost> candidates) {
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
                        Map.of("role", "user", "content", userPrompt(targetPost, candidates))
                )
        );

        String response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                .body(body)
                .retrieve()
                .body(String.class);
        return parseResponse(response, candidates);
    }

    private void validateConfig() {
        if (!properties.isEnabled()) throw new IllegalStateException("AI matching is disabled");
        if (isBlank(properties.getBaseUrl())) throw new IllegalStateException("AI_MATCH_BASE_URL is required");
        if (isBlank(properties.getApiKey())) throw new IllegalStateException("AI_MATCH_API_KEY is required");
        if (isBlank(properties.getModel())) throw new IllegalStateException("AI_MATCH_MODEL is required");
    }

    private String systemPrompt() {
        return """
                你是校园失物招领匹配助手。请判断目标单据和候选单据是否可能描述同一个物品。
                只返回 JSON，不要返回 Markdown。
                返回格式为 {"matches":[{"candidatePostId":数字,"matched":布尔值,"score":0到1的小数,"reason":"中文理由"}]}。
                reason 用中文简短说明匹配依据，但不要复述私密特征原文。
                """;
    }

    private String userPrompt(LostFoundPost targetPost, List<LostFoundPost> candidates) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "target", toPayload(targetPost),
                    "candidates", candidates.stream().map(this::toPayload).toList()
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AI match prompt", e);
        }
    }

    private Map<String, Object> toPayload(LostFoundPost post) {
        return Map.of(
                "id", post.getId(),
                "postType", value(post.getPostType()),
                "title", value(post.getTitle()),
                "itemName", value(post.getItemName()),
                "itemCategory", value(post.getItemCategory()),
                "description", value(post.getDescription()),
                "privateFeature", properties.isIncludePrivateFeature() ? value(post.getPrivateFeature()) : "",
                "campusArea", value(post.getCampusArea()),
                "locationName", value(post.getLocationName()),
                "eventTime", post.getEventTime() == null ? "" : post.getEventTime().toString()
        );
    }

    private AiMatchResult parseResponse(String response, List<LostFoundPost> candidates) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.at("/choices/0/message/content").asText();
            JsonNode matches = objectMapper.readTree(content).path("matches");
            List<Long> candidateIds = candidates.stream().map(LostFoundPost::getId).toList();
            AiMatchResult result = new AiMatchResult();
            List<AiMatchResult.Decision> decisions = new ArrayList<>();
            if (matches.isArray()) {
                for (JsonNode node : matches) {
                    Long candidatePostId = node.path("candidatePostId").asLong();
                    if (!candidateIds.contains(candidatePostId)) continue;
                    AiMatchResult.Decision decision = new AiMatchResult.Decision();
                    decision.setCandidatePostId(candidatePostId);
                    decision.setMatched(node.path("matched").asBoolean(false));
                    decision.setScore(new java.math.BigDecimal(node.path("score").asText("0")).max(java.math.BigDecimal.ZERO).min(java.math.BigDecimal.ONE));
                    decision.setReason(node.path("reason").asText(""));
                    decisions.add(decision);
                }
            }
            result.setMatches(decisions);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse AI match response", e);
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
```

- [ ] **Step 6: 运行真实 API 测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=OpenAiCompatibleMatchClientTest
```

预期：配置了 `AI_MATCH_API_KEY`、`AI_MATCH_BASE_URL`、`AI_MATCH_MODEL` 时 PASS；未配置任一环境变量时 FAIL 并指出缺少的变量名。

- [ ] **Step 7: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/src/main/resources/application.properties backend/src/main/java/com/shigui/config/AiMatchProperties.java backend/src/main/java/com/shigui/service/AiMatchClient.java backend/src/main/java/com/shigui/service/impl/OpenAiCompatibleMatchClient.java backend/src/test/java/com/shigui/service/OpenAiCompatibleMatchClientTest.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: add openai-compatible match client"
```

---

### Task 3: NotificationService

**Files:**
- Create: `backend/src/main/java/com/shigui/service/NotificationService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/NotificationServiceImpl.java`
- Create: `backend/src/test/java/com/shigui/service/NotificationServiceTest.java`

- [ ] **Step 1: 写 Service 测试**

创建 `backend/src/test/java/com/shigui/service/NotificationServiceTest.java`：

```java
package com.shigui.service;

import com.shigui.entity.Notification;
import com.shigui.mapper.NotificationMapper;
import com.shigui.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    private NotificationMapper notificationMapper;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        notificationService = new NotificationServiceImpl();
        injectMapper(notificationService, notificationMapper);
    }

    @Test
    void createMatchNotification_savesUnreadMatchNotification() {
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId(100L);
            assertThat(notification.getUserId()).isEqualTo(1L);
            assertThat(notification.getType()).isEqualTo("MATCH");
            assertThat(notification.getRelatedId()).isEqualTo(9L);
            assertThat(notification.getIsRead()).isZero();
            assertThat(notification.getDeleted()).isZero();
            assertThat(notification.getContent()).doesNotContain("后四位1234");
            return 1;
        });

        notificationService.createMatchNotification(1L, 9L, "校园卡", "南校园", "0.86", "校区和品类相似，时间接近。");

        verify(notificationMapper).insert(any(Notification.class));
    }

    private void injectMapper(NotificationServiceImpl service, NotificationMapper mapper) {
        try {
            Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=NotificationServiceTest
```

预期：FAIL，`NotificationService` 还不存在。

- [ ] **Step 3: 创建 NotificationService**

创建 `backend/src/main/java/com/shigui/service/NotificationService.java`：

```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.NotificationResponse;
import com.shigui.entity.Notification;

public interface NotificationService extends IService<Notification> {
    Notification createMatchNotification(Long userId, Long matchRecordId, String itemName, String campusArea, String score, String reason);
    Page<NotificationResponse> listMine(Long userId, int page, int size);
}
```

- [ ] **Step 4: 实现 NotificationServiceImpl**

创建 `backend/src/main/java/com/shigui/service/impl/NotificationServiceImpl.java`：

```java
package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.NotificationResponse;
import com.shigui.entity.Notification;
import com.shigui.mapper.NotificationMapper;
import com.shigui.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public Notification createMatchNotification(Long userId, Long matchRecordId, String itemName, String campusArea, String score, String reason) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType("MATCH");
        notification.setTitle("发现疑似匹配单据");
        notification.setContent("系统发现与你的" + itemName + "相关的疑似匹配，校区：" + campusArea + "，匹配分数：" + score + "。原因：" + sanitize(reason));
        notification.setRelatedId(matchRecordId);
        notification.setIsRead(0);
        notification.setDeleted(0);
        save(notification);
        return notification;
    }

    @Override
    public Page<NotificationResponse> listMine(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getDeleted, 0);
        wrapper.orderByAsc(Notification::getIsRead).orderByDesc(Notification::getCreatedAt);
        Page<Notification> entityPage = page(new Page<>(page, size), wrapper);
        List<NotificationResponse> responses = entityPage.getRecords().stream().map(this::toResponse).toList();
        Page<NotificationResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(entityPage.getTotal());
        return result;
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setRelatedId(notification.getRelatedId());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("后四位\\d+", "私密特征匹配");
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=NotificationServiceTest
```

预期：PASS。

- [ ] **Step 6: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/src/main/java/com/shigui/service/NotificationService.java backend/src/main/java/com/shigui/service/impl/NotificationServiceImpl.java backend/src/test/java/com/shigui/service/NotificationServiceTest.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: add notification service"
```

---

### Task 4: MatchRecordService 核心匹配流程

**Files:**
- Create: `backend/src/main/java/com/shigui/service/MatchRecordService.java`
- Create: `backend/src/main/java/com/shigui/service/impl/MatchRecordServiceImpl.java`
- Create: `backend/src/test/java/com/shigui/service/MatchRecordServiceTest.java`

- [ ] **Step 1: 写核心服务测试**

创建 `backend/src/test/java/com/shigui/service/MatchRecordServiceTest.java`：

```java
package com.shigui.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shigui.dto.AiMatchResult;
import com.shigui.entity.LostFoundPost;
import com.shigui.entity.MatchRecord;
import com.shigui.mapper.MatchRecordMapper;
import com.shigui.service.impl.MatchRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchRecordServiceTest {
    private LostFoundPostService lostFoundPostService;
    private AiMatchClient aiMatchClient;
    private NotificationService notificationService;
    private MatchRecordMapper matchRecordMapper;
    private MatchRecordServiceImpl matchRecordService;

    @BeforeEach
    void setUp() {
        lostFoundPostService = mock(LostFoundPostService.class);
        aiMatchClient = mock(AiMatchClient.class);
        notificationService = mock(NotificationService.class);
        matchRecordMapper = mock(MatchRecordMapper.class);
        matchRecordService = new MatchRecordServiceImpl(lostFoundPostService, aiMatchClient, notificationService);
        injectMapper(matchRecordService, matchRecordMapper);
    }

    @Test
    void generateMatchesForPost_aiMatchCreatesRecordAndNotifications() {
        LostFoundPost found = post(10L, 2L, "FOUND", "捡到绿色卡套校园卡", "校园卡", "证件", "南校园", "逸夫楼", "后四位1234");
        LostFoundPost lost = post(1L, 1L, "LOST", "丢失绿色卡套校园卡", "校园卡", "证件", "南校园", "逸夫楼", "后四位1234");
        when(lostFoundPostService.getById(10L)).thenReturn(found);
        when(lostFoundPostService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(lost));
        when(matchRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(matchRecordMapper.insert(any(MatchRecord.class))).thenAnswer(inv -> {
            MatchRecord record = inv.getArgument(0);
            record.setId(99L);
            assertThat(record.getLostPostId()).isEqualTo(1L);
            assertThat(record.getFoundPostId()).isEqualTo(10L);
            assertThat(record.getScore()).isEqualByComparingTo("0.86");
            assertThat(record.getReason()).doesNotContain("1234");
            return 1;
        });
        AiMatchResult result = new AiMatchResult();
        AiMatchResult.Decision decision = new AiMatchResult.Decision();
        decision.setCandidatePostId(1L);
        decision.setMatched(true);
        decision.setScore(new BigDecimal("0.86"));
        decision.setReason("校区、品类、地点和私密特征均高度吻合。");
        result.setMatches(List.of(decision));
        when(aiMatchClient.rankMatches(found, List.of(lost))).thenReturn(result);

        matchRecordService.generateMatchesForPost(10L);

        verify(matchRecordMapper).insert(any(MatchRecord.class));
        verify(notificationService).createMatchNotification(any(), any(), any(), any(), any(), any());
    }

    private LostFoundPost post(Long id, Long userId, String postType, String title, String itemName,
                               String itemCategory, String campusArea, String locationName, String privateFeature) {
        LostFoundPost post = new LostFoundPost();
        post.setId(id);
        post.setUserId(userId);
        post.setPostType(postType);
        post.setTitle(title);
        post.setItemName(itemName);
        post.setItemCategory(itemCategory);
        post.setDescription(title);
        post.setPrivateFeature(privateFeature);
        post.setCampusArea(campusArea);
        post.setLocationName(locationName);
        post.setEventTime(LocalDateTime.of(2026, 5, 14, 9, 0));
        post.setStatus("MATCHING");
        post.setDeleted(0);
        return post;
    }

    private void injectMapper(MatchRecordServiceImpl service, MatchRecordMapper mapper) {
        try {
            Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=MatchRecordServiceTest
```

预期：FAIL，`MatchRecordService` 还不存在。

- [ ] **Step 3: 创建 MatchRecordService 接口**

创建 `backend/src/main/java/com/shigui/service/MatchRecordService.java`：

```java
package com.shigui.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shigui.dto.MatchResponse;
import com.shigui.entity.MatchRecord;

public interface MatchRecordService extends IService<MatchRecord> {
    void generateMatchesForPost(Long postId);
    Page<MatchResponse> listMine(Long userId, int page, int size);
}
```

- [ ] **Step 4: 实现 MatchRecordServiceImpl**

创建 `backend/src/main/java/com/shigui/service/impl/MatchRecordServiceImpl.java`：

```java
package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.AiMatchResult;
import com.shigui.dto.MatchResponse;
import com.shigui.dto.PostResponse;
import com.shigui.entity.LostFoundPost;
import com.shigui.entity.MatchRecord;
import com.shigui.mapper.MatchRecordMapper;
import com.shigui.service.AiMatchClient;
import com.shigui.service.LostFoundPostService;
import com.shigui.service.MatchRecordService;
import com.shigui.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchRecordServiceImpl extends ServiceImpl<MatchRecordMapper, MatchRecord> implements MatchRecordService {
    private static final BigDecimal THRESHOLD = new BigDecimal("0.70");
    private static final int MAX_CANDIDATES = 20;
    private static final int MAX_RESULTS = 5;

    private final LostFoundPostService lostFoundPostService;
    private final AiMatchClient aiMatchClient;
    private final NotificationService notificationService;

    public MatchRecordServiceImpl(LostFoundPostService lostFoundPostService,
                                  AiMatchClient aiMatchClient,
                                  NotificationService notificationService) {
        this.lostFoundPostService = lostFoundPostService;
        this.aiMatchClient = aiMatchClient;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void generateMatchesForPost(Long postId) {
        LostFoundPost target = lostFoundPostService.getById(postId);
        if (target == null || !"MATCHING".equals(target.getStatus()) || Integer.valueOf(1).equals(target.getDeleted())) {
            return;
        }
        List<LostFoundPost> candidates = loadCandidates(target).stream()
                .sorted(Comparator.comparing(candidate -> ruleScore(target, candidate), Comparator.reverseOrder()))
                .limit(MAX_CANDIDATES)
                .toList();
        if (candidates.isEmpty()) return;
        AiMatchResult aiResult = callAi(target, candidates);
        Map<Long, LostFoundPost> candidateMap = candidates.stream().collect(Collectors.toMap(LostFoundPost::getId, item -> item));
        int created = 0;
        for (AiMatchResult.Decision decision : aiResult.getMatches()) {
            if (created >= MAX_RESULTS) break;
            LostFoundPost candidate = candidateMap.get(decision.getCandidatePostId());
            if (candidate == null || !Boolean.TRUE.equals(decision.getMatched())) continue;
            BigDecimal score = normalize(decision.getScore());
            if (score.compareTo(THRESHOLD) < 0) continue;
            if (createMatch(target, candidate, score, sanitize(decision.getReason()))) created++;
        }
    }

    @Override
    public Page<MatchResponse> listMine(Long userId, int page, int size) {
        Page<MatchRecord> entityPage = page(new Page<>(page, size),
                new LambdaQueryWrapper<MatchRecord>().eq(MatchRecord::getDeleted, 0).orderByDesc(MatchRecord::getCreatedAt));
        List<MatchResponse> responses = entityPage.getRecords().stream()
                .map(record -> toResponse(record, userId))
                .filter(response -> response.getMyPost() != null)
                .toList();
        Page<MatchResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(responses.size());
        return result;
    }

    private List<LostFoundPost> loadCandidates(LostFoundPost target) {
        String oppositeType = "LOST".equals(target.getPostType()) ? "FOUND" : "LOST";
        return lostFoundPostService.list(new LambdaQueryWrapper<LostFoundPost>()
                .eq(LostFoundPost::getPostType, oppositeType)
                .eq(LostFoundPost::getStatus, "MATCHING")
                .eq(LostFoundPost::getDeleted, 0)
                .ne(LostFoundPost::getId, target.getId()));
    }

    private AiMatchResult callAi(LostFoundPost target, List<LostFoundPost> candidates) {
        try {
            return aiMatchClient.rankMatches(target, candidates);
        } catch (Exception e) {
            AiMatchResult fallback = new AiMatchResult();
            fallback.setMatches(candidates.stream().map(candidate -> {
                AiMatchResult.Decision decision = new AiMatchResult.Decision();
                decision.setCandidatePostId(candidate.getId());
                decision.setMatched(true);
                decision.setScore(ruleScore(target, candidate));
                decision.setReason(ruleReason(target, candidate));
                return decision;
            }).toList());
            return fallback;
        }
    }

    private boolean createMatch(LostFoundPost target, LostFoundPost candidate, BigDecimal score, String reason) {
        Long lostId = "LOST".equals(target.getPostType()) ? target.getId() : candidate.getId();
        Long foundId = "FOUND".equals(target.getPostType()) ? target.getId() : candidate.getId();
        Long existing = count(new LambdaQueryWrapper<MatchRecord>()
                .eq(MatchRecord::getLostPostId, lostId)
                .eq(MatchRecord::getFoundPostId, foundId));
        if (existing != null && existing > 0) return false;
        MatchRecord record = new MatchRecord();
        record.setLostPostId(lostId);
        record.setFoundPostId(foundId);
        record.setScore(score.setScale(4, RoundingMode.HALF_UP));
        record.setReason(reason);
        record.setDeleted(0);
        save(record);
        LostFoundPost lostPost = "LOST".equals(target.getPostType()) ? target : candidate;
        LostFoundPost foundPost = "FOUND".equals(target.getPostType()) ? target : candidate;
        notificationService.createMatchNotification(lostPost.getUserId(), record.getId(), lostPost.getItemName(), lostPost.getCampusArea(), score.toPlainString(), reason);
        notificationService.createMatchNotification(foundPost.getUserId(), record.getId(), foundPost.getItemName(), foundPost.getCampusArea(), score.toPlainString(), reason);
        return true;
    }

    private BigDecimal ruleScore(LostFoundPost target, LostFoundPost candidate) {
        BigDecimal score = BigDecimal.ZERO;
        if (equalsText(target.getCampusArea(), candidate.getCampusArea())) score = score.add(new BigDecimal("0.20"));
        if (equalsText(target.getItemCategory(), candidate.getItemCategory())) score = score.add(new BigDecimal("0.25"));
        score = score.add(timeScore(target, candidate));
        if (containsEither(target.getTitle() + target.getItemName(), candidate.getTitle() + candidate.getItemName())) score = score.add(new BigDecimal("0.20"));
        if (containsEither(target.getPrivateFeature(), candidate.getPrivateFeature())) score = score.add(new BigDecimal("0.15"));
        return normalize(score);
    }

    private BigDecimal timeScore(LostFoundPost target, LostFoundPost candidate) {
        if (target.getEventTime() == null || candidate.getEventTime() == null) return BigDecimal.ZERO;
        long days = Math.abs(Duration.between(target.getEventTime(), candidate.getEventTime()).toDays());
        if (days <= 1) return new BigDecimal("0.20");
        if (days <= 3) return new BigDecimal("0.12");
        if (days <= 7) return new BigDecimal("0.06");
        return BigDecimal.ZERO;
    }

    private MatchResponse toResponse(MatchRecord record, Long userId) {
        LostFoundPost lost = lostFoundPostService.getById(record.getLostPostId());
        LostFoundPost found = lostFoundPostService.getById(record.getFoundPostId());
        MatchResponse response = new MatchResponse();
        response.setId(record.getId());
        response.setScore(record.getScore());
        response.setReason(record.getReason());
        response.setCreatedAt(record.getCreatedAt());
        if (lost != null && lost.getUserId().equals(userId)) {
            response.setMyPost(toPostResponse(lost));
            response.setMatchedPost(toPostResponse(found));
        } else if (found != null && found.getUserId().equals(userId)) {
            response.setMyPost(toPostResponse(found));
            response.setMatchedPost(toPostResponse(lost));
        }
        return response;
    }

    private PostResponse toPostResponse(LostFoundPost post) {
        if (post == null) return null;
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setPostType(post.getPostType());
        response.setTitle(post.getTitle());
        response.setItemName(post.getItemName());
        response.setItemCategory(post.getItemCategory());
        response.setDescription(post.getDescription());
        response.setCampusArea(post.getCampusArea());
        response.setLocationName(post.getLocationName());
        response.setStorageLocation(post.getStorageLocation());
        response.setEventTime(post.getEventTime());
        response.setPublishedAt(post.getPublishedAt());
        response.setStatus(post.getStatus());
        return response;
    }

    private String ruleReason(LostFoundPost target, LostFoundPost candidate) {
        return "系统根据校区、品类、时间和文本相似度判断为疑似匹配。";
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("\\d{3,}", "***");
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private boolean equalsText(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    private boolean containsEither(String left, String right) {
        if (left == null || right == null) return false;
        String a = left.replaceAll("\\s+", "");
        String b = right.replaceAll("\\s+", "");
        return !a.isBlank() && !b.isBlank() && (a.contains(b) || b.contains(a));
    }
}
```

- [ ] **Step 5: 运行核心服务测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=MatchRecordServiceTest
```

预期：PASS。

- [ ] **Step 6: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/src/main/java/com/shigui/service/MatchRecordService.java backend/src/main/java/com/shigui/service/impl/MatchRecordServiceImpl.java backend/src/test/java/com/shigui/service/MatchRecordServiceTest.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: generate ai match records and notifications"
```

---

### Task 5: 审核通过后触发匹配

**Files:**
- Modify: `backend/src/main/java/com/shigui/service/impl/AdminPostServiceImpl.java`
- Modify: `backend/src/test/java/com/shigui/service/AdminPostServiceTest.java`

- [ ] **Step 1: 更新 AdminPostServiceTest**

修改 `AdminPostServiceTest`：新增 mock 字段：

```java
@Mock
private MatchRecordService matchRecordService;
```

把 `setUp()` 改成：

```java
@BeforeEach
void setUp() {
    adminPostService = new AdminPostServiceImpl(lostFoundPostService, auditRecordService, matchRecordService);
}
```

在 `approvePost_success()` 末尾增加：

```java
verify(matchRecordService).generateMatchesForPost(1L);
```

在异常路径测试中增加：

```java
verify(matchRecordService, never()).generateMatchesForPost(anyLong());
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AdminPostServiceTest
```

预期：FAIL，构造函数还没有注入 `MatchRecordService`。

- [ ] **Step 3: 修改 AdminPostServiceImpl**

把 `backend/src/main/java/com/shigui/service/impl/AdminPostServiceImpl.java` 构造函数和字段改成：

```java
private final LostFoundPostService lostFoundPostService;
private final AuditRecordService auditRecordService;
private final MatchRecordService matchRecordService;

public AdminPostServiceImpl(LostFoundPostService lostFoundPostService,
                            AuditRecordService auditRecordService,
                            MatchRecordService matchRecordService) {
    this.lostFoundPostService = lostFoundPostService;
    this.auditRecordService = auditRecordService;
    this.matchRecordService = matchRecordService;
}
```

在 `approvePost()` 的 `auditRecordService.logApprove(adminId, postId);` 后追加：

```java
matchRecordService.generateMatchesForPost(postId);
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=AdminPostServiceTest
```

预期：PASS。

- [ ] **Step 5: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/src/main/java/com/shigui/service/impl/AdminPostServiceImpl.java backend/src/test/java/com/shigui/service/AdminPostServiceTest.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: trigger matching after post approval"
```

---

### Task 6: 匹配和通知 API

**Files:**
- Create: `backend/src/main/java/com/shigui/controller/MatchRecordController.java`
- Create: `backend/src/main/java/com/shigui/controller/NotificationController.java`
- Create: `backend/src/test/java/com/shigui/controller/MatchRecordControllerTest.java`
- Create: `backend/src/test/java/com/shigui/controller/NotificationControllerTest.java`

- [ ] **Step 1: 写 Controller 测试**

创建 `backend/src/test/java/com/shigui/controller/MatchRecordControllerTest.java`：

```java
package com.shigui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.dto.MatchResponse;
import com.shigui.service.AppUserService;
import com.shigui.service.MatchRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MatchRecordControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private MatchRecordService matchRecordService;
    @MockitoBean
    private AppUserService appUserService;

    @Test
    void mine_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/matches/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mine_loggedIn_returnsMatches() throws Exception {
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        Page<MatchResponse> page = new Page<>(1, 10);
        MatchResponse response = new MatchResponse();
        response.setId(9L);
        page.setRecords(List.of(response));
        page.setTotal(1);
        when(matchRecordService.listMine(eq(1L), eq(1), eq(10))).thenReturn(page);
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/matches/mine").header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(9));
    }

    private String loginAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"match_controller_user\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\\\"data\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
```

创建 `backend/src/test/java/com/shigui/controller/NotificationControllerTest.java`：

```java
package com.shigui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.dto.NotificationResponse;
import com.shigui.service.AppUserService;
import com.shigui.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private AppUserService appUserService;

    @Test
    void mine_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mine_loggedIn_returnsNotifications() throws Exception {
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        Page<NotificationResponse> page = new Page<>(1, 10);
        NotificationResponse response = new NotificationResponse();
        response.setId(8L);
        response.setTitle("发现疑似匹配单据");
        response.setContent("系统发现与你的校园卡相关的疑似匹配。");
        page.setRecords(List.of(response));
        page.setTotal(1);
        when(notificationService.listMine(eq(1L), eq(1), eq(10))).thenReturn(page);
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/notifications").header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].title").value("发现疑似匹配单据"));
    }

    private String loginAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"notification_controller_user\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\\\"data\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=MatchRecordControllerTest,NotificationControllerTest
```

预期：FAIL，Controller 还不存在。

- [ ] **Step 3: 创建 MatchRecordController**

创建 `backend/src/main/java/com/shigui/controller/MatchRecordController.java`：

```java
package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.MatchResponse;
import com.shigui.service.MatchRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchRecordController {
    private final MatchRecordService matchRecordService;

    public MatchRecordController(MatchRecordService matchRecordService) {
        this.matchRecordService = matchRecordService;
    }

    @GetMapping("/mine")
    public Result<Page<MatchResponse>> mine(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(matchRecordService.listMine(userId, page, size));
    }
}
```

- [ ] **Step 4: 创建 NotificationController**

创建 `backend/src/main/java/com/shigui/controller/NotificationController.java`：

```java
package com.shigui.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.common.Result;
import com.shigui.dto.NotificationResponse;
import com.shigui.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<Page<NotificationResponse>> mine(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(notificationService.listMine(userId, page, size));
    }
}
```

- [ ] **Step 5: 运行 Controller 测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test -Dtest=MatchRecordControllerTest,NotificationControllerTest
```

预期：PASS。

- [ ] **Step 6: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add backend/src/main/java/com/shigui/controller/MatchRecordController.java backend/src/main/java/com/shigui/controller/NotificationController.java backend/src/test/java/com/shigui/controller/MatchRecordControllerTest.java backend/src/test/java/com/shigui/controller/NotificationControllerTest.java
git -C /Users/cyrene/Dev/shigui commit -m "feat: expose match and notification APIs"
```

---

### Task 7: 小程序匹配和通知页面

**Files:**
- Modify: `miniapp/app.json`
- Modify: `miniapp/pages/mine/mine.js`
- Modify: `miniapp/pages/mine/mine.wxml`
- Create: `miniapp/pages/matches/matches.js`
- Create: `miniapp/pages/matches/matches.wxml`
- Create: `miniapp/pages/matches/matches.wxss`
- Create: `miniapp/pages/matches/matches.json`
- Create: `miniapp/pages/notifications/notifications.js`
- Create: `miniapp/pages/notifications/notifications.wxml`
- Create: `miniapp/pages/notifications/notifications.wxss`
- Create: `miniapp/pages/notifications/notifications.json`

- [ ] **Step 1: 注册页面**

在 `miniapp/app.json` 的 `pages` 数组中追加：

```json
"pages/matches/matches",
"pages/notifications/notifications"
```

- [ ] **Step 2: 更新 mine 页面入口**

在 `miniapp/pages/mine/mine.js` 追加：

```javascript
goMatches() {
  wx.navigateTo({ url: '/pages/matches/matches' })
},

goNotifications() {
  wx.navigateTo({ url: '/pages/notifications/notifications' })
}
```

在 `miniapp/pages/mine/mine.wxml` 中，把“匹配提醒”菜单项改为：

```xml
<view class="menu-item" bindtap="goNotifications">
  <image class="menu-icon" src="/assets/icons/bell.svg" />
  <text class="menu-text">匹配提醒</text>
  <view class="badge"></view>
  <image class="arrow-icon" src="/assets/icons/chevron-right.svg" />
</view>
<view class="menu-item" bindtap="goMatches">
  <image class="menu-icon" src="/assets/icons/doc.svg" />
  <text class="menu-text">我的匹配</text>
  <image class="arrow-icon" src="/assets/icons/chevron-right.svg" />
</view>
```

- [ ] **Step 3: 创建 matches 页面**

创建 `miniapp/pages/matches/matches.js`：

```javascript
const app = getApp()

Page({
  data: {
    matches: [],
    page: 1,
    loading: false,
    hasMore: true
  },

  onLoad() {
    this.loadMatches(true)
  },

  loadMatches(reset = false) {
    const token = app.globalData.token
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    if (this.data.loading || (!reset && !this.data.hasMore)) return
    const page = reset ? 1 : this.data.page
    this.setData({ loading: true })
    wx.request({
      url: `${app.globalData.baseUrl}/api/matches/mine?page=${page}&size=10`,
      header: { satoken: token },
      success: (res) => {
        if (res.data.code === 200) {
          const records = res.data.data.records || []
          this.setData({
            matches: reset ? records : [...this.data.matches, ...records],
            page: page + 1,
            hasMore: records.length === 10
          })
        } else {
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '网络错误', icon: 'none' }),
      complete: () => this.setData({ loading: false })
    })
  },

  onReachBottom() {
    this.loadMatches(false)
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  }
})
```

创建 `miniapp/pages/matches/matches.wxml`：

```xml
<view class="container">
  <view class="page-title">我的匹配</view>
  <view wx:if="{{matches.length === 0}}" class="empty">暂无匹配结果</view>
  <view wx:for="{{matches}}" wx:key="id" class="match-card">
    <view class="score">匹配分数 {{item.score}}</view>
    <view class="title">{{item.matchedPost.title}}</view>
    <view class="reason">{{item.reason}}</view>
    <button class="detail-btn" data-id="{{item.matchedPost.id}}" bindtap="goDetail">查看详情</button>
  </view>
</view>
```

创建 `miniapp/pages/matches/matches.wxss`：

```css
.container { min-height: 100vh; background: #f8f9fa; padding: 28rpx; box-sizing: border-box; }
.page-title { font-size: 40rpx; font-weight: 700; color: #1f2d2b; margin-bottom: 24rpx; }
.empty { color: #8a9490; text-align: center; padding: 160rpx 0; }
.match-card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.05); }
.score { color: #00573d; font-weight: 700; margin-bottom: 12rpx; }
.title { font-size: 32rpx; font-weight: 600; color: #1f2d2b; margin-bottom: 12rpx; }
.reason { color: #5f6b67; font-size: 26rpx; line-height: 1.6; margin-bottom: 20rpx; }
.detail-btn { background: #00573d; color: #fff; border-radius: 999rpx; font-size: 26rpx; }
```

创建 `miniapp/pages/matches/matches.json`：

```json
{
  "navigationBarTitleText": "我的匹配"
}
```

- [ ] **Step 4: 创建 notifications 页面**

创建 `miniapp/pages/notifications/notifications.js`：

```javascript
const app = getApp()

Page({
  data: {
    notifications: []
  },

  onLoad() {
    const token = app.globalData.token
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    wx.request({
      url: `${app.globalData.baseUrl}/api/notifications?page=1&size=20`,
      header: { satoken: token },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ notifications: res.data.data.records || [] })
        } else {
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '网络错误', icon: 'none' })
    })
  }
})
```

创建 `miniapp/pages/notifications/notifications.wxml`：

```xml
<view class="container">
  <view class="page-title">匹配提醒</view>
  <view wx:if="{{notifications.length === 0}}" class="empty">暂无通知</view>
  <view wx:for="{{notifications}}" wx:key="id" class="notification-card">
    <view class="title">{{item.title}}</view>
    <view class="content">{{item.content}}</view>
    <view class="time">{{item.createdAt}}</view>
  </view>
</view>
```

创建 `miniapp/pages/notifications/notifications.wxss`：

```css
.container { min-height: 100vh; background: #f8f9fa; padding: 28rpx; box-sizing: border-box; }
.page-title { font-size: 40rpx; font-weight: 700; color: #1f2d2b; margin-bottom: 24rpx; }
.empty { color: #8a9490; text-align: center; padding: 160rpx 0; }
.notification-card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.05); }
.title { font-size: 30rpx; font-weight: 700; color: #1f2d2b; margin-bottom: 12rpx; }
.content { color: #5f6b67; font-size: 26rpx; line-height: 1.6; }
.time { color: #9aa4a0; font-size: 22rpx; margin-top: 16rpx; }
```

创建 `miniapp/pages/notifications/notifications.json`：

```json
{
  "navigationBarTitleText": "匹配提醒"
}
```

- [ ] **Step 5: 小程序手工验证**

用微信开发者工具打开 `/Users/cyrene/Dev/shigui/miniapp`，验证：

```text
我的页能进入匹配提醒
我的页能进入我的匹配
未登录时提示请先登录
登录后能加载接口数据
点击匹配卡片能进入详情页
```

- [ ] **Step 6: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add miniapp/app.json miniapp/pages/mine/mine.js miniapp/pages/mine/mine.wxml miniapp/pages/matches miniapp/pages/notifications
git -C /Users/cyrene/Dev/shigui commit -m "feat: add miniapp match and notification pages"
```

---

### Task 8: 全量验证和文档同步

**Files:**
- Modify: `docs/第19组-拾归系统-测试报告.md`
- Modify: `docs/第19组-拾归系统-用户手册.md`

- [ ] **Step 1: 运行后端全量测试**

```bash
cd /Users/cyrene/Dev/shigui/backend && ./mvnw test
```

预期：PASS。该命令必须真实调用 OpenAI-compatible API；未设置 `AI_MATCH_API_KEY`、`AI_MATCH_BASE_URL`、`AI_MATCH_MODEL` 时必须 FAIL。

- [ ] **Step 2: 运行管理端构建**

```bash
cd /Users/cyrene/Dev/shigui/admin-web && npm run build
```

预期：`built` 成功；允许存在 Vite chunk size warning。

- [ ] **Step 3: 手工端到端验证**

```text
1. 启动后端并确保 AI_MATCH_* 环境变量存在
2. 小程序用户 A 发布 LOST 校园卡单据
3. 管理员审核通过该 LOST 单据
4. 小程序用户 B 发布 FOUND 校园卡单据
5. 管理员审核通过该 FOUND 单据
6. 后端自动生成 match_record
7. 两个用户都能在匹配提醒中看到 MATCH 通知
8. 两个用户都能在我的匹配中看到同一条匹配
9. 普通接口响应中看不到 privateFeature
```

- [ ] **Step 4: 更新测试报告**

在 `docs/第19组-拾归系统-测试报告.md` 中追加 S5 测试说明：

```markdown
### S5 智能匹配与通知

- 后端新增真实 OpenAI-compatible API 集成测试。
- 测试覆盖 1 条目标单据和 8-12 条候选单据，包含强匹配、中等匹配、弱匹配、干扰项和私密特征边界项。
- 匹配成功后写入 `match_record(score, reason)`，并为双方生成 `notification`。
- 验证普通用户响应不暴露 `privateFeature`。
```

- [ ] **Step 5: 更新用户手册**

在 `docs/第19组-拾归系统-用户手册.md` 的功能状态里，把智能匹配从“开发中”改为：

```markdown
| 智能匹配 | 已支持审核通过后自动匹配，并在“我的匹配/匹配提醒”中展示 |
```

- [ ] **Step 6: Commit**

```bash
git -C /Users/cyrene/Dev/shigui add docs/第19组-拾归系统-测试报告.md docs/第19组-拾归系统-用户手册.md
git -C /Users/cyrene/Dev/shigui commit -m "docs: update sprint 5 matching verification docs"
```

---

## 最终验收清单

- [ ] `scripts/init_schema.sql` 中 `match_record.reason` 已存在。
- [ ] `MatchRecord`、`Notification` 实体和 Mapper 已存在。
- [ ] `OpenAiCompatibleMatchClientTest` 在真实 API 环境变量存在时通过。
- [ ] `AdminPostService.approvePost()` 审核通过后触发 `generateMatchesForPost()`。
- [ ] 匹配只使用 `MATCHING`、未删除、相反类型候选。
- [ ] AI 输入默认包含 `privateFeature`。
- [ ] `privateFeature` 不出现在 `reason`、通知正文和普通用户响应中。
- [ ] 同一 lost/found pair 不重复写入。
- [ ] `GET /api/matches/mine` 只返回当前用户相关匹配。
- [ ] `GET /api/notifications` 只返回当前用户通知。
- [ ] 小程序能进入“我的匹配”和“匹配提醒”页面。
- [ ] `cd backend && ./mvnw test` 通过，并实际调用 AI API。
- [ ] `cd admin-web && npm run build` 通过。
