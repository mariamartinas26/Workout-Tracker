package com.marecca.workoutTracker.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marecca.workoutTracker.dto.response.AvailabilityResponse;
import com.marecca.workoutTracker.dto.response.ErrorResponse;
import com.marecca.workoutTracker.dto.request.RescheduleWorkoutRequest;
import com.marecca.workoutTracker.dto.response.SuccessResponse;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.service.ScheduledWorkoutService;
import com.marecca.workoutTracker.service.exceptions.UserNotFoundException;
import com.marecca.workoutTracker.service.exceptions.WorkoutAlreadyScheduledException;
import com.marecca.workoutTracker.service.exceptions.WorkoutPlanNotFoundException;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing scheduled workouts - JWT Protected
 */
@RestController
@RequestMapping("/api/scheduled-workouts")
@RequiredArgsConstructor
@Slf4j
public class ScheduledWorkoutController {

    private final ScheduledWorkoutService scheduledWorkoutService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Schedule a new workout (requires authentication)
     */
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleWorkout(@Valid @RequestBody ScheduleWorkoutRequest request, HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            // Verify user can only schedule workouts for themselves
            if (!request.getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only schedule workouts for yourself", HttpStatus.FORBIDDEN);
            }

            Long scheduledWorkoutId = scheduledWorkoutService.scheduleWorkout(
                    authenticatedUserId, // Use authenticated user ID
                    request.getWorkoutPlanId(),
                    request.getScheduledDate(),
                    request.getScheduledTime()
            );

            ScheduleWorkoutResponse response = ScheduleWorkoutResponse.builder()
                    .scheduledWorkoutId(scheduledWorkoutId)
                    .message("Workout scheduled successfully")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserNotFoundException e) {
            log.error("User not found: {}", e.getMessage());
            return jwtUtils.createErrorResponse("User not found", HttpStatus.NOT_FOUND);

        } catch (WorkoutPlanNotFoundException e) {
            log.error("Workout plan not found: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Workout plan not found", HttpStatus.NOT_FOUND);

        } catch (WorkoutAlreadyScheduledException e) {
            log.error("Workout already scheduled: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Workout already scheduled", HttpStatus.CONFLICT);

        } catch (IllegalArgumentException e) {
            log.error("Validation error while scheduling workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (RuntimeException e) {
            log.error("Database error while scheduling workout: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Database error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to schedule workouts");
        }
    }

