package com.marecca.workoutTracker.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtControllerUtils {

    private final JwtUtil jwtUtil;

    /**
     * Extract user ID from JWT token in Authorization header
     */
    public Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("No valid authentication token found");
    }

    /**
     * Extract email from JWT token in Authorization header
     */
    public String getEmailFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getEmailFromToken(token);
        }
        throw new RuntimeException("No valid authentication token found");
    }

    /**
     * Get the raw JWT token from Authorization header
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("No valid authentication token found");
    }

    /**
     * Validate that the authenticated user can access the requested user's data
     */
    public ResponseEntity<?> validateUserAccess(Long requestedUserId, Long authenticatedUserId) {
        if (!requestedUserId.equals(authenticatedUserId)) {
            return createForbiddenResponse("You can only access your own data");
        }
        return null; // Access granted
    }

    /**
     * Check if authenticated user matches the requested user ID
     */
    public boolean canAccessUserData(Long requestedUserId, Long authenticatedUserId) {
        return requestedUserId.equals(authenticatedUserId);
    }

    /**
     * Create standardized error response
     */
    public ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Create forbidden access response
     */
    public ResponseEntity<?> createForbiddenResponse(String message) {
        return createErrorResponse(message, HttpStatus.FORBIDDEN);
    }

    /**
     * Create unauthorized response
     */
    public ResponseEntity<?> createUnauthorizedResponse(String message) {
        return createErrorResponse(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Create bad request response
     */
    public ResponseEntity<?> createBadRequestResponse(String message) {
        return createErrorResponse(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Validate user access and return error response if invalid
     * Returns null if access is granted
     */
    public ResponseEntity<?> checkUserAccess(HttpServletRequest request, Long requestedUserId) {
        try {
            Long authenticatedUserId = getUserIdFromToken(request);
            return validateUserAccess(requestedUserId, authenticatedUserId);
        } catch (Exception e) {
            return createUnauthorizedResponse("Authentication failed: " + e.getMessage());
        }
    }
}