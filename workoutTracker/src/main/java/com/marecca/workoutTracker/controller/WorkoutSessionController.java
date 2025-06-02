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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing complete workout sessions
 * Provides a simplified workflow: schedule -> start -> log -> complete
 */
@RestController
@RequestMapping("/api/workout-sessions")
@RequiredArgsConstructor
@Slf4j
public class WorkoutSessionController {

    private final ScheduledWorkoutService scheduledWorkoutService;
    private final WorkoutExerciseLogService workoutExerciseLogService;

    /**
     * 1. Schedule a new workout (starting point)
     */
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleWorkout(@Valid @RequestBody ScheduleWorkoutRequest request) {
        try {
            Long scheduledWorkoutId = scheduledWorkoutService.scheduleWorkoutWithValidation(
                    request.getUserId(),
                    request.getWorkoutPlanId(),
                    request.getScheduledDate(),
                    request.getScheduledTime()
            );

            ScheduleWorkoutResponse response = ScheduleWorkoutResponse.builder()
                    .scheduledWorkoutId(scheduledWorkoutId)
                    .status(WorkoutStatusType.PLANNED)
                    .message("Workout scheduled successfully")
                    .nextAction("Start workout when you're ready")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error scheduling workout: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Scheduling error")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 2. Start a scheduled workout
     */
    @PostMapping("/{workoutId}/start")
    public ResponseEntity<?> startWorkout(@PathVariable Long workoutId) {
        try {
            ScheduledWorkout workout = scheduledWorkoutService.startWorkout(workoutId);

            WorkoutSessionResponse response = WorkoutSessionResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .startTime(workout.getActualStartTime())
                    .message("Workout started successfully")
                    .nextAction("Log exercises as you perform them")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error starting workout")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 3. Log an exercise during the workout
     */
    @PostMapping("/{workoutId}/exercises")
    public ResponseEntity<?> logExerciseInSession(
            @PathVariable Long workoutId,
            @Valid @RequestBody SessionExerciseLogRequest request) {
        try {
            // Check that workout is in progress
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
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

        } catch (Exception e) {
            log.error("Error logging exercise in session: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error logging exercise")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 4. Complete the workout
     */
    @PostMapping("/{workoutId}/complete")
    public ResponseEntity<?> completeWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody CompleteWorkoutRequest request) {
        try {
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

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error completing workout")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 5. Cancel a workout
     */
    @PostMapping("/{workoutId}/cancel")
    public ResponseEntity<?> cancelWorkout(@PathVariable Long workoutId) {
        try {
            ScheduledWorkout workout = scheduledWorkoutService.cancelWorkout(workoutId);

            WorkoutSessionResponse response = WorkoutSessionResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .message("Workout cancelled")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error cancelling workout")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Check workout status
     */
    @GetMapping("/{workoutId}/status")
    public ResponseEntity<?> getWorkoutStatus(@PathVariable Long workoutId) {
        Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);

        if (workoutOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ScheduledWorkout workout = workoutOpt.get();
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
    }

    /**
     * Find all logged exercises for a workout
     */
    @GetMapping("/{workoutId}/exercises")
    public ResponseEntity<List<WorkoutExerciseLog>> getWorkoutExercises(@PathVariable Long workoutId) {
        // Check that workout exists
        if (scheduledWorkoutService.findById(workoutId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<WorkoutExerciseLog> exerciseLogs = workoutExerciseLogService
                .findByScheduledWorkoutId(workoutId);

        return ResponseEntity.ok(exerciseLogs);
    }

    /**
     * Remove an exercise from current session
     */
    @DeleteMapping("/{workoutId}/exercises/{logId}")
    public ResponseEntity<?> removeExerciseFromSession(
            @PathVariable Long workoutId,
            @PathVariable Long logId) {
        try {
            // Check that workout is in progress
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
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

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error removing exercise")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Quick summary of workout in progress
     */
    @GetMapping("/{workoutId}/summary")
    public ResponseEntity<?> getWorkoutSummary(@PathVariable Long workoutId) {
        Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);

        if (workoutOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ScheduledWorkout workout = workoutOpt.get();
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
    }

    /**
     * Start a free workout (without a pre-established plan)
     */
    @PostMapping("/start-free")
    public ResponseEntity<?> startFreeWorkout(@Valid @RequestBody StartFreeWorkoutRequest request) {
        try {

            User user = new User();
            user.setUserId(request.getUserId());

            ScheduledWorkout freeWorkout = ScheduledWorkout.builder()
                    .user(user)
                    .scheduledDate(LocalDate.now())
                    .scheduledTime(LocalTime.now())
                    .notes(request.getNotes())
                    .build();

            // Save the workout
            ScheduledWorkout savedWorkout = scheduledWorkoutService.scheduleWorkout(freeWorkout);

            // Start the workout immediately
            ScheduledWorkout startedWorkout = scheduledWorkoutService.startWorkout(savedWorkout.getScheduledWorkoutId());

            FreeWorkoutResponse response = FreeWorkoutResponse.builder()
                    .workoutId(startedWorkout.getScheduledWorkoutId())
                    .status(startedWorkout.getStatus())
                    .startTime(startedWorkout.getActualStartTime())
                    .message("Free workout started successfully")
                    .nextAction("Log exercises as you perform them")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error starting free workout: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error starting free workout")
                            .message(e.getMessage())
                            .build());
        }
    }

}