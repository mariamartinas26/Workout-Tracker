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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service pentru gestionarea workout-urilor programate
 * Conține logica de business pentru operațiile cu workout-uri programate
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
     * Programează un workout nou
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

    // Metode private pentru validare și utilități

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
}