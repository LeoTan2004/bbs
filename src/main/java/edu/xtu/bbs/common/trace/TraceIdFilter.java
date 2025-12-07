package edu.xtu.bbs.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * TraceId Filter
 * Sets TraceId at the beginning of each HTTP request and cleans up at the end
 * Supports getting existing TraceId from request headers or generating new ones
 */
@Component
@Order(1) // Ensure this filter executes before other filters
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final Set<String> SILENT = Set.of("/actuator/**", "/favicon.ico");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final boolean isSilent = SILENT.stream()
                .anyMatch(pattern -> MATCHER.match(pattern, request.getRequestURI()));
        if (isSilent) {
            // For silent endpoints, just continue the filter chain without logging
            filterChain.doFilter(request, response);
            return;
        }

        String traceId = null;

        try {
            // 1. Try to get TraceId from request headers
            traceId = request.getHeader(TraceIdUtil.TRACE_ID_HEADER);

            // 2. If no TraceId in request headers, generate a new one
            if (!StringUtils.hasText(traceId)) {
                traceId = TraceIdUtil.generateTraceId();
            }

            // 3. Set to MDC
            TraceIdUtil.setTraceId(traceId);

            // 4. Add TraceId to response headers for frontend access
            response.setHeader(TraceIdUtil.TRACE_ID_HEADER, traceId);

            // 5. Log request start
            log.info("Request started: {} {}, TraceId: {}",
                    request.getMethod(), request.getRequestURI(), traceId);

            // 6. Continue executing subsequent filters and handlers
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Log exception but don't block request processing
            log.error("Error in TraceIdFilter: {}", e.getMessage(), e);
            throw e;
        } finally {
            // 7. Clean up MDC after request ends
            try {
                log.info("Request completed: {} {}, TraceId: {}, Status: {}",
                        request.getMethod(), request.getRequestURI(), traceId, response.getStatus());
            } finally {
                TraceIdUtil.clearAll();
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // This filter should process all requests
        return false;
    }
}