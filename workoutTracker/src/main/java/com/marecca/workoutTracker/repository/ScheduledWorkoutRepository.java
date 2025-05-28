package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledWorkoutRepository extends JpaRepository<ScheduledWorkout, Long> {


    List<ScheduledWorkout> findByUserUserIdOrderByScheduledDateDesc(Long userId);
    List<ScheduledWorkout> findByUserUserIdAndScheduledDateBetweenOrderByScheduledDate(
            Long userId, LocalDate startDate, LocalDate endDate);
    List<ScheduledWorkout> findByUserUserIdAndStatus(Long userId, WorkoutStatusType status);

    @Query("SELECT sw FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.scheduledDate = CURRENT_DATE")
    List<ScheduledWorkout> findTodaysWorkoutsForUser(@Param("userId") Long userId);


    @Query("SELECT COUNT(sw) FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.status = com.marecca.workoutTracker.entity.enums.WorkoutStatusType.COMPLETED")

    List<ScheduledWorkout> findMissedWorkoutsForUser(@Param("userId") Long userId);

    List<ScheduledWorkout> findByWorkoutPlanWorkoutPlanId(Long workoutPlanId);

    @Query("SELECT COUNT(sw) FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.status = 'COMPLETED'")
    Long countCompletedWorkoutsForUser(@Param("userId") Long userId);

    @Query("SELECT AVG(sw.actualDurationMinutes) FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND sw.status = 'COMPLETED'")
    Double getAverageWorkoutDurationForUser(@Param("userId") Long userId);


    List<ScheduledWorkout> findByUserUserIdAndStatusOrderByOverallRatingDesc(
            Long userId, WorkoutStatusType status);

    @Modifying
    @Query("UPDATE ScheduledWorkout sw SET sw.status = :status WHERE sw.scheduledWorkoutId = :workoutId")
    void updateWorkoutStatus(
            @Param("workoutId") Long workoutId,
            @Param("status") WorkoutStatusType status);


    @Modifying
    @Query("UPDATE ScheduledWorkout sw SET sw.status = 'IN_PROGRESS', sw.actualStartTime = :startTime " +
            "WHERE sw.scheduledWorkoutId = :workoutId")
    void startWorkout(
            @Param("workoutId") Long workoutId,
            @Param("startTime") LocalDateTime startTime);

    @Modifying
    @Query("UPDATE ScheduledWorkout sw SET sw.status = 'COMPLETED', sw.actualEndTime = :endTime, " +
            "sw.caloriesBurned = :caloriesBurned, sw.overallRating = :rating " +
            "WHERE sw.scheduledWorkoutId = :workoutId")
    void completeWorkout(
            @Param("workoutId") Long workoutId,
            @Param("endTime") LocalDateTime endTime,
            @Param("caloriesBurned") Integer caloriesBurned,
            @Param("rating") Integer rating);


    List<ScheduledWorkout> findTop5ByUserUserIdAndStatusOrderByActualEndTimeDesc(
            Long userId, WorkoutStatusType status);
}