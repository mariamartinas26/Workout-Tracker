package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.SaveWorkoutPlanRequest;
import com.marecca.workoutTracker.dto.WorkoutRecommendation;
import com.marecca.workoutTracker.dto.WorkoutRecommendationRequest;
import com.marecca.workoutTracker.service.WorkoutRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "http://localhost:3000")
public class WorkoutRecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(WorkoutRecommendationController.class);

    @Autowired
    private WorkoutRecommendationService workoutRecommendationService;

    /**
     * endpoint for getting workout recommendation
     * POST /api/workouts/recommend
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> getWorkoutRecommendations(@Valid @RequestBody WorkoutRecommendationRequest request) {
        try {
            logger.info("Getting workout recommendations for user: {} with goal: {}",
                    request.getUserId(), request.getGoalType());

            if (request.getUserId() == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("User ID is required"));
            }

            if (request.getGoalType() == null || request.getGoalType().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Goal type is required"));
            }

            if (!isValidGoalType(request.getGoalType())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid goal type. Valid values: WEIGHT_LOSS, MUSCLE_GAIN, MAINTENANCE"));
            }

            List<WorkoutRecommendation> recommendations = workoutRecommendationService
                    .getRecommendations(request.getUserId(), request.getGoalType());

            logger.info("Generated {} recommendations for user {}",
                    recommendations.size(), request.getUserId());

            //response
            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", recommendations);
            response.put("message", "Recommendations generated successfully");
            response.put("totalCount", recommendations.size());
            response.put("userId", request.getUserId());
            response.put("goalType", request.getGoalType());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for workout recommendations: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Runtime error getting workout recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to generate recommendations: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Unexpected error getting workout recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An unexpected error occurred while generating recommendations"));
        }
    }

    /**
     * Endpoint for saving workout plan based on recommendation
     * POST /api/workouts/save-recommended-plan
     */
    @PostMapping("/save-recommended-plan")
    public ResponseEntity<?> saveRecommendedWorkoutPlan(@Valid @RequestBody SaveWorkoutPlanRequest request) {
        try {
            logger.info("Saving workout plan for user: {} with goal: {} and planName: {}",
                    request.getUserId(), request.getGoalId(), request.getPlanName());

            if (request.getUserId() == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("User ID is required"));
            }

            if (request.getRecommendations() == null || request.getRecommendations().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Recommendations are required to save workout plan"));
            }

            String planName = request.getPlanName();
            if (planName == null || planName.trim().isEmpty()) {
                planName = "Recommended Workout";
            }

            Map<String, Object> savedPlan = workoutRecommendationService
                    .saveWorkoutPlan(request.getUserId(), request.getRecommendations(), request.getGoalId(), planName);

            logger.info("Successfully saved workout plan with ID: {} for user: {}",
                    savedPlan.get("workoutPlanId"), request.getUserId());

            //response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Workout plan saved successfully");
            response.put("workoutPlan", savedPlan);
            response.put("exerciseCount", request.getRecommendations().size());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for saving workout plan: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (RuntimeException e) {
            logger.error("Runtime error saving workout plan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to save workout plan: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Unexpected error saving workout plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An unexpected error occurred while saving the workout plan"));
        }
    }
    /**
     * Endpoint for getting user statistics
     * GET /api/workouts/user/{userId}/stats
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<?> getUserWorkoutStats(@PathVariable Long userId) {
        try {
            logger.info("Getting workout stats for user: {}", userId);

            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID"));
            }

            Map<String, Object> stats = workoutRecommendationService.getUserWorkoutStats(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User stats retrieved successfully");
            response.put("stats", stats);
            response.put("userId", userId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for user stats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            logger.error("Error getting user workout stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve user stats"));
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
     * Error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("success", false);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }

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

        return ResponseEntity.badRequest()
                .body(createErrorResponse(errorMessage.toString()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        logger.error("Unexpected error in WorkoutRecommendationController", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
    }
}