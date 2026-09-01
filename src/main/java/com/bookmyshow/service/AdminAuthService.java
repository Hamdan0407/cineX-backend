package com.bookmyshow.service;

import com.bookmyshow.entity.User;
import com.bookmyshow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for validating admin privileges and retrieving authenticated user identity.
 * Integrates directly with Spring Security's SecurityContext populated by ClerkJwtAuthenticationFilter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;

    /**
     * Validates whether the authenticated caller has explicit ADMIN privileges.
     * Throws SecurityException if not authorized.
     */
    public void validateAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            log.warn("Unauthorized attempt to access Admin API without ROLE_ADMIN authority");
            throw new SecurityException("Access Denied: Admin privileges required to perform this operation.");
        }
    }

    /**
     * Backward-compatible service API for older callers that provide an explicit role/email pair.
     * HTTP controllers must use validateAdmin(), which validates the authenticated security context.
     */
    @Deprecated
    public void validateAdmin(String role, String email) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        if (email != null && userRepository.findByEmail(email)
                .map(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
                .orElse(false)) {
            return;
        }

        throw new SecurityException("Access Denied: Admin privileges required to perform this operation.");
    }

    /**
     * Validates whether the authenticated user is an ADMIN or matches the target Clerk User ID.
     */
    public void validateOwnershipOrAdmin(String targetClerkUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
                return;
            }
            String currentUserId = auth.getPrincipal().toString();
            if (!currentUserId.equals(targetClerkUserId)) {
                log.warn("Ownership check failed: user {} attempted to access resource owned by {}", currentUserId, targetClerkUserId);
                throw new SecurityException("Access Denied: You do not have permission for this resource.");
            }
        } else {
            throw new SecurityException("Not authenticated");
        }
    }

    /**
     * Retrieves the authenticated Clerk User ID from Spring SecurityContext.
     */
    public String getAuthenticatedClerkUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            throw new SecurityException("Not authenticated");
        }
        return auth.getPrincipal().toString();
    }
}
