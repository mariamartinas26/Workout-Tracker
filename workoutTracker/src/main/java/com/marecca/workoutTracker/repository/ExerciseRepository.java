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

    // Query custom pentru căutarea în orice grup de mușchi (principal sau secundar)
    // Folosim sintaxa PostgreSQL pentru array-uri în loc de MEMBER OF
    @Query(value = "SELECT * FROM exercises e WHERE e.primary_muscle_group = CAST(:muscleGroup AS muscle_group_type) OR :muscleGroup = ANY(e.secondary_muscle_groups)", nativeQuery = true)
    List<Exercise> findByAnyMuscleGroup(@Param("muscleGroup") String muscleGroup);

    // Metodele standard care funcționează
    List<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup);
    Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup, Pageable pageable);

    List<Exercise> findByCategory(ExerciseCategoryType category);
    Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable);

    List<Exercise> findByDifficultyLevel(Integer difficultyLevel);

    // Pentru echipament - NUMELE CORECT: equipment (nu equipmentNeeded)
    List<Exercise> findByEquipmentContainingIgnoreCase(String equipment);

    // Pentru exerciții fără echipament
    @Query("SELECT e FROM Exercise e WHERE e.equipment IS NULL OR e.equipment = ''")
    List<Exercise> findExercisesWithoutEquipment();

    // Pentru căutare după nume
    List<Exercise> findByExerciseNameContainingIgnoreCase(String name);

    // Pentru găsirea după nume exact
    Optional<Exercise> findByExerciseName(String exerciseName);

    // Pentru exerciții pentru începători (dificultate 1-2)
    @Query("SELECT e FROM Exercise e WHERE e.difficultyLevel <= 2")
    List<Exercise> findBeginnerExercises();

    // Pentru exerciții avansate (dificultate 4-5)
    @Query("SELECT e FROM Exercise e WHERE e.difficultyLevel >= 4")
    List<Exercise> findAdvancedExercises();

    // Metode pentru dificultate
    List<Exercise> findByDifficultyLevelLessThanEqual(Integer difficultyLevel);
    List<Exercise> findByDifficultyLevelGreaterThanEqual(Integer difficultyLevel);

    // Pentru verificarea numelui
    boolean existsByExerciseName(String exerciseName);

    // Pentru numărarea după categorie
    long countByCategory(ExerciseCategoryType category);

    // Pentru numărarea după grupul de mușchi principal
    long countByPrimaryMuscleGroup(MuscleGroupType muscleGroup);

    // Pentru exerciții recente
    @Query("SELECT e FROM Exercise e WHERE e.createdAt >= :sinceDate ORDER BY e.createdAt DESC")
    List<Exercise> findRecentExercises(@Param("sinceDate") LocalDateTime sinceDate);

    // Pentru exerciții populare (sortate după data creării)
    @Query("SELECT e FROM Exercise e ORDER BY e.createdAt DESC")
    List<Exercise> findMostPopularExercises(Pageable pageable);

    // Metodă simplă pentru popularitate
    @Query(value = "SELECT * FROM exercises ORDER BY created_at DESC LIMIT ?1", nativeQuery = true)
    List<Exercise> findMostPopularExercises(int limit);

    // Exerciții similare
    @Query("SELECT e FROM Exercise e WHERE e.primaryMuscleGroup = :muscleGroup AND e.category = :category AND e.exerciseId != :excludeId")
    List<Exercise> findSimilarExercises(@Param("muscleGroup") MuscleGroupType muscleGroup,
                                        @Param("category") ExerciseCategoryType category,
                                        @Param("excludeId") Long excludeId,
                                        Pageable pageable);

    // Exerciții populare după categorie
    @Query("SELECT e FROM Exercise e WHERE e.category = :category ORDER BY e.createdAt DESC")
    List<Exercise> findPopularExercisesByCategory(@Param("category") String category);

    // Exerciții frecvent înregistrate (temporar la fel ca populare)
    @Query(value = "SELECT * FROM exercises ORDER BY created_at DESC LIMIT ?1", nativeQuery = true)
    List<Exercise> findMostLoggedExercises(int limit);

    // Exerciții nefolosite - temporar returnează toate (până când ai entitatea WorkoutExerciseDetail)
    @Query("SELECT e FROM Exercise e")
    List<Exercise> findUnusedExercises();

    // Numărarea planurilor - temporar returnează 0 (până când ai entitatea WorkoutExerciseDetail)
    @Query("SELECT 0")
    long countWorkoutPlansUsingExercise(@Param("exerciseId") Long exerciseId);

    // Căutare în grupuri secundare de mușchi
    @Query("SELECT e FROM Exercise e WHERE e.secondaryMuscleGroups IN :muscleGroups")
    List<Exercise> findBySecondaryMuscleGroupsIn(@Param("muscleGroups") String[] muscleGroups);

    // Căutare cu filtre multiple
    @Query("SELECT e FROM Exercise e WHERE " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:muscleGroup IS NULL OR e.primaryMuscleGroup = :muscleGroup) AND " +
            "(:maxDifficulty IS NULL OR e.difficultyLevel <= :maxDifficulty) AND " +
            "(:equipment IS NULL OR e.equipment LIKE CONCAT('%', :equipment, '%'))")
    List<Exercise> findFilteredExercises(@Param("category") ExerciseCategoryType category,
                                         @Param("muscleGroup") MuscleGroupType muscleGroup,
                                         @Param("maxDifficulty") Integer maxDifficulty,
                                         @Param("equipment") String equipment);

    // Pentru căutare complexă cu filtre multiple
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