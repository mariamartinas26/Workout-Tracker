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
 * Service pentru gestionarea exercițiilor
 * Conține logica de business pentru operațiile cu exerciții
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    /**
     * Creează un exercițiu nou
     * @param exercise obiectul Exercise cu datele de intrare
     * @return exercițiul creat
     * @throws IllegalArgumentException dacă numele exercițiului există deja
     */
    public Exercise createExercise(Exercise exercise) {
        log.info("Creating new exercise: {}", exercise.getExerciseName());

        validateExerciseData(exercise);
        validateUniqueExerciseName(exercise.getExerciseName());

        exercise.setCreatedAt(LocalDateTime.now());

        Exercise savedExercise = exerciseRepository.save(exercise);
        log.info("Exercise created successfully with ID: {}", savedExercise.getExerciseId());

        return savedExercise;
    }

    /**
     * Găsește un exercițiu după ID
     * @param exerciseId ID-ul exercițiului
     * @return Optional cu exercițiul găsit
     */
    @Transactional(readOnly = true)
    public Optional<Exercise> findById(Long exerciseId) {
        log.debug("Finding exercise by ID: {}", exerciseId);
        return exerciseRepository.findById(exerciseId);
    }

    /**
     * Găsește un exercițiu după nume
     * @param exerciseName numele exercițiului
     * @return Optional cu exercițiul găsit
     */
    @Transactional(readOnly = true)
    public Optional<Exercise> findByName(String exerciseName) {
        log.debug("Finding exercise by name: {}", exerciseName);
        return exerciseRepository.findByExerciseName(exerciseName);
    }

    /**
     * Găsește toate exercițiile
     * @return lista tuturor exercițiilor
     */
    @Transactional(readOnly = true)
    public List<Exercise> findAll() {
        log.debug("Finding all exercises");
        return exerciseRepository.findAll();
    }

    /**
     * Găsește exerciții cu paginare
     * @param pageable informații de paginare
     * @return pagina cu exerciții
     */
    @Transactional(readOnly = true)
    public Page<Exercise> findAll(Pageable pageable) {
        log.debug("Finding exercises with pagination: {}", pageable);
        return exerciseRepository.findAll(pageable);
    }

    /**
     * Găsește exerciții după categorie
     * @param category categoria exercițiului
     * @return lista exercițiilor din categoria specificată
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByCategory(ExerciseCategoryType category) {
        log.debug("Finding exercises by category: {}", category);
        return exerciseRepository.findByCategory(category);
    }

    /**
     * Găsește exerciții după categorie cu paginare
     * @param category categoria exercițiului
     * @param pageable informații de paginare
     * @return pagina cu exerciții din categoria specificată
     */
    @Transactional(readOnly = true)
    public Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable) {
        log.debug("Finding exercises by category: {} with pagination", category);
        return exerciseRepository.findByCategory(category, pageable);
    }

    /**
     * Găsește exerciții după grupul de mușchi principal
     * @param muscleGroup grupul de mușchi
     * @return lista exercițiilor pentru grupul de mușchi specificat
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup) {
        log.debug("Finding exercises by primary muscle group: {}", muscleGroup);
        return exerciseRepository.findByPrimaryMuscleGroup(muscleGroup);
    }

    /**
     * Găsește exerciții după grupul de mușchi principal cu paginare
     * @param muscleGroup grupul de mușchi
     * @param pageable informații de paginare
     * @return pagina cu exerciții pentru grupul de mușchi specificat
     */
    @Transactional(readOnly = true)
    public Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup, Pageable pageable) {
        log.debug("Finding exercises by primary muscle group: {} with pagination", muscleGroup);
        return exerciseRepository.findByPrimaryMuscleGroup(muscleGroup, pageable);
    }

    /**
     * Găsește exerciții după orice grup de mușchi (principal sau secundar)
     * @param muscleGroup grupul de mușchi
     * @return lista exercițiilor care lucrează grupul de mușchi specificat
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByAnyMuscleGroup(MuscleGroupType muscleGroup) {
        log.debug("Finding exercises by any muscle group: {}", muscleGroup);
        return exerciseRepository.findByAnyMuscleGroup(muscleGroup.name());
    }

    /**
     * Găsește exerciții după nivelul de dificultate
     * @param difficultyLevel nivelul de dificultate (1-5)
     * @return lista exercițiilor cu nivelul de dificultate specificat
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByDifficultyLevel(Integer difficultyLevel) {
        log.debug("Finding exercises by difficulty level: {}", difficultyLevel);
        validateDifficultyLevel(difficultyLevel);
        return exerciseRepository.findByDifficultyLevel(difficultyLevel);
    }

    /**
     * Caută exerciții după nume (case insensitive)
     * @param keyword cuvântul cheie pentru căutare
     * @return lista exercițiilor care conțin cuvântul cheie în nume
     */
    @Transactional(readOnly = true)
    public List<Exercise> searchByName(String keyword) {
        log.debug("Searching exercises by name keyword: {}", keyword);

        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        return exerciseRepository.findByExerciseNameContainingIgnoreCase(keyword.trim());
    }

    /**
     * Căutare avansată cu multiple criterii și paginare
     * @param keyword cuvânt cheie pentru nume sau descriere
     * @param category categoria exercițiului
     * @param muscleGroup grupul de mușchi principal
     * @param maxDifficulty nivelul maxim de dificultate
     * @param pageable informații de paginare
     * @return pagina cu exerciții filtrate
     */
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
                category != null ? category.name() : null,
                muscleGroup != null ? muscleGroup.name() : null,
                maxDifficulty,
                pageable);
    }

    /**
     * Găsește exerciții după echipament necesar
     * @param equipment echipamentul necesar
     * @return lista exercițiilor care necesită echipamentul specificat
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByEquipment(String equipment) {
        log.debug("Finding exercises by equipment: {}", equipment);

        if (!StringUtils.hasText(equipment)) {
            return List.of();
        }

        return exerciseRepository.findByEquipmentContainingIgnoreCase(equipment.trim());
    }

    /**
     * Găsește exerciții care nu necesită echipament
     * @return lista exercițiilor fără echipament
     */
    @Transactional(readOnly = true)
    public List<Exercise> findExercisesWithoutEquipment() {
        log.debug("Finding exercises without equipment");
        return exerciseRepository.findExercisesWithoutEquipment();
    }

    /**
     * Actualizează un exercițiu
     * @param exerciseId ID-ul exercițiului de actualizat
     * @param updatedExercise obiectul cu noile date
     * @return exercițiul actualizat
     * @throws IllegalArgumentException dacă exercițiul nu există sau datele nu sunt valide
     */
    public Exercise updateExercise(Long exerciseId, Exercise updatedExercise) {
        log.info("Updating exercise with ID: {}", exerciseId);

        Exercise existingExercise = findExerciseById(exerciseId);
        validateExerciseData(updatedExercise);

        // Verifică unicitatea numelui doar dacă a fost schimbat
        if (!existingExercise.getExerciseName().equals(updatedExercise.getExerciseName())) {
            validateUniqueExerciseName(updatedExercise.getExerciseName());
        }

        // Actualizează câmpurile
        updateExerciseFields(existingExercise, updatedExercise);

        Exercise savedExercise = exerciseRepository.save(existingExercise);
        log.info("Exercise updated successfully: {}", savedExercise.getExerciseName());

        return savedExercise;
    }

    /**
     * Șterge un exercițiu
     * @param exerciseId ID-ul exercițiului de șters
     * @throws IllegalArgumentException dacă exercițiul nu există
     */
    public void deleteExercise(Long exerciseId) {
        log.info("Deleting exercise with ID: {}", exerciseId);

        validateExerciseExists(exerciseId);

        // Verifică dacă exercițiul este folosit în planuri
        long usageCount = exerciseRepository.countWorkoutPlansUsingExercise(exerciseId);
        if (usageCount > 0) {
            throw new IllegalStateException(
                    String.format("Nu se poate șterge exercițiul - este folosit în %d planuri de workout", usageCount));
        }

        try {
            exerciseRepository.deleteById(exerciseId);
            log.info("Exercise deleted successfully: {}", exerciseId);
        } catch (Exception e) {
            log.error("Cannot delete exercise ID: {} - database constraint violation", exerciseId);
            throw new IllegalStateException("Nu se poate șterge exercițiul - este referențiat în alte tabele", e);
        }
    }

    /**
     * Găsește exerciții filtrate după mai multe criterii
     * @param category categoria exercițiului (opțional)
     * @param muscleGroup grupul de mușchi principal (opțional)
     * @param maxDifficulty nivelul maxim de dificultate (opțional)
     * @param equipment echipamentul necesar (opțional)
     * @return lista exercițiilor filtrate
     */
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

    /**
     * Găsește exerciții pentru începători (nivel 1-2)
     * @return lista exercițiilor pentru începători
     */
    @Transactional(readOnly = true)
    public List<Exercise> findBeginnerExercises() {
        log.debug("Finding beginner exercises");
        return exerciseRepository.findByDifficultyLevelLessThanEqual(2);
    }

    /**
     * Găsește exerciții avansate (nivel 4-5)
     * @return lista exercițiilor avansate
     */
    @Transactional(readOnly = true)
    public List<Exercise> findAdvancedExercises() {
        log.debug("Finding advanced exercises");
        return exerciseRepository.findByDifficultyLevelGreaterThanEqual(4);
    }

    /**
     * Găsește exerciții similare pe baza grupului de mușchi și categoriei
     * @param exerciseId ID-ul exercițiului de referință
     * @param limit numărul maxim de exerciții similare
     * @return lista exercițiilor similare
     */
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

    /**
     * Verifică dacă numele exercițiului este disponibil
     * @param exerciseName numele exercițiului de verificat
     * @return true dacă este disponibil, false altfel
     */
    @Transactional(readOnly = true)
    public boolean isExerciseNameAvailable(String exerciseName) {
        return !exerciseRepository.existsByExerciseName(exerciseName);
    }

    /**
     * Găsește exercițiile cele mai populare (folosite în cele mai multe planuri)
     * @param limit numărul maxim de exerciții populare
     * @return lista exercițiilor populare
     */
    @Transactional(readOnly = true)
    public List<Exercise> findMostPopularExercises(int limit) {
        log.debug("Finding {} most popular exercises", limit);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limita trebuie să fie pozitivă");
        }

        return exerciseRepository.findMostPopularExercises(limit);
    }

    /**
     * Găsește exercițiile cele mai populare pentru o categorie specifică
     * @param category categoria exercițiilor
     * @return lista exercițiilor populare din categoria specificată
     */
    @Transactional(readOnly = true)
    public List<Exercise> findPopularExercisesByCategory(ExerciseCategoryType category) {
        log.debug("Finding popular exercises for category: {}", category);
        return exerciseRepository.findPopularExercisesByCategory(category.name());
    }

    /**
     * Găsește exercițiile cele mai frecvent înregistrate în loguri
     * @param limit numărul maxim de exerciții
     * @return lista exercițiilor frecvent înregistrate
     */
    @Transactional(readOnly = true)
    public List<Exercise> findMostLoggedExercises(int limit) {
        log.debug("Finding {} most logged exercises", limit);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limita trebuie să fie pozitivă");
        }

        return exerciseRepository.findMostLoggedExercises(limit);
    }

    /**
     * Găsește exercițiile create recent
     * @param days numărul de zile în urmă
     * @return lista exercițiilor create în ultima perioadă
     */
    @Transactional(readOnly = true)
    public List<Exercise> findRecentExercises(int days) {
        log.debug("Finding exercises created in the last {} days", days);

        if (days <= 0) {
            throw new IllegalArgumentException("Numărul de zile trebuie să fie pozitiv");
        }

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return exerciseRepository.findRecentExercises(since);
    }

    /**
     * Găsește exercițiile nefolosite în planuri
     * @return lista exercițiilor nefolosite
     */
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

        // Convertește lista în array de stringuri pentru native query
        String[] muscleGroupNames = muscleGroups.stream()
                .map(Enum::name)
                .toArray(String[]::new);

        return exerciseRepository.findBySecondaryMuscleGroupsIn(muscleGroupNames);
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

    // Metode private pentru validare și utilități

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
            throw new IllegalArgumentException("Nivelul de dificultate trebuie să fie între 1 și 5");
        }
    }

    private void validateUniqueExerciseName(String exerciseName) {
        if (exerciseRepository.existsByExerciseName(exerciseName)) {
            throw new IllegalArgumentException("Exercițiul există deja: " + exerciseName);
        }
    }

    private Exercise findExerciseById(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercițiul nu a fost găsit cu ID-ul: " + exerciseId));
    }

    private void validateExerciseExists(Long exerciseId) {
        if (!exerciseRepository.existsById(exerciseId)) {
            throw new IllegalArgumentException("Exercițiul nu a fost găsit cu ID-ul: " + exerciseId);
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

    /**
     * Clasa pentru statistici despre exerciții
     */
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