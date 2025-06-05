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
}