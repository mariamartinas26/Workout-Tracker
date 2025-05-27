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

/**
 * Repository pentru entitatea WorkoutPlan
 */
@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    /**
     * Găsește toate planurile de workout ale unui utilizator
     */
    List<WorkoutPlan> findByUserUserId(Long userId);

    /**
     * Găsește toate planurile de workout ale unui utilizator, paginat
     */
    Page<WorkoutPlan> findByUserUserId(Long userId, Pageable pageable);

    /**
     * Găsește un plan de workout după numele său și ID-ul utilizatorului
     */
    Optional<WorkoutPlan> findByUserUserIdAndPlanName(Long userId, String planName);

    /**
     * Verifică dacă un utilizator are deja un plan cu acest nume
     */
    boolean existsByUserUserIdAndPlanName(Long userId, String planName);

    /**
     * Găsește planuri de workout după nivelul de dificultate
     */
    List<WorkoutPlan> findByDifficultyLevel(Integer difficultyLevel);

    /**
     * Găsește planuri de workout cu durata estimată mai mică sau egală cu valoarea specificată
     */
    List<WorkoutPlan> findByEstimatedDurationMinutesLessThanEqual(Integer maxDurationMinutes);

    /**
     * Găsește planuri de workout care conțin un anumit exercițiu
     */
    @Query("SELECT wp FROM WorkoutPlan wp JOIN wp.exerciseDetails wed WHERE wed.exercise.exerciseId = :exerciseId")
    List<WorkoutPlan> findByExerciseId(@Param("exerciseId") Long exerciseId);

    /**
     * Găsește planuri de workout care conțin exerciții dintr-o anumită categorie
     */
    @Query("SELECT DISTINCT wp FROM WorkoutPlan wp JOIN wp.exerciseDetails wed JOIN wed.exercise e WHERE e.category = :category")
    List<WorkoutPlan> findByExerciseCategory(@Param("category") String category);

    /**
     * Caută planuri de workout după cuvinte cheie în numele sau descrierea lor
     */
    @Query("SELECT wp FROM WorkoutPlan wp WHERE wp.user.userId = :userId AND " +
            "(LOWER(wp.planName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(wp.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<WorkoutPlan> searchByUserIdAndKeyword(
            @Param("userId") Long userId,
            @Param("keyword") String keyword);

    /**
     * Găsește planuri de workout sortate după popularitate (numărul de programări)
     */
    @Query(value = "SELECT wp.* FROM workout_plans wp " +
            "LEFT JOIN (SELECT workout_plan_id, COUNT(*) as usage_count " +
            "FROM scheduled_workouts " +
            "GROUP BY workout_plan_id) as usage " +
            "ON wp.workout_plan_id = usage.workout_plan_id " +
            "WHERE wp.user_id = :userId " +
            "ORDER BY COALESCE(usage.usage_count, 0) DESC",
            nativeQuery = true)
    List<WorkoutPlan> findByUserIdOrderByPopularity(@Param("userId") Long userId);

    /**
     * Calculează durata medie a planurilor de workout ale unui utilizator
     */
    @Query("SELECT AVG(wp.estimatedDurationMinutes) FROM WorkoutPlan wp WHERE wp.user.userId = :userId")
    Double calculateAverageDurationForUser(@Param("userId") Long userId);

    /**
     * Găsește planurile de workout create recent de un utilizator
     */
    List<WorkoutPlan> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
