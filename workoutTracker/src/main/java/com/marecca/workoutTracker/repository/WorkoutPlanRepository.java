package com.marecca.workoutTracker.repository;


import com.marecca.workoutTracker.entity.WorkoutPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {


    List<WorkoutPlan> findByUserUserId(Long userId);
    Page<WorkoutPlan> findByUserUserId(Long userId, Pageable pageable);
    Optional<WorkoutPlan> findByUserUserIdAndPlanName(Long userId, String planName);

    boolean existsByUserUserIdAndPlanName(Long userId, String planName);

    List<WorkoutPlan> findByDifficultyLevel(Integer difficultyLevel);
    List<WorkoutPlan> findByEstimatedDurationMinutesLessThanEqual(Integer maxDurationMinutes);

    @Query("SELECT wp FROM WorkoutPlan wp JOIN wp.exerciseDetails wed WHERE wed.exercise.exerciseId = :exerciseId")
    List<WorkoutPlan> findByExerciseId(@Param("exerciseId") Long exerciseId);

    @Query("SELECT DISTINCT wp FROM WorkoutPlan wp JOIN wp.exerciseDetails wed JOIN wed.exercise e WHERE e.category = :category")
    List<WorkoutPlan> findByExerciseCategory(@Param("category") String category);

    @Query("SELECT wp FROM WorkoutPlan wp WHERE wp.user.userId = :userId AND " +
            "(LOWER(wp.planName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(wp.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<WorkoutPlan> searchByUserIdAndKeyword(
            @Param("userId") Long userId,
            @Param("keyword") String keyword);

    @Query(value = "SELECT wp.* FROM workout_plans wp " +
            "LEFT JOIN (SELECT workout_plan_id, COUNT(*) as usage_count " +
            "FROM scheduled_workouts " +
            "GROUP BY workout_plan_id) as usage " +
            "ON wp.workout_plan_id = usage.workout_plan_id " +
            "WHERE wp.user_id = :userId " +
            "ORDER BY COALESCE(usage.usage_count, 0) DESC",
            nativeQuery = true)
    List<WorkoutPlan> findByUserIdOrderByPopularity(@Param("userId") Long userId);

    @Query("SELECT AVG(wp.estimatedDurationMinutes) FROM WorkoutPlan wp WHERE wp.user.userId = :userId")
    Double calculateAverageDurationForUser(@Param("userId") Long userId);

    List<WorkoutPlan> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
