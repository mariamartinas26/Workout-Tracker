package com.marecca.workoutTracker.controller;

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
 * Controller pentru înregistrarea și vizualizarea workout-urilor
 * Oferă endpoint-uri pentru gestionarea logurilor de exerciții și istoricul workout-urilor
 */
@RestController
@RequestMapping("/api/workout-logs")
@RequiredArgsConstructor
@Slf4j
public class WorkoutLogController {

    private final WorkoutExerciseLogService workoutExerciseLogService;
    private final ScheduledWorkoutService scheduledWorkoutService;

    // =============== ÎNREGISTRAREA WORKOUT-URILOR ===============

    /**
     * Înregistrează un exercițiu într-un workout
     */
    @PostMapping("/exercises")
    public ResponseEntity<?> logExercise(@Valid @RequestBody LogExerciseRequest request) {
        try {
            WorkoutExerciseLog exerciseLog = buildExerciseLogFromRequest(request);
            WorkoutExerciseLog savedLog = workoutExerciseLogService.logExercise(exerciseLog);

            LogExerciseResponse response = LogExerciseResponse.builder()
                    .logId(savedLog.getLogId())
                    .message("Exercițiu înregistrat cu succes")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error while logging exercise: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Unexpected error while logging exercise: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Eroare internă")
                            .message("A apărut o eroare neașteptată")
                            .build());
        }
    }

    /**
     * Înregistrează multiple exerciții pentru un workout (batch)
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
                    .message("Exerciții înregistrate cu succes")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Unexpected error while logging multiple exercises: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Eroare internă")
                            .message("A apărut o eroare neașteptată")
                            .build());
        }
    }

    /**
     * Actualizează un log de exercițiu
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
                            .message("Log de exercițiu actualizat cu succes")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());
        }
    }

    // =============== VIZUALIZAREA WORKOUT-URILOR ===============

    /**
     * Găsește toate workout-urile pentru un utilizator
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ScheduledWorkout>> getUserWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserId(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Găsește workout-urile pentru o perioadă specificată
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
     * Găsește workout-urile de astăzi pentru un utilizator
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<List<ScheduledWorkout>> getTodaysWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findTodaysWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Găsește workout-urile completate recent
     */
    @GetMapping("/user/{userId}/recent-completed")
    public ResponseEntity<List<ScheduledWorkout>> getRecentCompletedWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findRecentCompletedWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Găsește detaliile unui workout specific (cu toate exercițiile)
     */
    @GetMapping("/workout/{scheduledWorkoutId}/details")
    public ResponseEntity<WorkoutDetailsResponse> getWorkoutDetails(@PathVariable Long scheduledWorkoutId) {
        // Găsește workout-ul programat
        ScheduledWorkout workout = scheduledWorkoutService.findById(scheduledWorkoutId)
                .orElse(null);

        if (workout == null) {
            return ResponseEntity.notFound().build();
        }

        // Găsește toate exercițiile înregistrate pentru workout
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
     * Găsește toate logurile de exerciții pentru un utilizator
     */
    @GetMapping("/user/{userId}/exercise-logs")
    public ResponseEntity<List<WorkoutExerciseLog>> getUserExerciseLogs(@PathVariable Long userId) {
        List<WorkoutExerciseLog> logs = workoutExerciseLogService.findByUserId(userId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Găsește logurile de exerciții pentru o perioadă
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
     * Găsește progresul pentru un exercițiu specific
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
     * Calculează volumul total pentru un exercițiu într-o perioadă
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
     * Găsește cele mai recente loguri de exerciții
     */
    @GetMapping("/user/{userId}/recent-logs")
    public ResponseEntity<List<WorkoutExerciseLog>> getRecentExerciseLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {

        List<WorkoutExerciseLog> logs = workoutExerciseLogService.findRecentLogsForUser(userId, limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * Găsește exercițiile cu cele mai bune performanțe
     */
    @GetMapping("/user/{userId}/top-performing")
    public ResponseEntity<List<WorkoutExerciseLog>> getTopPerformingExercises(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") @Min(1) int limit) {

        List<WorkoutExerciseLog> logs = workoutExerciseLogService.findTopPerformingExercises(userId, limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * Statistici generale pentru utilizator
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<UserWorkoutStatistics> getUserStatistics(@PathVariable Long userId) {
        // Găsește toate workout-urile completate
        List<ScheduledWorkout> completedWorkouts = scheduledWorkoutService
                .findByUserIdAndStatus(userId, com.marecca.workoutTracker.entity.enums.WorkoutStatusType.COMPLETED);

        // Calculează statistici
        Long totalWorkouts = scheduledWorkoutService.countCompletedWorkouts(userId);
        Double averageDuration = scheduledWorkoutService.getAverageWorkoutDuration(userId);

        // Calculează caloriile totale
        Integer totalCalories = completedWorkouts.stream()
                .mapToInt(w -> w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0)
                .sum();

        // Găsește cele mai recente loguri pentru diversitate exerciții
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
     * Șterge un log de exercițiu
     */
    @DeleteMapping("/exercises/{logId}")
    public ResponseEntity<?> deleteExerciseLog(@PathVariable Long logId) {
        try {
            workoutExerciseLogService.deleteExerciseLog(logId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Log de exercițiu șters cu succes")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());
        }
    }

    // =============== METODE HELPER ===============

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

    // =============== DTO CLASSES ===============

    @lombok.Data
    @lombok.Builder
    public static class LogExerciseRequest {
        @NotNull(message = "ID-ul workout-ului programat este obligatoriu")
        private Long scheduledWorkoutId;

        @NotNull(message = "ID-ul exercițiului este obligatoriu")
        private Long exerciseId;

        @NotNull(message = "Ordinea exercițiului este obligatorie")
        @Positive(message = "Ordinea exercițiului trebuie să fie pozitivă")
        private Integer exerciseOrder;

        @NotNull(message = "Numărul de seturi completate este obligatoriu")
        @Min(value = 0, message = "Numărul de seturi nu poate fi negativ")
        private Integer setsCompleted;

        @Min(value = 0, message = "Numărul de repetări nu poate fi negativ")
        private Integer repsCompleted;

        private BigDecimal weightUsedKg;

        @Min(value = 0, message = "Durata nu poate fi negativă")
        private Integer durationSeconds;

        private BigDecimal distanceMeters;

        @Min(value = 0, message = "Caloriile nu pot fi negative")
        private Integer caloriesBurned;

        @Min(value = 1, message = "Rating-ul trebuie să fie între 1 și 5")
        @jakarta.validation.constraints.Max(value = 5, message = "Rating-ul trebuie să fie între 1 și 5")
        private Integer difficultyRating;

        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchLogExercisesRequest {
        @NotNull(message = "Lista de exerciții este obligatorie")
        private List<LogExerciseRequest> exercises;
    }

    @lombok.Data
    @lombok.Builder
    public static class LogExerciseResponse {
        private Long logId;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchLogExercisesResponse {
        private Integer loggedCount;
        private List<Long> logIds;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkoutDetailsResponse {
        private ScheduledWorkout workout;
        private List<WorkoutExerciseLog> exerciseLogs;
        private Integer totalExercises;
    }

    @lombok.Data
    @lombok.Builder
    public static class ExerciseProgressResponse {
        private List<WorkoutExerciseLog> progressLogs;
        private BigDecimal personalBestWeight;
        private Integer personalBestReps;
        private Double progressPercentage;
        private Integer totalSessions;
    }

    @lombok.Data
    @lombok.Builder
    public static class VolumeResponse {
        private BigDecimal totalVolume;
        private Long exerciseId;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserWorkoutStatistics {
        private Long totalCompletedWorkouts;
        private Double averageDurationMinutes;
        private Integer totalCaloriesBurned;
        private Long uniqueExercisesTrained;
    }

    @lombok.Data
    @lombok.Builder
    public static class SuccessResponse {
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private String error;
        private String message;
    }
}