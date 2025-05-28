package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.WorkoutExerciseLog;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import com.marecca.workoutTracker.repository.WorkoutExerciseLogRepository;
import com.marecca.workoutTracker.repository.ScheduledWorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pentru gestionarea logurilor de exerciții din workout-uri
 * Conține logica de business pentru înregistrarea și analiza performanțelor
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkoutExerciseLogService {

    private final WorkoutExerciseLogRepository workoutExerciseLogRepository;
    private final ScheduledWorkoutRepository scheduledWorkoutRepository;

    /**
     * Înregistrează un log de exercițiu
     * @param exerciseLog obiectul WorkoutExerciseLog cu datele de intrare
     * @return log-ul de exercițiu salvat
     * @throws IllegalArgumentException dacă datele nu sunt valide
     */
    public WorkoutExerciseLog logExercise(WorkoutExerciseLog exerciseLog) {
        log.info("Logging exercise ID: {} for scheduled workout ID: {}",
                exerciseLog.getExercise().getExerciseId(),
                exerciseLog.getScheduledWorkout().getScheduledWorkoutId());

        validateExerciseLogData(exerciseLog);

        // Verifică dacă workout-ul programat există și este în progres
        ScheduledWorkout scheduledWorkout = findScheduledWorkoutById(
                exerciseLog.getScheduledWorkout().getScheduledWorkoutId());

        if (scheduledWorkout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
            throw new IllegalStateException("Se pot înregistra exerciții doar pentru workout-uri în progres");
        }

        exerciseLog.setCreatedAt(LocalDateTime.now());

        WorkoutExerciseLog savedLog = workoutExerciseLogRepository.save(exerciseLog);
        log.info("Exercise log created successfully with ID: {}", savedLog.getLogId());

        return savedLog;
    }

    /**
     * Găsește un log de exercițiu după ID
     * @param logId ID-ul log-ului
     * @return Optional cu log-ul găsit
     */
    @Transactional(readOnly = true)
    public Optional<WorkoutExerciseLog> findById(Long logId) {
        log.debug("Finding exercise log by ID: {}", logId);
        return workoutExerciseLogRepository.findById(logId);
    }

    /**
     * Găsește toate logurile pentru un workout programat
     * @param scheduledWorkoutId ID-ul workout-ului programat
     * @return lista logurilor ordonate după ordinea exercițiilor
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findByScheduledWorkoutId(Long scheduledWorkoutId) {
        log.debug("Finding exercise logs for scheduled workout ID: {}", scheduledWorkoutId);
        validateScheduledWorkoutExists(scheduledWorkoutId);
        return workoutExerciseLogRepository.findByScheduledWorkoutIdOrderByExerciseOrder(scheduledWorkoutId);
    }

    /**
     * Găsește toate logurile pentru un exercițiu specific
     * @param exerciseId ID-ul exercițiului
     * @return lista logurilor pentru exercițiu
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findByExerciseId(Long exerciseId) {
        log.debug("Finding exercise logs for exercise ID: {}", exerciseId);
        return workoutExerciseLogRepository.findByExerciseExerciseId(exerciseId);
    }

    /**
     * Găsește toate logurile pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @return lista logurilor utilizatorului
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findByUserId(Long userId) {
        log.debug("Finding exercise logs for user ID: {}", userId);
        return workoutExerciseLogRepository.findByUserId(userId);
    }

    /**
     * Găsește logurile pentru un utilizator într-o perioadă specifică
     * @param userId ID-ul utilizatorului
     * @param startDate data de început
     * @param endDate data de sfârșit
     * @return lista logurilor din perioada specificată
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Finding exercise logs for user ID: {} between {} and {}", userId, startDate, endDate);

        validateDateRange(startDate, endDate);
        return workoutExerciseLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * Găsește progresul unui utilizator pentru un exercițiu specific
     * @param userId ID-ul utilizatorului
     * @param exerciseId ID-ul exercițiului
     * @return lista logurilor pentru exercițiu, ordonate cronologic
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findUserProgressForExercise(Long userId, Long exerciseId) {
        log.debug("Finding user progress for user ID: {} and exercise ID: {}", userId, exerciseId);
        return workoutExerciseLogRepository.findUserProgressForExercise(userId, exerciseId);
    }

    /**
     * Găsește cele mai recente loguri pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @param limit numărul maxim de loguri
     * @return lista celor mai recente loguri
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findRecentLogsForUser(Long userId, int limit) {
        log.debug("Finding {} recent exercise logs for user ID: {}", limit, userId);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limita trebuie să fie pozitivă");
        }

        return workoutExerciseLogRepository.findRecentExerciseLogsForUser(userId, limit);
    }

    /**
     * Actualizează un log de exercițiu
     * @param logId ID-ul log-ului de actualizat
     * @param updatedLog obiectul cu noile date
     * @return log-ul actualizat
     * @throws IllegalArgumentException dacă log-ul nu există
     */
    public WorkoutExerciseLog updateExerciseLog(Long logId, WorkoutExerciseLog updatedLog) {
        log.info("Updating exercise log with ID: {}", logId);

        WorkoutExerciseLog existingLog = findExerciseLogById(logId);

        // Verifică că workout-ul este încă în progres sau doar completat
        ScheduledWorkout scheduledWorkout = existingLog.getScheduledWorkout();
        if (scheduledWorkout.getStatus() != WorkoutStatusType.IN_PROGRESS &&
                scheduledWorkout.getStatus() != WorkoutStatusType.COMPLETED) {
            throw new IllegalStateException("Se pot actualiza loguri doar pentru workout-uri în progres sau completate");
        }

        validateExerciseLogData(updatedLog);

        // Actualizează câmpurile
        updateExerciseLogFields(existingLog, updatedLog);

        WorkoutExerciseLog savedLog = workoutExerciseLogRepository.save(existingLog);
        log.info("Exercise log updated successfully: {}", savedLog.getLogId());

        return savedLog;
    }

    /**
     * Șterge un log de exercițiu
     * @param logId ID-ul log-ului de șters
     * @throws IllegalArgumentException dacă log-ul nu poate fi șters
     */
    public void deleteExerciseLog(Long logId) {
        log.info("Deleting exercise log with ID: {}", logId);

        WorkoutExerciseLog exerciseLog = findExerciseLogById(logId);

        // Verifică că workout-ul nu este completat
        if (exerciseLog.getScheduledWorkout().getStatus() == WorkoutStatusType.COMPLETED) {
            throw new IllegalStateException("Nu se pot șterge loguri din workout-uri completate");
        }

        workoutExerciseLogRepository.deleteById(logId);
        log.info("Exercise log deleted successfully: {}", logId);
    }

    /**
     * Șterge toate logurile pentru un workout programat
     * @param scheduledWorkoutId ID-ul workout-ului programat
     */
    public void deleteByScheduledWorkoutId(Long scheduledWorkoutId) {
        log.info("Deleting all exercise logs for scheduled workout ID: {}", scheduledWorkoutId);

        validateScheduledWorkoutExists(scheduledWorkoutId);
        workoutExerciseLogRepository.deleteByScheduledWorkoutId(scheduledWorkoutId);

        log.info("All exercise logs deleted for scheduled workout: {}", scheduledWorkoutId);
    }

    /**
     * Găsește recordul personal pentru greutate la un exercițiu
     * @param userId ID-ul utilizatorului
     * @param exerciseId ID-ul exercițiului
     * @return greutatea maximă înregistrată
     */
    @Transactional(readOnly = true)
    public BigDecimal findPersonalBestWeight(Long userId, Long exerciseId) {
        log.debug("Finding personal best weight for user ID: {} and exercise ID: {}", userId, exerciseId);
        return workoutExerciseLogRepository.findPersonalBestWeightForExercise(userId, exerciseId);
    }

    /**
     * Găsește recordul personal pentru repetări la un exercițiu
     * @param userId ID-ul utilizatorului
     * @param exerciseId ID-ul exercițiului
     * @return numărul maxim de repetări înregistrate
     */
    @Transactional(readOnly = true)
    public Integer findPersonalBestReps(Long userId, Long exerciseId) {
        log.debug("Finding personal best reps for user ID: {} and exercise ID: {}", userId, exerciseId);
        return workoutExerciseLogRepository.findPersonalBestRepsForExercise(userId, exerciseId);
    }

    /**
     * Calculează volumul total pentru un exercițiu într-o perioadă
     * @param userId ID-ul utilizatorului
     * @param exerciseId ID-ul exercițiului
     * @param startDate data de început
     * @param endDate data de sfârșit
     * @return volumul total (seturi x repetări x greutate)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalVolume(Long userId, Long exerciseId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating total volume for user ID: {} and exercise ID: {} between {} and {}",
                userId, exerciseId, startDate, endDate);

        validateDateRange(startDate, endDate);
        return workoutExerciseLogRepository.calculateTotalVolumeForExercise(userId, exerciseId, startDate, endDate);
    }

    /**
     * Calculează progresul pentru un exercițiu (comparează primul și ultimul log)
     * @param userId ID-ul utilizatorului
     * @param exerciseId ID-ul exercițiului
     * @return procentul de progres pentru greutate sau null dacă nu există suficiente date
     */
    @Transactional(readOnly = true)
    public Double calculateProgressPercentage(Long userId, Long exerciseId) {
        log.debug("Calculating progress percentage for user ID: {} and exercise ID: {}", userId, exerciseId);

        List<WorkoutExerciseLog> progressLogs = findUserProgressForExercise(userId, exerciseId);

        if (progressLogs.size() < 2) {
            return null; // Nu există suficiente date pentru calculul progresului
        }

        WorkoutExerciseLog firstLog = progressLogs.get(0);
        WorkoutExerciseLog lastLog = progressLogs.get(progressLogs.size() - 1);

        // Calculează progresul pe baza greutății
        if (firstLog.getWeightUsedKg() != null && lastLog.getWeightUsedKg() != null &&
                firstLog.getWeightUsedKg().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal increase = lastLog.getWeightUsedKg().subtract(firstLog.getWeightUsedKg());
            BigDecimal percentage = increase.divide(firstLog.getWeightUsedKg(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            return percentage.doubleValue();
        }

        // Calculează progresul pe baza repetărilor dacă nu există greutate
        if (firstLog.getRepsCompleted() != null && lastLog.getRepsCompleted() != null &&
                firstLog.getRepsCompleted() > 0) {

            double increase = lastLog.getRepsCompleted() - firstLog.getRepsCompleted();
            return (increase / firstLog.getRepsCompleted()) * 100;
        }

        return null;
    }

    /**
     * Găsește exercițiile cu cele mai bune performanțe pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @param limit numărul maxim de exerciții
     * @return lista exercițiilor cu progres pozitiv
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findTopPerformingExercises(Long userId, int limit) {
        log.debug("Finding top {} performing exercises for user ID: {}", limit, userId);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limita trebuie să fie pozitivă");
        }

        return workoutExerciseLogRepository.findRecentExerciseLogsForUser(userId, limit * 3)
                .stream()
                .filter(log -> log.getDifficultyRating() != null && log.getDifficultyRating() >= 4)
                .limit(limit)
                .toList();
    }

    /**
     * Verifică dacă un log aparține unui utilizator
     * @param logId ID-ul log-ului
     * @param userId ID-ul utilizatorului
     * @return true dacă log-ul aparține utilizatorului
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long logId, Long userId) {
        log.debug("Checking ownership of exercise log ID: {} by user ID: {}", logId, userId);

        Optional<WorkoutExerciseLog> log = workoutExerciseLogRepository.findById(logId);
        return log.isPresent() &&
                log.get().getScheduledWorkout().getUser().getUserId().equals(userId);
    }

    /**
     * Numără logurile pentru un workout programat
     * @param scheduledWorkoutId ID-ul workout-ului programat
     * @return numărul de loguri
     */
    @Transactional(readOnly = true)
    public long countLogsByScheduledWorkout(Long scheduledWorkoutId) {
        log.debug("Counting exercise logs for scheduled workout ID: {}", scheduledWorkoutId);
        validateScheduledWorkoutExists(scheduledWorkoutId);
        return findByScheduledWorkoutId(scheduledWorkoutId).size();
    }

    // Metode private pentru validare și utilități

    private void validateExerciseLogData(WorkoutExerciseLog exerciseLog) {
        if (exerciseLog.getScheduledWorkout() == null ||
                exerciseLog.getScheduledWorkout().getScheduledWorkoutId() == null) {
            throw new IllegalArgumentException("Workout-ul programat este obligatoriu");
        }

        if (exerciseLog.getExercise() == null || exerciseLog.getExercise().getExerciseId() == null) {
            throw new IllegalArgumentException("Exercițiul este obligatoriu");
        }

        if (exerciseLog.getSetsCompleted() == null || exerciseLog.getSetsCompleted() < 0) {
            throw new IllegalArgumentException("Numărul de seturi completate trebuie să fie pozitiv sau zero");
        }

        if (exerciseLog.getExerciseOrder() == null || exerciseLog.getExerciseOrder() < 1) {
            throw new IllegalArgumentException("Ordinea exercițiului trebuie să fie pozitivă");
        }

        // Validări pentru valori pozitive
        if (exerciseLog.getRepsCompleted() != null && exerciseLog.getRepsCompleted() < 0) {
            throw new IllegalArgumentException("Numărul de repetări completate nu poate fi negativ");
        }

        if (exerciseLog.getWeightUsedKg() != null && exerciseLog.getWeightUsedKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Greutatea folosită nu poate fi negativă");
        }

        if (exerciseLog.getDurationSeconds() != null && exerciseLog.getDurationSeconds() < 0) {
            throw new IllegalArgumentException("Durata nu poate fi negativă");
        }

        if (exerciseLog.getDistanceMeters() != null && exerciseLog.getDistanceMeters().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Distanța nu poate fi negativă");
        }

        if (exerciseLog.getCaloriesBurned() != null && exerciseLog.getCaloriesBurned() < 0) {
            throw new IllegalArgumentException("Caloriile arse nu pot fi negative");
        }

        if (exerciseLog.getDifficultyRating() != null &&
                (exerciseLog.getDifficultyRating() < 1 || exerciseLog.getDifficultyRating() > 5)) {
            throw new IllegalArgumentException("Rating-ul de dificultate trebuie să fie între 1 și 5");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Data de început și sfârșit sunt obligatorii");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Data de început trebuie să fie înainte de data de sfârșit");
        }
    }

    private ScheduledWorkout findScheduledWorkoutById(Long scheduledWorkoutId) {
        return scheduledWorkoutRepository.findById(scheduledWorkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout-ul programat nu a fost găsit cu ID-ul: " + scheduledWorkoutId));
    }

    private void validateScheduledWorkoutExists(Long scheduledWorkoutId) {
        if (!scheduledWorkoutRepository.existsById(scheduledWorkoutId)) {
            throw new IllegalArgumentException("Workout-ul programat nu a fost găsit cu ID-ul: " + scheduledWorkoutId);
        }
    }

    private WorkoutExerciseLog findExerciseLogById(Long logId) {
        return workoutExerciseLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log-ul de exercițiu nu a fost găsit cu ID-ul: " + logId));
    }

    private void updateExerciseLogFields(WorkoutExerciseLog existing, WorkoutExerciseLog updated) {
        existing.setExerciseOrder(updated.getExerciseOrder());
        existing.setSetsCompleted(updated.getSetsCompleted());
        existing.setRepsCompleted(updated.getRepsCompleted());
        existing.setWeightUsedKg(updated.getWeightUsedKg());
        existing.setDurationSeconds(updated.getDurationSeconds());
        existing.setDistanceMeters(updated.getDistanceMeters());
        existing.setCaloriesBurned(updated.getCaloriesBurned());
        existing.setDifficultyRating(updated.getDifficultyRating());
        existing.setNotes(updated.getNotes());
    }
}