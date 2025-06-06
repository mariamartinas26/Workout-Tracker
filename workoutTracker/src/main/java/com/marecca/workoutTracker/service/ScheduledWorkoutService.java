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

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduledWorkoutService {

    private final ScheduledWorkoutRepository scheduledWorkoutRepository;
    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;

    @Transactional
    public Long scheduleWorkout(Long userId, Long workoutPlanId, LocalDate scheduledDate, LocalTime scheduledTime) {
        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("INVALID_USER_ID: User ID must be a positive number");
            }

            if (workoutPlanId == null || workoutPlanId <= 0) {
                throw new IllegalArgumentException("INVALID_WORKOUT_PLAN_ID: Workout plan ID must be a positive number");
            }

            if (scheduledDate == null || scheduledDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("INVALID_SCHEDULED_DATE: Scheduled date cannot be null or in the past");
            }

            if (!userRepository.existsById(userId)) {
                throw new UserNotFoundException(String.format("USER_NOT_FOUND: User with ID %s does not exist", userId));
            }

            if (!workoutPlanRepository.existsById(workoutPlanId)) {
                throw new WorkoutPlanNotFoundException(String.format("WORKOUT_PLAN_NOT_FOUND: Workout plan with ID %s does not exist", workoutPlanId));
            }

            Optional<WorkoutPlan> workoutPlanOpt = workoutPlanRepository.findById(workoutPlanId);
            if (workoutPlanOpt.isPresent() && !workoutPlanOpt.get().getUser().getUserId().equals(userId)) {
                throw new IllegalArgumentException(String.format("WORKOUT_PLAN_NOT_OWNED: Workout plan with ID %s does not belong to user %s", workoutPlanId, userId));
            }

            List<WorkoutStatusType> activeStatuses = Arrays.asList(WorkoutStatusType.PLANNED, WorkoutStatusType.IN_PROGRESS);

            boolean hasConflict = false;

            if (scheduledTime == null) {
                hasConflict = scheduledWorkoutRepository.hasWorkoutScheduledOnDate(userId, scheduledDate, activeStatuses);
            } else {
                hasConflict = scheduledWorkoutRepository.hasWorkoutScheduledAtSpecificTime(userId, scheduledDate, scheduledTime, activeStatuses);
            }

            if (hasConflict) {
                String timeInfo = scheduledTime != null ? scheduledTime.toString() : "no specific time";
                throw new WorkoutAlreadyScheduledException(
                        String.format("WORKOUT_ALREADY_SCHEDULED: User already has a workout scheduled at %s %s",
                                scheduledDate, timeInfo));
            }

            User user = userRepository.findById(userId).get();
            WorkoutPlan workoutPlan = workoutPlanRepository.findById(workoutPlanId).get();
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

            return savedWorkout.getScheduledWorkoutId();

        } catch (UserNotFoundException e) {
            throw new UserNotFoundException("User not found: " + e.getMessage());
        } catch (WorkoutPlanNotFoundException e) {
            throw new WorkoutPlanNotFoundException("Workout plan not found: " + e.getMessage());
        } catch (WorkoutAlreadyScheduledException e) {
            throw new WorkoutAlreadyScheduledException("Workout already scheduled: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Validation error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("DATABASE_ERROR: Unexpected error occurred while scheduling workout: " + e.getMessage(), e);
        }
    }

    /**
     * checks if a user can schedule a workout at a specific hour/date
     */
    @Transactional(readOnly = true)
    public boolean canScheduleWorkoutAt(Long userId, LocalDate scheduledDate, LocalTime scheduledTime) {
        try {
            List<WorkoutStatusType> activeStatuses = Arrays.asList(WorkoutStatusType.PLANNED, WorkoutStatusType.IN_PROGRESS);

            if (scheduledTime == null) {
                return !scheduledWorkoutRepository.hasWorkoutScheduledOnDate(userId, scheduledDate, activeStatuses);
            } else {
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


    @Transactional
    public List<ScheduledWorkout> MissedWorkouts(Long userId) {
        validateUserExists(userId);

        List<ScheduledWorkout> workouts = scheduledWorkoutRepository.findTodaysWorkoutsForUser(userId);

        LocalTime currentTime = LocalTime.now();

        for (ScheduledWorkout workout : workouts) {
            if (workout.getStatus() == WorkoutStatusType.PLANNED &&
                    workout.getScheduledTime() != null &&
                    currentTime.isAfter(workout.getScheduledTime())) {

                try {
                    scheduledWorkoutRepository.updateWorkoutStatus(workout.getScheduledWorkoutId(), WorkoutStatusType.MISSED);

                    // Update the workout object status for the response
                    workout.setStatus(WorkoutStatusType.MISSED);

                } catch (Exception e) {
                    System.err.println("Warning: Failed to mark workout as MISSED. ID: "
                            + workout.getScheduledWorkoutId() + " — " + e.getMessage());
                }
            }
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

            //update the workout
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