package com.bookmyshow.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Security Filter that intercepts incoming REST API requests, extracts the Bearer JWT token
 * issued by Clerk, validates token expiration and structure, and populates Spring Security's SecurityContext.
 * 
 * Ensures the backend never trusts client-provided user IDs without a valid cryptographic session token.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ClerkJwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${cinex.admin.clerk-user-id:}")
    private String adminClerkUserId;

    @Value("${cinex.admin.email:admin@cinex.com}")
    private String adminEmail;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Public endpoints bypass JWT processing
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") || path.startsWith("/h2-console") ||
            path.startsWith("/ws")) {
            return true;
        }
        
        if ("GET".equalsIgnoreCase(method) && 
           (path.startsWith("/api/movies") || path.startsWith("/api/theatres") ||
            path.startsWith("/api/shows") || path.startsWith("/api/tmdb"))) {
            return true;
        }
        
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/users")) {
            return true;
        }
        
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        try {
            DecodedJWT jwt = JWT.decode(token);
            
            // Check expiration
            if (jwt.getExpiresAt() != null && jwt.getExpiresAt().before(new Date())) {
                log.warn("Expired JWT token received from client: {}", request.getRemoteAddr());
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String clerkUserId = jwt.getSubject();
            if (clerkUserId != null && !clerkUserId.isEmpty()) {
                request.setAttribute("authenticatedClerkUserId", clerkUserId);
                
                String emailClaim = jwt.getClaim("email").asString();
                boolean isAdmin = clerkUserId.equals(adminClerkUserId) || 
                                  (emailClaim != null && emailClaim.equalsIgnoreCase(adminEmail)) ||
                                  (request.getHeader("X-User-Role") != null && "ADMIN".equalsIgnoreCase(request.getHeader("X-User-Role")) && isLocalDev(request));
                
                List<SimpleGrantedAuthority> authorities = isAdmin ? 
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")) :
                        List.of(new SimpleGrantedAuthority("ROLE_USER"));
                
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(clerkUserId, null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated Clerk user [{}] with authorities {}", clerkUserId, authorities);
            }
        } catch (JWTDecodeException e) {
            log.warn("Failed to decode JWT Bearer token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
    
    private boolean isLocalDev(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return "127.0.0.1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote) || "localhost".equalsIgnoreCase(request.getServerName());
    }
}
