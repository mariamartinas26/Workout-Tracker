package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.dto.request.ExerciseDetailRequest;
import com.marecca.workoutTracker.entity.Exercise;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.entity.WorkoutExerciseDetail;
import com.marecca.workoutTracker.entity.WorkoutPlan;
import com.marecca.workoutTracker.repository.ExerciseRepository;
import com.marecca.workoutTracker.repository.WorkoutPlanRepository;
import com.marecca.workoutTracker.repository.UserRepository;
import com.marecca.workoutTracker.repository.WorkoutExerciseDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for operations with workout plans
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkoutPlanService {

    private final WorkoutPlanRepository workoutPlanRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutExerciseDetailRepository workoutExerciseDetailRepository;
    /**
     * creates a workout plan with exercises
     */
    @Transactional
    public WorkoutPlan createWorkoutPlanWithExercises(WorkoutPlan workoutPlan, List<ExerciseDetailRequest> exerciseRequests) {
        // validates user
        User user = userRepository.findById(workoutPlan.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        workoutPlan.setUser(user);

        //saves workout plan
        WorkoutPlan savedPlan = workoutPlanRepository.save(workoutPlan);

        if (exerciseRequests != null && !exerciseRequests.isEmpty()) {
            for (ExerciseDetailRequest exerciseRequest : exerciseRequests) {
                Exercise exercise = exerciseRepository.findById(exerciseRequest.getExerciseId())
                        .orElseThrow(() -> new IllegalArgumentException("Exercise with ID " + exerciseRequest.getExerciseId() + " was not found"));

                WorkoutExerciseDetail exerciseDetail = WorkoutExerciseDetail.builder()
                        .workoutPlan(savedPlan)
                        .exercise(exercise)
                        .exerciseOrder(exerciseRequest.getExerciseOrder())
                        .targetSets(exerciseRequest.getTargetSets())
                        .targetRepsMin(exerciseRequest.getTargetRepsMin())
                        .targetRepsMax(exerciseRequest.getTargetRepsMax())
                        .targetWeightKg(exerciseRequest.getTargetWeightKg())
                        .targetDurationSeconds(exerciseRequest.getTargetDurationSeconds())
                        .targetDistanceMeters(exerciseRequest.getTargetDistanceMeters())
                        .restTimeSeconds(exerciseRequest.getRestTimeSeconds())
                        .notes(exerciseRequest.getNotes())
                        .build();

                workoutExerciseDetailRepository.save(exerciseDetail);
            }

        }
        return savedPlan;
    }
    /**
     * finds a workout plan by id
     * @param workoutPlanId
     * @return
     */
    @Transactional(readOnly = true)
    public Optional<WorkoutPlan> findById(Long workoutPlanId) {
        return workoutPlanRepository.findById(workoutPlanId);
    }

    /**
     * finds all plans for a user
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public List<WorkoutPlan> findByUserId(Long userId) {
        validateUserExists(userId);
        return workoutPlanRepository.findByUserUserId(userId);
    }


    /**
     * updates workout plan
     * @param workoutPlanId
     * @param updatedPlan
     * @return
     * @throws IllegalArgumentException
     */
    public WorkoutPlan updateWorkoutPlan(Long workoutPlanId, WorkoutPlan updatedPlan) {
        WorkoutPlan existingPlan = findWorkoutPlanById(workoutPlanId);
        validateWorkoutPlanData(updatedPlan);

        if (!existingPlan.getPlanName().equals(updatedPlan.getPlanName())) {
            validateUniquePlanName(existingPlan.getUser().getUserId(), updatedPlan.getPlanName());
        }

        updateWorkoutPlanFields(existingPlan, updatedPlan);
        existingPlan.setUpdatedAt(LocalDateTime.now());

        WorkoutPlan savedPlan = workoutPlanRepository.save(existingPlan);
        return savedPlan;
    }

    /**
     * delets a workout plan and all associated details
     * @param workoutPlanId
     * @throws IllegalArgumentException
     */
    public void deleteWorkoutPlan(Long workoutPlanId) {
        validateWorkoutPlanExists(workoutPlanId);

        try {
            //deletes exercises details
            workoutExerciseDetailRepository.deleteByWorkoutPlanId(workoutPlanId);

            //delets plan
            workoutPlanRepository.deleteById(workoutPlanId);
        } catch (Exception e) {
            throw new IllegalStateException("You can't delete the plan.It is used in scheduled workouts", e);
        }
    }

    private void validateWorkoutPlanData(WorkoutPlan workoutPlan) {
        if (!StringUtils.hasText(workoutPlan.getPlanName())) {
            throw new IllegalArgumentException("Plan name is required");
        }

        if (workoutPlan.getUser() == null || workoutPlan.getUser().getUserId() == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (workoutPlan.getEstimatedDurationMinutes() != null && workoutPlan.getEstimatedDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Estimated duration must be positive");
        }

        if (workoutPlan.getDifficultyLevel() != null) {
            validateDifficultyLevel(workoutPlan.getDifficultyLevel());
        }
    }

    private void validateDifficultyLevel(Integer difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("Difficulty level must be between 1 and 5");
        }
    }

    private void validateUniquePlanName(Long userId, String planName) {
        if (workoutPlanRepository.existsByUserUserIdAndPlanName(userId, planName)) {
            throw new IllegalArgumentException("Plan name already exists for this user: " + planName);
        }
    }


    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
    }

    private WorkoutPlan findWorkoutPlanById(Long workoutPlanId) {
        return workoutPlanRepository.findById(workoutPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Workout plan not found with ID: " + workoutPlanId));
    }

    private void validateWorkoutPlanExists(Long workoutPlanId) {
        if (!workoutPlanRepository.existsById(workoutPlanId)) {
            throw new IllegalArgumentException("Workout plan not found with ID: " + workoutPlanId);
        }
    }

    private void updateWorkoutPlanFields(WorkoutPlan existing, WorkoutPlan updated) {
        existing.setPlanName(updated.getPlanName());
        existing.setDescription(updated.getDescription());
        existing.setEstimatedDurationMinutes(updated.getEstimatedDurationMinutes());
        existing.setDifficultyLevel(updated.getDifficultyLevel());
        existing.setGoals(updated.getGoals());
        existing.setNotes(updated.getNotes());
    }
}