package com.marecca.workoutTracker.util;

import com.marecca.workoutTracker.service.exceptions.InvalidJwtTokenException;
import com.marecca.workoutTracker.service.exceptions.JwtTokenException;
import com.marecca.workoutTracker.service.exceptions.JwtTokenExpiredException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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

    public Long getUserIdFromToken(HttpServletRequest request) throws JwtTokenException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidJwtTokenException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (ExpiredJwtException e) {
            throw new JwtTokenExpiredException("Token has expired");
        } catch (JwtException e) {
            throw new InvalidJwtTokenException("Invalid token format");
        } catch (Exception e) {
            throw new JwtTokenException("Token validation failed");
        }
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

}