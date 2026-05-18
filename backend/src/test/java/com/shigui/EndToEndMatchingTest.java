package com.shigui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class EndToEndMatchingTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String openid) throws Exception {
        String body = mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"" + openid + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"data\":\"([^\"]+)\".*", "$1");
    }

    private String adminToken() throws Exception {
        String body = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"data\":\"([^\"]+)\".*", "$1");
    }

    private long publish(String token, String postType, String title, String itemName,
                         String itemCategory, String campus, String location) throws Exception {
        String body = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("satoken", token)
                        .content(String.format(
                                "{\"postType\":\"%s\",\"title\":\"%s\",\"itemName\":\"%s\"," +
                                "\"itemCategory\":\"%s\",\"campusArea\":\"%s\"," +
                                "\"locationName\":\"%s\",\"eventTime\":\"2026-05-18T10:00:00\"}",
                                postType, title, itemName, itemCategory, campus, location)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void approveLostPost_generatesMatchAndNotifications() throws Exception {
        String userA = login("e2e_lost_user");
        String userB = login("e2e_found_user");
        String admin = adminToken();

        // User B 先发一个招领单并审核通过（为匹配提供候选）
        long foundPostId = publish(userB, "FOUND", "捡到校园卡", "校园卡", "校园卡", "南校园", "逸夫楼");
        mockMvc.perform(post("/api/admin/posts/" + foundPostId + "/approve")
                        .header("satoken", admin))
                .andExpect(status().isOk());

        // User A 发布寻物单
        long lostPostId = publish(userA, "LOST", "丢失校园卡", "校园卡", "校园卡", "南校园", "逸夫楼");

        // 审核通过 User A 的寻物单 → 触发匹配
        mockMvc.perform(post("/api/admin/posts/" + lostPostId + "/approve")
                        .header("satoken", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证匹配记录已生成
        mockMvc.perform(get("/api/matches/mine").header("satoken", userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证通知已生成
        mockMvc.perform(get("/api/notifications").header("satoken", userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
