package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.controller.WorkoutPlanController;
import com.marecca.workoutTracker.dto.ExerciseDetailRequest;
import com.marecca.workoutTracker.entity.Exercise;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.entity.WorkoutExerciseDetail;
import com.marecca.workoutTracker.entity.WorkoutPlan;
import com.marecca.workoutTracker.repository.ExerciseRepository;
import com.marecca.workoutTracker.repository.WorkoutPlanRepository;
import com.marecca.workoutTracker.repository.UserRepository;
import com.marecca.workoutTracker.repository.WorkoutExerciseDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pentru gestionarea planurilor de workout
 * Conține logica de business pentru operațiile cu planuri de workout
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkoutPlanService {

    private final WorkoutPlanRepository workoutPlanRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutExerciseDetailRepository workoutExerciseDetailRepository;

    /**
     * Creează un plan de workout nou
     * @param workoutPlan obiectul WorkoutPlan cu datele de intrare
     * @return planul de workout creat
     * @throws IllegalArgumentException dacă utilizatorul nu există sau numele planului există deja
     */
    public WorkoutPlan createWorkoutPlan(WorkoutPlan workoutPlan) {
        log.info("Creating workout plan: {} for user ID: {}",
                workoutPlan.getPlanName(), workoutPlan.getUser().getUserId());

        validateWorkoutPlanData(workoutPlan);

        // Verifică dacă utilizatorul există
        User user = findUserById(workoutPlan.getUser().getUserId());

        // Verifică unicitatea numelui planului pentru utilizator
        validateUniquePlanName(user.getUserId(), workoutPlan.getPlanName());

        workoutPlan.setUser(user);
        workoutPlan.setCreatedAt(LocalDateTime.now());
        workoutPlan.setUpdatedAt(LocalDateTime.now());

        WorkoutPlan savedPlan = workoutPlanRepository.save(workoutPlan);
        log.info("Workout plan created successfully with ID: {}", savedPlan.getWorkoutPlanId());

        return savedPlan;
    }
    /**
     * Creează un plan de workout cu exercițiile sale
     */
    @Transactional
    public WorkoutPlan createWorkoutPlanWithExercises(WorkoutPlan workoutPlan, List<ExerciseDetailRequest> exerciseRequests) {
        log.debug("Creating workout plan with exercises: {}", workoutPlan.getPlanName());

        // Validează utilizatorul
        User user = userRepository.findById(workoutPlan.getUser().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost găsit"));

        workoutPlan.setUser(user);

        // Salvează planul de workout
        WorkoutPlan savedPlan = workoutPlanRepository.save(workoutPlan);

        // Adaugă exercițiile dacă există
        if (exerciseRequests != null && !exerciseRequests.isEmpty()) {
            for (ExerciseDetailRequest exerciseRequest : exerciseRequests) {
                // Verifică că exercițiul există
                Exercise exercise = exerciseRepository.findById(exerciseRequest.getExerciseId())
                        .orElseThrow(() -> new IllegalArgumentException("Exercițiul cu ID " + exerciseRequest.getExerciseId() + " nu a fost găsit"));

                // Creează detaliile exercițiului
                WorkoutExerciseDetail exerciseDetail = WorkoutExerciseDetail.builder()
                        .workoutPlan(savedPlan)
                        .exercise(exercise)
                        .exerciseOrder(exerciseRequest.getExerciseOrder())
                        .targetSets(exerciseRequest.getTargetSets())
                        .targetRepsMin(exerciseRequest.getTargetRepsMin())
                        .targetRepsMax(exerciseRequest.getTargetRepsMax())
                        .targetWeightKg(exerciseRequest.getTargetWeightKg())
                        .targetDurationSeconds(exerciseRequest.getTargetDurationSeconds())
                        .targetDistanceMeters(exerciseRequest.getTargetDistanceMeters())
                        .restTimeSeconds(exerciseRequest.getRestTimeSeconds())
                        .notes(exerciseRequest.getNotes())
                        .build();

                // Salvează detaliile exercițiului
                workoutExerciseDetailRepository.save(exerciseDetail);
            }

            log.info("Workout plan '{}' created with {} exercises", savedPlan.getPlanName(), exerciseRequests.size());
        } else {
            log.info("Workout plan '{}' created without exercises", savedPlan.getPlanName());
        }

        return savedPlan;
    }
    /**
     * Găsește un plan de workout după ID
     * @param workoutPlanId ID-ul planului de workout
     * @return Optional cu planul găsit
     */
    @Transactional(readOnly = true)
    public Optional<WorkoutPlan> findById(Long workoutPlanId) {
        log.debug("Finding workout plan by ID: {}", workoutPlanId);
        return workoutPlanRepository.findById(workoutPlanId);
    }

    /**
     * Găsește toate planurile unui utilizator
     * @param userId ID-ul utilizatorului
     * @return lista planurilor utilizatorului
     */
    @Transactional(readOnly = true)
    public List<WorkoutPlan> findByUserId(Long userId) {
        log.debug("Finding workout plans for user ID: {}", userId);
        validateUserExists(userId);
        return workoutPlanRepository.findByUserUserId(userId);
    }

    /**
     * Găsește planurile unui utilizator cu paginare
     * @param userId ID-ul utilizatorului
     * @param pageable informații de paginare
     * @return pagina cu planurile utilizatorului
     */
    @Transactional(readOnly = true)
    public Page<WorkoutPlan> findByUserId(Long userId, Pageable pageable) {
        log.debug("Finding workout plans for user ID: {} with pagination", userId);
        validateUserExists(userId);
        return workoutPlanRepository.findByUserUserId(userId, pageable);
    }

    /**
     * Găsește un plan după nume și utilizator
     * @param userId ID-ul utilizatorului
     * @param planName numele planului
     * @return Optional cu planul găsit
     */
    @Transactional(readOnly = true)
    public Optional<WorkoutPlan> findByUserIdAndPlanName(Long userId, String planName) {
        log.debug("Finding workout plan by user ID: {} and plan name: {}", userId, planName);
        validateUserExists(userId);
        return workoutPlanRepository.findByUserUserIdAndPlanName(userId, planName);
    }

    /**
     * Actualizează un plan de workout
     * @param workoutPlanId ID-ul planului de actualizat
     * @param updatedPlan obiectul cu noile date
     * @return planul actualizat
     * @throws IllegalArgumentException dacă planul nu există sau datele nu sunt valide
     */
    public WorkoutPlan updateWorkoutPlan(Long workoutPlanId, WorkoutPlan updatedPlan) {
        log.info("Updating workout plan with ID: {}", workoutPlanId);

        WorkoutPlan existingPlan = findWorkoutPlanById(workoutPlanId);
        validateWorkoutPlanData(updatedPlan);

        // Verifică unicitatea numelui doar dacă a fost schimbat
        if (!existingPlan.getPlanName().equals(updatedPlan.getPlanName())) {
            validateUniquePlanName(existingPlan.getUser().getUserId(), updatedPlan.getPlanName());
        }

        // Actualizează câmpurile
        updateWorkoutPlanFields(existingPlan, updatedPlan);
        existingPlan.setUpdatedAt(LocalDateTime.now());

        WorkoutPlan savedPlan = workoutPlanRepository.save(existingPlan);
        log.info("Workout plan updated successfully: {}", savedPlan.getPlanName());

        return savedPlan;
    }

    /**
     * Șterge un plan de workout și toate detaliile asociate
     * @param workoutPlanId ID-ul planului de șters
     * @throws IllegalArgumentException dacă planul nu există
     */
    public void deleteWorkoutPlan(Long workoutPlanId) {
        log.info("Deleting workout plan with ID: {}", workoutPlanId);

        validateWorkoutPlanExists(workoutPlanId);

        try {
            // Șterge mai întâi detaliile exercițiilor
            workoutExerciseDetailRepository.deleteByWorkoutPlanId(workoutPlanId);

            // Apoi șterge planul
            workoutPlanRepository.deleteById(workoutPlanId);
            log.info("Workout plan deleted successfully: {}", workoutPlanId);
        } catch (Exception e) {
            log.error("Cannot delete workout plan ID: {} - it may be referenced by scheduled workouts", workoutPlanId);
            throw new IllegalStateException("Nu se poate șterge planul - este folosit în workout-uri programate", e);
        }
    }

    /**
     * Găsește planuri după nivelul de dificultate
     * @param difficultyLevel nivelul de dificultate (1-5)
     * @return lista planurilor cu nivelul specificat
     */
    @Transactional(readOnly = true)
    public List<WorkoutPlan> findByDifficultyLevel(Integer difficultyLevel) {
        log.debug("Finding workout plans by difficulty level: {}", difficultyLevel);
        validateDifficultyLevel(difficultyLevel);
        return workoutPlanRepository.findByDifficultyLevel(difficultyLevel);
    }

    /**
     * Găsește planuri cu durata maximă specificată
     * @param maxDurationMinutes durata maximă în minute
     * @return lista planurilor cu durata mai mică sau egală cu cea specificată
     */
    @Transactional(readOnly = true)
    public List<WorkoutPlan> findByMaxDuration(Integer maxDurationMinutes) {
        log.debug("Finding workout plans with max duration: {} minutes", maxDurationMinutes);

        if (maxDurationMinutes != null && maxDurationMinutes <= 0) {
            throw new IllegalArgumentException("Durata maximă trebuie să fie pozitivă");
        }

        return workoutPlanRepository.findByEstimatedDurationMinutesLessThanEqual(maxDurationMinutes);
    }

    /**
     * Caută planuri după cuvinte cheie în nume sau descriere
     * @param userId ID-ul utilizatorului
     * @param keyword cuvântul cheie pentru căutare
     * @return lista planurilor care conțin cuvântul cheie
     */
    @Transactional(readOnly = true)
    public List<WorkoutPlan> searchPlans(Long userId, String keyword) {
        log.debug("Searching workout plans for user ID: {} with keyword: {}", userId, keyword);

        validateUserExists(userId);

        if (!StringUtils.hasText(keyword)) {
            return findByUserId(userId);
        }

        return workoutPlanRepository.searchByUserIdAndKeyword(userId, keyword.trim());
    }

    /**
     * Găsește planuri după popularitate (numărul de programări)
     * @param userId ID-ul utilizatorului
     * @return lista planurilor sortate după popularitate
     */
    @Transactional(readOnly = true)
    public List<WorkoutPlan> findByPopularity(Long userId) {
        log.debug("Finding workout plans by popularity for user ID: {}", userId);
        validateUserExists(userId);
        return workoutPlanRepository.findByUserIdOrderByPopularity(userId);
    }

    /**
     * Calculează durata medie a planurilor unui utilizator
     * @param userId ID-ul utilizatorului
     * @return durata medie în minute
     */
    @Transactional(readOnly = true)
    public Double calculateAverageDuration(Long userId) {
        log.debug("Calculating average duration for user ID: {}", userId);
        validateUserExists(userId);
        return workoutPlanRepository.calculateAverageDurationForUser(userId);
    }

    /**
     * Găsește planurile create recent
     * @param userId ID-ul utilizatorului
     * @param pageable informații de paginare
     * @return lista planurilor create recent
     */
    @Transactional(readOnly = true)
    public List<WorkoutPlan> findRecentPlans(Long userId, Pageable pageable) {
        log.debug("Finding recent workout plans for user ID: {}", userId);
        validateUserExists(userId);
        return workoutPlanRepository.findByUserUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Verifică dacă numele planului este disponibil pentru utilizator
     * @param userId ID-ul utilizatorului
     * @param planName numele planului de verificat
     * @return true dacă este disponibil, false altfel
     */
    @Transactional(readOnly = true)
    public boolean isPlanNameAvailable(Long userId, String planName) {
        return !workoutPlanRepository.existsByUserUserIdAndPlanName(userId, planName);
    }

    /**
     * Clonează un plan de workout cu un nume nou
     * @param workoutPlanId ID-ul planului de clonat
     * @param newPlanName numele noului plan
     * @return planul clonat
     * @throws IllegalArgumentException dacă planul nu există sau numele există deja
     */
    public WorkoutPlan cloneWorkoutPlan(Long workoutPlanId, String newPlanName) {
        log.info("Cloning workout plan with ID: {} to new plan: {}", workoutPlanId, newPlanName);

        WorkoutPlan originalPlan = findWorkoutPlanById(workoutPlanId);

        // Verifică unicitatea numelui
        validateUniquePlanName(originalPlan.getUser().getUserId(), newPlanName);

        // Creează noul plan
        WorkoutPlan clonedPlan = new WorkoutPlan();
        clonedPlan.setUser(originalPlan.getUser());
        clonedPlan.setPlanName(newPlanName);
        clonedPlan.setDescription("Copie de la " + originalPlan.getPlanName());
        clonedPlan.setEstimatedDurationMinutes(originalPlan.getEstimatedDurationMinutes());
        clonedPlan.setDifficultyLevel(originalPlan.getDifficultyLevel());
        clonedPlan.setGoals(originalPlan.getGoals());
        clonedPlan.setNotes(originalPlan.getNotes());
        clonedPlan.setCreatedAt(LocalDateTime.now());
        clonedPlan.setUpdatedAt(LocalDateTime.now());

        WorkoutPlan savedPlan = workoutPlanRepository.save(clonedPlan);
        log.info("Workout plan cloned successfully with ID: {}", savedPlan.getWorkoutPlanId());

        return savedPlan;
    }

    /**
     * Numără planurile unui utilizator
     * @param userId ID-ul utilizatorului
     * @return numărul de planuri
     */
    @Transactional(readOnly = true)
    public long countByUserId(Long userId) {
        log.debug("Counting workout plans for user ID: {}", userId);
        validateUserExists(userId);
        return workoutPlanRepository.countByUserUserId(userId);
    }

    /**
     * Verifică dacă un plan aparține unui utilizator
     * @param workoutPlanId ID-ul planului
     * @param userId ID-ul utilizatorului
     * @return true dacă planul aparține utilizatorului
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long workoutPlanId, Long userId) {
        log.debug("Checking ownership of workout plan ID: {} by user ID: {}", workoutPlanId, userId);

        Optional<WorkoutPlan> plan = workoutPlanRepository.findById(workoutPlanId);
        return plan.isPresent() && plan.get().getUser().getUserId().equals(userId);
    }

    /**
     * Găsește toate planurile (pentru admin)
     * @param pageable informații de paginare
     * @return pagina cu toate planurile
     */
    @Transactional(readOnly = true)
    public Page<WorkoutPlan> findAll(Pageable pageable) {
        log.debug("Finding all workout plans with pagination");
        return workoutPlanRepository.findAll(pageable);
    }

    // Metode private pentru validare și utilități

    private void validateWorkoutPlanData(WorkoutPlan workoutPlan) {
        if (!StringUtils.hasText(workoutPlan.getPlanName())) {
            throw new IllegalArgumentException("Numele planului este obligatoriu");
        }

        if (workoutPlan.getUser() == null || workoutPlan.getUser().getUserId() == null) {
            throw new IllegalArgumentException("Utilizatorul este obligatoriu");
        }

        if (workoutPlan.getEstimatedDurationMinutes() != null && workoutPlan.getEstimatedDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Durata estimată trebuie să fie pozitivă");
        }

        if (workoutPlan.getDifficultyLevel() != null) {
            validateDifficultyLevel(workoutPlan.getDifficultyLevel());
        }
    }

    private void validateDifficultyLevel(Integer difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("Nivelul de dificultate trebuie să fie între 1 și 5");
        }
    }

    private void validateUniquePlanName(Long userId, String planName) {
        if (workoutPlanRepository.existsByUserUserIdAndPlanName(userId, planName)) {
            throw new IllegalArgumentException("Numele planului există deja pentru acest utilizator: " + planName);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost găsit cu ID-ul: " + userId));
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Utilizatorul nu a fost găsit cu ID-ul: " + userId);
        }
    }

    private WorkoutPlan findWorkoutPlanById(Long workoutPlanId) {
        return workoutPlanRepository.findById(workoutPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Planul de workout nu a fost găsit cu ID-ul: " + workoutPlanId));
    }

    private void validateWorkoutPlanExists(Long workoutPlanId) {
        if (!workoutPlanRepository.existsById(workoutPlanId)) {
            throw new IllegalArgumentException("Planul de workout nu a fost găsit cu ID-ul: " + workoutPlanId);
        }
    }

    private void updateWorkoutPlanFields(WorkoutPlan existing, WorkoutPlan updated) {
        existing.setPlanName(updated.getPlanName());
        existing.setDescription(updated.getDescription());
        existing.setEstimatedDurationMinutes(updated.getEstimatedDurationMinutes());
        existing.setDifficultyLevel(updated.getDifficultyLevel());
        existing.setGoals(updated.getGoals());
        existing.setNotes(updated.getNotes());
    }
}