package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.*;
import com.marecca.workoutTracker.entity.WorkoutExerciseLog;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.service.WorkoutExerciseLogService;
import com.marecca.workoutTracker.service.ScheduledWorkoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller for logging and viewing workouts
 * Provides endpoints for managing exercise logs and workout history
 */
@RestController
@RequestMapping("/api/workout-logs")
@RequiredArgsConstructor
@Slf4j
public class WorkoutLogController {

    private final WorkoutExerciseLogService workoutExerciseLogService;
    private final ScheduledWorkoutService scheduledWorkoutService;

    /**
     * Log an exercise in a workout
     */
    @PostMapping("/exercises")
    public ResponseEntity<?> logExercise(@Valid @RequestBody LogExerciseRequest request) {
        try {
            WorkoutExerciseLog exerciseLog = buildExerciseLogFromRequest(request);
            WorkoutExerciseLog savedLog = workoutExerciseLogService.logExercise(exerciseLog);

            LogExerciseResponse response = LogExerciseResponse.builder()
                    .logId(savedLog.getLogId())
                    .message("Exercise logged successfully")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error while logging exercise: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Unexpected error while logging exercise: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Internal error")
                            .message("An unexpected error occurred")
                            .build());
        }
    }

    /**
     * Log multiple exercises for a workout
     */
    @PostMapping("/exercises/batch")
    public ResponseEntity<?> logMultipleExercises(@Valid @RequestBody BatchLogExercisesRequest request) {
        try {
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
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Unexpected error while logging multiple exercises: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Internal error")
                            .message("An unexpected error occurred")
                            .build());
        }
    }

    /**
     * Update an exercise log
     */
    @PutMapping("/exercises/{logId}")
    public ResponseEntity<?> updateExerciseLog(
            @PathVariable Long logId,
            @Valid @RequestBody LogExerciseRequest request) {
        try {
            WorkoutExerciseLog updatedLog = buildExerciseLogFromRequest(request);
            WorkoutExerciseLog savedLog = workoutExerciseLogService.updateExerciseLog(logId, updatedLog);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Exercise log updated successfully")
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
     * Find all workouts for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ScheduledWorkout>> getUserWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserId(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Find workouts for a specified period
     */
    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<ScheduledWorkout>> getUserWorkoutsByPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserIdAndDateRange(
                userId, startDate, endDate);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Find today's workouts for a user
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<List<ScheduledWorkout>> getTodaysWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findTodaysWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Find recently completed workouts
     */
    @GetMapping("/user/{userId}/recent-completed")
    public ResponseEntity<List<ScheduledWorkout>> getRecentCompletedWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findRecentCompletedWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Find details of a specific workout (with all exercises)
     */
    @GetMapping("/workout/{scheduledWorkoutId}/details")
    public ResponseEntity<WorkoutDetailsResponse> getWorkoutDetails(@PathVariable Long scheduledWorkoutId) {
        // Find the scheduled workout
        ScheduledWorkout workout = scheduledWorkoutService.findById(scheduledWorkoutId)
                .orElse(null);

        if (workout == null) {
            return ResponseEntity.notFound().build();
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
    }

    /**
     * Find all exercise logs for a user
     */
    @GetMapping("/user/{userId}/exercise-logs")
    public ResponseEntity<List<WorkoutExerciseLog>> getUserExerciseLogs(@PathVariable Long userId) {
        List<WorkoutExerciseLog> logs = workoutExerciseLogService.findByUserId(userId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Find exercise logs for a period
     */
    @GetMapping("/user/{userId}/exercise-logs/period")
    public ResponseEntity<List<WorkoutExerciseLog>> getUserExerciseLogsByPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<WorkoutExerciseLog> logs = workoutExerciseLogService.findByUserIdAndDateRange(
                userId, startDate, endDate);
        return ResponseEntity.ok(logs);
    }

    /**
     * Find progress for a specific exercise
     */
    @GetMapping("/user/{userId}/progress/exercise/{exerciseId}")
    public ResponseEntity<ExerciseProgressResponse> getExerciseProgress(
            @PathVariable Long userId,
            @PathVariable Long exerciseId) {

        List<WorkoutExerciseLog> progressLogs = workoutExerciseLogService
                .findUserProgressForExercise(userId, exerciseId);

        BigDecimal personalBestWeight = workoutExerciseLogService
                .findPersonalBestWeight(userId, exerciseId);

        Integer personalBestReps = workoutExerciseLogService
                .findPersonalBestReps(userId, exerciseId);

        Double progressPercentage = workoutExerciseLogService
                .calculateProgressPercentage(userId, exerciseId);

        ExerciseProgressResponse response = ExerciseProgressResponse.builder()
                .progressLogs(progressLogs)
                .personalBestWeight(personalBestWeight)
                .personalBestReps(personalBestReps)
                .progressPercentage(progressPercentage)
                .totalSessions(progressLogs.size())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Calculate total volume for an exercise in a period
     */
    @GetMapping("/user/{userId}/volume/exercise/{exerciseId}")
    public ResponseEntity<VolumeResponse> getExerciseVolume(
            @PathVariable Long userId,
            @PathVariable Long exerciseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        BigDecimal totalVolume = workoutExerciseLogService.calculateTotalVolume(
                userId, exerciseId, startDate, endDate);

        VolumeResponse response = VolumeResponse.builder()
                .totalVolume(totalVolume != null ? totalVolume : BigDecimal.ZERO)
                .exerciseId(exerciseId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Find the most recent exercise logs
     */
    @GetMapping("/user/{userId}/recent-logs")
    public ResponseEntity<List<WorkoutExerciseLog>> getRecentExerciseLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {

        List<WorkoutExerciseLog> logs = workoutExerciseLogService.findRecentLogsForUser(userId, limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * Find best performing exercises
     */
    @GetMapping("/user/{userId}/top-performing")
    public ResponseEntity<List<WorkoutExerciseLog>> getTopPerformingExercises(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") @Min(1) int limit) {

        List<WorkoutExerciseLog> logs = workoutExerciseLogService.findTopPerformingExercises(userId, limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * General statistics for user
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<UserWorkoutStatistics> getUserStatistics(@PathVariable Long userId) {
        // Find all completed workouts
        List<ScheduledWorkout> completedWorkouts = scheduledWorkoutService
                .findByUserIdAndStatus(userId, com.marecca.workoutTracker.entity.enums.WorkoutStatusType.COMPLETED);

        // Calculate statistics
        Long totalWorkouts = scheduledWorkoutService.countCompletedWorkouts(userId);
        Double averageDuration = scheduledWorkoutService.getAverageWorkoutDuration(userId);

        // Calculate total calories
        Integer totalCalories = completedWorkouts.stream()
                .mapToInt(w -> w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0)
                .sum();

        // Find most recent logs for exercise diversity
        List<WorkoutExerciseLog> recentLogs = workoutExerciseLogService.findRecentLogsForUser(userId, 50);
        Long uniqueExercises = recentLogs.stream()
                .map(log -> log.getExercise().getExerciseId())
                .distinct()
                .count();

        UserWorkoutStatistics stats = UserWorkoutStatistics.builder()
                .totalCompletedWorkouts(totalWorkouts != null ? totalWorkouts : 0L)
                .averageDurationMinutes(averageDuration != null ? averageDuration : 0.0)
                .totalCaloriesBurned(totalCalories)
                .uniqueExercisesTrained(uniqueExercises)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Delete an exercise log
     */
    @DeleteMapping("/exercises/{logId}")
    public ResponseEntity<?> deleteExerciseLog(@PathVariable Long logId) {
        try {
            workoutExerciseLogService.deleteExerciseLog(logId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Exercise log deleted successfully")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Validation error")
                            .message(e.getMessage())
                            .build());
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

}