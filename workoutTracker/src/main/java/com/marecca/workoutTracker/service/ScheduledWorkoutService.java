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
    public Long scheduleWorkout(Long userId, Long workoutPlanId, LocalDate scheduledDate, LocalTime scheduledTime) {
        log.info("Scheduling workout for user {} with plan {} on {} at {}", userId, workoutPlanId, scheduledDate, scheduledTime);

        try {
            // Validate input parameters - following exact PL/SQL function logic

            // Check: p_user_id IS NULL OR p_user_id <= 0
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("INVALID_USER_ID: User ID must be a positive number");
            }

            // Check: p_workout_plan_id IS NULL OR p_workout_plan_id <= 0
            if (workoutPlanId == null || workoutPlanId <= 0) {
                throw new IllegalArgumentException("INVALID_WORKOUT_PLAN_ID: Workout plan ID must be a positive number");
            }

            // Check: p_scheduled_date IS NULL OR p_scheduled_date < CURRENT_DATE
            if (scheduledDate == null || scheduledDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("INVALID_SCHEDULED_DATE: Scheduled date cannot be null or in the past");
            }

            // Check if user exists - following exact PL/SQL function logic
            if (!userRepository.existsById(userId)) {
                throw new UserNotFoundException(String.format("USER_NOT_FOUND: User with ID %s does not exist", userId));
            }

            // Check if workout plan exists - following exact PL/SQL function logic
            if (!workoutPlanRepository.existsById(workoutPlanId)) {
                throw new WorkoutPlanNotFoundException(String.format("WORKOUT_PLAN_NOT_FOUND: Workout plan with ID %s does not exist", workoutPlanId));
            }

            // Check if workout plan belongs to the user - following exact PL/SQL function logic
            Optional<WorkoutPlan> workoutPlanOpt = workoutPlanRepository.findById(workoutPlanId);
            if (workoutPlanOpt.isPresent() && !workoutPlanOpt.get().getUser().getUserId().equals(userId)) {
                throw new IllegalArgumentException(String.format("WORKOUT_PLAN_NOT_OWNED: Workout plan with ID %s does not belong to user %s", workoutPlanId, userId));
            }

            // Check if user already has a workout scheduled at the same date and time - following exact PL/SQL function logic
            List<WorkoutStatusType> activeStatuses = Arrays.asList(WorkoutStatusType.PLANNED, WorkoutStatusType.IN_PROGRESS);

            // Use the exact same logic as the PL/SQL function, but split into two cases for better JPA handling
            boolean hasConflict = false;

            if (scheduledTime == null) {
                // When scheduledTime is NULL, check for other workouts with NULL scheduledTime on same date
                hasConflict = scheduledWorkoutRepository.hasWorkoutScheduledOnDate(userId, scheduledDate, activeStatuses);
            } else {
                // When scheduledTime is NOT NULL, check for workouts at the exact same time
                hasConflict = scheduledWorkoutRepository.hasWorkoutScheduledAtSpecificTime(userId, scheduledDate, scheduledTime, activeStatuses);
            }

            if (hasConflict) {
                String timeInfo = scheduledTime != null ? scheduledTime.toString() : "no specific time";
                throw new WorkoutAlreadyScheduledException(
                        String.format("WORKOUT_ALREADY_SCHEDULED: User already has a workout scheduled at %s %s",
                                scheduledDate, timeInfo));
            }

            // Get the entities for saving
            User user = userRepository.findById(userId).get(); // We already validated it exists
            WorkoutPlan workoutPlan = workoutPlanRepository.findById(workoutPlanId).get(); // We already validated it exists

            // Insert the scheduled workout - following exact PL/SQL function logic
            ScheduledWorkout scheduledWorkout = ScheduledWorkout.builder()
                    .user(user)
                    .workoutPlan(workoutPlan)
                    .scheduledDate(scheduledDate)
                    .scheduledTime(scheduledTime)
                    .status(WorkoutStatusType.PLANNED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            ScheduledWorkout savedWorkout = scheduledWorkoutRepository.save(scheduledWorkout);

            log.info("Successfully scheduled workout with ID: {}", savedWorkout.getScheduledWorkoutId());
            return savedWorkout.getScheduledWorkoutId();

        } catch (UserNotFoundException e) {
            log.error("User not found error: {}", e.getMessage());
            throw e;
        } catch (WorkoutPlanNotFoundException e) {
            log.error("Workout plan not found error: {}", e.getMessage());
            throw e;
        } catch (WorkoutAlreadyScheduledException e) {
            log.error("Workout already scheduled error: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while scheduling workout", e);
            // Following PL/SQL function logic for unexpected errors
            throw new RuntimeException("DATABASE_ERROR: Unexpected error occurred while scheduling workout: " + e.getMessage(), e);
        }
    }

    /**
     * checks if a user can schedule a workout at a specific hour/date
     * Uses the same logic as the PL/SQL schedule_workout function
     */
    @Transactional(readOnly = true)
    public boolean canScheduleWorkoutAt(Long userId, LocalDate scheduledDate, LocalTime scheduledTime) {
        try {
            List<WorkoutStatusType> activeStatuses = Arrays.asList(WorkoutStatusType.PLANNED, WorkoutStatusType.IN_PROGRESS);

            // Use the exact same conflict checking logic as the PL/SQL function, split into two cases
            if (scheduledTime == null) {
                // When scheduledTime is NULL, check for other workouts with NULL scheduledTime on same date
                return !scheduledWorkoutRepository.hasWorkoutScheduledOnDate(userId, scheduledDate, activeStatuses);
            } else {
                // When scheduledTime is NOT NULL, check for workouts at the exact same time
                return !scheduledWorkoutRepository.hasWorkoutScheduledAtSpecificTime(userId, scheduledDate, scheduledTime, activeStatuses);
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

    @Transactional
    public List<ScheduledWorkout> findTodaysWorkouts(Long userId) {
        validateUserExists(userId);

        // Get today's workouts
        List<ScheduledWorkout> workouts = scheduledWorkoutRepository.findTodaysWorkoutsForUser(userId);

        // Check for missed workouts and mark them
        LocalTime currentTime = LocalTime.now();
        boolean hasUpdates = false;

        for (ScheduledWorkout workout : workouts) {
            if (workout.getStatus() == WorkoutStatusType.PLANNED &&
                    workout.getScheduledTime() != null &&
                    currentTime.isAfter(workout.getScheduledTime())) {

                try {
                    log.info("Marking workout {} as missed - scheduled time {} has passed (current time: {})",
                            workout.getScheduledWorkoutId(), workout.getScheduledTime(), currentTime);

                    // Update status directly in repository
                    scheduledWorkoutRepository.updateWorkoutStatus(workout.getScheduledWorkoutId(), WorkoutStatusType.MISSED);

                    // Update the workout object status for the response
                    workout.setStatus(WorkoutStatusType.MISSED);
                    hasUpdates = true;

                } catch (Exception e) {
                    log.warn("Failed to mark workout {} as missed: {}", workout.getScheduledWorkoutId(), e.getMessage());
                }
            }
        }

        if (hasUpdates) {
            log.info("Automatically marked {} planned workouts as missed for user {}",
                    workouts.stream().mapToLong(w -> w.getStatus() == WorkoutStatusType.MISSED ? 1 : 0).sum(), userId);
        }

        return workouts;
    }

    /**
     * start a workout( status IN_PROGRESS)
     */
    public ScheduledWorkout startWorkout(Long scheduledWorkoutId) {
        try {
            ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

            if (workout.getStatus() != WorkoutStatusType.PLANNED) {
                throw new InvalidWorkoutStatusException("The workout can only be started if it is scheduled");
            }

            LocalDateTime startTime = LocalDateTime.now();
            scheduledWorkoutRepository.startWorkout(scheduledWorkoutId, startTime);

            return findScheduledWorkoutById(scheduledWorkoutId);

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
            ScheduledWorkout workout = findScheduledWorkoutById(scheduledWorkoutId);

            if (workout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
                throw new InvalidWorkoutStatusException("The workout can only be completed if it is in progress");
            }

            validateCompletionData(caloriesBurned, rating);

            LocalDateTime endTime = LocalDateTime.now();
            scheduledWorkoutRepository.completeWorkout(scheduledWorkoutId, endTime, caloriesBurned, rating);

            return findScheduledWorkoutById(scheduledWorkoutId);

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

        return findScheduledWorkoutById(scheduledWorkoutId);
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
                .orElseThrow(() -> new WorkoutNotFoundException("Scheduled workout not found with ID: " + scheduledWorkoutId));
    }

    @lombok.Builder
    @lombok.Data
    public static class WorkoutStatistics {
        private Long completedWorkouts;
        private Double averageDurationMinutes;
    }
}