package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.entity.WorkoutPlan;
import com.marecca.workoutTracker.entity.WorkoutExerciseDetail;
import com.marecca.workoutTracker.service.WorkoutPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controller pentru gestionarea planurilor de workout
 * Oferă endpoint-uri pentru crearea, actualizarea și vizualizarea planurilor
 */
@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
@Slf4j
public class WorkoutPlanController {

    private final WorkoutPlanService workoutPlanService;

    /**
     * Creează un plan de workout nou (aceasta e cea mai importantă metodă pentru problema ta!)
     */
    @PostMapping
    public ResponseEntity<?> createWorkoutPlan(@Valid @RequestBody CreateWorkoutPlanRequest request) {
        try {
            // Creează planul principal
            User user = new User();
            user.setUserId(request.getUserId());

            WorkoutPlan workoutPlan = WorkoutPlan.builder()
                    .user(user)
                    .planName(request.getPlanName())
                    .description(request.getDescription())
                    .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                    .difficultyLevel(request.getDifficultyLevel())
                    .goals(request.getGoals())
                    .notes(request.getNotes())
                    .build();

            WorkoutPlan savedPlan = workoutPlanService.createWorkoutPlan(workoutPlan);

            CreateWorkoutPlanResponse response = CreateWorkoutPlanResponse.builder()
                    .workoutPlanId(savedPlan.getWorkoutPlanId())
                    .planName(savedPlan.getPlanName())
                    .totalExercises(request.getExercises() != null ? request.getExercises().size() : 0)
                    .message("Plan de workout creat cu succes")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating workout plan: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la crearea planului")
                            .message(e.getMessage())
                            .build());
        }
    }


    /**
     * Găsește toate planurile pentru un utilizator
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkoutPlan>> getUserWorkoutPlans(@PathVariable Long userId) {
        List<WorkoutPlan> plans = workoutPlanService.findByUserId(userId);
        return ResponseEntity.ok(plans);
    }

    /**
     * Găsește un plan specific cu toate detaliile exercițiilor
     */
    @GetMapping("/{planId}")
    public ResponseEntity<?> getWorkoutPlanDetails(@PathVariable Long planId) {
        Optional<WorkoutPlan> planOpt = workoutPlanService.findById(planId);

        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WorkoutPlan plan = planOpt.get();

        WorkoutPlanDetailsResponse response = WorkoutPlanDetailsResponse.builder()
                .workoutPlan(plan)
                .totalExercises(0) // Pentru moment, nu avem exercițiile
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Actualizează un plan de workout
     */
    @PutMapping("/{planId}")
    public ResponseEntity<?> updateWorkoutPlan(
            @PathVariable Long planId,
            @Valid @RequestBody UpdateWorkoutPlanRequest request) {
        try {
            // Găsește planul existent
            WorkoutPlan existingPlan = workoutPlanService.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Planul nu a fost găsit"));

            // Creează obiectul pentru actualizare
            WorkoutPlan updatedPlan = WorkoutPlan.builder()
                    .planName(request.getPlanName())
                    .description(request.getDescription())
                    .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                    .difficultyLevel(request.getDifficultyLevel())
                    .goals(request.getGoals())
                    .notes(request.getNotes())
                    .build();

            workoutPlanService.updateWorkoutPlan(planId, updatedPlan);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Plan actualizat cu succes")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la actualizarea planului")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Șterge un plan de workout
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<?> deleteWorkoutPlan(@PathVariable Long planId) {
        try {
            workoutPlanService.deleteWorkoutPlan(planId);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Plan șters cu succes")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la ștergerea planului")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Adaugă un exercițiu la un plan existent
     */
    @PostMapping("/{planId}/exercises")
    public ResponseEntity<?> addExerciseToWorkoutPlan(
            @PathVariable Long planId,
            @Valid @RequestBody ExerciseDetailRequest request) {
        try {
            // Pentru moment, returnează doar un mesaj de succes
            // Această funcționalitate poate fi implementată mai târziu
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(SuccessResponse.builder()
                            .message("Funcționalitatea va fi implementată în curând")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la adăugarea exercițiului")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Elimină un exercițiu din plan
     */
    @DeleteMapping("/{planId}/exercises/{detailId}")
    public ResponseEntity<?> removeExerciseFromWorkoutPlan(
            @PathVariable Long planId,
            @PathVariable Long detailId) {
        try {
            // Pentru moment, returnează doar un mesaj de succes
            // Această funcționalitate poate fi implementată mai târziu
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Funcționalitatea va fi implementată în curând")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare la eliminarea exercițiului")
                            .message(e.getMessage())
                            .build());
        }
    }

    // =============== DTO CLASSES ===============

    @lombok.Data
    @lombok.Builder
    public static class CreateWorkoutPlanRequest {
        @NotNull(message = "ID-ul utilizatorului este obligatoriu")
        private Long userId;

        @NotBlank(message = "Numele planului este obligatoriu")
        private String planName;

        private String description;

        @Min(value = 1, message = "Durata estimată trebuie să fie pozitivă")
        private Integer estimatedDurationMinutes;

        @Min(value = 1, message = "Nivelul de dificultate trebuie să fie între 1 și 5")
        @Max(value = 5, message = "Nivelul de dificultate trebuie să fie între 1 și 5")
        private Integer difficultyLevel;

        private String goals;
        private String notes;
        private List<ExerciseDetailRequest> exercises;
    }

    @lombok.Data
    @lombok.Builder
    public static class UpdateWorkoutPlanRequest {
        private String planName;
        private String description;
        private Integer estimatedDurationMinutes;
        private Integer difficultyLevel;
        private String goals;
        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    public static class ExerciseDetailRequest {
        @NotNull(message = "ID-ul exercițiului este obligatoriu")
        private Long exerciseId;

        private Integer exerciseOrder;

        @NotNull(message = "Numărul de seturi este obligatoriu")
        @Min(value = 1, message = "Numărul de seturi trebuie să fie pozitiv")
        private Integer targetSets;

        @Min(value = 1, message = "Repetările minime trebuie să fie pozitive")
        private Integer targetRepsMin;

        private Integer targetRepsMax;
        private BigDecimal targetWeightKg;
        private Integer targetDurationSeconds;
        private BigDecimal targetDistanceMeters;

        @Min(value = 0, message = "Timpul de odihnă nu poate fi negativ")
        private Integer restTimeSeconds = 60;

        private String notes;
    }

    @lombok.Data
    @lombok.Builder
    public static class CreateWorkoutPlanResponse {
        private Long workoutPlanId;
        private String planName;
        private Integer totalExercises;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class WorkoutPlanDetailsResponse {
        private WorkoutPlan workoutPlan;
        private Integer totalExercises;
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