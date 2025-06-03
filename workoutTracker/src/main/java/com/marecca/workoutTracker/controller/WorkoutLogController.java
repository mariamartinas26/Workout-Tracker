package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.*;
import com.marecca.workoutTracker.dto.request.BatchLogExercisesRequest;
import com.marecca.workoutTracker.dto.request.LogExerciseRequest;
import com.marecca.workoutTracker.dto.response.*;
import com.marecca.workoutTracker.entity.WorkoutExerciseLog;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.service.WorkoutExerciseLogService;
import com.marecca.workoutTracker.service.ScheduledWorkoutService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller for logging and viewing workouts - JWT Protected
 * Provides endpoints for managing exercise logs and workout history
 */
@RestController
@RequestMapping("/api/workout-logs")
@RequiredArgsConstructor
@Slf4j
public class WorkoutLogController {

    private final WorkoutExerciseLogService workoutExerciseLogService;
    private final ScheduledWorkoutService scheduledWorkoutService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Log an exercise in a workout (requires authentication)
     */
    @PostMapping("/exercises")
    public ResponseEntity<?> logExercise(@Valid @RequestBody LogExerciseRequest request, HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to log exercise by user: {}", authenticatedUserId);

            // Verify workout ownership before logging
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(request.getScheduledWorkoutId());
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to log exercise for workout {} owned by user {}",
                        authenticatedUserId, request.getScheduledWorkoutId(), workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only log exercises for your own workouts", HttpStatus.FORBIDDEN);
            }

            WorkoutExerciseLog exerciseLog = buildExerciseLogFromRequest(request);
            WorkoutExerciseLog savedLog = workoutExerciseLogService.logExercise(exerciseLog);

