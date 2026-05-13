package com.shigui.controller;

import com.shigui.entity.AppUser;
import com.shigui.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    /**
     * 登录接口应完成两件事：调用业务层确认用户，并返回 Sa-Token 生成的 token。
     */
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
        // /me 受 Sa-Token 拦截保护，未登录时应由全局异常处理器转换成 401。
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }
}
