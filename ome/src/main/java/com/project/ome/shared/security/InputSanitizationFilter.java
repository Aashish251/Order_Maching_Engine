// src/main/java/com/project/ome/shared/security/InputSanitizationFilter.java
package com.project.ome.shared.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InputSanitizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Block requests with oversized headers (prevent header injection)
        String contentType = request.getContentType();
        if (contentType != null && contentType.length() > 256) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid request headers");
            return;
        }

        // Add security headers to every response
        response.setHeader("X-Content-Type-Options",  "nosniff");
        response.setHeader("X-Frame-Options",          "DENY");
        response.setHeader("X-XSS-Protection",         "1; mode=block");
        response.setHeader("Referrer-Policy",           "no-referrer");

        chain.doFilter(request, response);
    }
}