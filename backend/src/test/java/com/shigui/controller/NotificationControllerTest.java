package com.shigui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.dto.NotificationResponse;
import com.shigui.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private String getUserToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        try {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.setContext(req, res);
            cn.dev33.satoken.stp.StpUtil.login(2L);
            return cn.dev33.satoken.stp.StpUtil.getTokenValue();
        } finally {
            cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil.clearContext();
        }
    }

    @Test
    void list_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_loggedIn_returnsPage() throws Exception {
        Page<NotificationResponse> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(notificationService.listMine(eq(2L), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/notifications").header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void list_loggedIn_returnsNotifications() throws Exception {
        NotificationResponse n = new NotificationResponse();
        n.setId(1L);
        n.setType("MATCH");
        n.setTitle("匹配提醒");
        n.setContent("系统发现疑似匹配");
        n.setIsRead(0);
        Page<NotificationResponse> page = new Page<>(1, 10);
        page.setRecords(List.of(n));
        page.setTotal(1);
        when(notificationService.listMine(2L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/notifications").header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].type").value("MATCH"))
                .andExpect(jsonPath("$.data.records[0].isRead").value(0));
    }
}
