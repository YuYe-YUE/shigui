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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimRecordService claimRecordService;

    @MockitoBean
    private AppUserService appUserService;

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
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING_ADMIN_REVIEW"));
    }

    @Test
    void listMine_loggedIn_returnsPage() throws Exception {
        Page<ClaimResponse> page = new Page<>(1, 10);
        page.setTotal(0);
        when(claimRecordService.listMine(2L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/claims/mine")
                        .header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void confirmReceive_loggedIn_returnsCompletedClaim() throws Exception {
        ClaimResponse response = new ClaimResponse();
        response.setId(10L);
        response.setStatus("COMPLETED");
        when(claimRecordService.confirmReceive(2L, 10L)).thenReturn(response);

        mockMvc.perform(put("/api/claims/10/confirm-receive")
                        .header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

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
}
