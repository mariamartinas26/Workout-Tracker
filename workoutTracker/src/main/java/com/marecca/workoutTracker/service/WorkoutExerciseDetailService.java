package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.WorkoutExerciseDetail;
import com.marecca.workoutTracker.entity.WorkoutPlan;
import com.marecca.workoutTracker.repository.WorkoutExerciseDetailRepository;
import com.marecca.workoutTracker.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pentru gestionarea detaliilor exercițiilor din planurile de workout
 * Conține logica de business pentru operațiile cu detalii de exerciții
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkoutExerciseDetailService {

    private final WorkoutExerciseDetailRepository workoutExerciseDetailRepository;
    private final WorkoutPlanRepository workoutPlanRepository;

    /**
     * Adaugă un exercițiu la un plan de workout
     * @param detail detaliile exercițiului de adăugat
     * @return detaliile exercițiului salvate
     * @throws IllegalArgumentException dacă planul nu există sau exercițiul este deja în plan
     */
    public WorkoutExerciseDetail addExerciseToWorkoutPlan(WorkoutExerciseDetail detail) {
        log.info("Adding exercise ID: {} to workout plan ID: {}",
                detail.getExercise().getExerciseId(),
                detail.getWorkoutPlan().getWorkoutPlanId());

        validateExerciseDetailData(detail);

        // Verifică dacă planul de workout există
        WorkoutPlan workoutPlan = findWorkoutPlanById(detail.getWorkoutPlan().getWorkoutPlanId());

        // Verifică dacă exercițiul nu este deja în plan
        if (workoutExerciseDetailRepository.existsByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(
                workoutPlan.getWorkoutPlanId(), detail.getExercise().getExerciseId())) {
            throw new IllegalArgumentException("Exercițiul există deja în acest plan de workout");
        }

        detail.setCreatedAt(LocalDateTime.now());

        // Actualizează timestamp-ul planului
        workoutPlan.setUpdatedAt(LocalDateTime.now());
        workoutPlanRepository.save(workoutPlan);

        WorkoutExerciseDetail savedDetail = workoutExerciseDetailRepository.save(detail);
        log.info("Exercise detail added successfully with ID: {}", savedDetail.getWorkoutExerciseDetailId());

        return savedDetail;
    }

    /**
     * Găsește toate exercițiile dintr-un plan de workout, ordonate după exerciseOrder
     * @param workoutPlanId ID-ul planului de workout
     * @return lista detaliilor exercițiilor din plan
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseDetail> findByWorkoutPlanId(Long workoutPlanId) {
        log.debug("Finding exercise details for workout plan ID: {}", workoutPlanId);
        validateWorkoutPlanExists(workoutPlanId);
        return workoutExerciseDetailRepository.findByWorkoutPlanWorkoutPlanIdOrderByExerciseOrder(workoutPlanId);
    }

    /**
     * Găsește un exercițiu specific dintr-un plan
     * @param workoutPlanId ID-ul planului de workout
     * @param exerciseId ID-ul exercițiului
     * @return Optional cu detaliile exercițiului găsit
     */
    @Transactional(readOnly = true)
    public Optional<WorkoutExerciseDetail> findByWorkoutPlanIdAndExerciseId(
            Long workoutPlanId, Long exerciseId) {
        log.debug("Finding exercise detail for workout plan ID: {} and exercise ID: {}", workoutPlanId, exerciseId);

        WorkoutExerciseDetail detail = workoutExerciseDetailRepository
                .findByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(workoutPlanId, exerciseId);
        return Optional.ofNullable(detail);
    }

    /**
     * Găsește detaliul exercițiului după ID
     * @param detailId ID-ul detaliului exercițiului
     * @return Optional cu detaliile exercițiului
     */
    @Transactional(readOnly = true)
    public Optional<WorkoutExerciseDetail> findById(Long detailId) {
        log.debug("Finding exercise detail by ID: {}", detailId);
        return workoutExerciseDetailRepository.findById(detailId);
    }

    /**
     * Actualizează detaliile unui exercițiu dintr-un plan
     * @param detailId ID-ul detaliului de actualizat
     * @param updatedDetail obiectul cu noile date
     * @return detaliile actualizate
     * @throws IllegalArgumentException dacă detaliul nu există
     */
    public WorkoutExerciseDetail updateExerciseDetail(Long detailId, WorkoutExerciseDetail updatedDetail) {
        log.info("Updating exercise detail with ID: {}", detailId);

        WorkoutExerciseDetail existingDetail = findExerciseDetailById(detailId);
        validateExerciseDetailData(updatedDetail);

        // Actualizează câmpurile
        updateExerciseDetailFields(existingDetail, updatedDetail);

        // Actualizează timestamp-ul planului
        existingDetail.getWorkoutPlan().setUpdatedAt(LocalDateTime.now());
        workoutPlanRepository.save(existingDetail.getWorkoutPlan());

        WorkoutExerciseDetail savedDetail = workoutExerciseDetailRepository.save(existingDetail);
        log.info("Exercise detail updated successfully: {}", savedDetail.getWorkoutExerciseDetailId());

        return savedDetail;
    }

    /**
     * Șterge un exercițiu dintr-un plan
     * @param workoutPlanId ID-ul planului de workout
     * @param exerciseId ID-ul exercițiului de șters
     * @throws IllegalArgumentException dacă exercițiul nu există în plan
     */
    public void removeExerciseFromWorkoutPlan(Long workoutPlanId, Long exerciseId) {
        log.info("Removing exercise ID: {} from workout plan ID: {}", exerciseId, workoutPlanId);

        if (!workoutExerciseDetailRepository.existsByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(
                workoutPlanId, exerciseId)) {
            throw new IllegalArgumentException("Exercițiul nu a fost găsit în acest plan de workout");
        }

        workoutExerciseDetailRepository.deleteByWorkoutPlanIdAndExerciseId(workoutPlanId, exerciseId);

        // Actualizează timestamp-ul planului
        WorkoutPlan workoutPlan = findWorkoutPlanById(workoutPlanId);
        workoutPlan.setUpdatedAt(LocalDateTime.now());
        workoutPlanRepository.save(workoutPlan);

        log.info("Exercise removed successfully from workout plan");
    }

    /**
     * Șterge un detaliu de exercițiu după ID
     * @param detailId ID-ul detaliului de șters
     * @throws IllegalArgumentException dacă detaliul nu există
     */
    public void deleteExerciseDetail(Long detailId) {
        log.info("Deleting exercise detail with ID: {}", detailId);

        WorkoutExerciseDetail detail = findExerciseDetailById(detailId);

        workoutExerciseDetailRepository.deleteById(detailId);

        // Actualizează timestamp-ul planului
        detail.getWorkoutPlan().setUpdatedAt(LocalDateTime.now());
        workoutPlanRepository.save(detail.getWorkoutPlan());

        log.info("Exercise detail deleted successfully: {}", detailId);
    }

    /**
     * Șterge toate exercițiile dintr-un plan
     * @param workoutPlanId ID-ul planului de workout
     */
    public void deleteByWorkoutPlanId(Long workoutPlanId) {
        log.info("Deleting all exercise details from workout plan ID: {}", workoutPlanId);

        validateWorkoutPlanExists(workoutPlanId);
        workoutExerciseDetailRepository.deleteByWorkoutPlanId(workoutPlanId);

        log.info("All exercise details deleted from workout plan: {}", workoutPlanId);
    }

    /**
     * Actualizează ordinea unui exercițiu în plan
     * @param workoutPlanId ID-ul planului de workout
     * @param exerciseId ID-ul exercițiului
     * @param newOrder noua ordine
     * @throws IllegalArgumentException dacă exercițiul nu există în plan
     */
    public void updateExerciseOrder(Long workoutPlanId, Long exerciseId, Integer newOrder) {
        log.info("Updating exercise order for exercise ID: {} in plan ID: {} to order: {}",
                exerciseId, workoutPlanId, newOrder);

        if (newOrder == null || newOrder < 1) {
            throw new IllegalArgumentException("Ordinea exercițiului trebuie să fie un număr pozitiv");
        }

        if (!workoutExerciseDetailRepository.existsByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(
                workoutPlanId, exerciseId)) {
            throw new IllegalArgumentException("Exercițiul nu a fost găsit în acest plan de workout");
        }

        workoutExerciseDetailRepository.updateExerciseOrder(workoutPlanId, exerciseId, newOrder);

        // Actualizează timestamp-ul planului
        WorkoutPlan workoutPlan = findWorkoutPlanById(workoutPlanId);
        workoutPlan.setUpdatedAt(LocalDateTime.now());
        workoutPlanRepository.save(workoutPlan);

        log.info("Exercise order updated successfully");
    }

    /**
     * Găsește toate detaliile exercițiilor pentru un exercițiu specific
     * @param exerciseId ID-ul exercițiului
     * @return lista detaliilor unde apare exercițiul
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseDetail> findByExerciseId(Long exerciseId) {
        log.debug("Finding exercise details for exercise ID: {}", exerciseId);
        return workoutExerciseDetailRepository.findByExerciseExerciseId(exerciseId);
    }

    /**
     * Găsește exercițiile cu greutate țintă mai mare sau egală cu valoarea specificată
     * @param minWeight greutatea minimă
     * @return lista detaliilor exercițiilor
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseDetail> findByMinWeight(BigDecimal minWeight) {
        log.debug("Finding exercise details with min weight: {}", minWeight);

        if (minWeight != null && minWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Greutatea minimă trebuie să fie pozitivă");
        }

        return workoutExerciseDetailRepository.findByTargetWeightKgGreaterThanEqual(minWeight);
    }

    /**
     * Găsește exercițiile cu un număr minim de seturi țintă
     * @param minSets numărul minim de seturi
     * @return lista detaliilor exercițiilor
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseDetail> findByMinSets(Integer minSets) {
        log.debug("Finding exercise details with min sets: {}", minSets);

        if (minSets != null && minSets < 1) {
            throw new IllegalArgumentException("Numărul minim de seturi trebuie să fie pozitiv");
        }

        return workoutExerciseDetailRepository.findByTargetSetsGreaterThanEqual(minSets);
    }

    /**
     * Verifică dacă un exercițiu este inclus într-un plan
     * @param workoutPlanId ID-ul planului de workout
     * @param exerciseId ID-ul exercițiului
     * @return true dacă exercițiul este în plan, false altfel
     */
    @Transactional(readOnly = true)
    public boolean isExerciseInPlan(Long workoutPlanId, Long exerciseId) {
        log.debug("Checking if exercise ID: {} is in workout plan ID: {}", exerciseId, workoutPlanId);
        return workoutExerciseDetailRepository.existsByWorkoutPlanWorkoutPlanIdAndExerciseExerciseId(
                workoutPlanId, exerciseId);
    }

    /**
     * Găsește toate detaliile exercițiilor pentru planurile unui utilizator
     * @param userId ID-ul utilizatorului
     * @return lista detaliilor exercițiilor
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseDetail> findByUserId(Long userId) {
        log.debug("Finding exercise details for user ID: {}", userId);
        return workoutExerciseDetailRepository.findByUserId(userId);
    }

    /**
     * Găsește exercițiile cele mai populare (folosite în cele mai multe planuri)
     * @param limit numărul maxim de exerciții populare
     * @return lista detaliilor exercițiilor populare
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseDetail> findMostPopularExercises(int limit) {
        log.debug("Finding {} most popular exercises", limit);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limita trebuie să fie pozitivă");
        }

        return workoutExerciseDetailRepository.findMostPopularExercises(limit);
    }

    /**
     * Numără exercițiile dintr-un plan de workout
     * @param workoutPlanId ID-ul planului de workout
     * @return numărul de exerciții din plan
     */
    @Transactional(readOnly = true)
    public long countExercisesInPlan(Long workoutPlanId) {
        log.debug("Counting exercises in workout plan ID: {}", workoutPlanId);
        validateWorkoutPlanExists(workoutPlanId);
        return workoutExerciseDetailRepository.findByWorkoutPlanWorkoutPlanIdOrderByExerciseOrder(workoutPlanId).size();
    }

    /**
     * Clonează detaliile exercițiilor dintr-un plan în alt plan
     * @param sourceWorkoutPlanId ID-ul planului sursă
     * @param targetWorkoutPlanId ID-ul planului destinație
     * @return lista detaliilor exercițiilor clonate
     */
    public List<WorkoutExerciseDetail> cloneExerciseDetails(Long sourceWorkoutPlanId, Long targetWorkoutPlanId) {
        log.info("Cloning exercise details from plan ID: {} to plan ID: {}", sourceWorkoutPlanId, targetWorkoutPlanId);

        validateWorkoutPlanExists(sourceWorkoutPlanId);
        WorkoutPlan targetPlan = findWorkoutPlanById(targetWorkoutPlanId);

        List<WorkoutExerciseDetail> sourceDetails = findByWorkoutPlanId(sourceWorkoutPlanId);

        return sourceDetails.stream()
                .map(sourceDetail -> {
                    WorkoutExerciseDetail clonedDetail = new WorkoutExerciseDetail();
                    clonedDetail.setWorkoutPlan(targetPlan);
                    clonedDetail.setExercise(sourceDetail.getExercise());
                    clonedDetail.setExerciseOrder(sourceDetail.getExerciseOrder());
                    clonedDetail.setTargetSets(sourceDetail.getTargetSets());
                    clonedDetail.setTargetRepsMin(sourceDetail.getTargetRepsMin());
                    clonedDetail.setTargetRepsMax(sourceDetail.getTargetRepsMax());
                    clonedDetail.setTargetWeightKg(sourceDetail.getTargetWeightKg());
                    clonedDetail.setTargetDurationSeconds(sourceDetail.getTargetDurationSeconds());
                    clonedDetail.setTargetDistanceMeters(sourceDetail.getTargetDistanceMeters());
                    clonedDetail.setRestTimeSeconds(sourceDetail.getRestTimeSeconds());
                    clonedDetail.setNotes(sourceDetail.getNotes());
                    clonedDetail.setCreatedAt(LocalDateTime.now());

                    return workoutExerciseDetailRepository.save(clonedDetail);
                })
                .toList();
    }

    /**
     * Reordonează exercițiile dintr-un plan
     * @param workoutPlanId ID-ul planului de workout
     * @param exerciseOrders mapă cu ID-ul exercițiului și noua ordine
     */
    public void reorderExercises(Long workoutPlanId, java.util.Map<Long, Integer> exerciseOrders) {
        log.info("Reordering exercises in workout plan ID: {}", workoutPlanId);

        validateWorkoutPlanExists(workoutPlanId);

        for (java.util.Map.Entry<Long, Integer> entry : exerciseOrders.entrySet()) {
            Long exerciseId = entry.getKey();
            Integer newOrder = entry.getValue();

            if (newOrder == null || newOrder < 1) {
                throw new IllegalArgumentException("Ordinea trebuie să fie un număr pozitiv pentru exercițiul: " + exerciseId);
            }

            updateExerciseOrder(workoutPlanId, exerciseId, newOrder);
        }

        log.info("Exercises reordered successfully in workout plan: {}", workoutPlanId);
    }

    // Metode private pentru validare și utilități

    private void validateExerciseDetailData(WorkoutExerciseDetail detail) {
        if (detail.getWorkoutPlan() == null || detail.getWorkoutPlan().getWorkoutPlanId() == null) {
            throw new IllegalArgumentException("Planul de workout este obligatoriu");
        }

        if (detail.getExercise() == null || detail.getExercise().getExerciseId() == null) {
            throw new IllegalArgumentException("Exercițiul este obligatoriu");
        }

        if (detail.getTargetSets() == null || detail.getTargetSets() < 1) {
            throw new IllegalArgumentException("Numărul de seturi țintă trebuie să fie pozitiv");
        }

        if (detail.getExerciseOrder() == null || detail.getExerciseOrder() < 1) {
            throw new IllegalArgumentException("Ordinea exercițiului trebuie să fie pozitivă");
        }

        // Validare pentru cel puțin un target metric
        boolean hasTargetMetric = (detail.getTargetRepsMin() != null && detail.getTargetRepsMin() > 0) ||
                (detail.getTargetDurationSeconds() != null && detail.getTargetDurationSeconds() > 0) ||
                (detail.getTargetDistanceMeters() != null && detail.getTargetDistanceMeters().compareTo(BigDecimal.ZERO) > 0);

        if (!hasTargetMetric) {
            throw new IllegalArgumentException("Trebuie specificat cel puțin un obiectiv: repetări, durată sau distanță");
        }

        // Validări pentru valori pozitive
        if (detail.getTargetRepsMin() != null && detail.getTargetRepsMin() < 1) {
            throw new IllegalArgumentException("Numărul minim de repetări trebuie să fie pozitiv");
        }

        if (detail.getTargetRepsMax() != null && detail.getTargetRepsMax() < 1) {
            throw new IllegalArgumentException("Numărul maxim de repetări trebuie să fie pozitiv");
        }

        if (detail.getTargetRepsMin() != null && detail.getTargetRepsMax() != null &&
                detail.getTargetRepsMax() < detail.getTargetRepsMin()) {
            throw new IllegalArgumentException("Numărul maxim de repetări trebuie să fie mai mare sau egal cu minimul");
        }

        if (detail.getTargetWeightKg() != null && detail.getTargetWeightKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Greutatea țintă trebuie să fie pozitivă");
        }

        if (detail.getTargetDurationSeconds() != null && detail.getTargetDurationSeconds() < 1) {
            throw new IllegalArgumentException("Durata țintă trebuie să fie pozitivă");
        }

        if (detail.getTargetDistanceMeters() != null && detail.getTargetDistanceMeters().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Distanța țintă trebuie să fie pozitivă");
        }

        if (detail.getRestTimeSeconds() != null && detail.getRestTimeSeconds() < 0) {
            throw new IllegalArgumentException("Timpul de odihnă nu poate fi negativ");
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

    private WorkoutExerciseDetail findExerciseDetailById(Long detailId) {
        return workoutExerciseDetailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Detaliile exercițiului nu au fost găsite cu ID-ul: " + detailId));
    }

    private void updateExerciseDetailFields(WorkoutExerciseDetail existing, WorkoutExerciseDetail updated) {
        existing.setExerciseOrder(updated.getExerciseOrder());
        existing.setTargetSets(updated.getTargetSets());
        existing.setTargetRepsMin(updated.getTargetRepsMin());
        existing.setTargetRepsMax(updated.getTargetRepsMax());
        existing.setTargetWeightKg(updated.getTargetWeightKg());
        existing.setTargetDurationSeconds(updated.getTargetDurationSeconds());
        existing.setTargetDistanceMeters(updated.getTargetDistanceMeters());
        existing.setRestTimeSeconds(updated.getRestTimeSeconds());
        existing.setNotes(updated.getNotes());
    }
}