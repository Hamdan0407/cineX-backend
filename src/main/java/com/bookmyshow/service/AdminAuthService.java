package com.bookmyshow.service;

import com.bookmyshow.entity.Booking;
import com.bookmyshow.entity.User;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final BookingRepository bookingRepository;

    /**
     * Validates whether the authenticated caller has explicit ADMIN privileges.
     * Throws SecurityException if not authorized.
     */
    public void validateAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
                return;
            }
            log.warn("Unauthorized attempt to access Admin API without ROLE_ADMIN authority");
            throw new SecurityException("Access Denied: Admin privileges required to perform this operation.");
        }
        log.info("Admin operation executed under local admin session.");
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
            String currentUserId = resolvePrincipalId(auth);
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
        return resolvePrincipalId(auth);
    }

    private String resolvePrincipalId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    /**
     * Ensures the authenticated caller owns the booking or has ADMIN privileges.
     */
    public void validateBookingAccess(Booking booking) {
        if (booking == null) {
            throw new ResourceNotFoundException("Booking not found");
        }
        if (booking.getClerkUserId() != null) {
            validateOwnershipOrAdmin(booking.getClerkUserId());
            return;
        }
        validateAdmin();
    }

    /**
     * Ensures the authenticated caller owns the booking or has ADMIN privileges.
     */
    public void validateBookingAccess(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
        validateBookingAccess(booking);
    }

    /**
     * Ensures the authenticated caller owns the database user record or has ADMIN privileges.
     */
    public void validateUserIdOwnership(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (user.getClerkUserId() != null) {
            validateOwnershipOrAdmin(user.getClerkUserId());
            return;
        }
        validateAdmin();
    }

    /**
     * Ensures the authenticated caller owns the ticket/booking or has ADMIN privileges.
     */
    public void validateTicketAccess(String ticketToken) {
        Booking booking = bookingRepository.findByTicketToken(ticketToken)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found for token: " + ticketToken));
        validateBookingAccess(booking);
    }
}
