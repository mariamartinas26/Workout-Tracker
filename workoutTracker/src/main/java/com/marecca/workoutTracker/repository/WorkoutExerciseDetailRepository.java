package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.WorkoutExerciseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository pentru entitatea WorkoutExerciseDetail
 */
@Repository
public interface WorkoutExerciseDetailRepository extends JpaRepository<WorkoutExerciseDetail, Long> {

    /**
     * Găsește toate detaliile exercițiilor pentru un plan de workout, ordonate după ordinea exercițiilor
     */
    List<WorkoutExerciseDetail> findByWorkoutPlanWorkoutPlanIdOrderByExerciseOrder(Long workoutPlanId);

    /**
     * Găsește toate detaliile pentru un anumit exercițiu în toate planurile
     */
    List<WorkoutExerciseDetail> findByExerciseExerciseId(Long exerciseId);

    /**
     * Găsește detaliile unui exercițiu specific într-un plan
     */
    WorkoutExerciseDetail findByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(Long workoutPlanId, Long exerciseId);

    /**
     * Găsește exercițiile cu o greutate țintă mai mare decât valoarea specificată
     */
    List<WorkoutExerciseDetail> findByTargetWeightKgGreaterThanEqual(BigDecimal minWeight);

    /**
     * Găsește exercițiile cu un număr minim de seturi țintă
     */
    List<WorkoutExerciseDetail> findByTargetSetsGreaterThanEqual(Integer minSets);

    /**
     * Verifică dacă un exercițiu este deja inclus într-un plan
     */
    boolean existsByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(Long workoutPlanId, Long exerciseId);

    /**
     * Șterge toate detaliile exercițiilor pentru un plan
     */
    @Modifying
    @Query("DELETE FROM WorkoutExerciseDetail wed WHERE wed.workoutPlan.workoutPlanId = :workoutPlanId")
    void deleteByWorkoutPlanId(@Param("workoutPlanId") Long workoutPlanId);

    /**
     * Șterge un exercițiu specific dintr-un plan
     */
    @Modifying
    @Query("DELETE FROM WorkoutExerciseDetail wed WHERE wed.workoutPlan.workoutPlanId = :workoutPlanId AND wed.exercise.exerciseId = :exerciseId")
    void deleteByWorkoutPlanIdAndExerciseId(
            @Param("workoutPlanId") Long workoutPlanId,
            @Param("exerciseId") Long exerciseId);

    /**
     * Găsește toate exercițiile din planurile unui utilizator
     */
    @Query("SELECT wed FROM WorkoutExerciseDetail wed WHERE wed.workoutPlan.user.userId = :userId")
    List<WorkoutExerciseDetail> findByUserId(@Param("userId") Long userId);

    /**
     * Găsește cele mai populare exerciții folosite în planuri
     */
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

    /**
     * Actualizează ordinea unui exercițiu într-un plan
     */
    @Modifying
    @Query("UPDATE WorkoutExerciseDetail wed SET wed.exerciseOrder = :newOrder " +
            "WHERE wed.workoutPlan.workoutPlanId = :workoutPlanId AND wed.exercise.exerciseId = :exerciseId")
    void updateExerciseOrder(
            @Param("workoutPlanId") Long workoutPlanId,
            @Param("exerciseId") Long exerciseId,
            @Param("newOrder") Integer newOrder);
}