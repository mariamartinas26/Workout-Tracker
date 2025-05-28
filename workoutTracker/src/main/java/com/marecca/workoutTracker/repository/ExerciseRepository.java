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

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {


    Optional<Exercise> findByExerciseName(String exerciseName);
    boolean existsByExerciseName(String exerciseName);

    List<Exercise> findByCategory(ExerciseCategoryType category);
    List<Exercise> findByPrimaryMuscleGroup(MuscleGroupType primaryMuscleGroup);
    List<Exercise> findByDifficultyLevel(Integer difficultyLevel);
    List<Exercise> findByDifficultyLevelLessThanEqual(Integer maxDifficulty);
    List<Exercise> findByDifficultyLevelGreaterThanEqual(Integer minDifficulty);
    List<Exercise> findByExerciseNameContainingIgnoreCase(String keyword);
    List<Exercise> findByEquipmentNeededContainingIgnoreCase(String equipment);


    @Query("SELECT e FROM Exercise e WHERE e.equipmentNeeded IS NULL OR e.equipmentNeeded = ''")
    List<Exercise> findExercisesWithoutEquipment();

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


    @Query("SELECT e FROM Exercise e WHERE e.exerciseId != :excludeId AND " +
            "(e.primaryMuscleGroup = :muscleGroup OR e.category = :category) " +
            "ORDER BY CASE WHEN e.primaryMuscleGroup = :muscleGroup THEN 1 ELSE 2 END")
    List<Exercise> findSimilarExercises(
            @Param("muscleGroup") MuscleGroupType muscleGroup,
            @Param("category") ExerciseCategoryType category,
            @Param("excludeId") Long excludeId,
            Pageable pageable);


    @Query(value = "SELECT e.* FROM exercises e " +
            "LEFT JOIN (SELECT exercise_id, COUNT(*) as usage_count " +
            "FROM workout_exercise_details " +
            "GROUP BY exercise_id) as usage " +
            "ON e.exercise_id = usage.exercise_id " +
            "ORDER BY COALESCE(usage.usage_count, 0) DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Exercise> findMostPopularExercises(@Param("limit") int limit);

    long countByCategory(ExerciseCategoryType category);
    long countByPrimaryMuscleGroup(MuscleGroupType primaryMuscleGroup);


    Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable);
    Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType primaryMuscleGroup, Pageable pageable);

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


    @Query("SELECT DISTINCT e FROM Exercise e JOIN e.secondaryMuscleGroups smg WHERE smg IN :muscleGroups")
    List<Exercise> findBySecondaryMuscleGroupsIn(@Param("muscleGroups") List<MuscleGroupType> muscleGroups);

    @Query("SELECT DISTINCT e FROM Exercise e LEFT JOIN e.secondaryMuscleGroups smg " +
            "WHERE e.primaryMuscleGroup = :muscleGroup OR smg = :muscleGroup")
    List<Exercise> findByAnyMuscleGroup(@Param("muscleGroup") MuscleGroupType muscleGroup);

    @Query("SELECT e FROM Exercise e WHERE e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<Exercise> findRecentExercises(@Param("since") java.time.LocalDateTime since);

    @Query(value = "SELECT e.* FROM exercises e " +
            "LEFT JOIN (SELECT exercise_id, COUNT(*) as usage_count " +
            "FROM workout_exercise_details " +
            "GROUP BY exercise_id) as usage " +
            "ON e.exercise_id = usage.exercise_id " +
            "WHERE e.category = :category " +
            "ORDER BY COALESCE(usage.usage_count, 0) DESC",
            nativeQuery = true)
    List<Exercise> findPopularExercisesByCategory(@Param("category") String category);

    @Query("SELECT e FROM Exercise e WHERE e.exerciseId NOT IN " +
            "(SELECT DISTINCT wed.exercise.exerciseId FROM WorkoutExerciseDetail wed)")
    List<Exercise> findUnusedExercises();


    @Query("SELECT COUNT(DISTINCT wed.workoutPlan.workoutPlanId) FROM WorkoutExerciseDetail wed " +
            "WHERE wed.exercise.exerciseId = :exerciseId")
    long countWorkoutPlansUsingExercise(@Param("exerciseId") Long exerciseId);

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