package com.bookmyshow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.trim().isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            MDC.put(MDC_TRACE_ID_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);

            // Never derive userId from the Authorization header: that wrote a slice of the raw
            // Clerk JWT into every log line. ClerkJwtAuthenticationFilter overwrites this with the
            // verified subject once the token is validated, so unverified requests stay unattributed.
            String authHeader = request.getHeader("Authorization");
            boolean bearerPresented = authHeader != null && authHeader.startsWith("Bearer ");
            MDC.put(MDC_USER_ID_KEY, bearerPresented ? "UNVERIFIED_BEARER" : "ANONYMOUS");

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
