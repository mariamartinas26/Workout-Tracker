package com.marecca.workoutTracker.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marecca.workoutTracker.dto.response.AvailabilityResponse;
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


@RestController
@RequestMapping("/api/scheduled-workouts")
@RequiredArgsConstructor
public class ScheduledWorkoutController {

    private final ScheduledWorkoutService scheduledWorkoutService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Schedule a new workout
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
            return jwtUtils.createErrorResponse("User not found", HttpStatus.NOT_FOUND);

        } catch (WorkoutPlanNotFoundException e) {
            return jwtUtils.createErrorResponse("Workout plan not found", HttpStatus.NOT_FOUND);

        } catch (WorkoutAlreadyScheduledException e) {
            return jwtUtils.createErrorResponse("Workout already scheduled", HttpStatus.CONFLICT);

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (RuntimeException e) {
            return jwtUtils.createErrorResponse("Database error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to schedule workouts");
        }
    }

    /**
     * Find all scheduled workouts for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            if (!userId.equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only access your own workouts", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserId(authenticatedUserId);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workouts");
        }
    }

    /**
     * Shows the scheduled workouts for today for a user
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<?> getTodaysWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            if (!userId.equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only access your own workouts", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.MissedWorkouts(authenticatedUserId);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access today's workouts");
        }
    }

    /**
     * Start a workout
     */
    @PutMapping("/{workoutId}/start")
    public ResponseEntity<?> startWorkout(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {

                return jwtUtils.createErrorResponse("You can only start your own workouts", HttpStatus.FORBIDDEN);
            }

            scheduledWorkoutService.startWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout started successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to start workouts");
        }
    }

    /**
     * Complete a workout
     */
    @PutMapping("/{workoutId}/complete")
    public ResponseEntity<?> completeWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody CompleteWorkoutRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
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
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to complete workouts");
        }
    }

    /**
     * Cancel a workout
     */
    @PutMapping("/{workoutId}/cancel")
    public ResponseEntity<?> cancelWorkout(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only cancel your own workouts", HttpStatus.FORBIDDEN);
            }

            scheduledWorkoutService.cancelWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout cancelled successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to cancel workouts");
        }
    }

    /**
     * Check if a user can schedule a workout at a specific date/time
     */
    @GetMapping("/user/{userId}/availability")
    public ResponseEntity<?> checkAvailability(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            if (!userId.equals(authenticatedUserId)) {
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
            return jwtUtils.createUnauthorizedResponse("Authentication required to check availability");
        }
    }

    /**
     * Shows the recently completed workouts
     */
    @GetMapping("/user/{userId}/recent-completed")
    public ResponseEntity<?> getRecentCompletedWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            if (!userId.equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only access your own workout history", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.findRecentCompletedWorkouts(authenticatedUserId);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout history");
        }
    }

    /**
     * Shows how many completed workouts are for a user
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<?> getUserStatistics(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            if (!userId.equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only access your own statistics", HttpStatus.FORBIDDEN);
            }

            ScheduledWorkoutService.WorkoutStatistics stats = scheduledWorkoutService.getUserWorkoutStatistics(authenticatedUserId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access statistics");
        }
    }

    /**
     * Reschedule an existing workout
     */
    @PutMapping("/{workoutId}/reschedule")
    public ResponseEntity<?> rescheduleWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody RescheduleWorkoutRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            // Verify workout ownership before rescheduling
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
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
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to reschedule workouts");
        }
    }

    /**
     * Get current user's workouts
     */
    @GetMapping("/my-workouts")
    public ResponseEntity<?> getMyWorkouts(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            return getUserWorkouts(authenticatedUserId, request);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workouts");
        }
    }

    /**
     * Get current user's today workouts
     */
    @GetMapping("/my-today")
    public ResponseEntity<?> getMyTodayWorkouts(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            return getTodaysWorkouts(authenticatedUserId, request);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access today's workouts");
        }
    }

    /**
     * Get current user's statistics
     */
    @GetMapping("/my-statistics")
    public ResponseEntity<?> getMyStatistics(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            return getUserStatistics(authenticatedUserId, request);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access statistics");
        }
    }

    /**
     * Check current user's availability
     */
    @GetMapping("/my-availability")
    public ResponseEntity<?> checkMyAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            return checkAvailability(authenticatedUserId, date, time, request);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to check availability");
        }
    }

    /**
     * Exception handlers for error handling
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return jwtUtils.createBadRequestResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        return jwtUtils.createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
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