package com.bookmyshow.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClerkJwtRoleResolverTest {

    @Test
    @DisplayName("Resolves ADMIN from role claim")
    void resolvesAdminFromRoleClaim() {
        DecodedJWT jwt = JWT.decode(tokenWithClaims(Map.of("role", "ADMIN")));
        assertTrue(ClerkJwtRoleResolver.isAdmin(jwt));
    }

    @Test
    @DisplayName("Resolves ADMIN from public_metadata.role")
    void resolvesAdminFromPublicMetadata() {
        DecodedJWT jwt = JWT.decode(tokenWithClaims(Map.of(
                "public_metadata", Map.of("role", "ADMIN")
        )));
        assertTrue(ClerkJwtRoleResolver.isAdmin(jwt));
    }

    @Test
    @DisplayName("USER role is not treated as admin")
    void userRoleIsNotAdmin() {
        DecodedJWT jwt = JWT.decode(tokenWithClaims(Map.of("role", "USER")));
        assertFalse(ClerkJwtRoleResolver.isAdmin(jwt));
    }

    private String tokenWithClaims(Map<String, Object> claims) {
        var builder = JWT.create()
                .withSubject("user_test_123")
                .withIssuer("https://test.clerk.dev")
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000));

        claims.forEach((key, value) -> {
            if (value instanceof String stringValue) {
                builder.withClaim(key, stringValue);
            } else if (value instanceof Map<?, ?> mapValue) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) mapValue;
                builder.withClaim(key, metadata);
            }
        });

        return builder.sign(Algorithm.HMAC256("test-secret"));
    }
}
