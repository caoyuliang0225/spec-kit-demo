package com.portal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.auth.dto.request.WechatBindRequest;
import com.portal.auth.dto.request.WechatLoginRequest;
import com.portal.auth.dto.response.AuthResponse;
import com.portal.auth.exception.AuthException;
import com.portal.auth.service.AuthService;
import com.portal.auth.service.WechatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WechatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WechatService wechatService;

    @MockBean
    private AuthService authService;

    // ── WeChat Login ──────────────────────────────────────

    @Test
    void wechatLogin_success() throws Exception {
        var request = new WechatLoginRequest();
        request.setCode("valid-auth-code");
        request.setChannel("official_account");
        request.setDeviceId("device-001");
        request.setDeviceType("web");

        var tokenResult = new WechatService.WechatTokenResult();
        var userInfo = new WechatService.WechatUserInfo();

        when(wechatService.exchangeCode("valid-auth-code", "official_account"))
                .thenReturn(tokenResult);
        when(wechatService.fetchUserInfo(any(), any()))
                .thenReturn(userInfo);
        when(authService.wechatLogin(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new AuthResponse("wx-access-token", "wx-refresh-token", 900000));

        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("wx-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("wx-refresh-token"));
    }

    @Test
    void wechatLogin_withInvalidCode_shouldFail() throws Exception {
        var request = new WechatLoginRequest();
        request.setCode("bad-code");
        request.setChannel("official_account");

        when(wechatService.exchangeCode("bad-code", "official_account"))
                .thenThrow(new AuthException("AUTH_020", "WeChat auth code exchange failed"));

        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_020"));
    }

    @Test
    void wechatLogin_withMissingFields_shouldFail() throws Exception {
        var request = new WechatLoginRequest();
        request.setCode("");

        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wechatLogin_withInvalidChannel_shouldFail() throws Exception {
        var request = new WechatLoginRequest();
        request.setCode("code");
        request.setChannel("invalid");

        when(wechatService.exchangeCode("code", "invalid"))
                .thenThrow(new AuthException("AUTH_400", "Invalid WeChat channel: invalid"));

        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_400"));
    }

    // ── WeChat Bind ───────────────────────────────────────

    @Test
    void wechatBind_withoutAuth_shouldFail() throws Exception {
        var request = new WechatBindRequest();
        request.setCode("code");
        request.setChannel("app");

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── QR URL ────────────────────────────────────────────

    @Test
    void getQrUrl_success() throws Exception {
        when(wechatService.createQrTicket()).thenReturn("ticket-uuid");
        var qrResult = new WechatService.QrCodeResult();
        when(wechatService.getQrCodeUrl("ticket-uuid")).thenReturn(qrResult);

        mockMvc.perform(get("/api/auth/wechat/qr-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("ticket-uuid"));
    }

    // ── QR Status ─────────────────────────────────────────

    @Test
    void getQrStatus_pending() throws Exception {
        when(wechatService.getQrStatus("ticket-1")).thenReturn("pending");

        mockMvc.perform(get("/api/auth/wechat/qr-status/ticket-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void getQrStatus_expired() throws Exception {
        when(wechatService.getQrStatus("ticket-expired")).thenReturn("expired");

        mockMvc.perform(get("/api/auth/wechat/qr-status/ticket-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("expired"));
    }

    @Test
    void getQrStatus_completed() throws Exception {
        when(wechatService.getQrStatus("ticket-done")).thenReturn("completed");
        when(wechatService.getQrResultAccessToken("ticket-done")).thenReturn("final-token");
        when(wechatService.getQrResultRefreshToken("ticket-done")).thenReturn("final-refresh");
        when(wechatService.getQrResultAccessToken("ticket-done")).thenReturn("final-token");

        mockMvc.perform(get("/api/auth/wechat/qr-status/ticket-done"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    // ── QR Callback ───────────────────────────────────────

    @Test
    void qrCallback_success() throws Exception {
        var tokenResult = new WechatService.WechatTokenResult();
        var userInfo = new WechatService.WechatUserInfo();

        when(wechatService.exchangeCode("auth-code", "qr")).thenReturn(tokenResult);
        when(wechatService.fetchUserInfo(any(), any())).thenReturn(userInfo);
        when(authService.wechatLogin(any(), any(), any(), any(), eq("qr"), any(), any(), any()))
                .thenReturn(new AuthResponse("qr-token", "qr-refresh", 900000));

        mockMvc.perform(post("/api/auth/wechat/qr-callback")
                        .param("code", "auth-code")
                        .param("state", "ticket-123"))
                .andExpect(status().isOk());
    }
}
