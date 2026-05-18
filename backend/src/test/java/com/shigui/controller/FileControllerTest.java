package com.shigui.controller;

import com.shigui.dto.FileUploadResponse;
import com.shigui.entity.AppUser;
import com.shigui.service.AppUserService;
import com.shigui.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private AppUserService appUserService;

    @Test
    void upload_notLoggedIn_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campus-card.png",
                "image/png",
                "png-content".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void upload_loggedIn_returnsUrl() throws Exception {
        when(appUserService.loginByWechat(anyString())).thenReturn(1L);
        FileUploadResponse response = new FileUploadResponse();
        response.setUrl("/uploads/posts/2026/05/18/test.png");
        when(fileStorageService.storePostImage(any())).thenReturn(response);

        String token = loginAndGetToken();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campus-card.png",
                "image/png",
                "png-content".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value("/uploads/posts/2026/05/18/test.png"));
    }

    private String loginAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/api/user/wx-login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"openid\":\"file_controller_user\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return body.replaceAll(".*\\\"data\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
