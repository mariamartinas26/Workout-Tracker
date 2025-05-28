package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.WorkoutExerciseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;


@Repository
public interface WorkoutExerciseDetailRepository extends JpaRepository<WorkoutExerciseDetail, Long> {

    List<WorkoutExerciseDetail> findByWorkoutPlanWorkoutPlanIdOrderByExerciseOrder(Long workoutPlanId);
    List<WorkoutExerciseDetail> findByExerciseExerciseId(Long exerciseId);

    WorkoutExerciseDetail findByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(Long workoutPlanId, Long exerciseId);

    List<WorkoutExerciseDetail> findByTargetWeightKgGreaterThanEqual(BigDecimal minWeight);
    List<WorkoutExerciseDetail> findByTargetSetsGreaterThanEqual(Integer minSets);

    boolean existsByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(Long workoutPlanId, Long exerciseId);


    @Modifying
    @Query("DELETE FROM WorkoutExerciseDetail wed WHERE wed.workoutPlan.workoutPlanId = :workoutPlanId")
    void deleteByWorkoutPlanId(@Param("workoutPlanId") Long workoutPlanId);


    @Modifying
    @Query("DELETE FROM WorkoutExerciseDetail wed WHERE wed.workoutPlan.workoutPlanId = :workoutPlanId AND wed.exercise.exerciseId = :exerciseId")
    void deleteByWorkoutPlanIdAndExerciseId(
            @Param("workoutPlanId") Long workoutPlanId,
            @Param("exerciseId") Long exerciseId);

    @Query("SELECT wed FROM WorkoutExerciseDetail wed WHERE wed.workoutPlan.user.userId = :userId")
    List<WorkoutExerciseDetail> findByUserId(@Param("userId") Long userId);


    @Query(value = "SELECT wed.* FROM workout_exercise_details wed " +
            "JOIN (SELECT exercise_id, COUNT(*) as usage_count " +
            "FROM workout_exercise_details " +
            "GROUP BY exercise_id " +
            "ORDER BY usage_count DESC " +
            "LIMIT :limit) as popular " +
            "ON wed.exercise_id = popular.exercise_id " +
            "ORDER BY popular.usage_count DESC",
            nativeQuery = true)
    List<WorkoutExerciseDetail> findMostPopularExercises(@Param("limit") int limit);


    @Modifying
    @Query("UPDATE WorkoutExerciseDetail wed SET wed.exerciseOrder = :newOrder " +
            "WHERE wed.workoutPlan.workoutPlanId = :workoutPlanId AND wed.exercise.exerciseId = :exerciseId")
    void updateExerciseOrder(
            @Param("workoutPlanId") Long workoutPlanId,
            @Param("exerciseId") Long exerciseId,
            @Param("newOrder") Integer newOrder);
}