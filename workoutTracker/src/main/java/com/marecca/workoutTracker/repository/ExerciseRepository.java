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

    List<Exercise> findByExerciseNameContainingIgnoreCase(String name);

    List<Exercise> findByDifficultyLevelLessThanEqual(Integer difficultyLevel);
}