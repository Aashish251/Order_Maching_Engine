// src/main/java/com/project/ome/shared/observability/RequestLoggingFilter.java
package com.project.ome.shared.observability;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)   // Run before everything else
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Generate or propagate requestId
        String requestId = httpReq.getHeader("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        long startTime = System.currentTimeMillis();

        // Add to MDC — appears in EVERY log line during this request
        MDC.put("requestId", requestId);
        MDC.put("method",    httpReq.getMethod());
        MDC.put("path",      httpReq.getRequestURI());

        // Add requestId to response header so clients can correlate
        httpResp.setHeader("X-Request-ID", requestId);

        try {
            log.debug("→ {} {}", httpReq.getMethod(), httpReq.getRequestURI());
            chain.doFilter(request, response);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("← {} {} {}ms status={}",
                    httpReq.getMethod(),
                    httpReq.getRequestURI(),
                    duration,
                    httpResp.getStatus());
        } finally {
            // ALWAYS clear MDC — thread pool reuses threads
            MDC.clear();
        }
    }
}