package com.shigui.controller;

import com.shigui.entity.LostFoundPost;
import com.shigui.service.AdminUserService;
import com.shigui.service.AppUserService;
import com.shigui.service.AuditRecordService;
import com.shigui.service.LostFoundPostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private LostFoundPostService lostFoundPostService;

    @MockitoBean
    private AuditRecordService auditRecordService;

    @MockitoBean
    private AppUserService appUserService;

    private String getAdminToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.setContext(request, response);
            cn.dev33.satoken.stp.StpUtil.login(1L + 10_000_000L);
            return cn.dev33.satoken.stp.StpUtil.getTokenValue();
        } finally {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.clearContext();
        }
    }

    /**
     * Controller 测试只关心 HTTP 入参和响应结构，具体密码校验由 Service 单元测试覆盖。
     */
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
        // 参数缺失时 Controller 直接返回业务错误码，不进入 Service。
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void listPosts_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isUnauthorized());
    }

    private String getUserToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.setContext(request, response);
            cn.dev33.satoken.stp.StpUtil.login(1L);  // 普通用户，ID < 10_000_000
            return cn.dev33.satoken.stp.StpUtil.getTokenValue();
        } finally {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.clearContext();
        }
    }

    @Test
    void listPosts_userToken_returns403() throws Exception {
        String token = getUserToken();
        mockMvc.perform(get("/api/admin/posts").header("satoken", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listPosts_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        when(lostFoundPostService.page(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));
        mockMvc.perform(get("/api/admin/posts").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void postDetail_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        LostFoundPost post = new LostFoundPost();
        post.setId(1L); post.setTitle("test");
        when(lostFoundPostService.getById(1L)).thenReturn(post);
        mockMvc.perform(get("/api/admin/posts/1").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void approvePost_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        LostFoundPost post = new LostFoundPost();
        post.setId(1L); post.setStatus("PENDING_AUDIT");
        when(lostFoundPostService.getById(1L)).thenReturn(post);
        mockMvc.perform(post("/api/admin/posts/1/approve").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void approvePost_alreadyDeleted_returns400() throws Exception {
        String token = getAdminToken();
        LostFoundPost post = new LostFoundPost();
        post.setId(1L); post.setStatus("PENDING_AUDIT"); post.setDeleted(1);
        when(lostFoundPostService.getById(1L)).thenReturn(post);
        mockMvc.perform(post("/api/admin/posts/1/approve").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deletePost_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        when(lostFoundPostService.getById(1L)).thenReturn(post);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/posts/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("satoken", token).content("{\"reason\":\"违规\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deletePost_noReason_returns400() throws Exception {
        String token = getAdminToken();
        LostFoundPost post = new LostFoundPost();
        post.setId(1L);
        when(lostFoundPostService.getById(1L)).thenReturn(post);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/posts/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("satoken", token).content("{\"reason\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void listUsers_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        when(appUserService.page(any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));
        mockMvc.perform(get("/api/admin/users").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void banUser_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(put("/api/admin/users/1/ban").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void unbanUser_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(put("/api/admin/users/1/unban").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}
