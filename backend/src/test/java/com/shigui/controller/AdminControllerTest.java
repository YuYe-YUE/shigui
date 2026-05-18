package com.shigui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.dto.AdminClaimResponse;
import com.shigui.entity.LostFoundPost;
import com.shigui.service.AdminPostService;
import com.shigui.service.AdminUserService;
import com.shigui.service.AppUserService;
import com.shigui.service.ClaimRecordService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private AdminPostService adminPostService;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private ClaimRecordService claimRecordService;

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
        mockMvc.perform(post("/api/admin/posts/1/approve").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void approvePost_alreadyDeleted_returns400() throws Exception {
        String token = getAdminToken();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("单据已被删除"))
                .when(adminPostService).approvePost(anyLong(), anyLong());
        mockMvc.perform(post("/api/admin/posts/1/approve").header("satoken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deletePost_loggedIn_returns200() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/posts/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("satoken", token).content("{\"reason\":\"违规\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deletePost_noReason_returns400() throws Exception {
        String token = getAdminToken();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("删除原因不能为空"))
                .when(adminPostService).deletePost(anyLong(), eq(1L), eq(""));
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

    @Test
    void approvePost_userToken_returns403() throws Exception {
        String token = getUserToken();
        mockMvc.perform(post("/api/admin/posts/1/approve").header("satoken", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePost_userToken_returns403() throws Exception {
        String token = getUserToken();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("satoken", token).content("{\"reason\":\"违规\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void banUser_userToken_returns403() throws Exception {
        String token = getUserToken();
        mockMvc.perform(put("/api/admin/users/1/ban").header("satoken", token))
                .andExpect(status().isForbidden());
    }

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

    @Test
    void listClaims_userToken_returns403() throws Exception {
        String token = getUserToken();
        mockMvc.perform(get("/api/admin/claims").header("satoken", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveClaim_userToken_returns403() throws Exception {
        String token = getUserToken();
        mockMvc.perform(put("/api/admin/claims/1/approve").header("satoken", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectClaim_userToken_returns403() throws Exception {
        String token = getUserToken();
        mockMvc.perform(put("/api/admin/claims/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("satoken", token)
                        .content("{\"reason\":\"答案不匹配\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listClaims_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/claims"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveClaim_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(put("/api/admin/claims/1/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectClaim_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(put("/api/admin/claims/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"答案不匹配\"}"))
                .andExpect(status().isUnauthorized());
    }
}
