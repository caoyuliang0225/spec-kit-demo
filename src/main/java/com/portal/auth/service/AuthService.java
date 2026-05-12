package com.portal.auth.service;

import com.portal.auth.dto.request.*;
import com.portal.auth.dto.response.AuthResponse;
import com.portal.auth.dto.response.VerifyOtpResponse;
import com.portal.auth.exception.AuthException;
import com.portal.auth.model.Role;
import com.portal.auth.model.User;
import com.portal.auth.model.UserRole;
import com.portal.auth.repository.RoleRepository;
import com.portal.auth.repository.UserRepository;
import com.portal.auth.repository.UserRoleRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final DeviceService deviceService;
    private final RateLimitService rateLimitService;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;
    private final String usernamePrefix;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       OtpService otpService,
                       TokenService tokenService,
                       DeviceService deviceService,
                       RateLimitService rateLimitService,
                       CaptchaService captchaService,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.username.prefix}") String usernamePrefix) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.deviceService = deviceService;
        this.rateLimitService = rateLimitService;
        this.captchaService = captchaService;
        this.passwordEncoder = passwordEncoder;
        this.usernamePrefix = usernamePrefix;
    }

    public void sendOtp(SendOtpRequest request) {
        otpService.sendOtp(request.getAccount(), request.getCaptchaToken());
    }

    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request, String purpose) {
        otpService.verifyOtp(request.getAccount(), request.getOtpCode(), purpose);
        return new VerifyOtpResponse(generateTempToken(request.getAccount(), purpose), purpose);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Claims claims = validateTempToken(request.getTempToken());
        String account = claims.getSubject();
        String purpose = claims.get("purpose", String.class);

        if (!"register".equals(purpose)) {
            throw new AuthException("AUTH_400", "Invalid token purpose");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("AUTH_004", "Username already taken", "username");
        }

        User user = new User();
        if (account.contains("@")) {
            if (userRepository.existsByEmail(account)) {
                throw new AuthException("AUTH_004", "Email already registered", "account");
            }
            user.setEmail(account);
        } else {
            if (userRepository.existsByPhone(account)) {
                throw new AuthException("AUTH_004", "Phone already registered", "account");
            }
            user.setPhone(account);
        }

        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setTokenVersion(0);
        user = userRepository.save(user);

        Role customerRole = roleRepository.findByName("CUSTOMER_USER")
                .orElseThrow(() -> new RuntimeException("Default role CUSTOMER_USER not found"));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(customerRole);
        userRoleRepository.save(userRole);

        var accessToken = tokenService.generateAccessToken(user.getId(), user.getTokenVersion(), null);
        var refreshToken = tokenService.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken, tokenService.getAccessTokenExpiration());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ip) {
        if (!rateLimitService.checkLoginLimit(request.getAccount(), ip)) {
            throw new AuthException("AUTH_007", "Too many login attempts. Try again later.", "account");
        }

        User user = findUserByAccount(request.getAccount());

        switch (request.getType()) {
            case "pwd" -> {
                if (user.getPasswordHash() == null) {
                    throw new AuthException("AUTH_005",
                            "This account has no password set. Use WeChat login.", "credential");
                }
                if (!passwordEncoder.matches(request.getCredential(), user.getPasswordHash())) {
                    throw new AuthException("AUTH_005", "Wrong password", "credential");
                }
            }
            case "otp" -> otpService.verifyOtp(request.getAccount(), request.getCredential(), "login");
            default -> throw new AuthException("AUTH_400", "Invalid login type", "type");
        }

        if (request.getDeviceId() != null) {
            deviceService.checkAndEnforceDeviceLimit(user.getId());
        }

        rateLimitService.resetLoginLimit(request.getAccount(), ip);

        var deviceId = request.getDeviceId();
        if (deviceId != null) {
            deviceService.registerDevice(user.getId(), deviceId, request.getDeviceType(), request.getDeviceName());
        }

        var accessToken = tokenService.generateAccessToken(user.getId(), user.getTokenVersion(), deviceId);
        var refreshToken = tokenService.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken, tokenService.getAccessTokenExpiration());
    }

    public void sendForgotPasswordOtp(ForgotPasswordRequest request) {
        findUserByAccount(request.getAccount());
        otpService.sendOtp(request.getAccount(), request.getCaptchaToken());
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        Claims claims = validateTempToken(request.getTempToken());
        String account = claims.getSubject();
        String purpose = claims.get("purpose", String.class);

        if (!"reset_password".equals(purpose)) {
            throw new AuthException("AUTH_400", "Invalid token purpose");
        }

        User user = findUserByAccount(account);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user = userRepository.save(user);

        var accessToken = tokenService.generateAccessToken(user.getId(), user.getTokenVersion(), null);
        var refreshToken = tokenService.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken, tokenService.getAccessTokenExpiration());
    }

    public AuthResponse refreshToken(String refreshToken) {
        String userId = tokenService.refreshAccessToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("AUTH_003", "User not found"));

        var accessToken = tokenService.generateAccessToken(user.getId(), user.getTokenVersion(), null);
        var newRefreshToken = tokenService.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, newRefreshToken, tokenService.getAccessTokenExpiration());
    }

    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    public int getTokenVersion(String userId) {
        return userRepository.findById(userId)
                .map(User::getTokenVersion)
                .orElseThrow(() -> new AuthException("AUTH_003", "User not found"));
    }

    @Transactional
    public AuthResponse wechatLogin(String openid, String unionid, String nickname,
                                    String headimgurl, String channel,
                                    String deviceId, String deviceType, String deviceName) {
        User user = null;

        if (unionid != null) {
            user = userRepository.findByWechatUnionid(unionid).orElse(null);
        }
        if (user == null) {
            String openidField = "qr".equals(channel) ? "wechatOpenidQr" : "wechatOpenid";
            user = "qr".equals(channel)
                    ? userRepository.findByWechatOpenidQr(openid).orElse(null)
                    : userRepository.findByWechatOpenid(openid).orElse(null);
        }

        boolean isNew = false;
        if (user == null) {
            user = new User();
            String baseUsername = sanitizeUsername(nickname);
            String username = baseUsername;
            int suffix = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + suffix;
                suffix++;
            }
            user.setUsername(username);
            user.setPasswordHash(null);
            user.setTokenVersion(0);
            user = userRepository.save(user);

            Role customerRole = roleRepository.findByName("CUSTOMER_USER")
                    .orElseThrow(() -> new RuntimeException("Default role CUSTOMER_USER not found"));
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(customerRole);
            userRoleRepository.save(userRole);

            if ("qr".equals(channel)) {
                user.setWechatOpenidQr(openid);
            } else {
                user.setWechatOpenid(openid);
            }
            user.setWechatUnionid(unionid);
            user.setWechatNickname(nickname);
            user.setWechatAvatar(headimgurl);
            user = userRepository.save(user);
            isNew = true;
        }

        if (deviceId != null) {
            deviceService.checkAndEnforceDeviceLimit(user.getId());
        }
        if (deviceId != null) {
            deviceService.registerDevice(user.getId(), deviceId, deviceType, deviceName);
        }

        var accessToken = tokenService.generateAccessToken(user.getId(), user.getTokenVersion(), deviceId);
        var refreshToken = tokenService.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken, tokenService.getAccessTokenExpiration());
    }

    @Transactional
    public void bindWechat(String userId, String openid, String unionid, String channel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("AUTH_003", "User not found"));

        if ("qr".equals(channel)) {
            if (userRepository.existsByWechatOpenidQr(openid)) {
                throw new AuthException("AUTH_021", "WeChat account already bound to another user");
            }
            user.setWechatOpenidQr(openid);
        } else {
            if (userRepository.existsByWechatOpenid(openid)) {
                throw new AuthException("AUTH_021", "WeChat account already bound to another user");
            }
            user.setWechatOpenid(openid);
        }
        user.setWechatUnionid(unionid);
        userRepository.save(user);
    }

    private User findUserByAccount(String account) {
        if (account.contains("@")) {
            return userRepository.findByEmail(account)
                    .orElseThrow(() -> new AuthException("AUTH_003", "Account not found", "account"));
        }
        return userRepository.findByPhone(account)
                .orElseThrow(() -> new AuthException("AUTH_003", "Account not found", "account"));
    }

    private String generateTempToken(String account, String purpose) {
        long nonce = ThreadLocalRandom.current().nextLong();
        return tokenService.generateAccessToken(account, 0, purpose) + ":" + nonce;
    }

    private Claims validateTempToken(String tempToken) {
        String jwtPart = tempToken.contains(":") ? tempToken.substring(0, tempToken.lastIndexOf(':')) : tempToken;
        try {
            return tokenService.parseAccessToken(jwtPart);
        } catch (Exception e) {
            throw new AuthException("AUTH_008", "Invalid or expired temporary token");
        }
    }

    private String sanitizeUsername(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return usernamePrefix + ThreadLocalRandom.current().nextInt(10000, 99999);
        }
        String clean = nickname.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff_]", "");
        if (clean.isBlank()) {
            return usernamePrefix + ThreadLocalRandom.current().nextInt(10000, 99999);
        }
        if (clean.length() > 20) {
            clean = clean.substring(0, 20);
        }
        return clean;
    }
}
