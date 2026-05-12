package com.portal.auth.middleware;

import com.portal.auth.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String endpoint = path.replaceAll("/api/", "").replace("/", "-");

        if (!rateLimitService.tryConsumeQps(endpoint)) {
            response.setStatus(429);
            response.setContentType("application/json");
            try {
                response.getWriter().write(
                        "{\"code\":\"AUTH_007\",\"message\":\"Rate limit exceeded. Try again later.\"}");
            } catch (Exception e) {
                // ignore
            }
            return false;
        }

        return true;
    }
}
