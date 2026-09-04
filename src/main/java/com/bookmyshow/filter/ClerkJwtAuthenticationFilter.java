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
import com.bookmyshow.config.SecurityErrorResponseHandler;
import com.bookmyshow.security.ClerkJwtRoleResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequiredArgsConstructor
public class ClerkJwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityErrorResponseHandler securityErrorResponseHandler;

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
        
        // Mirrors the permitAll GET list in SecurityConfig. /api/cities was absent here while
        // /api/cities/search was present, so a stale or malformed Clerk token made the public city
        // catalog answer 401 even though SecurityConfig permits it anonymously. The city picker
        // could then search cities but never list them.
        if ("GET".equalsIgnoreCase(method) &&
           (path.startsWith("/api/movies") || path.startsWith("/api/theatres") ||
            path.startsWith("/api/shows") || path.startsWith("/api/tmdb") ||
            path.startsWith("/api/seats/screen") ||
            path.equals("/api/cities") || path.equals("/api/cities/search"))) {
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
            Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth == null || !existingAuth.isAuthenticated()
                    || "anonymousUser".equals(existingAuth.getPrincipal())) {
                log.warn("Missing or invalid Authorization header on protected endpoint: {}", request.getRequestURI());
                SecurityContextHolder.clearContext();
            }
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authentication = authenticate(authHeader.substring(7).trim());
            request.setAttribute("authenticatedClerkUserId", authentication.getName());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Attribute subsequent log lines to the verified Clerk subject. MdcLoggingFilter owns the
            // MDC lifecycle and clears it after the request, so nothing leaks between requests.
            MDC.put("userId", authentication.getName());
            log.debug("Authenticated Clerk user [{}] with authorities {}", authentication.getName(), authentication.getAuthorities());
        } catch (IllegalStateException e) {
            log.error("Clerk JWT verification is unavailable: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            securityErrorResponseHandler.writeError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication configuration error");
            return;
        } catch (BadCredentialsException e) {
            log.warn("Rejected Clerk JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            securityErrorResponseHandler.writeError(response, HttpStatus.UNAUTHORIZED, e.getMessage());
            return;
        } catch (JWTVerificationException e) {
            log.warn("Failed to verify JWT Bearer token signature or claims: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            securityErrorResponseHandler.writeError(response, HttpStatus.UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication", e);
            SecurityContextHolder.clearContext();
            securityErrorResponseHandler.writeError(response, HttpStatus.UNAUTHORIZED, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Validates a Clerk JWT for HTTP and STOMP CONNECT authentication. */
    public Authentication authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Missing JWT token");
        }
        DecodedJWT jwt = JWT.decode(token);
        if (jwt.getExpiresAt() == null || jwt.getExpiresAt().before(new Date())) {
            throw new BadCredentialsException("Expired JWT token");
        }

        JwkProvider jwkProvider = getJwkProvider();
        if (jwkProvider != null) {
            try {
                Jwk jwk = jwkProvider.get(jwt.getKeyId());
                Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                JWT.require(algorithm).withIssuer(issuer).build().verify(token);
            } catch (JwkException e) {
                throw new BadCredentialsException("Invalid JWT token", e);
            }
        } else if (!isTestEnv()) {
            throw new IllegalStateException("CLERK_JWKS_URL is not configured");
        }

        String clerkUserId = jwt.getSubject();
        if (clerkUserId == null || clerkUserId.isBlank()) {
            throw new BadCredentialsException("JWT subject is missing");
        }
        List<SimpleGrantedAuthority> authorities = ClerkJwtRoleResolver.isAdmin(jwt)
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new UsernamePasswordAuthenticationToken(clerkUserId, null, authorities);
    }
    
    private boolean isTestEnv() {
        return "https://test.clerk.dev".equals(issuer);
    }
}
