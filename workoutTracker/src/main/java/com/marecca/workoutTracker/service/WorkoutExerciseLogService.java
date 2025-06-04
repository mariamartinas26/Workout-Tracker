package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.WorkoutExerciseLog;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import com.marecca.workoutTracker.repository.ScheduledWorkoutRepository;
import com.marecca.workoutTracker.repository.WorkoutExerciseLogRepository;
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
 * Business logic for analizing performances
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkoutExerciseLogService {

    private final WorkoutExerciseLogRepository workoutExerciseLogRepository;
    private final ScheduledWorkoutRepository scheduledWorkoutRepository;

    public WorkoutExerciseLog logExercise(WorkoutExerciseLog exerciseLog) {
        validateExerciseLogData(exerciseLog);

        ScheduledWorkout scheduledWorkout = findScheduledWorkoutById(
                exerciseLog.getScheduledWorkout().getScheduledWorkoutId());

        if (scheduledWorkout.getStatus() != WorkoutStatusType.IN_PROGRESS) {
            throw new IllegalStateException("You can register exercises only for IN progress workouts");
        }

        exerciseLog.setCreatedAt(LocalDateTime.now());

        WorkoutExerciseLog savedLog = workoutExerciseLogRepository.save(exerciseLog);
        return savedLog;
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutExerciseLog> findById(Long logId) {
        return workoutExerciseLogRepository.findById(logId);
    }


    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findByScheduledWorkoutId(Long scheduledWorkoutId) {
        validateScheduledWorkoutExists(scheduledWorkoutId);
        return workoutExerciseLogRepository.findByScheduledWorkoutIdOrderByExerciseOrder(scheduledWorkoutId);
    }

    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findByUserId(Long userId) {
        return workoutExerciseLogRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return workoutExerciseLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * finds progress of a user for a specific exercise
     */
    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findUserProgressForExercise(Long userId, Long exerciseId) {
        return workoutExerciseLogRepository.findUserProgressForExercise(userId, exerciseId);
    }

    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findRecentLogsForUser(Long userId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit should be positive");
        }

        return workoutExerciseLogRepository.findRecentExerciseLogsForUser(userId, limit);
    }

    public WorkoutExerciseLog updateExerciseLog(Long logId, WorkoutExerciseLog updatedLog) {
        WorkoutExerciseLog existingLog = findExerciseLogById(logId);

        ScheduledWorkout scheduledWorkout = existingLog.getScheduledWorkout();
        if (scheduledWorkout.getStatus() != WorkoutStatusType.IN_PROGRESS &&
                scheduledWorkout.getStatus() != WorkoutStatusType.COMPLETED) {
            throw new IllegalStateException("you can update logs only for completed or in progress workouts");
        }

        validateExerciseLogData(updatedLog);
        updateExerciseLogFields(existingLog, updatedLog);

        WorkoutExerciseLog savedLog = workoutExerciseLogRepository.save(existingLog);
        return savedLog;
    }

    public void deleteExerciseLog(Long logId) {
        WorkoutExerciseLog exerciseLog = findExerciseLogById(logId);

        if (exerciseLog.getScheduledWorkout().getStatus() == WorkoutStatusType.COMPLETED) {
            throw new IllegalStateException("Can't delete log of a completed workout");
        }

        workoutExerciseLogRepository.deleteById(logId);
    }

    /**
     * finds personal record for weight used for a specific exercise
     */
    @Transactional(readOnly = true)
    public BigDecimal findPersonalBestWeight(Long userId, Long exerciseId) {
        return workoutExerciseLogRepository.findPersonalBestWeightForExercise(userId, exerciseId);
    }

    /**
     * finds personal record for reps for a specific exercise
     */
    @Transactional(readOnly = true)
    public Integer findPersonalBestReps(Long userId, Long exerciseId) {
        return workoutExerciseLogRepository.findPersonalBestRepsForExercise(userId, exerciseId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalVolume(Long userId, Long exerciseId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return workoutExerciseLogRepository.calculateTotalVolumeForExercise(userId, exerciseId, startDate, endDate);
    }

    /**
     * calculates progress for an exercise(compares first log with the last)
     */
    @Transactional(readOnly = true)
    public Double calculateProgressPercentage(Long userId, Long exerciseId) {
        List<WorkoutExerciseLog> progressLogs = findUserProgressForExercise(userId, exerciseId);

        if (progressLogs.size() < 2) {
            return null; //not enough data for progress
        }

        WorkoutExerciseLog firstLog = progressLogs.get(0);
        WorkoutExerciseLog lastLog = progressLogs.get(progressLogs.size() - 1);

        //progress based on weight used
        if (firstLog.getWeightUsedKg() != null && lastLog.getWeightUsedKg() != null &&
                firstLog.getWeightUsedKg().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal increase = lastLog.getWeightUsedKg().subtract(firstLog.getWeightUsedKg());
            BigDecimal percentage = increase.divide(firstLog.getWeightUsedKg(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            return percentage.doubleValue();
        }

        //progress based on reps if no weight was used
        if (firstLog.getRepsCompleted() != null && lastLog.getRepsCompleted() != null &&
                firstLog.getRepsCompleted() > 0) {

            double increase = lastLog.getRepsCompleted() - firstLog.getRepsCompleted();
            return (increase / firstLog.getRepsCompleted()) * 100;
        }

        return null;
    }


    @Transactional(readOnly = true)
    public List<WorkoutExerciseLog> findTopPerformingExercises(Long userId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit should be positive");
        }

        return workoutExerciseLogRepository.findRecentExerciseLogsForUser(userId, limit * 3)
                .stream()
                .filter(log -> log.getDifficultyRating() != null && log.getDifficultyRating() >= 4)
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countLogsByScheduledWorkout(Long scheduledWorkoutId) {
        validateScheduledWorkoutExists(scheduledWorkoutId);
        return findByScheduledWorkoutId(scheduledWorkoutId).size();
    }

    private void validateExerciseLogData(WorkoutExerciseLog exerciseLog) {
        if (exerciseLog.getScheduledWorkout() == null ||
                exerciseLog.getScheduledWorkout().getScheduledWorkoutId() == null) {
            throw new IllegalArgumentException("Scheduled workout is required");
        }

        if (exerciseLog.getExercise() == null || exerciseLog.getExercise().getExerciseId() == null) {
            throw new IllegalArgumentException("Exercise is required");
        }

        if (exerciseLog.getSetsCompleted() == null || exerciseLog.getSetsCompleted() < 0) {
            throw new IllegalArgumentException("Number of completed sets must be zero or positive");
        }

        if (exerciseLog.getExerciseOrder() == null || exerciseLog.getExerciseOrder() < 1) {
            throw new IllegalArgumentException("Exercise order must be positive");
        }

        if (exerciseLog.getRepsCompleted() != null && exerciseLog.getRepsCompleted() < 0) {
            throw new IllegalArgumentException("Number of completed reps cannot be negative");
        }

        if (exerciseLog.getWeightUsedKg() != null && exerciseLog.getWeightUsedKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weight used cannot be negative");
        }

        if (exerciseLog.getDurationSeconds() != null && exerciseLog.getDurationSeconds() < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        if (exerciseLog.getDistanceMeters() != null && exerciseLog.getDistanceMeters().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }

        if (exerciseLog.getCaloriesBurned() != null && exerciseLog.getCaloriesBurned() < 0) {
            throw new IllegalArgumentException("Calories burned cannot be negative");
        }

        if (exerciseLog.getDifficultyRating() != null &&
                (exerciseLog.getDifficultyRating() < 1 || exerciseLog.getDifficultyRating() > 5)) {
            throw new IllegalArgumentException("Difficulty rating must be between 1 and 5");
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

    private ScheduledWorkout findScheduledWorkoutById(Long scheduledWorkoutId) {
        return scheduledWorkoutRepository.findById(scheduledWorkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled workout not found with ID: " + scheduledWorkoutId));
    }

    private void validateScheduledWorkoutExists(Long scheduledWorkoutId) {
        if (!scheduledWorkoutRepository.existsById(scheduledWorkoutId)) {
            throw new IllegalArgumentException("Scheduled workout not found with ID: " + scheduledWorkoutId);
        }
    }

    private WorkoutExerciseLog findExerciseLogById(Long logId) {
        return workoutExerciseLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise log not found with ID: " + logId));
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