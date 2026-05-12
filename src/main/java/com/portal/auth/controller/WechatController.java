package com.portal.auth.controller;

import com.portal.auth.dto.request.WechatBindRequest;
import com.portal.auth.dto.request.WechatLoginRequest;
import com.portal.auth.dto.response.AuthResponse;
import com.portal.auth.dto.response.WechatQrResponse;
import com.portal.auth.dto.response.WechatQrStatusResponse;
import com.portal.auth.exception.AuthException;
import com.portal.auth.service.AuthService;
import com.portal.auth.service.TokenService;
import com.portal.auth.service.WechatService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/wechat")
public class WechatController {

    private final WechatService wechatService;
    private final AuthService authService;
    private final TokenService tokenService;

    public WechatController(WechatService wechatService,
                            AuthService authService,
                            TokenService tokenService) {
        this.wechatService = wechatService;
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody WechatLoginRequest request) {
        var tokenResult = wechatService.exchangeCode(request.getCode(), request.getChannel());
        var userInfo = wechatService.fetchUserInfo(tokenResult.getAccessToken(), tokenResult.getOpenid());

        var response = authService.wechatLogin(
                userInfo.getOpenid(),
                userInfo.getUnionid(),
                userInfo.getNickname(),
                userInfo.getHeadimgurl(),
                request.getChannel(),
                request.getDeviceId(),
                request.getDeviceType(),
                request.getDeviceName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/bind")
    public ResponseEntity<Void> bind(@Valid @RequestBody WechatBindRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthException("AUTH_008", "Authentication required");
        }
        String userId;
        if (auth.getDetails() instanceof Claims claims) {
            userId = claims.getSubject();
        } else {
            userId = auth.getName();
        }

        var tokenResult = wechatService.exchangeCode(request.getCode(), request.getChannel());
        authService.bindWechat(userId, tokenResult.getOpenid(), tokenResult.getUnionid(), request.getChannel());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/qr-url")
    public ResponseEntity<WechatQrResponse> getQrUrl() {
        String ticket = wechatService.createQrTicket();
        var qrResult = wechatService.getQrCodeUrl(ticket);
        return ResponseEntity.ok(new WechatQrResponse(qrResult.getUrl(), qrResult.getTicket()));
    }

    @GetMapping("/qr-status/{ticket}")
    public ResponseEntity<WechatQrStatusResponse> getQrStatus(@PathVariable String ticket) {
        String status = wechatService.getQrStatus(ticket);

        if ("completed".equals(status)) {
            String accessToken = wechatService.getQrResultAccessToken(ticket);
            String refreshToken = wechatService.getQrResultRefreshToken(ticket);
            wechatService.deleteQrTicket(ticket);
            var resp = new WechatQrStatusResponse("completed");
            resp.setAccessToken(accessToken);
            resp.setRefreshToken(refreshToken);
            resp.setExpiresIn(tokenService.getAccessTokenExpiration());
            return ResponseEntity.ok(resp);
        }

        return ResponseEntity.ok(new WechatQrStatusResponse(status));
    }

    @PostMapping("/qr-callback")
    public ResponseEntity<Void> qrCallback(
            @RequestParam String code,
            @RequestParam String state) {
        var tokenResult = wechatService.exchangeCode(code, "qr");
        var userInfo = wechatService.fetchUserInfo(tokenResult.getAccessToken(), tokenResult.getOpenid());

        var authResponse = authService.wechatLogin(
                userInfo.getOpenid(),
                userInfo.getUnionid(),
                userInfo.getNickname(),
                userInfo.getHeadimgurl(),
                "qr",
                null, null, null);

        wechatService.completeQrTicket(state,
                authResponse.getAccessToken(),
                authResponse.getRefreshToken());

        return ResponseEntity.ok().build();
    }
}
