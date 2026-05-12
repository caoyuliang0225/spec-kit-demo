package com.portal.auth;

import com.portal.auth.model.Role;
import com.portal.auth.model.User;
import com.portal.auth.model.UserRole;
import com.portal.auth.repository.RoleRepository;
import com.portal.auth.repository.UserRepository;
import com.portal.auth.repository.UserRoleRepository;
import com.portal.auth.service.*;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private TokenService tokenService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<UserRole> userRoleCaptor;

    @Test
    void register_createsUserRoleWithCustomerUser() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("test@example.com");
        when(claims.get("purpose", String.class)).thenReturn("register");

        Role customerRole = new Role();
        customerRole.setName("CUSTOMER_USER");

        when(roleRepository.findByName("CUSTOMER_USER")).thenReturn(Optional.of(customerRole));
        when(tokenService.parseAccessToken(any())).thenReturn(claims);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.generateAccessToken(any(), anyInt(), any())).thenReturn("access-token");
        when(tokenService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(tokenService.getAccessTokenExpiration()).thenReturn(900000l);

        var request = new com.portal.auth.dto.request.RegisterRequest();
        request.setTempToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicHVycG9zZSI6InJlZ2lzdGVyIn0.signature");
        request.setPassword("strongPassword123");
        request.setUsername("testuser");

        authService.register(request);

        verify(userRoleRepository).save(userRoleCaptor.capture());
        UserRole savedUserRole = userRoleCaptor.getValue();
        assertNotNull(savedUserRole.getUser());
        assertEquals("CUSTOMER_USER", savedUserRole.getRole().getName());
    }
}
