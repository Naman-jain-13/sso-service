package com.yourapp.security; // change this to your app's package

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * SSO Auth Filter — Spring Boot apps (Cesium, CLM, Helix)
 *
 * Drop this file into your Spring Boot app's security package.
 *
 * HOW TO WIRE IT:
 *   In your SecurityConfig, add:
 *     http.addFilterBefore(new SsoAuthFilter(), UsernamePasswordAuthenticationFilter.class);
 *
 * HOW IT WORKS:
 *   Every request must carry:   Authorization: Bearer <jwt-token>
 *   This filter calls your SSO /validate endpoint to verify the token.
 *   If valid  → sets username in request attribute and continues.
 *   If invalid → returns 401 Unauthorized.
 */
@Component
public class SsoAuthFilter extends OncePerRequestFilter {

    private static final String SSO_VALIDATE_URL = "http://localhost:8080/validate"; // change in production
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token provided → reject
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing Authorization header\"}");
            return;
        }

        // Call SSO /validate
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> ssoResponse = restTemplate.exchange(
                    SSO_VALIDATE_URL, HttpMethod.GET, entity, Map.class);

            Map<?, ?> body = ssoResponse.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("valid"))) {
                // Token is valid — store username so your controllers can use it
                request.setAttribute("sso_username", body.get("username"));
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception ignored) {
            // SSO service unreachable or returned error
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
    }

    /**
     * Skip the filter for public endpoints (health checks, login redirects, etc.)
     * Add paths you want to exclude here.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/health") || path.equals("/public");
    }
}