            LogExerciseResponse response = LogExerciseResponse.builder()
                    .logId(savedLog.getLogId())
                    .message("Exercise logged successfully")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error while logging exercise: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to log exercises");
        }
    }

    /**
     * Log multiple exercises for a workout (requires authentication)
     */
    @PostMapping("/exercises/batch")
    public ResponseEntity<?> logMultipleExercises(@Valid @RequestBody BatchLogExercisesRequest request, HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to log multiple exercises by user: {}", authenticatedUserId);

            // Verify all workouts belong to the user (assuming all exercises are for the same workout)
            if (!request.getExercises().isEmpty()) {
                Long workoutId = request.getExercises().get(0).getScheduledWorkoutId();
                Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
                if (workoutOpt.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }

                ScheduledWorkout workout = workoutOpt.get();
                if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                    log.warn("User {} attempted to log exercises for workout {} owned by user {}",
                            authenticatedUserId, workoutId, workout.getUser().getUserId());
                    return jwtUtils.createErrorResponse("You can only log exercises for your own workouts", HttpStatus.FORBIDDEN);
                }
            }

            List<WorkoutExerciseLog> savedLogs = request.getExercises().stream()
                    .map(exerciseReq -> {
                        WorkoutExerciseLog exerciseLog = buildExerciseLogFromRequest(exerciseReq);
                        return workoutExerciseLogService.logExercise(exerciseLog);
                    })
                    .toList();

            BatchLogExercisesResponse response = BatchLogExercisesResponse.builder()
                    .loggedCount(savedLogs.size())
                    .logIds(savedLogs.stream().map(WorkoutExerciseLog::getLogId).toList())
                    .message("Exercises logged successfully")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error while logging multiple exercises: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());

        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to log exercises");
        }
    }

    /**
     * Update an exercise log (requires authentication)
     */
    @PutMapping("/exercises/{logId}")
    public ResponseEntity<?> updateExerciseLog(
            @PathVariable Long logId,
            @Valid @RequestBody LogExerciseRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to update exercise log: {} by user: {}", logId, authenticatedUserId);

            // Verify log ownership
            Optional<WorkoutExerciseLog> existingLogOpt = workoutExerciseLogService.findById(logId);
            if (existingLogOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            WorkoutExerciseLog existingLog = existingLogOpt.get();
            if (!existingLog.getScheduledWorkout().getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to update exercise log {} owned by user {}",
                        authenticatedUserId, logId, existingLog.getScheduledWorkout().getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only update your own exercise logs", HttpStatus.FORBIDDEN);
            }

            WorkoutExerciseLog updatedLog = buildExerciseLogFromRequest(request);
            WorkoutExerciseLog savedLog = workoutExerciseLogService.updateExerciseLog(logId, updatedLog);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Exercise log updated successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error while updating exercise log: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to update exercise logs");
        }
    }

    /**
     * Find all workouts for a user (requires authentication)
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
     * Find workouts for a specified period (requires authentication)
     */
    @GetMapping("/user/{userId}/period")
    public ResponseEntity<?> getUserWorkoutsByPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get workouts by period for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access workouts for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own workouts", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserIdAndDateRange(
                    authenticatedUserId, startDate, endDate);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workouts");
        }
    }

    /**
     * Find today's workouts for a user (requires authentication)
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<?> getTodaysWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get today's workouts for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access workouts for user {}", authenticatedUserId, userId);
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
     * Find recently completed workouts (requires authentication)
     */
    @GetMapping("/user/{userId}/recent-completed")
    public ResponseEntity<?> getRecentCompletedWorkouts(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get recent completed workouts for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access workouts for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own workouts", HttpStatus.FORBIDDEN);
            }

            List<ScheduledWorkout> workouts = scheduledWorkoutService.findRecentCompletedWorkouts(authenticatedUserId);
            return ResponseEntity.ok(workouts);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access completed workouts");
        }
    }

    /**
     * Find details of a specific workout (requires authentication)
     */
    @GetMapping("/workout/{scheduledWorkoutId}/details")
    public ResponseEntity<?> getWorkoutDetails(@PathVariable Long scheduledWorkoutId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get workout details: {} by user: {}", scheduledWorkoutId, authenticatedUserId);

            // Find the scheduled workout and verify ownership
            ScheduledWorkout workout = scheduledWorkoutService.findById(scheduledWorkoutId)
                    .orElse(null);

            if (workout == null) {
                return ResponseEntity.notFound().build();
            }

            if (!workout.getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to access workout details {} owned by user {}",
                        authenticatedUserId, scheduledWorkoutId, workout.getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only access your own workout details", HttpStatus.FORBIDDEN);
            }

            // Find all logged exercises for the workout
            List<WorkoutExerciseLog> exerciseLogs = workoutExerciseLogService
                    .findByScheduledWorkoutId(scheduledWorkoutId);

            WorkoutDetailsResponse response = WorkoutDetailsResponse.builder()
                    .workout(workout)
                    .exerciseLogs(exerciseLogs)
                    .totalExercises(exerciseLogs.size())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout details");
        }
    }

    /**
     * Find all exercise logs for a user (requires authentication)
     */
    @GetMapping("/user/{userId}/exercise-logs")
    public ResponseEntity<?> getUserExerciseLogs(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercise logs for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access exercise logs for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own exercise logs", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> logs = workoutExerciseLogService.findByUserId(authenticatedUserId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercise logs");
        }
    }

    /**
     * Find exercise logs for a period (requires authentication)
     */
    @GetMapping("/user/{userId}/exercise-logs/period")
    public ResponseEntity<?> getUserExerciseLogsByPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercise logs by period for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access exercise logs for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own exercise logs", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> logs = workoutExerciseLogService.findByUserIdAndDateRange(
                    authenticatedUserId, startDate, endDate);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercise logs");
        }
    }

    /**
     * Find progress for a specific exercise (requires authentication)
     */
    @GetMapping("/user/{userId}/progress/exercise/{exerciseId}")
    public ResponseEntity<?> getExerciseProgress(
            @PathVariable Long userId,
            @PathVariable Long exerciseId,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercise progress for user: {} exercise: {} by authenticated user: {}",
                    userId, exerciseId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access progress for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own progress", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> progressLogs = workoutExerciseLogService
                    .findUserProgressForExercise(authenticatedUserId, exerciseId);

            BigDecimal personalBestWeight = workoutExerciseLogService
                    .findPersonalBestWeight(authenticatedUserId, exerciseId);

            Integer personalBestReps = workoutExerciseLogService
                    .findPersonalBestReps(authenticatedUserId, exerciseId);

            Double progressPercentage = workoutExerciseLogService
                    .calculateProgressPercentage(authenticatedUserId, exerciseId);

            ExerciseProgressResponse response = ExerciseProgressResponse.builder()
                    .progressLogs(progressLogs)
                    .personalBestWeight(personalBestWeight)
                    .personalBestReps(personalBestReps)
                    .progressPercentage(progressPercentage)
                    .totalSessions(progressLogs.size())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercise progress");
        }
    }

    /**
     * Calculate total volume for an exercise in a period (requires authentication)
     */
    @GetMapping("/user/{userId}/volume/exercise/{exerciseId}")
    public ResponseEntity<?> getExerciseVolume(
            @PathVariable Long userId,
            @PathVariable Long exerciseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercise volume for user: {} exercise: {} by authenticated user: {}",
                    userId, exerciseId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access volume for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own volume data", HttpStatus.FORBIDDEN);
            }

            BigDecimal totalVolume = workoutExerciseLogService.calculateTotalVolume(
                    authenticatedUserId, exerciseId, startDate, endDate);

            VolumeResponse response = VolumeResponse.builder()
                    .totalVolume(totalVolume != null ? totalVolume : BigDecimal.ZERO)
                    .exerciseId(exerciseId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access volume data");
        }
    }

    /**
     * Find the most recent exercise logs (requires authentication)
     */
    @GetMapping("/user/{userId}/recent-logs")
    public ResponseEntity<?> getRecentExerciseLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get recent exercise logs for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access recent logs for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own exercise logs", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> logs = workoutExerciseLogService.findRecentLogsForUser(authenticatedUserId, limit);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access recent logs");
        }
    }

    /**
     * Find best performing exercises (requires authentication)
     */
    @GetMapping("/user/{userId}/top-performing")
    public ResponseEntity<?> getTopPerformingExercises(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") @Min(1) int limit,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get top performing exercises for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access top performing exercises for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own performance data", HttpStatus.FORBIDDEN);
            }

            List<WorkoutExerciseLog> logs = workoutExerciseLogService.findTopPerformingExercises(authenticatedUserId, limit);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access performance data");
        }
    }

    /**
     * General statistics for user (requires authentication)
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

            // Find all completed workouts
            List<ScheduledWorkout> completedWorkouts = scheduledWorkoutService
                    .findByUserIdAndStatus(authenticatedUserId, com.marecca.workoutTracker.entity.enums.WorkoutStatusType.COMPLETED);

            // Calculate statistics
            Long totalWorkouts = scheduledWorkoutService.countCompletedWorkouts(authenticatedUserId);
            Double averageDuration = scheduledWorkoutService.getAverageWorkoutDuration(authenticatedUserId);

            // Calculate total calories
            Integer totalCalories = completedWorkouts.stream()
                    .mapToInt(w -> w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0)
                    .sum();

            // Find most recent logs for exercise diversity
            List<WorkoutExerciseLog> recentLogs = workoutExerciseLogService.findRecentLogsForUser(authenticatedUserId, 50);
            Long uniqueExercises = recentLogs.stream()
                    .map(log -> log.getExercise().getExerciseId())
                    .distinct()
                    .count();

            UserWorkoutStatisticsDTO stats = UserWorkoutStatisticsDTO.builder()
                    .totalCompletedWorkouts(totalWorkouts != null ? totalWorkouts : 0L)
                    .averageDurationMinutes(averageDuration != null ? averageDuration : 0.0)
                    .totalCaloriesBurned(totalCalories)
                    .uniqueExercisesTrained(uniqueExercises)
                    .build();

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access statistics");
        }
    }

    /**
     * Delete an exercise log (requires authentication)
     */
    @DeleteMapping("/exercises/{logId}")
    public ResponseEntity<?> deleteExerciseLog(@PathVariable Long logId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to delete exercise log: {} by user: {}", logId, authenticatedUserId);

            // Verify log ownership
            Optional<WorkoutExerciseLog> existingLogOpt = workoutExerciseLogService.findById(logId);
            if (existingLogOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            WorkoutExerciseLog existingLog = existingLogOpt.get();
            if (!existingLog.getScheduledWorkout().getUser().getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to delete exercise log {} owned by user {}",
                        authenticatedUserId, logId, existingLog.getScheduledWorkout().getUser().getUserId());
                return jwtUtils.createErrorResponse("You can only delete your own exercise logs", HttpStatus.FORBIDDEN);
            }

            workoutExerciseLogService.deleteExerciseLog(logId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Exercise log deleted successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error while deleting exercise log: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to delete exercise logs");
        }
    }

    /**
     * Get current user's workout statistics (convenience endpoint)
     */
    @GetMapping("/my-statistics")
    public ResponseEntity<?> getMyStatistics(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my statistics for user: {}", authenticatedUserId);

            // Redirect to the main statistics endpoint
            return getUserStatistics(authenticatedUserId, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access statistics");
        }
    }

    /**
     * Get current user's recent logs (convenience endpoint)
     */
    @GetMapping("/my-recent-logs")
    public ResponseEntity<?> getMyRecentLogs(
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my recent logs for user: {}", authenticatedUserId);

            return getRecentExerciseLogs(authenticatedUserId, limit, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access recent logs");
        }
    }

    private WorkoutExerciseLog buildExerciseLogFromRequest(LogExerciseRequest request) {
        return WorkoutExerciseLog.builder()
                .scheduledWorkout(ScheduledWorkout.builder()
                        .scheduledWorkoutId(request.getScheduledWorkoutId())
                        .build())
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