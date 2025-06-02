package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.entity.WorkoutPlan;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import com.marecca.workoutTracker.repository.ScheduledWorkoutRepository;
import com.marecca.workoutTracker.repository.UserRepository;
import com.marecca.workoutTracker.repository.WorkoutPlanRepository;
import com.marecca.workoutTracker.service.exceptions.UserNotFoundException;
import com.marecca.workoutTracker.service.exceptions.WorkoutAlreadyScheduledException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.marecca.workoutTracker.service.exceptions.WorkoutPlanNotFoundException;
import com.marecca.workoutTracker.service.exceptions.WorkoutNotFoundException;
import com.marecca.workoutTracker.service.exceptions.InvalidWorkoutStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    @Transactional
    public Long scheduleWorkoutWithFunction(Long userId, Long workoutPlanId,
                                            LocalDate scheduledDate, LocalTime scheduledTime) {
        log.info("Scheduling workout with PostgreSQL function for user {} with plan {} on {} at {}",
                userId, workoutPlanId, scheduledDate, scheduledTime);

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
            String errorMessage = e.getMessage();
            String sqlState = extractSQLState(e);

            log.error("Database error while scheduling workout: {}", errorMessage);

            // Handle specific PL/SQL exceptions based on error codes and messages
            if (sqlState != null) {
                switch (sqlState) {
                    case "00001":
                        // INVALID_USER_ID
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "INVALID_USER_ID"));

                    case "00002":
                        // INVALID_WORKOUT_PLAN_ID
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "INVALID_WORKOUT_PLAN_ID"));

                    case "00003":
                        // USER_NOT_FOUND
                        throw new UserNotFoundException(
                                extractCustomErrorMessage(errorMessage, "USER_NOT_FOUND"));

                    case "00004":
                        // WORKOUT_PLAN_NOT_FOUND
                        throw new WorkoutPlanNotFoundException(
                                extractCustomErrorMessage(errorMessage, "WORKOUT_PLAN_NOT_FOUND"));

                    case "00005":
                        // WORKOUT_PLAN_NOT_OWNED
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "WORKOUT_PLAN_NOT_OWNED"));

                    case "00006":
                        // INVALID_SCHEDULED_DATE
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "INVALID_SCHEDULED_DATE"));

                    case "00007":
                        // WORKOUT_ALREADY_SCHEDULED
                        throw new WorkoutAlreadyScheduledException(
                                extractCustomErrorMessage(errorMessage, "WORKOUT_ALREADY_SCHEDULED"));

                    case "00999":
                        // DATABASE_ERROR
                        throw new RuntimeException(
                                "Database error occurred: " +
                                        extractCustomErrorMessage(errorMessage, "DATABASE_ERROR"), e);

                    default:
                        break;
                }
            }

            // Fallback error message extraction
            if (errorMessage.contains("User with ID") && errorMessage.contains("does not exist")) {
                throw new UserNotFoundException("Utilizatorul specificat nu există");
            }
            if (errorMessage.contains("Workout plan with ID") && errorMessage.contains("does not exist")) {
                throw new WorkoutPlanNotFoundException("Planul de workout specificat nu există");
            }
            if (errorMessage.contains("already has a workout scheduled")) {
                throw new WorkoutAlreadyScheduledException("Utilizatorul are deja un workout programat la această dată și oră");
            }
            if (errorMessage.contains("WORKOUT_PLAN_NOT_OWNED")) {
                throw new IllegalArgumentException("Planul de workout nu aparține acestui utilizator");
            }
            if (errorMessage.contains("INVALID_SCHEDULED_DATE")) {
                throw new IllegalArgumentException("Data programată nu poate fi în trecut");
            }

            throw new RuntimeException("Eroare la programarea workout-ului: " + errorMessage, e);

        } catch (UserNotFoundException | WorkoutPlanNotFoundException |
                 WorkoutAlreadyScheduledException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("A apărut o eroare neașteptată la programarea workout-ului", e);
        }
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
     */
    public ScheduledWorkout startWorkout(Long scheduledWorkoutId) {
        log.info("Starting workout with ID: {}", scheduledWorkoutId);

        try {
            LocalDateTime startTime = LocalDateTime.now();
            scheduledWorkoutRepository.startWorkout(scheduledWorkoutId, startTime);

            // Reîncarcă entitatea pentru a obține datele actualizate
            ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);
            log.info("Workout started successfully at: {}", startTime);

            return workout;

        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            String sqlState = extractSQLState(e);

            log.error("Database error while starting workout: {}", errorMessage);

            if (sqlState != null) {
                switch (sqlState) {
                    case "00001":
                        // WORKOUT_NOT_FOUND
                        throw new WorkoutNotFoundException(
                                extractCustomErrorMessage(errorMessage, "WORKOUT_NOT_FOUND"));

                    case "00002":
                        // INVALID_WORKOUT_STATUS
                        throw new InvalidWorkoutStatusException(
                                extractCustomErrorMessage(errorMessage, "INVALID_WORKOUT_STATUS"));

                    case "00999":
                        // DATABASE_ERROR
                        throw new RuntimeException(
                                "Database error occurred: " +
                                        extractCustomErrorMessage(errorMessage, "DATABASE_ERROR"), e);

                    default:
                        break;
                }
            }

            // Fallback error handling
            if (errorMessage.contains("WORKOUT_NOT_FOUND")) {
                throw new WorkoutNotFoundException("Workout-ul nu a fost găsit");
            }
            if (errorMessage.contains("INVALID_WORKOUT_STATUS")) {
                throw new InvalidWorkoutStatusException("Workout-ul poate fi început doar dacă este planificat");
            }

            throw new RuntimeException("Eroare la începerea workout-ului: " + errorMessage, e);

        } catch (WorkoutNotFoundException | InvalidWorkoutStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("A apărut o eroare neașteptată la începerea workout-ului", e);
        }
    }


    /**
     * Completează un workout
     */
    public ScheduledWorkout completeWorkout(Long scheduledWorkoutId, Integer caloriesBurned, Integer rating) {
        log.info("Completing workout with ID: {}", scheduledWorkoutId);

        try {
            validateCompletionData(caloriesBurned, rating);

            LocalDateTime endTime = LocalDateTime.now();
            scheduledWorkoutRepository.completeWorkout(scheduledWorkoutId, endTime, caloriesBurned, rating);

            // Reîncarcă entitatea pentru a obține datele actualizate
            ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);
            log.info("Workout completed successfully at: {}", endTime);

            return workout;

        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            String sqlState = extractSQLState(e);

            log.error("Database error while completing workout: {}", errorMessage);

            if (sqlState != null) {
                switch (sqlState) {
                    case "00001":
                        // WORKOUT_NOT_FOUND
                        throw new WorkoutNotFoundException(
                                extractCustomErrorMessage(errorMessage, "WORKOUT_NOT_FOUND"));

                    case "00002":
                        // INVALID_WORKOUT_STATUS
                        throw new InvalidWorkoutStatusException(
                                extractCustomErrorMessage(errorMessage, "INVALID_WORKOUT_STATUS"));

                    case "00003":
                        // INVALID_CALORIES
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "INVALID_CALORIES"));

                    case "00004":
                        // INVALID_RATING
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "INVALID_RATING"));

                    case "00999":
                        // DATABASE_ERROR
                        throw new RuntimeException(
                                "Database error occurred: " +
                                        extractCustomErrorMessage(errorMessage, "DATABASE_ERROR"), e);

                    default:
                        break;
                }
            }

            // Fallback error handling
            if (errorMessage.contains("WORKOUT_NOT_FOUND")) {
                throw new WorkoutNotFoundException("Workout-ul nu a fost găsit");
            }
            if (errorMessage.contains("INVALID_WORKOUT_STATUS")) {
                throw new InvalidWorkoutStatusException("Workout-ul poate fi completat doar dacă este în progres");
            }
            if (errorMessage.contains("INVALID_CALORIES")) {
                throw new IllegalArgumentException("Caloriile arse nu pot fi negative");
            }
            if (errorMessage.contains("INVALID_RATING")) {
                throw new IllegalArgumentException("Rating-ul trebuie să fie între 1 și 5");
            }

            throw new RuntimeException("Eroare la completarea workout-ului: " + errorMessage, e);

        } catch (WorkoutNotFoundException | InvalidWorkoutStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("A apărut o eroare neașteptată la completarea workout-ului", e);
        }
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
    /**
     * Reprogramează un workout existent
     */
    @Transactional
    public void rescheduleWorkout(Long scheduledWorkoutId, LocalDate newDate, LocalTime newTime) {
        log.info("Attempting to reschedule workout with ID: {}", scheduledWorkoutId);

        try {
            // Găsește workout-ul existent
            ScheduledWorkout scheduledWorkout = scheduledWorkoutRepository.findById(scheduledWorkoutId)
                    .orElseThrow(() -> new WorkoutNotFoundException("Workout-ul programat nu a fost găsit"));

            // Verifică dacă workout-ul poate fi reprogramat
            if (scheduledWorkout.getStatus() == WorkoutStatusType.COMPLETED) {
                throw new InvalidWorkoutStatusException("Nu se poate reprograma un workout finalizat");
            }

            if (scheduledWorkout.getStatus() == WorkoutStatusType.IN_PROGRESS) {
                throw new InvalidWorkoutStatusException("Nu se poate reprograma un workout în desfășurare");
            }

            // Validează noua dată
            if (newDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Nu se poate programa un workout în trecut");
            }

            // Verifică conflictele doar dacă data/ora s-au schimbat
            boolean dateChanged = !newDate.equals(scheduledWorkout.getScheduledDate());
            boolean timeChanged = (newTime != null && !newTime.equals(scheduledWorkout.getScheduledTime())) ||
                    (newTime == null && scheduledWorkout.getScheduledTime() != null);

            if (dateChanged || timeChanged) {
                if (!isTimeSlotAvailable(scheduledWorkout.getUser().getUserId(), newDate, newTime, scheduledWorkoutId)) {
                    throw new WorkoutAlreadyScheduledException("Slotul selectat nu este disponibil - există deja un workout programat");
                }
            }

            // Păstrează statusul original
            WorkoutStatusType originalStatus = scheduledWorkout.getStatus();

            // Actualizează workout-ul
            scheduledWorkout.setScheduledDate(newDate);
            scheduledWorkout.setScheduledTime(newTime);
            scheduledWorkout.setUpdatedAt(LocalDateTime.now());

            // Gestionează schimbările de status
            switch (scheduledWorkout.getStatus()) {
                case CANCELLED:
                case MISSED:
                    scheduledWorkout.setStatus(WorkoutStatusType.PLANNED);
                    log.info("Reactivated {} workout to PLANNED status", originalStatus);
                    break;
                case PLANNED:
                    // Menține statusul PLANNED
                    break;
                default:
                    scheduledWorkout.setStatus(WorkoutStatusType.PLANNED);
                    log.warn("Changed status from {} to PLANNED during reschedule", originalStatus);
            }

            scheduledWorkoutRepository.save(scheduledWorkout);

            log.info("Workout rescheduled successfully: ID={}, OriginalStatus={}, NewStatus={}, NewDate={}, NewTime={}",
                    scheduledWorkoutId, originalStatus, scheduledWorkout.getStatus(), newDate, newTime);

        } catch (WorkoutNotFoundException | InvalidWorkoutStatusException |
                 WorkoutAlreadyScheduledException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during reschedule: {}", e.getMessage(), e);
            throw new RuntimeException("A apărut o eroare neașteptată la reprogramarea workout-ului", e);
        }
    }


    /**
     * Verifică dacă un slot este disponibil pentru reschedule, excluzând workout-ul curent
     */
    private boolean isTimeSlotAvailable(Long userId, LocalDate date, LocalTime time, Long excludeWorkoutId) {
        List<WorkoutStatusType> activeStatuses = Arrays.asList(
                WorkoutStatusType.PLANNED,
                WorkoutStatusType.IN_PROGRESS
        );

        try {
            if (time == null) {
                // If no specific time, check if there are any other workouts that day
                List<ScheduledWorkout> dayWorkouts = scheduledWorkoutRepository
                        .findWorkoutsForDateExcluding(userId, date, excludeWorkoutId, activeStatuses);
                return dayWorkouts.isEmpty();
            }

            // Check for exact time conflicts first (most common case)
            List<ScheduledWorkout> exactConflicts = scheduledWorkoutRepository
                    .findExactTimeConflictsForReschedule(userId, date, time, excludeWorkoutId, activeStatuses);

            if (!exactConflicts.isEmpty()) {
                log.info("Found exact time conflict at {}", time);
                return false;
            }

            // Check for time overlap using Java logic
            List<ScheduledWorkout> dayWorkouts = scheduledWorkoutRepository
                    .findWorkoutsForDateExcluding(userId, date, excludeWorkoutId, activeStatuses);

            // Assume each workout is 1 hour long
            LocalTime newWorkoutStart = time;
            LocalTime newWorkoutEnd = time.plusHours(1);

            for (ScheduledWorkout existingWorkout : dayWorkouts) {
                LocalTime existingStart = existingWorkout.getScheduledTime();

                if (existingStart == null) {
                    continue; // Skip workouts without specific time
                }

                LocalTime existingEnd = existingStart.plusHours(1);

                // Check for time overlap
                boolean hasOverlap = !(newWorkoutEnd.isBefore(existingStart) ||
                        newWorkoutStart.isAfter(existingEnd) ||
                        newWorkoutEnd.equals(existingStart) ||
                        newWorkoutStart.equals(existingEnd));

                if (hasOverlap) {
                    log.info("Found time overlap: new workout {}-{} conflicts with existing workout {}-{}",
                            newWorkoutStart, newWorkoutEnd, existingStart, existingEnd);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.warn("Error checking time slot availability, allowing scheduling: {}", e.getMessage());
            return true; // If we can't check, allow the scheduling
        }
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

    /**
     * Extracts SQL state from DataAccessException
     */
    private String extractSQLState(DataAccessException e) {
        try {
            if (e.getCause() != null && e.getCause().getCause() != null) {
                String message = e.getCause().getCause().getMessage();
                if (message != null && message.contains("ERROR:")) {
                    // Extract SQL state from PostgreSQL error message
                    Pattern pattern = Pattern.compile("ERROR:\\s*(\\d{5})");
                    Matcher matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to extract SQL state: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Extracts custom error message from PostgreSQL exception
     */
    private String extractCustomErrorMessage(String errorMessage, String errorType) {
        try {
            // Try to extract custom message from PostgreSQL RAISE statement
            Pattern pattern = Pattern.compile(errorType + ":\\s*([^\\n\\r]+)");
            Matcher matcher = pattern.matcher(errorMessage);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }

            // Fallback: look for the error type and return a default message
            if (errorMessage.contains(errorType)) {
                return getDefaultErrorMessage(errorType);
            }
        } catch (Exception e) {
            log.warn("Failed to extract custom error message for {}: {}", errorType, e.getMessage());
        }

        return getDefaultErrorMessage(errorType);
    }

    /**
     * Returns default error messages for different error types
     */
    private String getDefaultErrorMessage(String errorType) {
        switch (errorType) {
            case "INVALID_USER_ID":
                return "ID-ul utilizatorului este invalid";
            case "INVALID_WORKOUT_PLAN_ID":
                return "ID-ul planului de workout este invalid";
            case "USER_NOT_FOUND":
                return "Utilizatorul nu a fost găsit";
            case "WORKOUT_PLAN_NOT_FOUND":
                return "Planul de workout nu a fost găsit";
            case "WORKOUT_PLAN_NOT_OWNED":
                return "Planul de workout nu aparține acestui utilizator";
            case "INVALID_SCHEDULED_DATE":
                return "Data programată este invalidă";
            case "WORKOUT_ALREADY_SCHEDULED":
                return "Există deja un workout programat la această dată și oră";
            case "WORKOUT_NOT_FOUND":
                return "Workout-ul nu a fost găsit";
            case "INVALID_WORKOUT_STATUS":
                return "Statusul workout-ului nu permite această operație";
            case "INVALID_CALORIES":
                return "Caloriile arse sunt invalide";
            case "INVALID_RATING":
                return "Rating-ul este invalid";
            case "DATABASE_ERROR":
                return "A apărut o eroare de bază de date";
            default:
                return "A apărut o eroare neașteptată";
        }
    }


    @lombok.Builder
    @lombok.Data
    public static class WorkoutStatistics {
        private Long completedWorkouts;
        private Double averageDurationMinutes;
    }
}