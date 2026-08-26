package com.bookmyshow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Production Security Hardening Filter for CineX.
 * Injects OWASP recommended HTTP response headers (HSTS, CSP, X-Frame-Options, X-Content-Type-Options).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Prevent MIME-sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        // Prevent Clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        // Cross-site scripting protection
        response.setHeader("X-XSS-Protection", "1; mode=block");
        // Strict Transport Security (HSTS) - 1 year
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        // Content Security Policy
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:;");
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        filterChain.doFilter(request, response);
    }
}
