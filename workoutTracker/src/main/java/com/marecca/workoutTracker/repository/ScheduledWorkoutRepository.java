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

    /**
     * Varianta 1: Apelare directă a funcției PostgreSQL
     * Folosește funcția schedule_workout creată în PostgreSQL
     */
    @Query(value = "SELECT schedule_workout(:userId, :workoutPlanId, :scheduledDate, :scheduledTime)",
            nativeQuery = true)
    Long scheduleWorkoutWithFunction(
            @Param("userId") Long userId,
            @Param("workoutPlanId") Long workoutPlanId,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("scheduledTime") LocalTime scheduledTime);

    /**
     * Varianta 2: Apelare funcție cu parametru opțional pentru timp
     * Pentru cazurile când scheduled_time este null
     */
    @Query(value = "SELECT schedule_workout(:userId, :workoutPlanId, :scheduledDate)",
            nativeQuery = true)
    Long scheduleWorkoutWithoutTime(
            @Param("userId") Long userId,
            @Param("workoutPlanId") Long workoutPlanId,
            @Param("scheduledDate") LocalDate scheduledDate);

    /**
     * Verificare pentru cazul când scheduled_time este specificat
     */
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

    /**
     * Verificare pentru cazul când scheduled_time este null (oricând în ziua respectivă)
     */
    @Query("SELECT COUNT(sw) > 0 FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId " +
            "AND sw.scheduledDate = :scheduledDate " +
            "AND sw.status IN :statuses")
    boolean hasWorkoutScheduledOnDate(
            @Param("userId") Long userId,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("statuses") List<WorkoutStatusType> statuses
    );


    /**
     * Varianta 4: Verificare dacă workout plan aparține utilizatorului
     * (pentru validare suplimentară în Java)
     */
    @Query("SELECT COUNT(wp) > 0 FROM WorkoutPlan wp " +
            "WHERE wp.workoutPlanId = :workoutPlanId AND wp.user.userId = :userId")
    boolean isWorkoutPlanOwnedByUser(
            @Param("workoutPlanId") Long workoutPlanId,
            @Param("userId") Long userId);

    // Dashboard Summary
    @Query(value = "SELECT * FROM get_dashboard_summary(:userId, :currentDate)", nativeQuery = true)
    List<Object[]> getDashboardSummary(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

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


    // Update Streak (called after workout completion)
    @Query(value = "SELECT * FROM update_workout_streak(:userId, :workoutDate)", nativeQuery = true)
    List<Object[]> updateWorkoutStreak(@Param("userId") Long userId, @Param("workoutDate") LocalDate workoutDate);
}