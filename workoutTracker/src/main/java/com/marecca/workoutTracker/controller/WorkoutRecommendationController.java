package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.request.SaveWorkoutPlanRequest;
import com.marecca.workoutTracker.dto.WorkoutRecommendationDTO;
import com.marecca.workoutTracker.dto.request.WorkoutRecommendationRequest;
import com.marecca.workoutTracker.service.WorkoutRecommendationService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for workout recommendations - JWT Protected
 */
@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class WorkoutRecommendationController {

    private final WorkoutRecommendationService workoutRecommendationService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Endpoint for getting workout recommendation (requires authentication)
     * POST /api/workouts/recommend
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> getWorkoutRecommendations(@Valid @RequestBody WorkoutRecommendationRequest request,
                                                       HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request for workout recommendations by user: {} with goal: {}",
                    authenticatedUserId, request.getGoalType());

            // Verify user can only get recommendations for themselves
            if (request.getUserId() == null) {
                return jwtUtils.createBadRequestResponse("User ID is required");
            }

            if (!request.getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to get recommendations for user {}", authenticatedUserId, request.getUserId());
                return jwtUtils.createErrorResponse("You can only get recommendations for yourself", HttpStatus.FORBIDDEN);
            }

            if (request.getGoalType() == null || request.getGoalType().trim().isEmpty()) {
                return jwtUtils.createBadRequestResponse("Goal type is required");
            }

            if (!isValidGoalType(request.getGoalType())) {
                return jwtUtils.createBadRequestResponse("Invalid goal type. Valid values: WEIGHT_LOSS, MUSCLE_GAIN, MAINTENANCE");
            }

            List<WorkoutRecommendationDTO> recommendations = workoutRecommendationService
                    .getRecommendations(authenticatedUserId, request.getGoalType()); // Use authenticated user ID

            log.info("Generated {} recommendations for user {}", recommendations.size(), authenticatedUserId);

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", recommendations);
            response.put("message", "Recommendations generated successfully");
            response.put("totalCount", recommendations.size());
            response.put("userId", authenticatedUserId);
            response.put("goalType", request.getGoalType());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for workout recommendations: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (RuntimeException e) {
            log.error("Runtime error getting workout recommendations: {}", e.getMessage(), e);
            return jwtUtils.createErrorResponse("Failed to generate recommendations: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to get workout recommendations");
        }
    }

    /**
     * Endpoint for saving workout plan based on recommendation (requires authentication)
     * POST /api/workouts/save-recommended-plan
     */
    @PostMapping("/save-recommended-plan")
    public ResponseEntity<?> saveRecommendedWorkoutPlan(@Valid @RequestBody SaveWorkoutPlanRequest request,
                                                        HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to save workout plan by user: {} with goal: {} and planName: {}",
                    authenticatedUserId, request.getGoalId(), request.getPlanName());

            // Verify user can only save plans for themselves
            if (request.getUserId() == null) {
                return jwtUtils.createBadRequestResponse("User ID is required");
            }

            if (!request.getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to save plan for user {}", authenticatedUserId, request.getUserId());
                return jwtUtils.createErrorResponse("You can only save workout plans for yourself", HttpStatus.FORBIDDEN);
            }

            if (request.getRecommendations() == null || request.getRecommendations().isEmpty()) {
                return jwtUtils.createBadRequestResponse("Recommendations are required to save workout plan");
            }

            String planName = request.getPlanName();
            if (planName == null || planName.trim().isEmpty()) {
                planName = "Recommended Workout";
            }

            Map<String, Object> savedPlan = workoutRecommendationService
                    .saveWorkoutPlan(authenticatedUserId, request.getRecommendations(), request.getGoalId(), planName); // Use authenticated user ID

            log.info("Successfully saved workout plan with ID: {} for user: {}",
                    savedPlan.get("workoutPlanId"), authenticatedUserId);

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Workout plan saved successfully");
            response.put("workoutPlan", savedPlan);
            response.put("exerciseCount", request.getRecommendations().size());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for saving workout plan: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (RuntimeException e) {
            log.error("Runtime error saving workout plan: {}", e.getMessage(), e);
            return jwtUtils.createErrorResponse("Failed to save workout plan: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to save workout plans");
        }
    }

    /**
     * Endpoint for getting user statistics (requires authentication)
     * GET /api/workouts/user/{userId}/stats
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<?> getUserWorkoutStats(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to get workout stats for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (userId == null || userId <= 0) {
                return jwtUtils.createBadRequestResponse("Invalid user ID");
            }

            // Verify user can only access their own stats
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access stats for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own workout stats", HttpStatus.FORBIDDEN);
            }

            Map<String, Object> stats = workoutRecommendationService.getUserWorkoutStats(authenticatedUserId); // Use authenticated user ID

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User stats retrieved successfully");
            response.put("stats", stats);
            response.put("userId", authenticatedUserId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for user stats: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout stats");
        }
    }

    /**
     * Get current user's workout stats (convenience endpoint)
     * GET /api/workouts/my-stats
     */
    @GetMapping("/my-stats")
    public ResponseEntity<?> getMyWorkoutStats(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my workout stats for user: {}", authenticatedUserId);

            Map<String, Object> stats = workoutRecommendationService.getUserWorkoutStats(authenticatedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User stats retrieved successfully");
            response.put("stats", stats);
            response.put("userId", authenticatedUserId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for user stats: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout stats");
        }
    }

    /**
     * Get recommendations for current user (convenience endpoint)
     * POST /api/workouts/my-recommendations
     */
    @PostMapping("/my-recommendations")
    public ResponseEntity<?> getMyWorkoutRecommendations(@RequestParam String goalType, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request for my workout recommendations by user: {} with goal: {}", authenticatedUserId, goalType);

            if (goalType == null || goalType.trim().isEmpty()) {
                return jwtUtils.createBadRequestResponse("Goal type is required");
            }

            if (!isValidGoalType(goalType)) {
                return jwtUtils.createBadRequestResponse("Invalid goal type. Valid values: WEIGHT_LOSS, MUSCLE_GAIN, MAINTENANCE");
            }

            List<WorkoutRecommendationDTO> recommendations = workoutRecommendationService
                    .getRecommendations(authenticatedUserId, goalType);

            log.info("Generated {} recommendations for user {}", recommendations.size(), authenticatedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", recommendations);
            response.put("message", "Recommendations generated successfully");
            response.put("totalCount", recommendations.size());
            response.put("userId", authenticatedUserId);
            response.put("goalType", goalType);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for workout recommendations: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (RuntimeException e) {
            log.error("Runtime error getting workout recommendations: {}", e.getMessage(), e);
            return jwtUtils.createErrorResponse("Failed to generate recommendations: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to get workout recommendations");
        }
    }

    /**
     * Validates type of goal
     */
    private boolean isValidGoalType(String goalType) {
        return goalType != null && (
                goalType.equals("WEIGHT_LOSS") ||
                        goalType.equals("MUSCLE_GAIN") ||
                        goalType.equals("MAINTENANCE")
        );
    }

    /**
     * Error response (fallback for compatibility)
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("success", false);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }

    /**
     * Exception handlers for error handling
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errorMessage.append(error.getField())
                        .append(" ")
                        .append(error.getDefaultMessage())
                        .append("; ")
        );

        return jwtUtils.createBadRequestResponse(errorMessage.toString());
    }

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
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("Unexpected error in WorkoutRecommendationController", ex);
        return jwtUtils.createErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}