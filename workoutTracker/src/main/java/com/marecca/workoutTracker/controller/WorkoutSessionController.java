package com.marecca.workoutTracker.controller;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller pentru gestionarea sesiunilor complete de workout
 * Oferă un workflow simplificat: programare -> început -> înregistrare -> finalizare
 */
@RestController
@RequestMapping("/api/workout-sessions")
@RequiredArgsConstructor
@Slf4j
public class WorkoutSessionController {

    private final ScheduledWorkoutService scheduledWorkoutService;
    private final WorkoutExerciseLogService workoutExerciseLogService;


    /**
     * 1. Programează un workout nou (punct de plecare)
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
                    .message("Workout programat cu succes")
                    .nextAction("Începe workout-ul când ești gata")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error scheduling workout: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la programare")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 2. Începe un workout programat
     */
    @PostMapping("/{workoutId}/start")
    public ResponseEntity<?> startWorkout(@PathVariable Long workoutId) {
        try {
            ScheduledWorkout workout = scheduledWorkoutService.startWorkout(workoutId);

            WorkoutSessionResponse response = WorkoutSessionResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .startTime(workout.getActualStartTime())
                    .message("Workout început cu succes")
                    .nextAction("Înregistrează exercițiile pe măsură ce le faci")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la începerea workout-ului")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 3. Înregistrează un exercițiu în timpul workout-ului
     */
    @PostMapping("/{workoutId}/exercises")
    public ResponseEntity<?> logExerciseInSession(
            @PathVariable Long workoutId,
            @Valid @RequestBody SessionExerciseLogRequest request) {
        try {
            // Verifică că workout-ul este în progres
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (workout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.builder()
                                .error("Workout nu este în progres")
                                .message("Poți înregistra exerciții doar pentru workout-uri în progres")
                                .build());
            }

            // Creează log-ul de exercițiu
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

            // Numără câte exerciții au fost înregistrate
            long totalExercises = workoutExerciseLogService.countLogsByScheduledWorkout(workoutId);

            SessionExerciseLogResponse response = SessionExerciseLogResponse.builder()
                    .logId(savedLog.getLogId())
                    .exerciseOrder(savedLog.getExerciseOrder())
                    .totalExercisesLogged((int) totalExercises)
                    .message("Exercițiu înregistrat cu succes")
                    .nextAction("Continuă cu următorul exercițiu sau finalizează workout-ul")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error logging exercise in session: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la înregistrarea exercițiului")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 4. Finalizează workout-ul
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

            // Găsește toate exercițiile înregistrate
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
                    .message("Workout finalizat cu succes! Felicitări!")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la finalizarea workout-ului")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 5. Anulează un workout
     */
    @PostMapping("/{workoutId}/cancel")
    public ResponseEntity<?> cancelWorkout(@PathVariable Long workoutId) {
        try {
            ScheduledWorkout workout = scheduledWorkoutService.cancelWorkout(workoutId);

            WorkoutSessionResponse response = WorkoutSessionResponse.builder()
                    .workoutId(workoutId)
                    .status(workout.getStatus())
                    .message("Workout anulat")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la anularea workout-ului")
                            .message(e.getMessage())
                            .build());
        }
    }

    // =============== STATUSUL SESIUNII ===============

    /**
     * Verifică statusul unui workout
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
     * Găsește toate exercițiile înregistrate pentru un workout
     */
    @GetMapping("/{workoutId}/exercises")
    public ResponseEntity<List<WorkoutExerciseLog>> getWorkoutExercises(@PathVariable Long workoutId) {
        // Verifică că workout-ul există
        if (scheduledWorkoutService.findById(workoutId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<WorkoutExerciseLog> exerciseLogs = workoutExerciseLogService
                .findByScheduledWorkoutId(workoutId);

        return ResponseEntity.ok(exerciseLogs);
    }

    /**
     * Șterge un exercițiu din sesiunea curentă
     */
    @DeleteMapping("/{workoutId}/exercises/{logId}")
    public ResponseEntity<?> removeExerciseFromSession(
            @PathVariable Long workoutId,
            @PathVariable Long logId) {
        try {
            // Verifică că workout-ul este în progres
            Optional<ScheduledWorkout> workoutOpt = scheduledWorkoutService.findById(workoutId);
            if (workoutOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ScheduledWorkout workout = workoutOpt.get();
            if (workout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.builder()
                                .error("Operație nepermisă")
                                .message("Poți șterge exerciții doar din workout-uri în progres")
                                .build());
            }

            workoutExerciseLogService.deleteExerciseLog(logId);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Exercițiu șters din sesiune")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la ștergerea exercițiului")
                            .message(e.getMessage())
                            .build());
        }
    }

    // =============== STATISTICI RAPIDE ===============

    /**
     * Sumar rapid al workout-ului în progres
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

        // Calculează statistici
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
     * Începe un workout liber (fără plan prestabilit)
     */
    @PostMapping("/start-free")
    public ResponseEntity<?> startFreeWorkout(@Valid @RequestBody StartFreeWorkoutRequest request) {
        try {
            // Creează un ScheduledWorkout fără plan
            // Creează obiectul User simplu, fără builder
            User user = new User();
            user.setUserId(request.getUserId());

            ScheduledWorkout freeWorkout = ScheduledWorkout.builder()
                    .user(user)
                    .scheduledDate(LocalDate.now())
                    .scheduledTime(LocalTime.now())
                    .notes(request.getNotes())
                    .build();


            // Salvează workout-ul
            ScheduledWorkout savedWorkout = scheduledWorkoutService.scheduleWorkout(freeWorkout);

            // Începe imediat workout-ul
            ScheduledWorkout startedWorkout = scheduledWorkoutService.startWorkout(savedWorkout.getScheduledWorkoutId());

            FreeWorkoutResponse response = FreeWorkoutResponse.builder()
                    .workoutId(startedWorkout.getScheduledWorkoutId())
                    .status(startedWorkout.getStatus())
                    .startTime(startedWorkout.getActualStartTime())
                    .message("Workout liber început cu succes")
                    .nextAction("Înregistrează exercițiile pe măsură ce le faci")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error starting free workout: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la începerea workout-ului liber")
                            .message(e.getMessage())
                            .build());
        }
    }

    // =============== DTO CLASSES ===============

    @lombok.Data
    @lombok.Builder
    public static class ScheduleWorkoutRequest {
        @NotNull(message = "ID-ul utilizatorului este obligatoriu")
        private Long userId;

        @NotNull(message = "ID-ul planului de workout este obligatoriu")
        private Long workoutPlanId;

        @NotNull(message = "Data programată este obligatorie")
        private LocalDate scheduledDate;

        private LocalTime scheduledTime;
    }

    @lombok.Data
    @lombok.Builder
    public static class StartFreeWorkoutRequest {
        @NotNull(message = "ID-ul utilizatorului este obligatoriu")
        private Long userId;

        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    public static class SessionExerciseLogRequest {
        @NotNull(message = "ID-ul exercițiului este obligatoriu")
        private Long exerciseId;

        @NotNull(message = "Ordinea exercițiului este obligatorie")
        @Min(value = 1, message = "Ordinea trebuie să fie pozitivă")
        private Integer exerciseOrder;

        @NotNull(message = "Numărul de seturi este obligatoriu")
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
        @Max(value = 5, message = "Rating-ul trebuie să fie între 1 și 5")
        private Integer difficultyRating;

        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    public static class CompleteWorkoutRequest {
        @Min(value = 0, message = "Caloriile nu pot fi negative")
        private Integer totalCaloriesBurned;

        @Min(value = 1, message = "Rating-ul trebuie să fie între 1 și 5")
        @Max(value = 5, message = "Rating-ul trebuie să fie între 1 și 5")
        private Integer overallRating;

        @Min(value = 1, message = "Nivelul de energie trebuie să fie între 1 și 5")
        @Max(value = 5, message = "Nivelul de energie trebuie să fie între 1 și 5")
        private Integer energyLevelAfter;

        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    public static class ScheduleWorkoutResponse {
        private Long scheduledWorkoutId;
        private WorkoutStatusType status;
        private String message;
        private String nextAction;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkoutSessionResponse {
        private Long workoutId;
        private WorkoutStatusType status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String message;
        private String nextAction;
    }

    @lombok.Data
    @lombok.Builder
    public static class SessionExerciseLogResponse {
        private Long logId;
        private Integer exerciseOrder;
        private Integer totalExercisesLogged;
        private String message;
        private String nextAction;
    }

    @lombok.Data
    @lombok.Builder
    public static class CompleteWorkoutResponse {
        private Long workoutId;
        private WorkoutStatusType status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes;
        private Integer totalExercises;
        private Integer totalCaloriesBurned;
        private Integer overallRating;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkoutStatusResponse {
        private Long workoutId;
        private WorkoutStatusType status;
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes;
        private Integer exercisesLogged;
        private Integer caloriesBurned;
        private Integer overallRating;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkoutSummaryResponse {
        private Long workoutId;
        private WorkoutStatusType status;
        private Integer totalExercises;
        private Integer totalSets;
        private Integer estimatedCalories;
        private Integer elapsedMinutes;
    }

    @lombok.Data
    @lombok.Builder
    public static class FreeWorkoutResponse {
        private Long workoutId;
        private WorkoutStatusType status;
        private LocalDateTime startTime;
        private String message;
        private String nextAction;
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