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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public Exercise createExercise(Exercise exercise) {
        log.info("Creating new exercise: {}", exercise.getExerciseName());

        validateExerciseData(exercise);
        validateUniqueExerciseName(exercise.getExerciseName());

        exercise.setCreatedAt(LocalDateTime.now());

        Exercise savedExercise = exerciseRepository.save(exercise);
        log.info("Exercise created successfully with ID: {}", savedExercise.getExerciseId());

        return savedExercise;
    }

    @Transactional(readOnly = true)
    public Optional<Exercise> findById(Long exerciseId) {
        log.debug("Finding exercise by ID: {}", exerciseId);
        return exerciseRepository.findById(exerciseId);
    }

    @Transactional(readOnly = true)
    public Optional<Exercise> findByName(String exerciseName) {
        log.debug("Finding exercise by name: {}", exerciseName);
        return exerciseRepository.findByExerciseName(exerciseName);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findAll() {
        log.debug("Finding all exercises");
        return exerciseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Exercise> findAll(Pageable pageable) {
        log.debug("Finding exercises with pagination: {}", pageable);
        return exerciseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findByCategory(ExerciseCategoryType category) {
        log.debug("Finding exercises by category: {}", category);
        return exerciseRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable) {
        log.debug("Finding exercises by category: {} with pagination", category);
        return exerciseRepository.findByCategory(category, pageable);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup) {
        log.debug("Finding exercises by primary muscle group: {}", muscleGroup);
        return exerciseRepository.findByPrimaryMuscleGroup(muscleGroup);
    }


    @Transactional(readOnly = true)
    public Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup, Pageable pageable) {
        log.debug("Finding exercises by primary muscle group: {} with pagination", muscleGroup);
        return exerciseRepository.findByPrimaryMuscleGroup(muscleGroup, pageable);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findByAnyMuscleGroup(MuscleGroupType muscleGroup) {
        log.debug("Finding exercises by any muscle group: {}", muscleGroup);
        return exerciseRepository.findByAnyMuscleGroup(muscleGroup);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findByDifficultyLevel(Integer difficultyLevel) {
        log.debug("Finding exercises by difficulty level: {}", difficultyLevel);
        validateDifficultyLevel(difficultyLevel);
        return exerciseRepository.findByDifficultyLevel(difficultyLevel);
    }

    @Transactional(readOnly = true)
    public List<Exercise> searchByName(String keyword) {
        log.debug("Searching exercises by name keyword: {}", keyword);

        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        return exerciseRepository.findByExerciseNameContainingIgnoreCase(keyword.trim());
    }

    @Transactional(readOnly = true)
    public Page<Exercise> searchExercises(
            String keyword,
            ExerciseCategoryType category,
            MuscleGroupType muscleGroup,
            Integer maxDifficulty,
            Pageable pageable) {

        log.debug("Advanced search - Keyword: {}, Category: {}, MuscleGroup: {}, MaxDifficulty: {}",
                keyword, category, muscleGroup, maxDifficulty);

        if (maxDifficulty != null) {
            validateDifficultyLevel(maxDifficulty);
        }

        return exerciseRepository.searchExercises(
                StringUtils.hasText(keyword) ? keyword.trim() : null,
                category,
                muscleGroup,
                maxDifficulty,
                pageable);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findByEquipment(String equipment) {
        log.debug("Finding exercises by equipment: {}", equipment);

        if (!StringUtils.hasText(equipment)) {
            return List.of();
        }

        return exerciseRepository.findByEquipmentNeededContainingIgnoreCase(equipment.trim());
    }

    @Transactional(readOnly = true)
    public List<Exercise> findExercisesWithoutEquipment() {
        log.debug("Finding exercises without equipment");
        return exerciseRepository.findExercisesWithoutEquipment();
    }

    public Exercise updateExercise(Long exerciseId, Exercise updatedExercise) {
        log.info("Updating exercise with ID: {}", exerciseId);

        Exercise existingExercise = findExerciseById(exerciseId);
        validateExerciseData(updatedExercise);

        if (!existingExercise.getExerciseName().equals(updatedExercise.getExerciseName())) {
            validateUniqueExerciseName(updatedExercise.getExerciseName());
        }

        updateExerciseFields(existingExercise, updatedExercise);

        Exercise savedExercise = exerciseRepository.save(existingExercise);
        log.info("Exercise updated successfully: {}", savedExercise.getExerciseName());

        return savedExercise;
    }

    public void deleteExercise(Long exerciseId) {
        log.info("Deleting exercise with ID: {}", exerciseId);

        validateExerciseExists(exerciseId);

        long usageCount = exerciseRepository.countWorkoutPlansUsingExercise(exerciseId);
        if (usageCount > 0) {
            throw new IllegalStateException(
                    String.format("Cannot delete the exercise - it is used in %d workout plans", usageCount));
        }

        try {
            exerciseRepository.deleteById(exerciseId);
            log.info("Exercise deleted successfully: {}", exerciseId);
        } catch (Exception e) {
            log.error("Cannot delete exercise ID: {} - database constraint violation", exerciseId);
            throw new IllegalStateException("Cannot delete the exercise - it is refrenced in other tables", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Exercise> findFilteredExercises(
            ExerciseCategoryType category,
            MuscleGroupType muscleGroup,
            Integer maxDifficulty,
            String equipment) {

        log.debug("Finding filtered exercises - Category: {}, MuscleGroup: {}, MaxDifficulty: {}, Equipment: {}",
                category, muscleGroup, maxDifficulty, equipment);

        if (maxDifficulty != null) {
            validateDifficultyLevel(maxDifficulty);
        }

        return exerciseRepository.findFilteredExercises(category, muscleGroup, maxDifficulty, equipment);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findBeginnerExercises() {
        log.debug("Finding beginner exercises");
        return exerciseRepository.findByDifficultyLevelLessThanEqual(2);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findAdvancedExercises() {
        log.debug("Finding advanced exercises");
        return exerciseRepository.findByDifficultyLevelGreaterThanEqual(4);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findSimilarExercises(Long exerciseId, int limit) {
        log.debug("Finding similar exercises to exercise ID: {} (limit: {})", exerciseId, limit);

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
        log.debug("Finding {} most popular exercises", limit);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit should be positive");
        }

        return exerciseRepository.findMostPopularExercises(limit);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findPopularExercisesByCategory(ExerciseCategoryType category) {
        log.debug("Finding popular exercises for category: {}", category);
        return exerciseRepository.findPopularExercisesByCategory(category.name());
    }

    @Transactional(readOnly = true)
    public List<Exercise> findMostLoggedExercises(int limit) {
        log.debug("Finding {} most logged exercises", limit);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit should be positive");
        }

        return exerciseRepository.findMostLoggedExercises(limit);
    }


    @Transactional(readOnly = true)
    public List<Exercise> findRecentExercises(int days) {
        log.debug("Finding exercises created in the last {} days", days);

        if (days <= 0) {
            throw new IllegalArgumentException("Nr of days should be positive");
        }

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return exerciseRepository.findRecentExercises(since);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findUnusedExercises() {
        log.debug("Finding unused exercises");
        return exerciseRepository.findUnusedExercises();
    }

    /**
     * Numără exercițiile după categorie
     * @param category categoria exercițiilor
     * @return numărul de exerciții din categoria respectivă
     */
    @Transactional(readOnly = true)
    public long countByCategory(ExerciseCategoryType category) {
        return exerciseRepository.countByCategory(category);
    }

    /**
     * Numără exercițiile după grupul de mușchi
     * @param muscleGroup grupul de mușchi
     * @return numărul de exerciții pentru grupul de mușchi respectiv
     */
    @Transactional(readOnly = true)
    public long countByMuscleGroup(MuscleGroupType muscleGroup) {
        return exerciseRepository.countByPrimaryMuscleGroup(muscleGroup);
    }

    /**
     * Numără câte planuri folosesc un exercițiu
     * @param exerciseId ID-ul exercițiului
     * @return numărul de planuri care folosesc exercițiul
     */
    @Transactional(readOnly = true)
    public long countWorkoutPlansUsingExercise(Long exerciseId) {
        log.debug("Counting workout plans using exercise ID: {}", exerciseId);
        validateExerciseExists(exerciseId);
        return exerciseRepository.countWorkoutPlansUsingExercise(exerciseId);
    }

    /**
     * Găsește exercițiile pentru grupuri secundare de mușchi
     * @param muscleGroups lista grupurilor de mușchi
     * @return lista exercițiilor care au cel puțin unul din grupurile secundare specificate
     */
    @Transactional(readOnly = true)
    public List<Exercise> findBySecondaryMuscleGroups(List<MuscleGroupType> muscleGroups) {
        log.debug("Finding exercises by secondary muscle groups: {}", muscleGroups);

        if (muscleGroups == null || muscleGroups.isEmpty()) {
            return List.of();
        }

        return exerciseRepository.findBySecondaryMuscleGroupsIn(muscleGroups);
    }

    /**
     * Obține statistici despre exerciții
     * @return obiect cu statistici generale
     */
    @Transactional(readOnly = true)
    public ExerciseStatistics getExerciseStatistics() {
        log.debug("Getting exercise statistics");

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
            throw new IllegalArgumentException("Numele exercițiului este obligatoriu");
        }

        if (exercise.getCategory() == null) {
            throw new IllegalArgumentException("Categoria exercițiului este obligatorie");
        }

        if (exercise.getPrimaryMuscleGroup() == null) {
            throw new IllegalArgumentException("Grupul de mușchi principal este obligatoriu");
        }

        if (exercise.getDifficultyLevel() != null) {
            validateDifficultyLevel(exercise.getDifficultyLevel());
        }

        // Validare pentru grupurile secundare de mușchi
        if (exercise.getSecondaryMuscleGroups() != null) {
            // Verifică că grupul principal nu este și în grupurile secundare
            if (exercise.getSecondaryMuscleGroups().contains(exercise.getPrimaryMuscleGroup())) {
                throw new IllegalArgumentException("Grupul de mușchi principal nu poate fi și în grupurile secundare");
            }
        }
    }

    private void validateDifficultyLevel(Integer difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("Level of difficulty has to be between 1 and 5");
        }
    }

    private void validateUniqueExerciseName(String exerciseName) {
        if (exerciseRepository.existsByExerciseName(exerciseName)) {
            throw new IllegalArgumentException("Exercise already exists: " + exerciseName);
        }
    }

    private Exercise findExerciseById(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise could not be found with the id: " + exerciseId));
    }

    private void validateExerciseExists(Long exerciseId) {
        if (!exerciseRepository.existsById(exerciseId)) {
            throw new IllegalArgumentException("Exercise could not be found with the id: " + exerciseId);
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