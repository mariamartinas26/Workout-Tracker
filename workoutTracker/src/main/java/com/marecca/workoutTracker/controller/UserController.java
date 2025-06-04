package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.response.DetailedUserResponse;
import com.marecca.workoutTracker.dto.request.UpdateProfileRequest;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.service.UserService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for user management - JWT Protected
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Update user profile (requires authentication)
     */
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateCompleteProfile(@PathVariable Long userId,
                                                   @RequestBody UpdateProfileRequest request,
                                                   HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to update profile for user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only update their own profile
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to update profile for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only update your own profile", HttpStatus.FORBIDDEN);
            }

            // Check if user exists
            Optional<User> existingUserOptional = userService.findById(authenticatedUserId);
            if (existingUserOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User existingUser = existingUserOptional.get();

            if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
                existingUser.setFirstName(request.getFirstName().trim());
            }

            if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
                existingUser.setLastName(request.getLastName().trim());
            }

            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                // Check if email is already used by another user
                Optional<User> userWithEmail = userService.findByEmail(request.getEmail().trim());
                if (userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(authenticatedUserId)) {
                    return jwtUtils.createBadRequestResponse("Email already exists");
                }
                existingUser.setEmail(request.getEmail().trim().toLowerCase());
            }

            if (request.getDateOfBirth() != null) {
                existingUser.setDateOfBirth(request.getDateOfBirth());
            }

            if (request.getHeightCm() != null) {
                if (request.getHeightCm() < 50 || request.getHeightCm() > 300) {
                    return jwtUtils.createBadRequestResponse("Height must be between 50 and 300 cm");
                }
                existingUser.setHeightCm(request.getHeightCm());
            }

            if (request.getWeightKg() != null) {
                if (request.getWeightKg().compareTo(BigDecimal.valueOf(20)) < 0 ||
                        request.getWeightKg().compareTo(BigDecimal.valueOf(1000)) > 0) {
                    return jwtUtils.createBadRequestResponse("Weight must be between 20 and 1000 kg");
                }
                existingUser.setWeightKg(request.getWeightKg());
            }

            if (request.getFitnessLevel() != null) {
                if (!List.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(request.getFitnessLevel())) {
                    return jwtUtils.createBadRequestResponse("Invalid fitness level. Must be BEGINNER, INTERMEDIATE, or ADVANCED");
                }
                existingUser.setFitnessLevel(request.getFitnessLevel());
            }

            // Update user
            User savedUser = userService.updateUser(authenticatedUserId, existingUser);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", savedUser.getUserId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("firstName", savedUser.getFirstName());
            response.put("lastName", savedUser.getLastName());
            response.put("dateOfBirth", savedUser.getDateOfBirth());
            response.put("heightCm", savedUser.getHeightCm());
            response.put("weightKg", savedUser.getWeightKg());
            response.put("fitnessLevel", savedUser.getFitnessLevel());
            response.put("isActive", savedUser.getIsActive());
            response.put("createdAt", savedUser.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error updating profile: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to update profile");
        }
    }

    /**
     * Get user profile by ID (requires authentication)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get profile for user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only access their own profile
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access profile for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own profile", HttpStatus.FORBIDDEN);
            }

            Optional<User> userOptional = userService.findById(authenticatedUserId);

            if (userOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            DetailedUserResponse userResponse = new DetailedUserResponse(userOptional.get());
            return ResponseEntity.ok(userResponse);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access profile");
        }
    }

    /**
     * Complete user profile (requires authentication)
     */
    @PutMapping("/complete-profile")
    public ResponseEntity<?> completeProfileUser(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to complete profile for user: {}", authenticatedUserId);

            // Verify userId in request matches authenticated user
            Object userIdObj = request.get("userId");
            if (userIdObj != null) {
                Long requestUserId;
                try {
                    if (userIdObj instanceof Number) {
                        requestUserId = ((Number) userIdObj).longValue();
                    } else {
                        requestUserId = Long.valueOf(userIdObj.toString());
                    }

                    if (!requestUserId.equals(authenticatedUserId)) {
                        log.warn("User {} attempted to complete profile for user {}", authenticatedUserId, requestUserId);
                        return jwtUtils.createErrorResponse("You can only complete your own profile", HttpStatus.FORBIDDEN);
                    }
                } catch (NumberFormatException e) {
                    return jwtUtils.createBadRequestResponse("Invalid user ID format");
                }
            }

            Optional<User> userOptional = userService.findById(authenticatedUserId);
            if (userOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            Object dateOfBirthObj = request.get("dateOfBirth");
            if (dateOfBirthObj != null && !dateOfBirthObj.toString().trim().isEmpty()) {
                try {
                    user.setDateOfBirth(LocalDate.parse(dateOfBirthObj.toString()));
                } catch (Exception e) {
                    return jwtUtils.createBadRequestResponse("Invalid date format for dateOfBirth");
                }
            }

            Object heightCmObj = request.get("heightCm");
            if (heightCmObj != null) {
                try {
                    Integer height = heightCmObj instanceof Number ?
                            ((Number) heightCmObj).intValue() :
                            Integer.valueOf(heightCmObj.toString());

                    if (height < 50 || height > 300) {
                        return jwtUtils.createBadRequestResponse("Height must be between 50 and 300 cm");
                    }
                    user.setHeightCm(height);
                } catch (NumberFormatException e) {
                    return jwtUtils.createBadRequestResponse("Invalid height format");
                }
            }

            Object weightKgObj = request.get("weightKg");
            if (weightKgObj != null) {
                try {
                    BigDecimal weight = weightKgObj instanceof Number ?
                            BigDecimal.valueOf(((Number) weightKgObj).doubleValue()) :
                            new BigDecimal(weightKgObj.toString());

                    if (weight.compareTo(BigDecimal.valueOf(20)) < 0 ||
                            weight.compareTo(BigDecimal.valueOf(1000)) > 0) {
                        return jwtUtils.createBadRequestResponse("Weight must be between 20 and 1000 kg");
                    }
                    user.setWeightKg(weight);
                } catch (NumberFormatException e) {
                    return jwtUtils.createBadRequestResponse("Invalid weight format");
                }
            }

            Object fitnessLevelObj = request.get("fitnessLevel");
            if (fitnessLevelObj != null && !fitnessLevelObj.toString().trim().isEmpty()) {
                String fitnessLevel = fitnessLevelObj.toString().toUpperCase();
                if (!List.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(fitnessLevel)) {
                    return jwtUtils.createBadRequestResponse("Invalid fitness level. Must be BEGINNER, INTERMEDIATE, or ADVANCED");
                }
                user.setFitnessLevel(fitnessLevel);
            }

            User savedUser = userService.updateUser(authenticatedUserId, user);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", savedUser.getUserId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("firstName", savedUser.getFirstName());
            response.put("lastName", savedUser.getLastName());
            response.put("dateOfBirth", savedUser.getDateOfBirth());
            response.put("heightCm", savedUser.getHeightCm());
            response.put("weightKg", savedUser.getWeightKg());
            response.put("fitnessLevel", savedUser.getFitnessLevel());
            response.put("isActive", savedUser.getIsActive());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error completing profile: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to complete profile");
        }
    }

    /**
     * Get current user's profile (convenience endpoint)
     */
    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my profile for user: {}", authenticatedUserId);

            return getUserProfile(authenticatedUserId, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access profile");
        }
    }

    /**
     * Get users by fitness level (admin/public endpoint - requires authentication)
     */
    @GetMapping("/by-fitness-level/{fitnessLevel}")
    public ResponseEntity<?> getUsersByFitnessLevel(@PathVariable String fitnessLevel, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get users by fitness level: {} by user: {}", fitnessLevel, authenticatedUserId);

            // Validate fitness level
            if (!List.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(fitnessLevel.toUpperCase())) {
                return jwtUtils.createBadRequestResponse("Invalid fitness level. Must be BEGINNER, INTERMEDIATE, or ADVANCED");
            }

            List<User> users = userService.findByFitnessLevel(fitnessLevel.toUpperCase());

            // Return limited user information for privacy
            List<Map<String, Object>> userResponses = users.stream()
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("userId", user.getUserId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("fitnessLevel", user.getFitnessLevel());
                        userInfo.put("firstName", user.getFirstName());
                        // Don't expose sensitive information like email, weight, etc.
                        return userInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userResponses);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access user data");
        }
    }

    /**
     * Deactivate user account (requires authentication - own account only)
     */
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to deactivate user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only deactivate their own account
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to deactivate user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only deactivate your own account", HttpStatus.FORBIDDEN);
            }

            userService.deactivateUser(authenticatedUserId);

            log.info("User deactivated successfully: {}", authenticatedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deactivated successfully");
            response.put("userId", authenticatedUserId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Deactivation validation error for user ID {}: {}", userId, e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to deactivate account");
        }
    }

    /**
     * Activate user account (requires authentication - own account only)
     */
    @PostMapping("/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to activate user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only activate their own account
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to activate user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only activate your own account", HttpStatus.FORBIDDEN);
            }

            userService.activateUser(authenticatedUserId);

            log.info("User activated successfully: {}", authenticatedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account activated successfully");
            response.put("userId", authenticatedUserId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Activation validation error for user ID {}: {}", userId, e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to activate account");
        }
    }

    /**
     * Get user statistics (public endpoint but requires authentication)
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get user stats by user: {}", authenticatedUserId);

            List<User> allActiveUsers = userService.findActiveUsers();

            long beginnerCount = allActiveUsers.stream()
                    .filter(u -> "BEGINNER".equals(u.getFitnessLevel()))
                    .count();

            long intermediateCount = allActiveUsers.stream()
                    .filter(u -> "INTERMEDIATE".equals(u.getFitnessLevel()))
                    .count();

            long advancedCount = allActiveUsers.stream()
                    .filter(u -> "ADVANCED".equals(u.getFitnessLevel()))
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalActiveUsers", allActiveUsers.size());
            stats.put("beginnerUsers", beginnerCount);
            stats.put("intermediateUsers", intermediateCount);
            stats.put("advancedUsers", advancedCount);
            stats.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access user statistics");
        }
    }

    /**
     * Search users by username or email (requires authentication)
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to search users with query: {} by user: {}", query, authenticatedUserId);

            if (query == null || query.trim().length() < 2) {
                return jwtUtils.createBadRequestResponse("Search query must be at least 2 characters");
            }

            String searchTerm = query.trim().toLowerCase();
            List<User> allActiveUsers = userService.findActiveUsers();

            // Filter users that contain the search term
            List<User> filteredUsers = allActiveUsers.stream()
                    .filter(user ->
                            user.getUsername().toLowerCase().contains(searchTerm) ||
                                    user.getEmail().toLowerCase().contains(searchTerm) ||
                                    (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(searchTerm)) ||
                                    (user.getLastName() != null && user.getLastName().toLowerCase().contains(searchTerm))
                    )
                    .collect(Collectors.toList());

            // Return limited user information for privacy
            List<Map<String, Object>> userResponses = filteredUsers.stream()
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("userId", user.getUserId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("firstName", user.getFirstName());
                        userInfo.put("lastName", user.getLastName());
                        userInfo.put("fitnessLevel", user.getFitnessLevel());
                        // Don't expose sensitive information
                        return userInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("totalResults", userResponses.size());
            response.put("users", userResponses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to search users");
        }
    }

    /**
     * Get users with at least a minimum number of workout plans (requires authentication)
     */
    @GetMapping("/with-min-plans")
    public ResponseEntity<?> getUsersWithMinimumPlans(@RequestParam(defaultValue = "1") Long minPlans, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get users with min plans: {} by user: {}", minPlans, authenticatedUserId);

            if (minPlans < 0) {
                return jwtUtils.createBadRequestResponse("Minimum plans must be non-negative");
            }

            List<User> users = userService.findUsersWithMinimumPlans(minPlans);

            // Return limited user information
            List<Map<String, Object>> userResponses = users.stream()
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("userId", user.getUserId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("fitnessLevel", user.getFitnessLevel());
                        return userInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("minPlans", minPlans);
            response.put("totalResults", userResponses.size());
            response.put("users", userResponses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access user data");
        }
    }

    /**
     * Get users with at least a minimum number of completed workouts (requires authentication)
     */
    @GetMapping("/with-min-workouts")
    public ResponseEntity<?> getUsersWithMinimumWorkouts(@RequestParam(defaultValue = "1") Integer minWorkouts, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get users with min workouts: {} by user: {}", minWorkouts, authenticatedUserId);

            if (minWorkouts < 0) {
                return jwtUtils.createBadRequestResponse("Minimum workouts must be non-negative");
            }

            List<User> users = userService.findUsersWithMinimumCompletedWorkouts(minWorkouts);

            // Return limited user information
            List<Map<String, Object>> userResponses = users.stream()
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("userId", user.getUserId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("fitnessLevel", user.getFitnessLevel());
                        return userInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("minWorkouts", minWorkouts);
            response.put("totalResults", userResponses.size());
            response.put("users", userResponses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access user data");
        }
    }

    /**
     * Exception handlers for error handling
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return jwtUtils.createBadRequestResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage());
        return jwtUtils.createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return jwtUtils.createErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}