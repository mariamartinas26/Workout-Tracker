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
 * Business logic for operations with scheduled workout
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
    public Long scheduleWorkoutWithFunction(Long userId, Long workoutPlanId,LocalDate scheduledDate, LocalTime scheduledTime) {
        try {
            Long scheduledWorkoutId;

            if (scheduledTime != null) {
                scheduledWorkoutId = scheduledWorkoutRepository.scheduleWorkoutWithFunction(
                        userId, workoutPlanId, scheduledDate, scheduledTime);
            } else {
                scheduledWorkoutId = scheduledWorkoutRepository.scheduleWorkoutWithoutTime(
                        userId, workoutPlanId, scheduledDate);
            }
            return scheduledWorkoutId;

        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            String sqlState = extractSQLState(e);

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

            if (errorMessage.contains("User with ID") && errorMessage.contains("does not exist")) {
                throw new UserNotFoundException("The specified user does not exist");
            }
            if (errorMessage.contains("Workout plan with ID") && errorMessage.contains("does not exist")) {
                throw new WorkoutPlanNotFoundException("The specified workout plan does not exist");
            }
            if (errorMessage.contains("already has a workout scheduled")) {
                throw new WorkoutAlreadyScheduledException("The user already has a workout scheduled at this date and time");
            }
            if (errorMessage.contains("WORKOUT_PLAN_NOT_OWNED")) {
                throw new IllegalArgumentException("The workout plan does not belong to this user");
            }
            if (errorMessage.contains("INVALID_SCHEDULED_DATE")) {
                throw new IllegalArgumentException("The scheduled date cannot be in the past");
            }

            throw new RuntimeException("Error while scheduling the workout: " + errorMessage, e);

        } catch (UserNotFoundException | WorkoutPlanNotFoundException |
                 WorkoutAlreadyScheduledException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while scheduling the workout", e);
        }
    }


    /**
     * checks if a user can schedule a workout at a specific hour/date
     * @param userId
     * @param scheduledDate
     * @param scheduledTime
     * @return
     */
    @Transactional(readOnly = true)
    public boolean canScheduleWorkoutAt(Long userId, LocalDate scheduledDate, LocalTime scheduledTime) {
        try {
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
     * finds a scheduled workout by id
     * @param scheduledWorkoutId
     * @return
     */
    @Transactional(readOnly = true)
    public Optional<ScheduledWorkout> findById(Long scheduledWorkoutId) {
        return scheduledWorkoutRepository.findById(scheduledWorkoutId);
    }

    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByUserId(Long userId) {
        validateUserExists(userId);
        return scheduledWorkoutRepository.findByUserUserIdOrderByScheduledDateDesc(userId);
    }


    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        validateUserExists(userId);
        validateDateRange(startDate, endDate);

        return scheduledWorkoutRepository.findByUserUserIdAndScheduledDateBetweenOrderByScheduledDate(
                userId, startDate, endDate);
    }


    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findByUserIdAndStatus(Long userId, WorkoutStatusType status) {
        validateUserExists(userId);
        return scheduledWorkoutRepository.findByUserUserIdAndStatus(userId, status);
    }

    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findTodaysWorkouts(Long userId) {
        validateUserExists(userId);
        return scheduledWorkoutRepository.findTodaysWorkoutsForUser(userId);
    }

    /**
     * start a workout( status IN_PROGRESS)
     */
    public ScheduledWorkout startWorkout(Long scheduledWorkoutId) {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            scheduledWorkoutRepository.startWorkout(scheduledWorkoutId, startTime);

            ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);
            return workout;

        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            String sqlState = extractSQLState(e);
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

            if (errorMessage.contains("WORKOUT_NOT_FOUND")) {
                throw new WorkoutNotFoundException("The workout was not found");
            }
            if (errorMessage.contains("INVALID_WORKOUT_STATUS")) {
                throw new InvalidWorkoutStatusException("The workout can only be started if it is scheduled");
            }

            throw new RuntimeException("Error while starting the workout: " + errorMessage, e);

        } catch (WorkoutNotFoundException | InvalidWorkoutStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while starting the workout", e);
        }
    }


    /**
     * Complete a workout
     */
    public ScheduledWorkout completeWorkout(Long scheduledWorkoutId, Integer caloriesBurned, Integer rating) {
        try {
            validateCompletionData(caloriesBurned, rating);

            LocalDateTime endTime = LocalDateTime.now();
            scheduledWorkoutRepository.completeWorkout(scheduledWorkoutId, endTime, caloriesBurned, rating);

            ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);
            return workout;

        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            String sqlState = extractSQLState(e);

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
            if (errorMessage.contains("WORKOUT_NOT_FOUND")) {
                throw new WorkoutNotFoundException("The workout was not found");
            }
            if (errorMessage.contains("INVALID_WORKOUT_STATUS")) {
                throw new InvalidWorkoutStatusException("The workout can only be completed if it is in progress");
            }
            if (errorMessage.contains("INVALID_CALORIES")) {
                throw new IllegalArgumentException("Burned calories cannot be negative");
            }
            if (errorMessage.contains("INVALID_RATING")) {
                throw new IllegalArgumentException("The rating must be between 1 and 5");
            }

            throw new RuntimeException("Error while completing the workout: " + errorMessage, e);

        } catch (WorkoutNotFoundException | InvalidWorkoutStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while completing the workout", e);
        }
    }

    /**
     * cancel a scheduled workout
     */
    public ScheduledWorkout cancelWorkout(Long scheduledWorkoutId) {
        ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

        if (workout.getStatus() == WorkoutStatusType.COMPLETED) {
            throw new IllegalStateException("You can't cancel a completed workout");
        }

        scheduledWorkoutRepository.updateWorkoutStatus(scheduledWorkoutId, WorkoutStatusType.CANCELLED);

        workout = findScheduledWorkoutById(scheduledWorkoutId);
        return workout;
    }

    /**
     * Marks a workout as missed
     * @return workout updated
     */
    public ScheduledWorkout markWorkoutAsMissed(Long scheduledWorkoutId) {
        log.info("Marking workout as missed with ID: {}", scheduledWorkoutId);

        ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

        if (workout.getStatus() != WorkoutStatusType.PLANNED) {
            throw new IllegalStateException("Doar workout-urile planificate pot fi marcate ca ratate");
        }

        scheduledWorkoutRepository.updateWorkoutStatus(scheduledWorkoutId, WorkoutStatusType.MISSED);

        workout = findScheduledWorkoutById(scheduledWorkoutId);
        log.info("Workout marked as missed successfully");

        return workout;
    }

    @Transactional(readOnly = true)
    public Long countCompletedWorkouts(Long userId) {
        validateUserExists(userId);
        return scheduledWorkoutRepository.countCompletedWorkoutsForUser(userId);
    }

    @Transactional(readOnly = true)
    public Double getAverageWorkoutDuration(Long userId) {
        validateUserExists(userId);
        return scheduledWorkoutRepository.getAverageWorkoutDurationForUser(userId);
    }


    @Transactional(readOnly = true)
    public List<ScheduledWorkout> findRecentCompletedWorkouts(Long userId) {
        validateUserExists(userId);
        return scheduledWorkoutRepository.findTop5ByUserUserIdAndStatusOrderByActualEndTimeDesc(
                userId, WorkoutStatusType.COMPLETED);
    }

    /**
     * reschedule an existing workout
     */
    @Transactional
    public void rescheduleWorkout(Long scheduledWorkoutId, LocalDate newDate, LocalTime newTime) {
        try {
            ScheduledWorkout scheduledWorkout = scheduledWorkoutRepository.findById(scheduledWorkoutId)
                    .orElseThrow(() -> new WorkoutNotFoundException("The scheduled workout was not found"));

            if (scheduledWorkout.getStatus() == WorkoutStatusType.COMPLETED) {
                throw new InvalidWorkoutStatusException("A completed workout cannot be rescheduled");
            }

            if (scheduledWorkout.getStatus() == WorkoutStatusType.IN_PROGRESS) {
                throw new InvalidWorkoutStatusException("A workout in progress cannot be rescheduled");
            }

            // Validate the new date
            if (newDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("A workout cannot be scheduled in the past");
            }

            boolean dateChanged = !newDate.equals(scheduledWorkout.getScheduledDate());
            boolean timeChanged = (newTime != null && !newTime.equals(scheduledWorkout.getScheduledTime())) ||
                    (newTime == null && scheduledWorkout.getScheduledTime() != null);

            if (dateChanged || timeChanged) {
                if (!isTimeSlotAvailable(scheduledWorkout.getUser().getUserId(), newDate, newTime, scheduledWorkoutId)) {
                    throw new WorkoutAlreadyScheduledException("The selected time slot is not available – a workout is already scheduled");
                }
            }

            // Update the workout
            scheduledWorkout.setScheduledDate(newDate);
            scheduledWorkout.setScheduledTime(newTime);
            scheduledWorkout.setUpdatedAt(LocalDateTime.now());

            switch (scheduledWorkout.getStatus()) {
                case CANCELLED:
                case MISSED:
                    scheduledWorkout.setStatus(WorkoutStatusType.PLANNED);
                    break;
                case PLANNED:
                    break;
                default:
                    scheduledWorkout.setStatus(WorkoutStatusType.PLANNED);
            }

            scheduledWorkoutRepository.save(scheduledWorkout);
        } catch (WorkoutNotFoundException | InvalidWorkoutStatusException |
                 WorkoutAlreadyScheduledException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while rescheduling the workout", e);
        }
    }


    private boolean isTimeSlotAvailable(Long userId, LocalDate date, LocalTime time, Long excludeWorkoutId) {
        List<WorkoutStatusType> activeStatuses = Arrays.asList(
                WorkoutStatusType.PLANNED,
                WorkoutStatusType.IN_PROGRESS
        );

        try {
            if (time == null) {
                List<ScheduledWorkout> dayWorkouts = scheduledWorkoutRepository
                        .findWorkoutsForDateExcluding(userId, date, excludeWorkoutId, activeStatuses);
                return dayWorkouts.isEmpty();
            }


            List<ScheduledWorkout> exactConflicts = scheduledWorkoutRepository
                    .findExactTimeConflictsForReschedule(userId, date, time, excludeWorkoutId, activeStatuses);

            if (!exactConflicts.isEmpty()) {
                return false;
            }

            List<ScheduledWorkout> dayWorkouts = scheduledWorkoutRepository
                    .findWorkoutsForDateExcluding(userId, date, excludeWorkoutId, activeStatuses);

            LocalTime newWorkoutStart = time;
            LocalTime newWorkoutEnd = time.plusHours(1);

            for (ScheduledWorkout existingWorkout : dayWorkouts) {
                LocalTime existingStart = existingWorkout.getScheduledTime();

                if (existingStart == null) {
                    continue;
                }

                LocalTime existingEnd = existingStart.plusHours(1);

                boolean hasOverlap = !(newWorkoutEnd.isBefore(existingStart) ||
                        newWorkoutStart.isAfter(existingEnd) ||
                        newWorkoutEnd.equals(existingStart) ||
                        newWorkoutStart.equals(existingEnd));

                if (hasOverlap) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            return true;
        }
    }


    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }

    private void validateCompletionData(Integer caloriesBurned, Integer rating) {
        if (caloriesBurned != null && caloriesBurned < 0) {
            throw new IllegalArgumentException("Burned calories cannot be negative");
        }

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
    }

    private ScheduledWorkout findScheduledWorkoutById(Long scheduledWorkoutId) {
        return scheduledWorkoutRepository.findById(scheduledWorkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled workout not found with ID: " + scheduledWorkoutId));
    }


    private String extractSQLState(DataAccessException e) {
        try {
            if (e.getCause() != null && e.getCause().getCause() != null) {
                String message = e.getCause().getCause().getMessage();
                if (message != null && message.contains("ERROR:")) {
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


    private String extractCustomErrorMessage(String errorMessage, String errorType) {
        try {
            Pattern pattern = Pattern.compile(errorType + ":\\s*([^\\n\\r]+)");
            Matcher matcher = pattern.matcher(errorMessage);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }

            if (errorMessage.contains(errorType)) {
                return getDefaultErrorMessage(errorType);
            }
        } catch (Exception e) {
            log.warn("Failed to extract custom error message for {}: {}", errorType, e.getMessage());
        }

        return getDefaultErrorMessage(errorType);
    }


    private String getDefaultErrorMessage(String errorType) {
        switch (errorType) {
            case "INVALID_USER_ID":
                return "The user ID is invalid";
            case "INVALID_WORKOUT_PLAN_ID":
                return "The workout plan ID is invalid";
            case "USER_NOT_FOUND":
                return "The user was not found";
            case "WORKOUT_PLAN_NOT_FOUND":
                return "The workout plan was not found";
            case "WORKOUT_PLAN_NOT_OWNED":
                return "The workout plan does not belong to this user";
            case "INVALID_SCHEDULED_DATE":
                return "The scheduled date is invalid";
            case "WORKOUT_ALREADY_SCHEDULED":
                return "A workout is already scheduled at this date and time";
            case "WORKOUT_NOT_FOUND":
                return "The workout was not found";
            case "INVALID_WORKOUT_STATUS":
                return "The workout status does not allow this operation";
            case "INVALID_CALORIES":
                return "Burned calories value is invalid";
            case "INVALID_RATING":
                return "The rating is invalid";
            case "DATABASE_ERROR":
                return "A database error occurred";
            default:
                return "An unexpected error occurred";
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class WorkoutStatistics {
        private Long completedWorkouts;
        private Double averageDurationMinutes;
    }
}