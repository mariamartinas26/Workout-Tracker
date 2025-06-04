package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.request.CompleteWorkoutRequest;
import com.marecca.workoutTracker.dto.request.ScheduleWorkoutRequest;
import com.marecca.workoutTracker.dto.request.SessionExerciseLogRequest;
import com.marecca.workoutTracker.dto.request.StartFreeWorkoutRequest;
import com.marecca.workoutTracker.dto.response.*;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.entity.WorkoutExerciseLog;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import com.marecca.workoutTracker.service.ScheduledWorkoutService;
import com.marecca.workoutTracker.service.WorkoutExerciseLogService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing complete workout sessions - JWT Protected
 * Provides a simplified workflow: schedule -> start -> log -> complete
 */
@RestController
@RequestMapping("/api/workout-sessions")
@RequiredArgsConstructor
@Slf4j
public class WorkoutSessionController {

    private final ScheduledWorkoutService scheduledWorkoutService;
    private final WorkoutExerciseLogService workoutExerciseLogService;
    private final JwtControllerUtils jwtUtils;

    /**
     * 2. Start a scheduled workout (requires authentication)
     */
    @PostMapping("/{workoutId}/start")
    public ResponseEntity<?> startWorkout(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to start workout: {} by user: {}", workoutId, authenticatedUserId);

            // Verify workout ownership before starting
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout existingWorkout = workoutOpt.get();
            if (!existingWorkout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to start workout {} owned by user {}",
                        authenticatedUserId, workoutId, existingWorkout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only start your own workouts", HttpStatus.FORBIDDEN);
            }

            ScheduledWorkout workout = scheduledWorkoutService.startWorkout(workoutId);

            WorkoutSessionResponse response = WorkoutSessionResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .startTime(workout.getActualStartTime())
                    .message("Workout started successfully")
                    .nextAction("Log exercises as you perform them")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Error starting workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to start workouts");
        }
    }

    /**
     * 3. Log an exercise during the workout (requires authentication)
     */
    @PostMapping("/{workoutId}/exercises")
    public ResponseEntity<?> logExerciseInSession(
            @PathVariable Long workoutId,
            @Valid @RequestBody SessionExerciseLogRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to log exercise in workout: {} by user: {}", workoutId, authenticatedUserId);

            // Check that workout exists and user owns it
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();

            // Verify ownership
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to log exercise in workout {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only log exercises for your own workouts", HttpStatus.FORBIDDEN);
            }

            // Check workout status
            if (workout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.builder()
                                .error("Workout not in progress")
                                .message("You can only log exercises for workouts that are in progress")
                                .build());
            }

            // Create exercise log
            WorkoutExerciseLog exerciseLog = WorkoutExerciseLog.builder()
                    .scheduledWorkout(workout)
                    .exercise(com.marecca.workoutTracker.entity.Exercise.builder()
                            .exerciseId(request.getExerciseId())
                            .build())
                    .exerciseOrder(request.getExerciseOrder())
                    .setsCompleted(request.getSetsCompleted())
                    .repsCompleted(request.getRepsCompleted())
                    .weightUsedKg(request.getWeightUsedKg())
                    .durationSeconds(request.getDurationSeconds())
                    .distanceMeters(request.getDistanceMeters())
                    .caloriesBurned(request.getCaloriesBurned())
                    .difficultyRating(request.getDifficultyRating())
                    .notes(request.getNotes())
                    .build();

            WorkoutExerciseLog savedLog = workoutExerciseLogService.logExercise(exerciseLog);

            // Count how many exercises have been logged
            long totalExercises = workoutExerciseLogService.countLogsByScheduledWorkout(workoutId);

            SessionExerciseLogResponse response = SessionExerciseLogResponse.builder()
                    .logId(savedLog.getLogId())
                    .exerciseOrder(savedLog.getExerciseOrder())
                    .totalExercisesLogged((int) totalExercises)
                    .message("Exercise logged successfully")
                    .nextAction("Continue with next exercise or complete workout")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Error logging exercise in session: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to log exercises");
        }
    }

    /**
     * 4. Complete the workout (requires authentication)
     */
    @PostMapping("/{workoutId}/complete")
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

            ScheduledWorkout existingWorkout = workoutOpt.get();
            if (!existingWorkout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to complete workout {} owned by user {}",
                        authenticatedUserId, workoutId, existingWorkout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only complete your own workouts", HttpStatus.FORBIDDEN);
            }

            ScheduledWorkout workout = scheduledWorkoutService.completeWorkout(
                    workoutId,
                    request.getTotalCaloriesBurned(),
                    request.getOverallRating()
            );

            // Find all logged exercises
            List<WorkoutExerciseLog> exerciseLogs = workoutExerciseLogService
                    .findByScheduledWorkoutId(workoutId);

            CompleteWorkoutResponse response = CompleteWorkoutResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .startTime(workout.getActualStartTime())
                    .endTime(workout.getActualEndTime())
                    .durationMinutes(workout.getActualDurationMinutes())
                    .totalExercises(exerciseLogs.size())
                    .totalCaloriesBurned(workout.getCaloriesBurned())
                    .overallRating(workout.getOverallRating())
                    .message("Workout completed successfully! Congratulations!")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Error completing workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to complete workouts");
        }
    }

    /**
     * 5. Cancel a workout (requires authentication)
     */
    @PostMapping("/{workoutId}/cancel")
    public ResponseEntity<?> cancelWorkout(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to cancel workout: {} by user: {}", workoutId, authenticatedUserId);

            // Verify workout ownership before cancelling
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout existingWorkout = workoutOpt.get();
            if (!existingWorkout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to cancel workout {} owned by user {}",
                        authenticatedUserId, workoutId, existingWorkout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only cancel your own workouts", HttpStatus.FORBIDDEN);
            }

            ScheduledWorkout workout = scheduledWorkoutService.cancelWorkout(workoutId);

            WorkoutSessionResponse response = WorkoutSessionResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .message("Workout cancelled")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Error cancelling workout: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to cancel workouts");
        }
    }

    /**
     * Check workout status (requires authentication)
     */
    @GetMapping("/{workoutId}/status")
    public ResponseEntity<?> getWorkoutStatus(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get workout status: {} by user: {}", workoutId, authenticatedUserId);

            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);

            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();

            // Verify ownership
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to access workout status {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only access your own workout status", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> exerciseLogs = workoutExerciseLogService
                    .findByScheduledWorkoutId(workoutId);

            WorkoutStatusResponse response = WorkoutStatusResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .scheduledDate(workout.getScheduledDate())
                    .scheduledTime(workout.getScheduledTime())
                    .startTime(workout.getActualStartTime())
                    .endTime(workout.getActualEndTime())
                    .durationMinutes(workout.getActualDurationMinutes())
                    .exercisesLogged(exerciseLogs.size())
                    .caloriesBurned(workout.getCaloriesBurned())
                    .overallRating(workout.getOverallRating())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout status");
        }
    }

    /**
     * Find all logged exercises for a workout (requires authentication)
     */
    @GetMapping("/{workoutId}/exercises")
    public ResponseEntity<?> getWorkoutExercises(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get workout exercises: {} by user: {}", workoutId, authenticatedUserId);

            // Check that workout exists and verify ownership
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to access exercises for workout {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only access exercises for your own workouts", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> exerciseLogs = workoutExerciseLogService
                    .findByScheduledWorkoutId(workoutId);

            return ResponseEntity.ok(exerciseLogs);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout exercises");
        }
    }

    /**
     * Remove an exercise from current session (requires authentication)
     */
    @DeleteMapping("/{workoutId}/exercises/{logId}")
    public ResponseEntity<?> removeExerciseFromSession(
            @PathVariable Long workoutId,
            @PathVariable Long logId,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to remove exercise {} from workout: {} by user: {}", logId, workoutId, authenticatedUserId);

            // Check that workout exists and verify ownership
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to modify workout {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only modify your own workouts", HttpStatus.FORBIDDEN);
            }

            if (workout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.builder()
                                .error("Operation not allowed")
                                .message("You can only delete exercises from workouts in progress")
                                .build());
            }

            workoutExerciseLogService.deleteExerciseLog(logId);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Exercise removed from session")
                            .build());

        } catch (IllegalArgumentException e) {
            log.error("Error removing exercise from session: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to modify workouts");
        }
    }

    /**
     * Quick summary of workout in progress (requires authentication)
     */
    @GetMapping("/{workoutId}/summary")
    public ResponseEntity<?> getWorkoutSummary(@PathVariable Long workoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get workout summary: {} by user: {}", workoutId, authenticatedUserId);

            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);

            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();

            // Verify ownership
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to access workout summary {} owned by user {}",
                        authenticatedUserId, workoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only access your own workout summary", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> exerciseLogs = workoutExerciseLogService
                    .findByScheduledWorkoutId(workoutId);

            int totalCaloriesFromExercises = exerciseLogs.stream()
                    .mapToInt(exerciseLog -> exerciseLog.getCaloriesBurned() != null ? exerciseLog.getCaloriesBurned() : 0)
                    .sum();

            int totalSets = exerciseLogs.stream()
                    .mapToInt(WorkoutExerciseLog::getSetsCompleted)
                    .sum();

            WorkoutSummaryResponse response = WorkoutSummaryResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .totalExercises(exerciseLogs.size())
                    .totalSets(totalSets)
                    .estimatedCalories(totalCaloriesFromExercises)
                    .elapsedMinutes(workout.getActualStartTime() != null ?
                            (int) java.time.Duration.between(workout.getActualStartTime(),
                                    LocalDateTime.now()).toMinutes() : 0)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout summary");
        }
    }

    /**
     * Get current user's active workouts (convenience endpoint)
     */
    @GetMapping("/my-active")
    public ResponseEntity<?> getMyActiveWorkouts(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get active workouts for user: {}", authenticatedUserId);

            // This would need to be implemented in the service layer
            // List<ScheduledWorkout> activeWorkouts = scheduledWorkoutService.findActiveByUserId(authenticatedUserId);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Active workouts endpoint - to be implemented")
                            .build());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access active workouts");
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