    /**
     * Find all scheduled workouts for a user (requires authentication)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get workouts for user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only access their own workouts
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access workouts for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own workouts", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserId(authenticatedUserId);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workouts");
        }
    }

    /**
     * Shows the scheduled workouts for today for a user (requires authentication)
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<?> getTodaysWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get today's workouts for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access today's workouts for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own workouts", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.findTodaysWorkouts(authenticatedUserId);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access today's workouts");
        }
    }

    /**
     * Start a workout (requires authentication)
     */
    @PutMapping("/{workoutId}/start")
    public ResponseEntity<?> startWorkout(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to start workout: {} by user: {}", workoutId, authenticatedUserId);

            // Verify workout ownership before starting
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to start workout {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only start your own workouts", HttpStatus.FORBIDDEN);
            }

            scheduledWorkoutService.startWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout started successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error starting workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to start workouts");
        }
    }

    /**
     * Complete a workout (requires authentication)
     */
    @PutMapping("/{workoutId}/complete")
    public ResponseEntity<?> completeWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody CompleteWorkoutRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to complete workout: {} by user: {}", workoutId, authenticatedUserId);

            // Verify workout ownership before completing
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to complete workout {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only complete your own workouts", HttpStatus.FORBIDDEN);
            }

            scheduledWorkoutService.completeWorkout(
                    workoutId,
                    request.getCaloriesBurned(),
                    request.getRating()
            );

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout completed successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error completing workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to complete workouts");
        }
    }

    /**
     * Cancel a workout (requires authentication)
     */
    @PutMapping("/{workoutId}/cancel")
    public ResponseEntity<?> cancelWorkout(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to cancel workout: {} by user: {}", workoutId, authenticatedUserId);

            // Verify workout ownership before cancelling
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to cancel workout {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only cancel your own workouts", HttpStatus.FORBIDDEN);
            }

            scheduledWorkoutService.cancelWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout cancelled successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error cancelling workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to cancel workouts");
        }
    }

    /**
     * Check if a user can schedule a workout at a specific date/time (requires authentication)
     */
    @GetMapping("/user/{userId}/availability")
    public ResponseEntity<?> checkAvailability(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to check availability for user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only check their own availability
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to check availability for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only check your own availability", HttpStatus.FORBIDDEN);
            }

            boolean available = scheduledWorkoutService.canScheduleWorkoutAt(authenticatedUserId, date, time);

            AvailabilityResponse response = AvailabilityResponse.builder()
                    .available(available)
                    .date(date)
                    .time(time)
                    .message(available ? "Slot available" : "Slot occupied")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to check availability");
        }
    }

    /**
     * Shows the recently completed workouts (requires authentication)
     */
    @GetMapping("/user/{userId}/recent-completed")
    public ResponseEntity<?> getRecentCompletedWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get recent completed workouts for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access recent completed workouts for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own workout history", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.findRecentCompletedWorkouts(authenticatedUserId);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout history");
        }
    }

    /**
     * Shows how many completed workouts are for a user (requires authentication)
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<?> getUserStatistics(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get statistics for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access statistics for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own statistics", HttpStatus.FORBIDDEN);
            }

            ScheduledWorkoutService.WorkoutStatistics stats = scheduledWorkoutService.getUserWorkoutStatistics(authenticatedUserId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access statistics");
        }
    }

    /**
     * Reschedule an existing workout (requires authentication)
     */
    @PutMapping("/{workoutId}/reschedule")
    public ResponseEntity<?> rescheduleWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody RescheduleWorkoutRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to reschedule workout: {} by user: {}", workoutId, authenticatedUserId);

            // Verify workout ownership before rescheduling
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to reschedule workout {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only reschedule your own workouts", HttpStatus.FORBIDDEN);
            }

            scheduledWorkoutService.rescheduleWorkout(
                    workoutId,
                    request.getScheduledDate(),
                    request.getScheduledTime()
            );

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout rescheduled successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error rescheduling workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to reschedule workouts");
        }
    }

    /**
     * Get current user's workouts (convenience endpoint)
     */
    @GetMapping("/my-workouts")
    public ResponseEntity<?> getMyWorkouts(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my workouts for user: {}", authenticatedUserId);

            return getUserWorkouts(authenticatedUserId, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workouts");
        }
    }

    /**
     * Get current user's today workouts (convenience endpoint)
     */
    @GetMapping("/my-today")
    public ResponseEntity<?> getMyTodayWorkouts(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my today workouts for user: {}", authenticatedUserId);

            return getTodaysWorkouts(authenticatedUserId, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access today's workouts");
        }
    }

    /**
     * Get current user's statistics (convenience endpoint)
     */
    @GetMapping("/my-statistics")
    public ResponseEntity<?> getMyStatistics(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my statistics for user: {}", authenticatedUserId);

            return getUserStatistics(authenticatedUserId, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access statistics");
        }
    }

    /**
     * Check current user's availability (convenience endpoint)
     */
    @GetMapping("/my-availability")
    public ResponseEntity<?> checkMyAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to check my availability for user: {}", authenticatedUserId);

            return checkAvailability(authenticatedUserId, date, time, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to check availability");
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

    // Inner classes for requests/responses
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScheduleWorkoutRequest {
        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        private Long userId;

        @NotNull(message = "Workout plan ID is required")
        @Positive(message = "Workout plan ID must be positive")
        private Long workoutPlanId;

        @NotNull(message = "Scheduled date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate scheduledDate;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime scheduledTime;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CompleteWorkoutRequest {
        private Integer caloriesBurned;
        private Integer rating;
    }

    @lombok.Data
    @lombok.Builder
    public static class ScheduleWorkoutResponse {
        private Long scheduledWorkoutId;
        private String message;
    }
}