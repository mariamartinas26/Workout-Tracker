package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.entity.WorkoutPlan;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import com.marecca.workoutTracker.repository.ScheduledWorkoutRepository;
import com.marecca.workoutTracker.repository.UserRepository;
import com.marecca.workoutTracker.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service pentru gestionarea workout-urilor programate
 * Conține logica de business pentru operațiile cu workout-uri programate
 * Integrează funcția PostgreSQL schedule_workout cu metodele existente
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledWorkoutService {

    private final ScheduledWorkoutRepository scheduledWorkoutRepository;
    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;


    /**
     * Programează un workout folosind funcția PostgreSQL optimizată
     *
     * @param userId ID-ul utilizatorului
     * @param workoutPlanId ID-ul planului de workout
     * @param scheduledDate Data programată
     * @param scheduledTime Ora programată (opțională)
     * @return ID-ul workout-ului programat
     * @throws RuntimeException dacă apar erori de validare sau de bază de date
     */
    @Transactional
    public Long scheduleWorkoutWithFunction(Long userId, Long workoutPlanId,
                                            LocalDate scheduledDate, LocalTime scheduledTime) {
        log.info("Scheduling workout with PostgreSQL function for user {} with plan {} on {} at {}",
                userId, workoutPlanId, scheduledDate, scheduledTime);

        validateScheduleWorkoutInput(userId, workoutPlanId, scheduledDate);

        try {
            Long scheduledWorkoutId;

            if (scheduledTime != null) {
                scheduledWorkoutId = scheduledWorkoutRepository.scheduleWorkoutWithFunction(
                        userId, workoutPlanId, scheduledDate, scheduledTime);
            } else {
                scheduledWorkoutId = scheduledWorkoutRepository.scheduleWorkoutWithoutTime(
                        userId, workoutPlanId, scheduledDate);
            }

            log.info("Workout scheduled successfully with ID: {}", scheduledWorkoutId);
            return scheduledWorkoutId;

        } catch (DataAccessException e) {
            log.error("Database error while scheduling workout: {}", e.getMessage());
            String errorMessage = extractUserFriendlyError(e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Programează workout cu validare suplimentară în Java
     */
    @Transactional
    public Long scheduleWorkoutWithValidation(Long userId, Long workoutPlanId, LocalDate scheduledDate, LocalTime scheduledTime) {
        log.debug("Scheduling workout for user ID: {}, plan ID: {}, date: {}, time: {}",
                userId, workoutPlanId, scheduledDate, scheduledTime);

        // Validează utilizatorul
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost găsit"));

        // ✅ VALIDEAZĂ ȘI ÎNCARCĂ WORKOUT PLAN-UL
        WorkoutPlan workoutPlan = null;
        if (workoutPlanId != null) {
            workoutPlan = workoutPlanRepository.findById(workoutPlanId)
                    .orElseThrow(() -> new IllegalArgumentException("Planul de workout nu a fost găsit cu ID: " + workoutPlanId));

            // Verifică că planul aparține utilizatorului
            if (!workoutPlan.getUser().getUserId().equals(userId)) {
                throw new IllegalArgumentException("Planul de workout nu aparține acestui utilizator");
            }
        }

        // Validează data
        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Nu poți programa un workout în trecut");
        }

        // Verifică disponibilitatea slotului (opțional)
        if (!canScheduleWorkoutAt(userId, scheduledDate, scheduledTime)) {
            throw new IllegalStateException("Slotul selectat nu este disponibil");
        }

        // Creează workout-ul programat
        ScheduledWorkout scheduledWorkout = ScheduledWorkout.builder()
                .user(user)
                .workoutPlan(workoutPlan)  // ✅ SETEAZĂ WORKOUT PLAN-UL
                .scheduledDate(scheduledDate)
                .scheduledTime(scheduledTime)
                .status(WorkoutStatusType.PLANNED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ScheduledWorkout saved = scheduledWorkoutRepository.save(scheduledWorkout);

        log.info("Workout scheduled successfully with ID: {} for plan: {}",
                saved.getScheduledWorkoutId(),
                workoutPlan != null ? workoutPlan.getPlanName() : "No plan");

        return saved.getScheduledWorkoutId();
    }

    /**
     * Programează workout doar pentru o dată (fără oră specificată)
     */
    @Transactional
    public Long scheduleWorkoutForDate(Long userId, Long workoutPlanId, LocalDate scheduledDate) {
        return scheduleWorkoutWithFunction(userId, workoutPlanId, scheduledDate, null);
    }

    /**
     * Verifică dacă un utilizator poate programa un workout la o anumită dată/oră
     */
    @Transactional(readOnly = true)
    public boolean canScheduleWorkoutAt(Long userId, LocalDate scheduledDate, LocalTime scheduledTime) {
        try {
            // Încearcă să programezi (dry-run)
            // Dacă funcția PostgreSQL nu aruncă excepție, înseamnă că se poate programa
            // ATENȚIE: aceasta este doar pentru verificare, nu face inserarea efectivă

            // Alternativ, folosește o verificare mai simplă:
            List<WorkoutStatusType> activeStatuses = Arrays.asList(WorkoutStatusType.PLANNED, WorkoutStatusType.IN_PROGRESS);

            if (scheduledTime != null) {
                return !scheduledWorkoutRepository.hasWorkoutScheduledAtSpecificTime(
                        userId, scheduledDate, scheduledTime, activeStatuses);
            } else {
                return !scheduledWorkoutRepository.hasWorkoutScheduledOnDate(
                        userId, scheduledDate, activeStatuses);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Statistici workout-uri pentru utilizator (folosind metode optimizate)
     */
    @Transactional(readOnly = true)
    public WorkoutStatistics getUserWorkoutStatistics(Long userId) {
        Long completedCount = scheduledWorkoutRepository.countCompletedWorkoutsForUser(userId);
        Double averageDuration = scheduledWorkoutRepository.getAverageWorkoutDurationForUser(userId);

        return WorkoutStatistics.builder()
                .completedWorkouts(completedCount != null ? completedCount : 0L)
                .averageDurationMinutes(averageDuration != null ? averageDuration : 0.0)
                .build();
    }

    /**
     * Programează un workout nou (metoda originală îmbunătățită)
     * @param scheduledWorkout obiectul ScheduledWorkout cu datele de intrare
     * @return workout-ul programat creat
     * @throws IllegalArgumentException dacă utilizatorul sau planul nu există
     */
    public ScheduledWorkout scheduleWorkout(ScheduledWorkout scheduledWorkout) {
        log.info("Scheduling workout for user ID: {} on date: {}",
                scheduledWorkout.getUser().getUserId(),
                scheduledWorkout.getScheduledDate());

        validateScheduledWorkoutData(scheduledWorkout);

        // Verifică dacă utilizatorul există
        User user = findUserById(scheduledWorkout.getUser().getUserId());
        scheduledWorkout.setUser(user);

        // Verifică dacă planul de workout există (dacă este specificat)
        if (scheduledWorkout.getWorkoutPlan() != null &&
                scheduledWorkout.getWorkoutPlan().getWorkoutPlanId() != null) {
            WorkoutPlan workoutPlan = findWorkoutPlanById(scheduledWorkout.getWorkoutPlan().getWorkoutPlanId());

            // Verifică dacă planul aparține utilizatorului
            if (!workoutPlan.getUser().getUserId().equals(user.getUserId())) {
                throw new IllegalArgumentException("Planul de workout nu aparține utilizatorului");
            }

            scheduledWorkout.setWorkoutPlan(workoutPlan);
        }

        scheduledWorkout.setStatus(WorkoutStatusType.PLANNED);
        scheduledWorkout.setCreatedAt(LocalDateTime.now());
        scheduledWorkout.setUpdatedAt(LocalDateTime.now());

        ScheduledWorkout savedWorkout = scheduledWorkoutRepository.save(scheduledWorkout);
        log.info("Workout scheduled successfully with ID: {}", savedWorkout.getScheduledWorkoutId());

        return savedWorkout;
    }

    /**
     * Găsește un workout programat după ID
     * @param scheduledWorkoutId ID-ul workout-ului programat
     * @return Optional cu workout-ul găsit
     */
    @Transactional(readOnly = true)
    public Optional<ScheduledWorkout> findById(Long scheduledWorkoutId) {
        log.debug("Finding scheduled workout by ID: {}", scheduledWorkoutId);
        return scheduledWorkoutRepository.findById(scheduledWorkoutId);
    }

    /**
     * Găsește toate workout-urile programate pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @return lista workout-urilor programate, ordonate după dată (descrescător)
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByUserId(Long userId) {
        log.debug("Finding scheduled workouts for user ID: {}", userId);
        validateUserExists(userId);
        return scheduledWorkoutRepository.findByUserUserIdOrderByScheduledDateDesc(userId);
    }

    /**
     * Găsește workout-urile programate pentru un utilizator într-o perioadă specifică
     * @param userId ID-ul utilizatorului
     * @param startDate data de început
     * @param endDate data de sfârșit
     * @return lista workout-urilor din perioada specificată
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Finding scheduled workouts for user ID: {} between {} and {}", userId, startDate, endDate);

        validateUserExists(userId);
        validateDateRange(startDate, endDate);

        return scheduledWorkoutRepository.findByUserUserIdAndScheduledDateBetweenOrderByScheduledDate(
                userId, startDate, endDate);
    }

    /**
     * Găsește workout-urile programate pentru un utilizator cu un anumit status
     * @param userId ID-ul utilizatorului
     * @param status statusul workout-ului
     * @return lista workout-urilor cu statusul specificat
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByUserIdAndStatus(Long userId, WorkoutStatusType status) {
        log.debug("Finding scheduled workouts for user ID: {} with status: {}", userId, status);
        validateUserExists(userId);
        return scheduledWorkoutRepository.findByUserUserIdAndStatus(userId, status);
    }

    /**
     * Găsește workout-urile programate pentru astăzi pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @return lista workout-urilor de astăzi
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findTodaysWorkouts(Long userId) {
        log.debug("Finding today's workouts for user ID: {}", userId);
        validateUserExists(userId);
        return scheduledWorkoutRepository.findTodaysWorkoutsForUser(userId);
    }

    /**
     * Găsește workout-urile ratate pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @return lista workout-urilor ratate
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findMissedWorkouts(Long userId) {
        log.debug("Finding missed workouts for user ID: {}", userId);
        validateUserExists(userId);
        return scheduledWorkoutRepository.findMissedWorkoutsForUser(userId);
    }

    /**
     * Găsește workout-urile pentru un anumit plan
     * @param workoutPlanId ID-ul planului de workout
     * @return lista workout-urilor pentru plan
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByWorkoutPlanId(Long workoutPlanId) {
        log.debug("Finding scheduled workouts for workout plan ID: {}", workoutPlanId);
        validateWorkoutPlanExists(workoutPlanId);
        return scheduledWorkoutRepository.findByWorkoutPlanWorkoutPlanId(workoutPlanId);
    }

    /**
     * Actualizează un workout programat
     * @param scheduledWorkoutId ID-ul workout-ului de actualizat
     * @param updatedWorkout obiectul cu noile date
     * @return workout-ul actualizat
     * @throws IllegalArgumentException dacă workout-ul nu există
     */
    public ScheduledWorkout updateScheduledWorkout(Long scheduledWorkoutId, ScheduledWorkout updatedWorkout) {
        log.info("Updating scheduled workout with ID: {}", scheduledWorkoutId);

        ScheduledWorkout existingWorkout = findScheduledWorkoutById(scheduledWorkoutId);

        // Nu permite actualizarea dacă workout-ul este în progres sau completat
        if (existingWorkout.getStatus() == WorkoutStatusType.IN_PROGRESS ||
                existingWorkout.getStatus() == WorkoutStatusType.COMPLETED) {
            throw new IllegalStateException("Nu se poate actualiza un workout în progres sau completat");
        }

        // Actualizează doar câmpurile permise
        if (updatedWorkout.getScheduledDate() != null) {
            existingWorkout.setScheduledDate(updatedWorkout.getScheduledDate());
        }
        if (updatedWorkout.getScheduledTime() != null) {
            existingWorkout.setScheduledTime(updatedWorkout.getScheduledTime());
        }
        if (updatedWorkout.getNotes() != null) {
            existingWorkout.setNotes(updatedWorkout.getNotes());
        }

        existingWorkout.setUpdatedAt(LocalDateTime.now());

        ScheduledWorkout savedWorkout = scheduledWorkoutRepository.save(existingWorkout);
        log.info("Scheduled workout updated successfully: {}", savedWorkout.getScheduledWorkoutId());

        return savedWorkout;
    }

    /**
     * Începe un workout (schimbă statusul la IN_PROGRESS)
     * @param scheduledWorkoutId ID-ul workout-ului
     * @return workout-ul actualizat
     * @throws IllegalArgumentException dacă workout-ul nu poate fi început
     */
    public ScheduledWorkout startWorkout(Long scheduledWorkoutId) {
        log.info("Starting workout with ID: {}", scheduledWorkoutId);

        ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

        if (workout.getStatus() != WorkoutStatusType.PLANNED) {
            throw new IllegalStateException("Workout-ul poate fi început doar dacă este planificat");
        }

        LocalDateTime startTime = LocalDateTime.now();
        scheduledWorkoutRepository.startWorkout(scheduledWorkoutId, startTime);

        // Reîncarcă entitatea pentru a obține datele actualizate
        workout = findScheduledWorkoutById(scheduledWorkoutId);
        log.info("Workout started successfully at: {}", startTime);

        return workout;
    }

    /**
     * Completează un workout
     * @param scheduledWorkoutId ID-ul workout-ului
     * @param caloriesBurned caloriile arse
     * @param rating rating-ul general (1-5)
     * @return workout-ul actualizat
     * @throws IllegalArgumentException dacă workout-ul nu poate fi completat
     */
    public ScheduledWorkout completeWorkout(Long scheduledWorkoutId, Integer caloriesBurned, Integer rating) {
        log.info("Completing workout with ID: {}", scheduledWorkoutId);

        ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

        if (workout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
            throw new IllegalStateException("Workout-ul poate fi completat doar dacă este în progres");
        }

        validateCompletionData(caloriesBurned, rating);

        LocalDateTime endTime = LocalDateTime.now();
        scheduledWorkoutRepository.completeWorkout(scheduledWorkoutId, endTime, caloriesBurned, rating);

        // Reîncarcă entitatea pentru a obține datele actualizate
        workout = findScheduledWorkoutById(scheduledWorkoutId);
        log.info("Workout completed successfully at: {}", endTime);

        return workout;
    }

    /**
     * Anulează un workout programat
     * @param scheduledWorkoutId ID-ul workout-ului
     * @return workout-ul actualizat
     * @throws IllegalArgumentException dacă workout-ul nu poate fi anulat
     */
    public ScheduledWorkout cancelWorkout(Long scheduledWorkoutId) {
        log.info("Cancelling workout with ID: {}", scheduledWorkoutId);

        ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

        if (workout.getStatus() == WorkoutStatusType.COMPLETED) {
            throw new IllegalStateException("Nu se poate anula un workout completat");
        }

        scheduledWorkoutRepository.updateWorkoutStatus(scheduledWorkoutId, WorkoutStatusType.CANCELLED);

        // Reîncarcă entitatea pentru a obține datele actualizate
        workout = findScheduledWorkoutById(scheduledWorkoutId);
        log.info("Workout cancelled successfully");

        return workout;
    }

    /**
     * Marchează un workout ca ratat
     * @param scheduledWorkoutId ID-ul workout-ului
     * @return workout-ul actualizat
     */
    public ScheduledWorkout markWorkoutAsMissed(Long scheduledWorkoutId) {
        log.info("Marking workout as missed with ID: {}", scheduledWorkoutId);

        ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

        if (workout.getStatus() != WorkoutStatusType.PLANNED) {
            throw new IllegalStateException("Doar workout-urile planificate pot fi marcate ca ratate");
        }

        scheduledWorkoutRepository.updateWorkoutStatus(scheduledWorkoutId, WorkoutStatusType.MISSED);

        // Reîncarcă entitatea pentru a obține datele actualizate
        workout = findScheduledWorkoutById(scheduledWorkoutId);
        log.info("Workout marked as missed successfully");

        return workout;
    }

    /**
     * Șterge un workout programat
     * @param scheduledWorkoutId ID-ul workout-ului de șters
     * @throws IllegalArgumentException dacă workout-ul nu poate fi șters
     */
    public void deleteScheduledWorkout(Long scheduledWorkoutId) {
        log.info("Deleting scheduled workout with ID: {}", scheduledWorkoutId);

        ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

        if (workout.getStatus() == WorkoutStatusType.IN_PROGRESS) {
            throw new IllegalStateException("Nu se poate șterge un workout în progres");
        }

        scheduledWorkoutRepository.deleteById(scheduledWorkoutId);
        log.info("Scheduled workout deleted successfully: {}", scheduledWorkoutId);
    }

    /**
     * Calculează statistici despre workout-uri pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @return numărul de workout-uri completate
     */
    @Transactional(readOnly = true)
    public Long countCompletedWorkouts(Long userId) {
        log.debug("Counting completed workouts for user ID: {}", userId);
        validateUserExists(userId);
        return scheduledWorkoutRepository.countCompletedWorkoutsForUser(userId);
    }

    /**
     * Calculează durata medie a workout-urilor pentru un utilizator
     * @param userId ID-ul utilizatorului
     * @return durata medie în minute
     */
    @Transactional(readOnly = true)
    public Double getAverageWorkoutDuration(Long userId) {
        log.debug("Getting average workout duration for user ID: {}", userId);
        validateUserExists(userId);
        return scheduledWorkoutRepository.getAverageWorkoutDurationForUser(userId);
    }

    /**
     * Găsește workout-urile ordonate după rating
     * @param userId ID-ul utilizatorului
     * @param status statusul workout-urilor
     * @return lista workout-urilor sortate după rating (descrescător)
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByUserIdAndStatusOrderByRating(Long userId, WorkoutStatusType status) {
        log.debug("Finding workouts for user ID: {} with status: {} ordered by rating", userId, status);
        validateUserExists(userId);
        return scheduledWorkoutRepository.findByUserUserIdAndStatusOrderByOverallRatingDesc(userId, status);
    }

    /**
     * Găsește ultimele workout-uri completate
     * @param userId ID-ul utilizatorului
     * @return lista ultimelor 5 workout-uri completate
     */
    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findRecentCompletedWorkouts(Long userId) {
        log.debug("Finding recent completed workouts for user ID: {}", userId);
        validateUserExists(userId);
        return scheduledWorkoutRepository.findTop5ByUserUserIdAndStatusOrderByActualEndTimeDesc(
                userId, WorkoutStatusType.COMPLETED);
    }

    /**
     * Verifică dacă un workout aparține unui utilizator
     * @param scheduledWorkoutId ID-ul workout-ului
     * @param userId ID-ul utilizatorului
     * @return true dacă workout-ul aparține utilizatorului
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long scheduledWorkoutId, Long userId) {
        log.debug("Checking ownership of scheduled workout ID: {} by user ID: {}", scheduledWorkoutId, userId);

        Optional<ScheduledWorkout> workout = scheduledWorkoutRepository.findById(scheduledWorkoutId);
        return workout.isPresent() && workout.get().getUser().getUserId().equals(userId);
    }

    // =============== METODE PRIVATE PENTRU VALIDARE ===============

    private void validateScheduleWorkoutInput(Long userId, Long workoutPlanId, LocalDate scheduledDate) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID-ul utilizatorului trebuie să fie pozitiv");
        }

        if (workoutPlanId == null || workoutPlanId <= 0) {
            throw new IllegalArgumentException("ID-ul planului de workout trebuie să fie pozitiv");
        }

        if (scheduledDate == null) {
            throw new IllegalArgumentException("Data programată este obligatorie");
        }

        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Nu se pot programa workout-uri în trecut");
        }
    }

    private void validateScheduledWorkoutData(ScheduledWorkout workout) {
        if (workout.getUser() == null || workout.getUser().getUserId() == null) {
            throw new IllegalArgumentException("Utilizatorul este obligatoriu");
        }

        if (workout.getScheduledDate() == null) {
            throw new IllegalArgumentException("Data programată este obligatorie");
        }

        if (workout.getScheduledDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data programată nu poate fi în trecut");
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

    private void validateCompletionData(Integer caloriesBurned, Integer rating) {
        if (caloriesBurned != null && caloriesBurned < 0) {
            throw new IllegalArgumentException("Caloriile arse nu pot fi negative");
        }

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating-ul trebuie să fie între 1 și 5");
        }
    }

    private String extractUserFriendlyError(String sqlError) {
        if (sqlError.contains("User with ID") && sqlError.contains("does not exist")) {
            return "Utilizatorul specificat nu există";
        }
        if (sqlError.contains("Workout plan with ID") && sqlError.contains("does not exist")) {
            return "Planul de workout specificat nu există sau nu aparține utilizatorului";
        }
        if (sqlError.contains("already has a workout scheduled")) {
            return "Utilizatorul are deja un workout programat la această dată și oră";
        }
        return "Eroare la programarea workout-ului";
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

    private ScheduledWorkout findScheduledWorkoutById(Long scheduledWorkoutId) {
        return scheduledWorkoutRepository.findById(scheduledWorkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout-ul programat nu a fost găsit cu ID-ul: " + scheduledWorkoutId));
    }



    @lombok.Builder
    @lombok.Data
    public static class WorkoutStatistics {
        private Long completedWorkouts;
        private Double averageDurationMinutes;
    }
}