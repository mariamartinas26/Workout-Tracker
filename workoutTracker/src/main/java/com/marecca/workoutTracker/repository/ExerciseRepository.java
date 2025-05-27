package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.Exercise;
import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pentru entitatea Exercise
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    /**
     * Găsește un exercițiu după nume
     */
    Optional<Exercise> findByExerciseName(String exerciseName);

    /**
     * Verifică dacă există un exercițiu cu numele dat
     */
    boolean existsByExerciseName(String exerciseName);

    /**
     * Găsește exerciții după categorie
     */
    List<Exercise> findByCategory(ExerciseCategoryType category);

    /**
     * Găsește exerciții după grupul de mușchi principal
     */
    List<Exercise> findByPrimaryMuscleGroup(MuscleGroupType primaryMuscleGroup);

    /**
     * Găsește exerciții după nivelul de dificultate
     */
    List<Exercise> findByDifficultyLevel(Integer difficultyLevel);

    /**
     * Găsește exerciții cu nivelul de dificultate mai mic sau egal
     */
    List<Exercise> findByDifficultyLevelLessThanEqual(Integer maxDifficulty);

    /**
     * Găsește exerciții cu nivelul de dificultate mai mare sau egal
     */
    List<Exercise> findByDifficultyLevelGreaterThanEqual(Integer minDifficulty);

    /**
     * Caută exerciții după nume (case insensitive)
     */
    List<Exercise> findByExerciseNameContainingIgnoreCase(String keyword);

    /**
     * Găsește exerciții după echipament necesar (case insensitive)
     */
    List<Exercise> findByEquipmentNeededContainingIgnoreCase(String equipment);

    /**
     * Găsește exerciții care nu necesită echipament
     */
    @Query("SELECT e FROM Exercise e WHERE e.equipmentNeeded IS NULL OR e.equipmentNeeded = ''")
    List<Exercise> findExercisesWithoutEquipment();

    /**
     * Găsește exerciții filtrate după mai multe criterii
     */
    @Query("SELECT e FROM Exercise e WHERE " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:muscleGroup IS NULL OR e.primaryMuscleGroup = :muscleGroup) AND " +
            "(:maxDifficulty IS NULL OR e.difficultyLevel <= :maxDifficulty) AND " +
            "(:equipment IS NULL OR LOWER(e.equipmentNeeded) LIKE LOWER(CONCAT('%', :equipment, '%')))")
    List<Exercise> findFilteredExercises(
            @Param("category") ExerciseCategoryType category,
            @Param("muscleGroup") MuscleGroupType muscleGroup,
            @Param("maxDifficulty") Integer maxDifficulty,
            @Param("equipment") String equipment);

    /**
     * Găsește exerciții similare pe baza grupului de mușchi și categoriei
     */
    @Query("SELECT e FROM Exercise e WHERE e.exerciseId != :excludeId AND " +
            "(e.primaryMuscleGroup = :muscleGroup OR e.category = :category) " +
            "ORDER BY CASE WHEN e.primaryMuscleGroup = :muscleGroup THEN 1 ELSE 2 END")
    List<Exercise> findSimilarExercises(
            @Param("muscleGroup") MuscleGroupType muscleGroup,
            @Param("category") ExerciseCategoryType category,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    /**
     * Găsește exerciții populare (folosite în cele mai multe planuri)
     */
    @Query(value = "SELECT e.* FROM exercises e " +
            "LEFT JOIN (SELECT exercise_id, COUNT(*) as usage_count " +
            "FROM workout_exercise_details " +
            "GROUP BY exercise_id) as usage " +
            "ON e.exercise_id = usage.exercise_id " +
            "ORDER BY COALESCE(usage.usage_count, 0) DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Exercise> findMostPopularExercises(@Param("limit") int limit);

    /**
     * Numără exercițiile după categorie
     */
    long countByCategory(ExerciseCategoryType category);

    /**
     * Numără exercițiile după grupul de mușchi principal
     */
    long countByPrimaryMuscleGroup(MuscleGroupType primaryMuscleGroup);

    /**
     * Găsește exerciții după categorie cu paginare
     */
    Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable);

    /**
     * Găsește exerciții după grupul de mușchi cu paginare
     */
    Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType primaryMuscleGroup, Pageable pageable);

    /**
     * Caută exerciții în funcție de mai multe criterii cu paginare
     */
    @Query("SELECT e FROM Exercise e WHERE " +
            "(:keyword IS NULL OR LOWER(e.exerciseName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:muscleGroup IS NULL OR e.primaryMuscleGroup = :muscleGroup) AND " +
            "(:maxDifficulty IS NULL OR e.difficultyLevel <= :maxDifficulty)")
    Page<Exercise> searchExercises(
            @Param("keyword") String keyword,
            @Param("category") ExerciseCategoryType category,
            @Param("muscleGroup") MuscleGroupType muscleGroup,
            @Param("maxDifficulty") Integer maxDifficulty,
            Pageable pageable);

    /**
     * Găsește exerciții care au unul din grupurile secundare de mușchi specificate
     */
    @Query("SELECT DISTINCT e FROM Exercise e JOIN e.secondaryMuscleGroups smg WHERE smg IN :muscleGroups")
    List<Exercise> findBySecondaryMuscleGroupsIn(@Param("muscleGroups") List<MuscleGroupType> muscleGroups);

    /**
     * Găsește exerciții pentru un anumit grup de mușchi (principal sau secundar)
     */
    @Query("SELECT DISTINCT e FROM Exercise e LEFT JOIN e.secondaryMuscleGroups smg " +
            "WHERE e.primaryMuscleGroup = :muscleGroup OR smg = :muscleGroup")
    List<Exercise> findByAnyMuscleGroup(@Param("muscleGroup") MuscleGroupType muscleGroup);

    /**
     * Găsește exerciții create în ultima perioadă
     */
    @Query("SELECT e FROM Exercise e WHERE e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<Exercise> findRecentExercises(@Param("since") java.time.LocalDateTime since);

    /**
     * Găsește exerciții ordonate după popularitate pentru o categorie specifică
     */
    @Query(value = "SELECT e.* FROM exercises e " +
            "LEFT JOIN (SELECT exercise_id, COUNT(*) as usage_count " +
            "FROM workout_exercise_details " +
            "GROUP BY exercise_id) as usage " +
            "ON e.exercise_id = usage.exercise_id " +
            "WHERE e.category = :category " +
            "ORDER BY COALESCE(usage.usage_count, 0) DESC",
            nativeQuery = true)
    List<Exercise> findPopularExercisesByCategory(@Param("category") String category);

    /**
     * Găsește exerciții care nu sunt folosite în niciun plan
     */
    @Query("SELECT e FROM Exercise e WHERE e.exerciseId NOT IN " +
            "(SELECT DISTINCT wed.exercise.exerciseId FROM WorkoutExerciseDetail wed)")
    List<Exercise> findUnusedExercises();

    /**
     * Numără câte planuri de workout folosesc un exercițiu
     */
    @Query("SELECT COUNT(DISTINCT wed.workoutPlan.workoutPlanId) FROM WorkoutExerciseDetail wed " +
            "WHERE wed.exercise.exerciseId = :exerciseId")
    long countWorkoutPlansUsingExercise(@Param("exerciseId") Long exerciseId);

    /**
     * Găsește exercițiile cele mai frecvent înregistrate în loguri
     */
    @Query(value = "SELECT e.* FROM exercises e " +
            "INNER JOIN (SELECT exercise_id, COUNT(*) as log_count " +
            "FROM workout_exercise_logs " +
            "GROUP BY exercise_id " +
            "ORDER BY log_count DESC " +
            "LIMIT :limit) as logs " +
            "ON e.exercise_id = logs.exercise_id " +
            "ORDER BY logs.log_count DESC",
            nativeQuery = true)
    List<Exercise> findMostLoggedExercises(@Param("limit") int limit);
}