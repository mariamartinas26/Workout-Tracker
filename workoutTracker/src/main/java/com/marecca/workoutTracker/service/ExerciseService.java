package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.Exercise;
import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import com.marecca.workoutTracker.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for operations with exercises
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    /**
     * creates a new exercise
     */
    public Exercise createExercise(Exercise exercise) {
        validateExerciseData(exercise);
        validateUniqueExerciseName(exercise.getExerciseName());

        exercise.setCreatedAt(LocalDateTime.now());

        Exercise savedExercise = exerciseRepository.save(exercise);

        return savedExercise;
    }

    /**
     * finds exercise by id
     */
    @Transactional(readOnly = true)
    public Optional<Exercise> findById(Long exerciseId) {
        return exerciseRepository.findById(exerciseId);
    }

    /**
     * finds exercise by name
    */
    @Transactional(readOnly = true)
    public Optional<Exercise> findByName(String exerciseName) {
        return exerciseRepository.findByExerciseName(exerciseName);
    }


    @Transactional(readOnly = true)
    public Page<Exercise> findAll(Pageable pageable) {
        return exerciseRepository.findAll(pageable);
    }


    @Transactional(readOnly = true)
    public Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable) {
        log.debug("Finding exercises by category: {} with pagination", category);
        return exerciseRepository.findByCategory(category, pageable);
    }


    @Transactional(readOnly = true)
    public Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup, Pageable pageable) {
        log.debug("Finding exercises by primary muscle group: {} with pagination", muscleGroup);
        return exerciseRepository.findByPrimaryMuscleGroup(muscleGroup, pageable);
    }

    /**
     * find exercises by any timpe of muscle group
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByAnyMuscleGroup(MuscleGroupType muscleGroup) {
        return exerciseRepository.findByAnyMuscleGroup(muscleGroup.name());
    }

    /**
     * finds exercises by level of difficulty
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByDifficultyLevel(Integer difficultyLevel) {
        validateDifficultyLevel(difficultyLevel);
        return exerciseRepository.findByDifficultyLevel(difficultyLevel);
    }

    @Transactional(readOnly = true)
    public List<Exercise> searchByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return exerciseRepository.findByExerciseNameContainingIgnoreCase(keyword.trim());
    }

    /**
     * advanced search for many criterias
     */
    @Transactional(readOnly = true)
    public Page<Exercise> searchExercises(
            String keyword,
            ExerciseCategoryType category,
            MuscleGroupType muscleGroup,
            Integer maxDifficulty,
            Pageable pageable) {

        if (maxDifficulty != null) {
            validateDifficultyLevel(maxDifficulty);
        }

        return exerciseRepository.searchExercises(
                StringUtils.hasText(keyword) ? keyword.trim() : null,
                category != null ? category.name() : null,
                muscleGroup != null ? muscleGroup.name() : null,
                maxDifficulty,
                pageable);
    }

    /**
     * find exercises by required equipment
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByEquipment(String equipment) {
        if (!StringUtils.hasText(equipment)) {
            return List.of();
        }

        return exerciseRepository.findByEquipmentContainingIgnoreCase(equipment.trim());
    }

    /**
     * finds exercises that don't require equipment
     * @return lista exercițiilor fără echipament
     */
    @Transactional(readOnly = true)
    public List<Exercise> findExercisesWithoutEquipment() {
        return exerciseRepository.findExercisesWithoutEquipment();
    }


    public Exercise updateExercise(Long exerciseId, Exercise updatedExercise) {
        Exercise existingExercise = findExerciseById(exerciseId);
        validateExerciseData(updatedExercise);

        if (!existingExercise.getExerciseName().equals(updatedExercise.getExerciseName())) {
            validateUniqueExerciseName(updatedExercise.getExerciseName());
        }

        updateExerciseFields(existingExercise, updatedExercise);

        Exercise savedExercise = exerciseRepository.save(existingExercise);
        return savedExercise;
    }

    public void deleteExercise(Long exerciseId) {
        validateExerciseExists(exerciseId);

        //checks if it's used in any plans
        long usageCount = exerciseRepository.countWorkoutPlansUsingExercise(exerciseId);
        if (usageCount > 0) {
            throw new IllegalStateException(
                    String.format("You can't delete this exercise because it's used in %d plans", usageCount));
        }

        try {
            exerciseRepository.deleteById(exerciseId);
        } catch (Exception e) {
            throw new IllegalStateException("You can't delete de exercise", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Exercise> findFilteredExercises(
            ExerciseCategoryType category,
            MuscleGroupType muscleGroup,
            Integer maxDifficulty,
            String equipment) {

        if (maxDifficulty != null) {
            validateDifficultyLevel(maxDifficulty);
        }

        return exerciseRepository.findFilteredExercises(category, muscleGroup, maxDifficulty, equipment);
    }


    @Transactional(readOnly = true)
    public List<Exercise> findBeginnerExercises() {
        return exerciseRepository.findByDifficultyLevelLessThanEqual(2);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findAdvancedExercises() {
        return exerciseRepository.findByDifficultyLevelGreaterThanEqual(4);
    }

    /**
     * Find similar exrcises based on muscular group and category
     */
    @Transactional(readOnly = true)
    public List<Exercise> findSimilarExercises(Long exerciseId, int limit) {
        Exercise referenceExercise = findExerciseById(exerciseId);

        Pageable pageable = PageRequest.of(0, limit);

        return exerciseRepository.findSimilarExercises(
                referenceExercise.getPrimaryMuscleGroup(),
                referenceExercise.getCategory(),
                exerciseId,
                pageable);
    }

    @Transactional(readOnly = true)
    public boolean isExerciseNameAvailable(String exerciseName) {
        return !exerciseRepository.existsByExerciseName(exerciseName);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findMostPopularExercises(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit should be positive!");
        }

        return exerciseRepository.findMostPopularExercises(limit);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findPopularExercisesByCategory(ExerciseCategoryType category) {
        return exerciseRepository.findPopularExercisesByCategory(category.name());
    }

    @Transactional(readOnly = true)
    public List<Exercise> findMostLoggedExercises(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit should be positive!");
        }

        return exerciseRepository.findMostLoggedExercises(limit);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findRecentExercises(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Number of days should be positive!");
        }

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return exerciseRepository.findRecentExercises(since);
    }

    /**
     * finds unused exercises in plans
     */
    @Transactional(readOnly = true)
    public List<Exercise> findUnusedExercises() {
        return exerciseRepository.findUnusedExercises();
    }

    @Transactional(readOnly = true)
    public long countByCategory(ExerciseCategoryType category) {
        return exerciseRepository.countByCategory(category);
    }

    @Transactional(readOnly = true)
    public long countByMuscleGroup(MuscleGroupType muscleGroup) {
        return exerciseRepository.countByPrimaryMuscleGroup(muscleGroup);
    }


    @Transactional(readOnly = true)
    public long countWorkoutPlansUsingExercise(Long exerciseId) {
        validateExerciseExists(exerciseId);
        return exerciseRepository.countWorkoutPlansUsingExercise(exerciseId);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findBySecondaryMuscleGroups(List<MuscleGroupType> muscleGroups) {
        if (muscleGroups == null || muscleGroups.isEmpty()) {
            return List.of();
        }

        String[] muscleGroupNames = muscleGroups.stream()
                .map(Enum::name)
                .toArray(String[]::new);

        return exerciseRepository.findBySecondaryMuscleGroupsIn(muscleGroupNames);
    }

    /**
     * statistics of exercises
     * @return
     */
    @Transactional(readOnly = true)
    public ExerciseStatistics getExerciseStatistics() {
        long totalExercises = exerciseRepository.count();
        long beginnerExercises = exerciseRepository.findByDifficultyLevelLessThanEqual(2).size();
        long advancedExercises = exerciseRepository.findByDifficultyLevelGreaterThanEqual(4).size();
        long exercisesWithoutEquipment = exerciseRepository.findExercisesWithoutEquipment().size();
        long unusedExercises = exerciseRepository.findUnusedExercises().size();

        return ExerciseStatistics.builder()
                .totalExercises(totalExercises)
                .beginnerExercises(beginnerExercises)
                .advancedExercises(advancedExercises)
                .exercisesWithoutEquipment(exercisesWithoutEquipment)
                .unusedExercises(unusedExercises)
                .build();
    }


    private void validateExerciseData(Exercise exercise) {
        if (!StringUtils.hasText(exercise.getExerciseName())) {
            throw new IllegalArgumentException("Exercise name is required");
        }

        if (exercise.getCategory() == null) {
            throw new IllegalArgumentException("Exercise category is required");
        }

        if (exercise.getPrimaryMuscleGroup() == null) {
            throw new IllegalArgumentException("Primary muscle group is required");
        }

        if (exercise.getDifficultyLevel() != null) {
            validateDifficultyLevel(exercise.getDifficultyLevel());
        }

        // Validate secondary muscle groups
        if (exercise.getSecondaryMuscleGroups() != null) {
            // Ensure the primary muscle group is not also listed as secondary
            if (exercise.getSecondaryMuscleGroups().contains(exercise.getPrimaryMuscleGroup())) {
                throw new IllegalArgumentException("Primary muscle group cannot be included in secondary muscle groups");
            }
        }
    }

    private void validateDifficultyLevel(Integer difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("Difficulty level must be between 1 and 5");
        }
    }

    private void validateUniqueExerciseName(String exerciseName) {
        if (exerciseRepository.existsByExerciseName(exerciseName)) {
            throw new IllegalArgumentException("Exercise already exists: " + exerciseName);
        }
    }

    private Exercise findExerciseById(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found with ID: " + exerciseId));
    }

    private void validateExerciseExists(Long exerciseId) {
        if (!exerciseRepository.existsById(exerciseId)) {
            throw new IllegalArgumentException("Exercise not found with ID: " + exerciseId);
        }
    }


    private void updateExerciseFields(Exercise existing, Exercise updated) {
        existing.setExerciseName(updated.getExerciseName());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setPrimaryMuscleGroup(updated.getPrimaryMuscleGroup());
        existing.setSecondaryMuscleGroups(updated.getSecondaryMuscleGroups());
        existing.setEquipmentNeeded(updated.getEquipmentNeeded());
        existing.setDifficultyLevel(updated.getDifficultyLevel());
        existing.setInstructions(updated.getInstructions());
    }


    @lombok.Data
    @lombok.Builder
    public static class ExerciseStatistics {
        private long totalExercises;
        private long beginnerExercises;
        private long advancedExercises;
        private long exercisesWithoutEquipment;
        private long unusedExercises;

        public double getBeginnerPercentage() {
            return totalExercises > 0 ? (beginnerExercises * 100.0) / totalExercises : 0.0;
        }

        public double getAdvancedPercentage() {
            return totalExercises > 0 ? (advancedExercises * 100.0) / totalExercises : 0.0;
        }

        public double getNoEquipmentPercentage() {
            return totalExercises > 0 ? (exercisesWithoutEquipment * 100.0) / totalExercises : 0.0;
        }

        public double getUnusedPercentage() {
            return totalExercises > 0 ? (unusedExercises * 100.0) / totalExercises : 0.0;
        }
    }
}