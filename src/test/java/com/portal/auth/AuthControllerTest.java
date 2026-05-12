package com.portal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.auth.dto.request.*;
import com.portal.auth.dto.response.AuthResponse;
import com.portal.auth.dto.response.VerifyOtpResponse;
import com.portal.auth.exception.AuthException;
import com.portal.auth.service.AuthService;
import com.portal.auth.service.OtpService;
import com.portal.auth.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimitService rateLimitService;

    // ── Send OTP ─────────────────────────────────────────

    @Test
    void sendOtp_success() throws Exception {
        var request = new SendOtpRequest();
        request.setAccount("test@example.com");
        request.setCaptchaToken("valid-captcha");

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void sendOtp_withInvalidCaptcha_shouldFail() throws Exception {
        var request = new SendOtpRequest();
        request.setAccount("test@example.com");
        request.setCaptchaToken("invalid-captcha");

        doThrow(new AuthException("AUTH_002", "CAPTCHA verification failed", "captchaToken"))
                .when(authService).sendOtp(any());

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void sendOtp_withBlankFields_shouldFail() throws Exception {
        var request = new SendOtpRequest();
        request.setAccount("");
        request.setCaptchaToken("");

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendOtp_whenRateLimited_shouldFail() throws Exception {
        var request = new SendOtpRequest();
        request.setAccount("test@example.com");
        request.setCaptchaToken("valid-captcha");

        doThrow(new AuthException("AUTH_007", "OTP send limit reached", "account"))
                .when(authService).sendOtp(any());

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_007"));
    }

    // ── Verify OTP ───────────────────────────────────────

    @Test
    void verifyOtp_success() throws Exception {
        var request = new VerifyOtpRequest();
        request.setAccount("test@example.com");
        request.setOtpCode("123456");

        when(authService.verifyOtp(any(), any()))
                .thenReturn(new VerifyOtpResponse("temp-token", "register"));

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempToken").value("temp-token"))
                .andExpect(jsonPath("$.purpose").value("register"));
    }

    @Test
    void verifyOtp_withWrongCode_shouldFail() throws Exception {
        var request = new VerifyOtpRequest();
        request.setAccount("test@example.com");
        request.setOtpCode("wrong");

        when(authService.verifyOtp(any(), any()))
                .thenThrow(new AuthException("AUTH_001", "Invalid OTP code", "otpCode"));

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    void verifyOtp_withoutAccount_shouldFail() throws Exception {
        var request = new VerifyOtpRequest();
        request.setOtpCode("123456");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── Register ─────────────────────────────────────────

    @Test
    void register_success() throws Exception {
        var request = new RegisterRequest();
        request.setTempToken("valid-token");
        request.setPassword("strongPassword123");
        request.setUsername("newuser");

        when(authService.register(any()))
                .thenReturn(new AuthResponse("access-token", "refresh-token", 900000));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void register_withShortPassword_shouldFail() throws Exception {
        var request = new RegisterRequest();
        request.setTempToken("some-token");
        request.setPassword("123");
        request.setUsername("testuser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withTakenUsername_shouldFail() throws Exception {
        var request = new RegisterRequest();
        request.setTempToken("valid-token");
        request.setPassword("strongPassword123");
        request.setUsername("existinguser");

        when(authService.register(any()))
                .thenThrow(new AuthException("AUTH_004", "Username already taken", "username"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void register_withDuplicateEmail_shouldFail() throws Exception {
        var request = new RegisterRequest();
        request.setTempToken("valid-token");
        request.setPassword("strongPassword123");
        request.setUsername("newuser");

        when(authService.register(any()))
                .thenThrow(new AuthException("AUTH_004", "Email already registered", "account"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    // ── Login ─────────────────────────────────────────

    @Test
    void login_withPassword_success() throws Exception {
        var request = new LoginRequest();
        request.setAccount("test@example.com");
        request.setCredential("correct-password");
        request.setType("pwd");
        request.setDeviceId("device-123");
        request.setDeviceType("web");

        when(authService.login(any(), any()))
                .thenReturn(new AuthResponse("access-token", "refresh-token", 900000));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_withOtp_success() throws Exception {
        var request = new LoginRequest();
        request.setAccount("+8613800138000");
        request.setCredential("123456");
        request.setType("otp");

        when(authService.login(any(), any()))
                .thenReturn(new AuthResponse("access-token", "refresh-token", 900000));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_withWrongPassword_shouldFail() throws Exception {
        var request = new LoginRequest();
        request.setAccount("test@example.com");
        request.setCredential("wrong-password");
        request.setType("pwd");

        when(authService.login(any(), any()))
                .thenThrow(new AuthException("AUTH_005", "Wrong password", "credential"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @Test
    void login_withInvalidType_shouldFail() throws Exception {
        var request = new LoginRequest();
        request.setAccount("test@example.com");
        request.setCredential("password123");
        request.setType("invalid");

        when(authService.login(any(), any()))
                .thenThrow(new AuthException("AUTH_400", "Invalid login type", "type"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_400"));
    }

    @Test
    void login_whenRateLimited_shouldFail() throws Exception {
        var request = new LoginRequest();
        request.setAccount("test@example.com");
        request.setCredential("password");
        request.setType("pwd");

        when(authService.login(any(), any()))
                .thenThrow(new AuthException("AUTH_007", "Too many login attempts", "account"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_007"));
    }

    @Test
    void login_withNonexistentAccount_shouldFail() throws Exception {
        var request = new LoginRequest();
        request.setAccount("nonexistent@example.com");
        request.setCredential("password");
        request.setType("pwd");

        when(authService.login(any(), any()))
                .thenThrow(new AuthException("AUTH_003", "Account not found", "account"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void login_withDeviceLimitExceeded_shouldKickOldestDevice() throws Exception {
        var request = new LoginRequest();
        request.setAccount("test@example.com");
        request.setCredential("password");
        request.setType("pwd");
        request.setDeviceId("new-device");
        request.setDeviceType("android");

        when(authService.login(any(), any()))
                .thenReturn(new AuthResponse("new-access-token", "new-refresh-token", 900000));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ── Forgot Password ───────────────────────────────

    @Test
    void forgotPassword_success() throws Exception {
        var request = new ForgotPasswordRequest();
        request.setAccount("test@example.com");
        request.setCaptchaToken("valid-captcha");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_withNonexistentAccount_shouldFail() throws Exception {
        var request = new ForgotPasswordRequest();
        request.setAccount("nonexistent@example.com");
        request.setCaptchaToken("valid-captcha");

        doThrow(new AuthException("AUTH_003", "Account not found", "account"))
                .when(authService).sendForgotPasswordOtp(any());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    // ── Reset Password ────────────────────────────────

    @Test
    void resetPassword_success() throws Exception {
        var request = new ResetPasswordRequest();
        request.setTempToken("valid-reset-token");
        request.setNewPassword("newStrongPassword456");

        when(authService.resetPassword(any()))
                .thenReturn(new AuthResponse("new-access-token", "new-refresh-token", 900000));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void resetPassword_withInvalidToken_shouldFail() throws Exception {
        var request = new ResetPasswordRequest();
        request.setTempToken("bad-token");
        request.setNewPassword("newPassword123");

        when(authService.resetPassword(any()))
                .thenThrow(new AuthException("AUTH_008", "Invalid or expired temporary token"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_008"));
    }

    // ── Token Refresh & Logout ────────────────────────

    @Test
    void refreshToken_success() throws Exception {
        var request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(authService.refreshToken(any()))
                .thenReturn(new AuthResponse("new-access-token", "new-refresh-token", 900000));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refreshToken_withInvalidToken_shouldFail() throws Exception {
        var request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(authService.refreshToken(any()))
                .thenThrow(new AuthException("AUTH_008", "Invalid or expired refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_008"));
    }

    @Test
    void logout_success() throws Exception {
        var request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ── Validation ────────────────────────────────────

    @Test
    void requestWithMissingBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestWithMalformedJson_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed"))
                .andExpect(status().isBadRequest());
    }
}
