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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for managing scheduled workouts
 */
@RestController
@RequestMapping("/api/scheduled-workouts")
@RequiredArgsConstructor
@Slf4j
public class ScheduledWorkoutController {

    private final ScheduledWorkoutService scheduledWorkoutService;

    /**
     * Schedule a new workout
     */
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleWorkout(@Valid @RequestBody ScheduleWorkoutRequest request) {
        try {
            Long scheduledWorkoutId = scheduledWorkoutService.scheduleWorkoutWithFunction(
                    request.getUserId(),
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .error("User not found")
                            .message(e.getMessage())
                            .build());

        } catch (WorkoutPlanNotFoundException e) {
            log.error("Workout plan not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .error("Workout plan not found")
                            .message(e.getMessage())
                            .build());

        } catch (WorkoutAlreadyScheduledException e) {
            log.error("Workout already scheduled: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.builder()
                            .error("Workout already scheduled")
                            .message(e.getMessage())
                            .build());

        } catch (IllegalArgumentException e) {
            log.error("Validation error while scheduling workout: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());

        } catch (RuntimeException e) {
            log.error("Database error while scheduling workout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Database error")
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Unexpected error while scheduling workout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Internal error")
                            .message("An unexpected error occurred")
                            .build());
        }
    }

    /**
     * Find all scheduled workouts for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ScheduledWorkout>> getUserWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserId(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Shows the scheduled workouts for today for a user
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<List<ScheduledWorkout>> getTodaysWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findTodaysWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Start a workout
     */
    @PutMapping("/{workoutId}/start")
    public ResponseEntity<?> startWorkout(@PathVariable Long workoutId) {
        try {
            scheduledWorkoutService.startWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout started successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Complete a workout
     */
    @PutMapping("/{workoutId}/complete")
    public ResponseEntity<?> completeWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody CompleteWorkoutRequest request) {
        try {
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
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Cancel a workout
     */
    @PutMapping("/{workoutId}/cancel")
    public ResponseEntity<?> cancelWorkout(@PathVariable Long workoutId) {
        try {
            scheduledWorkoutService.cancelWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout cancelled successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Check if a user can schedule a workout at a specific date/time
     */
    @GetMapping("/user/{userId}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {

        boolean available = scheduledWorkoutService.canScheduleWorkoutAt(userId, date, time);

        AvailabilityResponse response = AvailabilityResponse.builder()
                .available(available)
                .date(date)
                .time(time)
                .message(available ? "Slot available" : "Slot occupied")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Shows the recently completed workouts
     */
    @GetMapping("/user/{userId}/recent-completed")
    public ResponseEntity<List<ScheduledWorkout>> getRecentCompletedWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findRecentCompletedWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Shows how many completed workouts are for a user
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<ScheduledWorkoutService.WorkoutStatistics> getUserStatistics(@PathVariable Long userId) {
        ScheduledWorkoutService.WorkoutStatistics stats = scheduledWorkoutService.getUserWorkoutStatistics(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Reschedule an existing workout
     */
    @PutMapping("/{workoutId}/reschedule")
    public ResponseEntity<?> rescheduleWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody RescheduleWorkoutRequest request) {
        try {
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
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error while rescheduling workout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Internal error")
                            .message("An unexpected error occurred")
                            .build());
        }
    }


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