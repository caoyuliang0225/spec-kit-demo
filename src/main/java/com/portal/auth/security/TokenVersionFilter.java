package com.portal.auth.security;

import com.portal.auth.exception.AuthException;
import com.portal.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class TokenVersionFilter implements Filter {

    private final AuthService authService;

    public TokenVersionFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (auth != null && auth.getDetails() instanceof Claims claims) {
            String userId = claims.getSubject();
            int tokenVersion = claims.get("tokenVersion", Integer.class);
            int currentVersion = authService.getTokenVersion(userId);

            if (tokenVersion < currentVersion) {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"code\":\"AUTH_009\",\"message\":\"Session expired. Please login again.\"}");
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
