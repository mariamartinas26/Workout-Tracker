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
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduledWorkoutRepository extends JpaRepository<ScheduledWorkout, Long> {

    @Query("SELECT sw FROM ScheduledWorkout sw " +
            "JOIN FETCH sw.user u " +
            "LEFT JOIN FETCH sw.workoutPlan wp " +
            "WHERE u.userId = :userId " +
            "ORDER BY sw.scheduledDate DESC")
    List<ScheduledWorkout> findByUserUserIdOrderByScheduledDateDesc(@Param("userId") Long userId);

    List<ScheduledWorkout> findByUserUserIdAndScheduledDateBetweenOrderByScheduledDate(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<ScheduledWorkout> findByUserUserIdAndStatus(Long userId, WorkoutStatusType status);

    @Query("SELECT sw FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.scheduledDate = CURRENT_DATE")
    List<ScheduledWorkout> findTodaysWorkoutsForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(sw) FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.status = 'COMPLETED'")
    Long countCompletedWorkoutsForUser(@Param("userId") Long userId);

    @Query("SELECT AVG(sw.actualDurationMinutes) FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND sw.status = 'COMPLETED'")
    Double getAverageWorkoutDurationForUser(@Param("userId") Long userId);

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

    /**
     * Simple exact time conflict check for reschedule
     */
    @Query("SELECT sw FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.scheduledDate = :date " +
            "AND sw.scheduledTime = :scheduledTime " +
            "AND sw.scheduledWorkoutId != :excludeWorkoutId " +
            "AND sw.status IN :activeStatuses")
    List<ScheduledWorkout> findExactTimeConflictsForReschedule(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("scheduledTime") LocalTime scheduledTime,
            @Param("excludeWorkoutId") Long excludeWorkoutId,
            @Param("activeStatuses") List<WorkoutStatusType> activeStatuses
    );

    @Query("SELECT sw FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.scheduledDate = :date " +
            "AND sw.scheduledWorkoutId != :excludeWorkoutId " +
            "AND sw.status IN :activeStatuses " +
            "AND sw.scheduledTime IS NOT NULL")
    List<ScheduledWorkout> findWorkoutsForDateExcluding(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("excludeWorkoutId") Long excludeWorkoutId,
            @Param("activeStatuses") List<WorkoutStatusType> activeStatuses
    );

    @Query("SELECT COUNT(sw) > 0 FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.scheduledDate = :scheduledDate " +
            "AND sw.scheduledTime = :scheduledTime " +
            "AND sw.status IN :statuses")
    boolean hasWorkoutScheduledAtSpecificTime(
            @Param("userId") Long userId,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("scheduledTime") LocalTime scheduledTime,
            @Param("statuses") List<WorkoutStatusType> statuses
    );

    @Query("SELECT COUNT(sw) > 0 FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.scheduledDate = :scheduledDate " +
            "AND sw.scheduledTime IS NULL " +
            "AND sw.status IN :statuses")
    boolean hasWorkoutScheduledOnDate(
            @Param("userId") Long userId,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("statuses") List<WorkoutStatusType> statuses
    );

    // Dashboard Summary Queries - replacing the PL/SQL function
    @Query("SELECT COUNT(sw), " +
            "COALESCE(SUM(sw.caloriesBurned), 0), " +
            "AVG(CASE WHEN sw.actualDurationMinutes > 0 THEN sw.actualDurationMinutes ELSE NULL END), " +
            "AVG(sw.overallRating), " +
            "COUNT(DISTINCT sw.scheduledDate) " +
            "FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.status = 'COMPLETED' " +
            "AND sw.scheduledDate BETWEEN :startDate AND :endDate")
    List<Object[]> getWorkoutStatsForPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(sw), " +
            "COALESCE(SUM(sw.caloriesBurned), 0), " +
            "COUNT(DISTINCT sw.scheduledDate), " +
            "AVG(CASE WHEN sw.actualDurationMinutes > 0 THEN sw.actualDurationMinutes ELSE NULL END), " +
            "MIN(sw.scheduledDate) " +
            "FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.status = 'COMPLETED'")
    List<Object[]> getLifetimeWorkoutStats(@Param("userId") Long userId);

    // Keep the old PL/SQL function calls for other features (can be converted later)
    // Dashboard Summary - OLD (can be removed after testing new implementation)
    @Query(value = "SELECT * FROM get_dashboard_summary(:userId, :currentDate)", nativeQuery = true)
    List<Object[]> getDashboardSummaryOld(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    // Workout Calendar
    @Query(value = "SELECT * FROM get_workout_calendar(:userId, :startDate, :endDate)", nativeQuery = true)
    List<Object[]> getWorkoutCalendar(@Param("userId") Long userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    // Workout Trends
    @Query(value = "SELECT * FROM get_workout_trends(:userId, :periodType, :startDate, :endDate)", nativeQuery = true)
    List<Object[]> getWorkoutTrends(@Param("userId") Long userId,
                                    @Param("periodType") String periodType,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    // Workout Type Breakdown
    @Query(value = "SELECT * FROM get_workout_type_breakdown(:userId, :startDate, :endDate)", nativeQuery = true)
    List<Object[]> getWorkoutTypeBreakdown(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
}