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
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
@Slf4j
public class WorkoutPlanController {

    private final WorkoutPlanService workoutPlanService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Create a new workout plan
     */
    @PostMapping
    public ResponseEntity<?> createWorkoutPlan(@Valid @RequestBody CreateWorkoutPlanRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            // Get authenticated user ID from JWT token
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            // Verify that the user is creating a plan for themselves
            if (!request.getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only create plans for yourself", HttpStatus.FORBIDDEN);
            }

            // Create the main plan
            User user = new User();
            user.setUserId(authenticatedUserId); // Use authenticated user ID

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

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to create workout plans");
        }
    }

    /**
     * Find all plans for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserWorkoutPlans(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            // Users can only access their own plans
            if (!userId.equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only access your own workout plans", HttpStatus.FORBIDDEN);
            }

            List<WorkoutPlan> plans = workoutPlanService.findByUserId(userId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout plans");
        }
    }

    /**
     * Find a specific plan with all exercise details
     */
    @GetMapping("/{planId}")
    public ResponseEntity<?> getWorkoutPlanDetails(@PathVariable Long planId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            Optional<WorkoutPlan> planOpt = workoutPlanService.findById(planId);

            if (planOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            WorkoutPlan plan = planOpt.get();

            // Verify that the user owns this plan
            if (!plan.getUser().getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only access your own workout plans", HttpStatus.FORBIDDEN);
            }

            WorkoutPlanDetailsResponse response = WorkoutPlanDetailsResponse.builder()
                    .workoutPlan(plan)
                    .totalExercises(0)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout plan details");
        }
    }

    /**
     * Update a workout plan
     */
    @PutMapping("/{planId}")
    public ResponseEntity<?> updateWorkoutPlan(
            @PathVariable Long planId,
            @Valid @RequestBody UpdateWorkoutPlanRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            // Find existing plan and verify ownership
            WorkoutPlan existingPlan = workoutPlanService.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

            if (!existingPlan.getUser().getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only update your own workout plans", HttpStatus.FORBIDDEN);
            }

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

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to update workout plans");
        }
    }

    /**
     * Delete a workout plan
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<?> deleteWorkoutPlan(@PathVariable Long planId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            WorkoutPlan existingPlan = workoutPlanService.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

            if (!existingPlan.getUser().getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only delete your own workout plans", HttpStatus.FORBIDDEN);
            }

            workoutPlanService.deleteWorkoutPlan(planId);

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Plan deleted successfully")
                            .build());

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to delete workout plans");
        }
    }

    /**
     * Add an exercise to an existing plan
     */
    @PostMapping("/{planId}/exercises")
    public ResponseEntity<?> addExerciseToWorkoutPlan(
            @PathVariable Long planId,
            @Valid @RequestBody ExerciseDetailRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            // Find existing plan and verify ownership
            WorkoutPlan existingPlan = workoutPlanService.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

            if (!existingPlan.getUser().getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only modify your own workout plans", HttpStatus.FORBIDDEN);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(SuccessResponse.builder()
                            .message("Functionality will be implemented soon")
                            .build());

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to modify workout plans");
        }
    }

    /**
     * Remove an exercise from plan
     */
    @DeleteMapping("/{planId}/exercises/{detailId}")
    public ResponseEntity<?> removeExerciseFromWorkoutPlan(
            @PathVariable Long planId,
            @PathVariable Long detailId,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            // Find existing plan and verify ownership
            WorkoutPlan existingPlan = workoutPlanService.findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

            if (!existingPlan.getUser().getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only modify your own workout plans", HttpStatus.FORBIDDEN);
            }

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Functionality will be implemented soon")
                            .build());

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to modify workout plans");
        }
    }

    /**
     * Get current user's workout plans
     */
    @GetMapping("/my-plans")
    public ResponseEntity<?> getMyWorkoutPlans(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            List<WorkoutPlan> plans = workoutPlanService.findByUserId(authenticatedUserId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access workout plans");
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
}