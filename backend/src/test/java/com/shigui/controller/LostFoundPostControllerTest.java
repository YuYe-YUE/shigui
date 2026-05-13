package com.shigui.controller;

import com.shigui.dto.PostResponse;
import com.shigui.service.AppUserService;
import com.shigui.service.LostFoundPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LostFoundPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LostFoundPostService lostFoundPostService;

    @MockitoBean
    private AppUserService appUserService;

    @Test
    void publish_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publish_loggedIn_returnsPostResponse() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(10L);
        response.setPostType("LOST");
        response.setTitle("丢失校园卡");
        response.setStatus("PENDING_AUDIT");
        response.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(lostFoundPostService.publish(anyLong(), any(com.shigui.dto.CreatePostRequest.class))).thenReturn(response);

        String token = loginAndGetToken();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("satoken", token)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING_AUDIT"));
    }

    @Test
    void getDetail_loggedIn_returnsPostResponse() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(10L);
        response.setPostType("LOST");
        response.setTitle("丢失校园卡");
        response.setStatus("PENDING_AUDIT");
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(lostFoundPostService.getDetail(10L)).thenReturn(response);

        String token = loginAndGetToken();

        mockMvc.perform(get("/api/posts/10")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("丢失校园卡"));
    }

    @Test
    void getDetail_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/posts/10"))
                .andExpect(status().isUnauthorized());
    }

    private String validJson() {
        return """
                {
                  "postType": "LOST",
                  "title": "丢失校园卡",
                  "itemName": "校园卡",
                  "itemCategory": "证件",
                  "description": "绿色卡套",
                  "privateFeature": "卡号后四位 1234",
                  "campusArea": "南校园",
                  "locationName": "逸夫楼",
                  "longitude": 113.2931234,
                  "latitude": 23.0961234,
                  "storageLocation": "",
                  "eventTime": "2026-05-13T09:30:00"
                }
                """;
    }

    private String loginAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"controller_publish_user\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return body.replaceAll(".*\\\"data\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
