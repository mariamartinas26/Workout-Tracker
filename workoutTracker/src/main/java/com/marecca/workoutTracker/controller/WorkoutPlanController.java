package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.request.CreateWorkoutPlanRequest;
import com.marecca.workoutTracker.dto.request.ExerciseDetailRequest;
import com.marecca.workoutTracker.dto.request.UpdateWorkoutPlanRequest;
import com.marecca.workoutTracker.dto.response.CreateWorkoutPlanResponse;
import com.marecca.workoutTracker.dto.response.ErrorResponse;
import com.marecca.workoutTracker.dto.response.SuccessResponse;
import com.marecca.workoutTracker.dto.response.WorkoutPlanDetailsResponse;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.entity.WorkoutPlan;
import com.marecca.workoutTracker.service.WorkoutPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing workout plans
 * Provides endpoints for creating, updating and viewing workout plans
 */
@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
@Slf4j
public class WorkoutPlanController {

    private final WorkoutPlanService workoutPlanService;

    /**
     * Create a new workout plan
     */
    @PostMapping
    public ResponseEntity<?> createWorkoutPlan(@Valid @RequestBody CreateWorkoutPlanRequest request) {
        try {
            // Create the main plan
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

            WorkoutPlan savedPlan = workoutPlanService.createWorkoutPlanWithExercises(
                    workoutPlan,
                    request.getExercises()
            );

            CreateWorkoutPlanResponse response = CreateWorkoutPlanResponse.builder()
                    .workoutPlanId(savedPlan.getWorkoutPlanId())
                    .planName(savedPlan.getPlanName())
                    .totalExercises(request.getExercises() != null ? request.getExercises().size() : 0)
                    .message("Workout plan created successfully")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating workout plan: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error creating plan")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Find all plans for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkoutPlan>> getUserWorkoutPlans(@PathVariable Long userId) {
        List<WorkoutPlan> plans = workoutPlanService.findByUserId(userId);
        return ResponseEntity.ok(plans);
    }

    /**
     * Find a specific plan with all exercise details
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
                .totalExercises(0)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Update a workout plan
     */
    @PutMapping("/{planId}")
    public ResponseEntity<?> updateWorkoutPlan(
            @PathVariable Long planId,
            @Valid @RequestBody UpdateWorkoutPlanRequest request) {
        try {
            // Find existing plan
            WorkoutPlan existingPlan = workoutPlanService.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

            // Create object for update
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
                            .message("Plan updated successfully")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error updating plan")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Delete a workout plan
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<?> deleteWorkoutPlan(@PathVariable Long planId) {
        try {
            workoutPlanService.deleteWorkoutPlan(planId);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Plan deleted successfully")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error deleting plan")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Add an exercise to an existing plan
     */
    @PostMapping("/{planId}/exercises")
    public ResponseEntity<?> addExerciseToWorkoutPlan(
            @PathVariable Long planId,
            @Valid @RequestBody ExerciseDetailRequest request) {
        try {

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(SuccessResponse.builder()
                            .message("Functionality will be implemented soon")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error adding exercise")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Remove an exercise from plan
     */
    @DeleteMapping("/{planId}/exercises/{detailId}")
    public ResponseEntity<?> removeExerciseFromWorkoutPlan(
            @PathVariable Long planId,
            @PathVariable Long detailId) {
        try {

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Functionality will be implemented soon")
                            .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Error removing exercise")
                            .message(e.getMessage())
                            .build());
        }
    }

}