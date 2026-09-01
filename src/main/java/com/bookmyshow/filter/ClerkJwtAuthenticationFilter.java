package com.bookmyshow.filter;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
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
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

/**
 * Security Filter that intercepts incoming REST API requests, extracts the Bearer JWT token
 * issued by Clerk, validates token signature, expiration and structure, and populates Spring Security's SecurityContext.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ClerkJwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${cinex.admin.clerk-user-id:}")
    private String adminClerkUserId;

    @Value("${cinex.admin.email:admin@cinex.com}")
    private String adminEmail;

    @Value("${clerk.jwks-url:}")
    private String jwksUrl;

    @Value("${clerk.issuer:}")
    private String issuer;

    private JwkProvider provider;

    private JwkProvider getJwkProvider() {
        if (provider == null && jwksUrl != null && !jwksUrl.isEmpty()) {
            try {
                provider = new UrlJwkProvider(new URL(jwksUrl));
            } catch (Exception e) {
                log.error("Failed to initialize JWK Provider", e);
            }
        }
        return provider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") || path.startsWith("/h2-console") ||
            path.startsWith("/ws")) {
            return true;
        }
        
        if ("GET".equalsIgnoreCase(method) && 
           (path.startsWith("/api/movies") || path.startsWith("/api/theatres") ||
            path.startsWith("/api/shows") || path.startsWith("/api/tmdb") || 
            path.startsWith("/api/seats/screen"))) {
            return true;
        }
        
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth")) {
            return true;
        }
        
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header on protected endpoint: {}", request.getRequestURI());
            SecurityContextHolder.clearContext();
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
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Expired JWT token");
                return;
            }

            // Verify signature using JWKS
            JwkProvider jwkProvider = getJwkProvider();
            if (jwkProvider != null) {
                Jwk jwk = jwkProvider.get(jwt.getKeyId());
                Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                
                JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
                
                verifier.verify(token); // Throws JWTVerificationException if invalid
            } else if (!isTestEnv()) {
                log.error("JWKS URL is not configured. Rejecting request.");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication configuration error");
                return;
            } else {
                // In test environment, skip signature verification if jwks-url is explicitly left blank
                log.info("Test environment detected without JWKS URL, trusting decoded token");
            }

            String clerkUserId = jwt.getSubject();
            if (clerkUserId != null && !clerkUserId.isEmpty()) {
                request.setAttribute("authenticatedClerkUserId", clerkUserId);
                
                String emailClaim = jwt.getClaim("email").asString();
                // Determine admin based strictly on configured identities, NEVER from client headers
                boolean isAdmin = clerkUserId.equals(adminClerkUserId) || 
                                  (emailClaim != null && emailClaim.equalsIgnoreCase(adminEmail));
                
                List<SimpleGrantedAuthority> authorities = isAdmin ? 
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")) :
                        List.of(new SimpleGrantedAuthority("ROLE_USER"));
                
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(clerkUserId, null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated Clerk user [{}] with authorities {}", clerkUserId, authorities);
            }
        } catch (JwkException | JWTVerificationException e) {
            log.warn("Failed to verify JWT Bearer token signature or claims: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication", e);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    private boolean isTestEnv() {
        return "https://test.clerk.dev".equals(issuer);
    }
}
