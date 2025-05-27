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

/**
 * Repository pentru entitatea WorkoutExerciseLog
 */
@Repository
public interface WorkoutExerciseLogRepository extends JpaRepository<WorkoutExerciseLog, Long> {

    /**
     * Găsește toate logurile pentru un workout programat
     */
    List<WorkoutExerciseLog> findByScheduledWorkoutScheduledWorkoutIdOrderByExerciseOrder(Long scheduledWorkoutId);

    /**
     * Găsește toate logurile pentru un exercițiu specific
     */
    List<WorkoutExerciseLog> findByExerciseExerciseId(Long exerciseId);

    /**
     * Găsește toate logurile pentru exercițiile unui utilizator
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId")
    List<WorkoutExerciseLog> findByUserId(@Param("userId") Long userId);

    /**
     * Găsește toate logurile pentru exercițiile unui utilizator într-o perioadă specifică
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND " +
            "sw.scheduledDate BETWEEN :startDate AND :endDate")
    List<WorkoutExerciseLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculează progresul pentru un exercițiu specific pentru un utilizator
     */
    @Query("SELECT wel FROM WorkoutExerciseLog wel " +
            "JOIN wel.scheduledWorkout sw " +
            "JOIN wel.exercise e " +
            "WHERE sw.user.userId = :userId AND e.exerciseId = :exerciseId " +
            "ORDER BY sw.scheduledDate")
    List<WorkoutExerciseLog> findUserProgressForExercise(
            @Param("userId") Long userId,
            @Param("exerciseId") Long exerciseId);

    /**
     * Găsește cele mai recente loguri pentru exercițiile unui utilizator
     */
    @Query(value = "SELECT * FROM workout_exercise_logs wel " +
            "JOIN scheduled_workouts sw ON wel.scheduled_workout_id = sw.scheduled_workout_id " +
            "WHERE sw.user_id = :userId " +
            "ORDER BY sw.scheduled_date DESC, wel.exercise_order " +
            "LIMIT :limit",
            nativeQuery = true)
    List<WorkoutExerciseLog> findRecentExerciseLogsForUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    /**
     * Șterge toate logurile pentru un workout programat
     */
    @Modifying
    @Query("DELETE FROM WorkoutExerciseLog wel WHERE wel.scheduledWorkout.scheduledWorkoutId = :scheduledWorkoutId")
    void deleteByScheduledWorkoutId(@Param("scheduledWorkoutId") Long scheduledWorkoutId);

    /**
     * Găsește recordurile personale pentru un exercițiu
     */
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

    /**
     * Calculează volumul total pentru un exercițiu într-o perioadă de timp
     */
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