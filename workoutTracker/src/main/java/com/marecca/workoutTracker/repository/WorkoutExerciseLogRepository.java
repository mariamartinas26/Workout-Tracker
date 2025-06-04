package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.WorkoutExerciseLog;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkoutExerciseLogRepository extends JpaRepository<WorkoutExerciseLog, Long> {
    /**
     * Find exercise logs by user, exercise and workout status
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId " +
            "AND e.exerciseId = :exerciseId " +
            "AND sw.status = :status")
    List<WorkoutExerciseLog> findLogsByUserExerciseAndStatus(@Param("userId") Long userId,
                                                             @Param("exerciseId") Long exerciseId,
                                                             @Param("status") WorkoutStatusType status);

    /**
     * Check if user did a specific exercise recently
     */
    @Query("SELECT COUNT(wel) > 0 FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId " +
            "AND e.exerciseId = :exerciseId " +
            "AND sw.actualStartTime >= :startDate")
    boolean existsRecentExerciseLog(@Param("userId") Long userId,
                                    @Param("exerciseId") Long exerciseId,
                                    @Param("startDate") LocalDateTime startDate);


    /**
     * Find all logs for a specific scheduled workout
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "WHERE wel.scheduledWorkout.scheduledWorkoutId = :scheduledWorkoutId " +
            "ORDER BY wel.exerciseOrder")
    List<WorkoutExerciseLog> findByScheduledWorkoutIdOrderByExerciseOrder(@Param("scheduledWorkoutId") Long scheduledWorkoutId);

    /**
     * Find all logs for a specific user
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId")
    List<WorkoutExerciseLog> findByUserId(@Param("userId") Long userId);

    /**
     * Find logs for user within date range
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.scheduledDate BETWEEN :startDate AND :endDate")
    List<WorkoutExerciseLog> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Find user's progress for a specific exercise (chronological order)
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId " +
            "AND e.exerciseId = :exerciseId " +
            "ORDER BY sw.scheduledDate ASC")
    List<WorkoutExerciseLog> findUserProgressForExercise(@Param("userId") Long userId,
                                                         @Param("exerciseId") Long exerciseId);

    /**
     * Find recent exercise logs for user (using native query for LIMIT support)
     */
    @Query(value = "SELECT wel.* FROM workout_exercise_logs wel " +
            "JOIN scheduled_workouts sw ON wel.scheduled_workout_id = sw.scheduled_workout_id " +
            "WHERE sw.user_id = :userId " +
            "ORDER BY sw.scheduled_date DESC, wel.exercise_order " +
            "LIMIT :limit",
            nativeQuery = true)
    List<WorkoutExerciseLog> findRecentExerciseLogsForUser(@Param("userId") Long userId,
                                                           @Param("limit") int limit);


    /**
     * Find user's personal best weight for a specific exercise
     */
    @Query("SELECT MAX(wel.weightUsedKg) FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId " +
            "AND e.exerciseId = :exerciseId " +
            "AND wel.weightUsedKg IS NOT NULL")
    BigDecimal findPersonalBestWeightForExercise(@Param("userId") Long userId,
                                                 @Param("exerciseId") Long exerciseId);

    /**
     * Find user's personal best reps for a specific exercise
     */
    @Query("SELECT MAX(wel.repsCompleted) FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId " +
            "AND e.exerciseId = :exerciseId " +
            "AND wel.repsCompleted IS NOT NULL")
    Integer findPersonalBestRepsForExercise(@Param("userId") Long userId,
                                            @Param("exerciseId") Long exerciseId);

    /**
     * Calculate total volume for exercise in date range
     * Volume = sets × reps × weight
     */
    @Query("SELECT COALESCE(SUM(wel.setsCompleted * wel.repsCompleted * wel.weightUsedKg), 0) " +
            "FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId " +
            "AND e.exerciseId = :exerciseId " +
            "AND sw.scheduledDate BETWEEN :startDate AND :endDate " +
            "AND wel.setsCompleted IS NOT NULL " +
            "AND wel.repsCompleted IS NOT NULL " +
            "AND wel.weightUsedKg IS NOT NULL")
    BigDecimal calculateTotalVolumeForExercise(@Param("userId") Long userId,
                                               @Param("exerciseId") Long exerciseId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

}