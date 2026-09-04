package com.bookmyshow.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Map;

/**
 * Resolves application roles from verified Clerk JWT claims.
 * Admin status is never taken from request headers or body.
 */
public final class ClerkJwtRoleResolver {

    private ClerkJwtRoleResolver() {
    }

    public static boolean isAdmin(DecodedJWT jwt) {
        if (jwt == null) {
            return false;
        }

        String roleClaim = asNonBlankString(jwt.getClaim("role"));
        if (isAdminRole(roleClaim)) {
            return true;
        }

        if (hasAdminRole(jwt.getClaim("public_metadata"))) {
            return true;
        }

        if (hasAdminRole(jwt.getClaim("metadata"))) {
            return true;
        }

        String orgRole = asNonBlankString(jwt.getClaim("org_role"));
        return isAdminRole(orgRole);
    }

    private static boolean hasAdminRole(Claim claim) {
        if (claim == null || claim.isNull()) {
            return false;
        }
        Map<String, Object> metadata = claim.asMap();
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        Object role = metadata.get("role");
        return role != null && isAdminRole(String.valueOf(role));
    }

    private static String asNonBlankString(Claim claim) {
        if (claim == null || claim.isNull()) {
            return null;
        }
        String value = claim.asString();
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean isAdminRole(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }
}
