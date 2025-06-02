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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    @Query(value = "SELECT * FROM exercises e WHERE e.primary_muscle_group = CAST(:muscleGroup AS muscle_group_type) OR :muscleGroup = ANY(e.secondary_muscle_groups)", nativeQuery = true)
    List<Exercise> findByAnyMuscleGroup(@Param("muscleGroup") String muscleGroup);

    Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup, Pageable pageable);

    Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable);

    List<Exercise> findByDifficultyLevel(Integer difficultyLevel);

    List<Exercise> findByEquipmentContainingIgnoreCase(String equipment);

    @Query("SELECT e FROM Exercise e WHERE e.equipment IS NULL OR e.equipment = ''")
    List<Exercise> findExercisesWithoutEquipment();

    List<Exercise> findByExerciseNameContainingIgnoreCase(String name);

    Optional<Exercise> findByExerciseName(String exerciseName);

    List<Exercise> findByDifficultyLevelLessThanEqual(Integer difficultyLevel);
    List<Exercise> findByDifficultyLevelGreaterThanEqual(Integer difficultyLevel);

    boolean existsByExerciseName(String exerciseName);

    long countByCategory(ExerciseCategoryType category);

    long countByPrimaryMuscleGroup(MuscleGroupType muscleGroup);

    @Query("SELECT e FROM Exercise e WHERE e.createdAt >= :sinceDate ORDER BY e.createdAt DESC")
    List<Exercise> findRecentExercises(@Param("sinceDate") LocalDateTime sinceDate);

    @Query(value = "SELECT * FROM exercises ORDER BY created_at DESC LIMIT ?1", nativeQuery = true)
    List<Exercise> findMostPopularExercises(int limit);

    @Query("SELECT e FROM Exercise e WHERE e.primaryMuscleGroup = :muscleGroup AND e.category = :category AND e.exerciseId != :excludeId")
    List<Exercise> findSimilarExercises(@Param("muscleGroup") MuscleGroupType muscleGroup,
                                        @Param("category") ExerciseCategoryType category,
                                        @Param("excludeId") Long excludeId,
                                        Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE e.category = :category ORDER BY e.createdAt DESC")
    List<Exercise> findPopularExercisesByCategory(@Param("category") String category);

    @Query(value = "SELECT * FROM exercises ORDER BY created_at DESC LIMIT ?1", nativeQuery = true)
    List<Exercise> findMostLoggedExercises(int limit);

    @Query("SELECT e FROM Exercise e")
    List<Exercise> findUnusedExercises();

    @Query("SELECT 0")
    long countWorkoutPlansUsingExercise(@Param("exerciseId") Long exerciseId);

    @Query("SELECT e FROM Exercise e WHERE e.secondaryMuscleGroups IN :muscleGroups")
    List<Exercise> findBySecondaryMuscleGroupsIn(@Param("muscleGroups") String[] muscleGroups);

    @Query("SELECT e FROM Exercise e WHERE " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:muscleGroup IS NULL OR e.primaryMuscleGroup = :muscleGroup) AND " +
            "(:maxDifficulty IS NULL OR e.difficultyLevel <= :maxDifficulty) AND " +
            "(:equipment IS NULL OR e.equipment LIKE CONCAT('%', :equipment, '%'))")
    List<Exercise> findFilteredExercises(@Param("category") ExerciseCategoryType category,
                                         @Param("muscleGroup") MuscleGroupType muscleGroup,
                                         @Param("maxDifficulty") Integer maxDifficulty,
                                         @Param("equipment") String equipment);

    @Query(value = "SELECT * FROM exercises e WHERE " +
            "(:keyword IS NULL OR LOWER(e.exercise_name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR e.category = CAST(:category AS exercise_category_type)) AND " +
            "(:muscleGroup IS NULL OR e.primary_muscle_group = CAST(:muscleGroup AS muscle_group_type) OR :muscleGroup = ANY(e.secondary_muscle_groups)) AND " +
            "(:maxDifficulty IS NULL OR e.difficulty_level <= :maxDifficulty)", nativeQuery = true)
    Page<Exercise> searchExercises(@Param("keyword") String keyword,
                                   @Param("category") String category,
                                   @Param("muscleGroup") String muscleGroup,
                                   @Param("maxDifficulty") Integer maxDifficulty,
                                   Pageable pageable);
}