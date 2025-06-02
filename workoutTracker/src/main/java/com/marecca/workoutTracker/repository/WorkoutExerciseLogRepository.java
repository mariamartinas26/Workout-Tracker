package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.WorkoutExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Repository
public interface WorkoutExerciseLogRepository extends JpaRepository<WorkoutExerciseLog, Long> {


    @Query("SELECT wel FROM WorkoutExerciseLog wel WHERE wel.scheduledWorkout.scheduledWorkoutId = :scheduledWorkoutId ORDER BY wel.exerciseOrder")
    List<WorkoutExerciseLog> findByScheduledWorkoutIdOrderByExerciseOrder(@Param("scheduledWorkoutId") Long scheduledWorkoutId);
    List<WorkoutExerciseLog> findByExerciseExerciseId(Long exerciseId);


    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId")
    List<WorkoutExerciseLog> findByUserId(@Param("userId") Long userId);

    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND " +
            "sw.scheduledDate BETWEEN :startDate AND :endDate")
    List<WorkoutExerciseLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId AND e.exerciseId = :exerciseId " +
            "ORDER BY sw.scheduledDate")
    List<WorkoutExerciseLog> findUserProgressForExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId);

    @Query(value = "SELECT * FROM workout_exercise_logs wel " +
            "JOIN scheduled_workouts sw ON wel.scheduled_workout_id = sw.scheduled_workout_id " +
            "WHERE sw.user_id = :userId " +
            "ORDER BY sw.scheduled_date DESC, wel.exercise_order " +
            "LIMIT :limit",
            nativeQuery = true)
    List<WorkoutExerciseLog> findRecentExerciseLogsForUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);


    @Query("SELECT MAX(wel.weightUsedKg) FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND wel.exercise.exerciseId = :exerciseId")
    BigDecimal findPersonalBestWeightForExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId);

    @Query("SELECT MAX(wel.repsCompleted) FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND wel.exercise.exerciseId = :exerciseId")
    Integer findPersonalBestRepsForExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId);

    @Query("SELECT SUM(wel.setsCompleted * wel.repsCompleted * wel.weightUsedKg) FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND wel.exercise.exerciseId = :exerciseId " +
            "AND sw.scheduledDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalVolumeForExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}