package com.shigui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shigui.dto.MatchResponse;
import com.shigui.service.MatchRecordService;
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
class MatchRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchRecordService matchRecordService;

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
    void mine_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/matches/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mine_loggedIn_returnsPage() throws Exception {
        Page<MatchResponse> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(matchRecordService.listMine(eq(2L), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/matches/mine").header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void mine_loggedIn_returnsMatchData() throws Exception {
        MatchResponse match = new MatchResponse();
        match.setId(1L);
        match.setScore(new java.math.BigDecimal("0.86"));
        match.setReason("特征匹配");
        Page<MatchResponse> page = new Page<>(1, 10);
        page.setRecords(List.of(match));
        page.setTotal(1);
        when(matchRecordService.listMine(2L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/matches/mine").header("satoken", getUserToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].score").value(0.86));
    }
}
