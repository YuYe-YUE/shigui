package com.shigui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.dto.MapPostResponse;
import com.shigui.dto.PostResponse;
import com.shigui.service.AppUserService;
import com.shigui.service.FileStorageService;
import com.shigui.service.LostFoundPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    void publish_notLoggedIn_returns401() throws Exception {
        // SaToken 不再拦截 /api/posts，Controller 内部校验登录态
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void publish_loggedIn_returnsPostResponse() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(10L);
        response.setPostType("LOST");
        response.setTitle("丢失校园卡");
        response.setStatus("PENDING_AUDIT");
        response.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
        response.setCoverImageUrl("/uploads/posts/2026/05/18/a.jpg");
        response.setImageUrls(List.of(
                "/uploads/posts/2026/05/18/a.jpg",
                "/uploads/posts/2026/05/18/b.jpg"
        ));
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
                .andExpect(jsonPath("$.data.status").value("PENDING_AUDIT"))
                .andExpect(jsonPath("$.data.coverImageUrl").value("/uploads/posts/2026/05/18/a.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("/uploads/posts/2026/05/18/a.jpg"));
    }

    @Test
    void getDetail_loggedIn_returnsPostResponse() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(10L);
        response.setPostType("LOST");
        response.setTitle("丢失校园卡");
        response.setStatus("PENDING_AUDIT");
        response.setPublishedAt(LocalDateTime.of(2026, 5, 13, 10, 0));
        response.setCoverImageUrl("/uploads/posts/2026/05/18/a.jpg");
        response.setImageUrls(List.of("/uploads/posts/2026/05/18/a.jpg"));
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(lostFoundPostService.getDetail(eq(10L), anyLong())).thenReturn(response);

        String token = loginAndGetToken();

        mockMvc.perform(get("/api/posts/10")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("丢失校园卡"))
                .andExpect(jsonPath("$.data.coverImageUrl").value("/uploads/posts/2026/05/18/a.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("/uploads/posts/2026/05/18/a.jpg"))
                // 时间字段存在
                .andExpect(jsonPath("$.data.publishedAt").exists());
    }

    @Test
    void getDetail_notLoggedIn_returnsPostIfMatching() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(10L);
        response.setStatus("MATCHING");
        when(lostFoundPostService.getDetail(eq(10L), eq(0L))).thenReturn(response);
        mockMvc.perform(get("/api/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listPublic_noAuth_returns200() throws Exception {
        when(lostFoundPostService.listPublic(1, 10, null, null, null, null))
                .thenReturn(new Page<>(1, 10));
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listPublic_loggedIn_returns200() throws Exception {
        Page<PostResponse> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(lostFoundPostService.listPublic(eq(1), eq(10), isNull(), isNull(), isNull(), isNull())).thenReturn(emptyPage);

        String token = loginAndGetToken();

        mockMvc.perform(get("/api/posts")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void mine_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/posts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void mine_loggedIn_returns200() throws Exception {
        Page<PostResponse> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        when(lostFoundPostService.listMine(eq(1L), eq(1), eq(10), isNull())).thenReturn(emptyPage);

        String token = loginAndGetToken();

        mockMvc.perform(get("/api/posts/mine")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void mapPoints_noAuth_returnsFoundMarkers() throws Exception {
        MapPostResponse marker = new MapPostResponse();
        marker.setId(88L);
        marker.setItemName("校园卡");
        marker.setItemCategory("证件");
        marker.setCampusArea("南校园");
        marker.setLocationName("逸夫楼门口");
        marker.setLongitude(113.2931234);
        marker.setLatitude(23.0961234);
        marker.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));
        when(lostFoundPostService.listMapPosts()).thenReturn(List.of(marker));

        mockMvc.perform(get("/api/posts/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(88))
                .andExpect(jsonPath("$.data[0].itemName").value("校园卡"))
                .andExpect(jsonPath("$.data[0].itemCategory").value("证件"))
                .andExpect(jsonPath("$.data[0].campusArea").value("南校园"))
                .andExpect(jsonPath("$.data[0].locationName").value("逸夫楼门口"))
                .andExpect(jsonPath("$.data[0].longitude").value(113.2931234))
                .andExpect(jsonPath("$.data[0].latitude").value(23.0961234))
                .andExpect(jsonPath("$.data[0].eventTime").value("2026-05-13T09:30:00"))
                .andExpect(jsonPath("$.data[0].privateFeature").doesNotExist())
                .andExpect(jsonPath("$.data[0].storageLocation").doesNotExist())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data[0].description").doesNotExist())
                .andExpect(jsonPath("$.data[0].postType").doesNotExist())
                .andExpect(jsonPath("$.data[0].title").doesNotExist())
                .andExpect(jsonPath("$.data[0].status").doesNotExist());
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
                  "eventTime": "2026-05-13T09:30:00",
                  "imageUrls": [
                    "/uploads/posts/2026/05/18/a.jpg",
                    "/uploads/posts/2026/05/18/b.jpg"
                  ]
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
