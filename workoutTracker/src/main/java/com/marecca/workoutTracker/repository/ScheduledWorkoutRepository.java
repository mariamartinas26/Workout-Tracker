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

/**
 * Repository pentru entitatea ScheduledWorkout
 */
@Repository
public interface ScheduledWorkoutRepository extends JpaRepository<ScheduledWorkout, Long> {

    /**
     * Găsește toate workout-urile programate pentru un utilizator, ordonate după dată
     */
    List<ScheduledWorkout> findByUserUserIdOrderByScheduledDateDesc(Long userId);

    /**
     * Găsește workout-urile programate pentru un utilizator într-o perioadă specifică
     */
    List<ScheduledWorkout> findByUserUserIdAndScheduledDateBetweenOrderByScheduledDate(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Găsește workout-urile programate pentru un utilizator cu un anumit status
     */
    List<ScheduledWorkout> findByUserUserIdAndStatus(Long userId, WorkoutStatusType status);

    /**
     * Găsește workout-urile programate pentru astăzi pentru un utilizator
     */
    @Query("SELECT sw FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.scheduledDate = CURRENT_DATE")
    List<ScheduledWorkout> findTodaysWorkoutsForUser(@Param("userId") Long userId);

    /**
     * Găsește workout-urile care nu au fost completate în trecut
     */
    @Query("SELECT COUNT(sw) FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.status = com.marecca.workoutTracker.entity.enums.WorkoutStatusType.COMPLETED")

    List<ScheduledWorkout> findMissedWorkoutsForUser(@Param("userId") Long userId);

    /**
     * Găsește workout-urile pentru un anumit plan
     */
    List<ScheduledWorkout> findByWorkoutPlanWorkoutPlanId(Long workoutPlanId);

    /**
     * Calculează statistici despre workout-uri pentru un utilizator
     */
    @Query("SELECT COUNT(sw) FROM ScheduledWorkout sw WHERE sw.user.userId = :userId AND sw.status = 'COMPLETED'")
    Long countCompletedWorkoutsForUser(@Param("userId") Long userId);

    @Query("SELECT AVG(sw.actualDurationMinutes) FROM ScheduledWorkout sw " +
            "WHERE sw.user.userId = :userId AND sw.status = 'COMPLETED'")
    Double getAverageWorkoutDurationForUser(@Param("userId") Long userId);

    /**
     * Găsește workout-urile ordonate după rating
     */
    List<ScheduledWorkout> findByUserUserIdAndStatusOrderByOverallRatingDesc(
            Long userId, WorkoutStatusType status);

    /**
     * Actualizează statusul unui workout programat
     */
    @Modifying
    @Query("UPDATE ScheduledWorkout sw SET sw.status = :status WHERE sw.scheduledWorkoutId = :workoutId")
    void updateWorkoutStatus(
            @Param("workoutId") Long workoutId,
            @Param("status") WorkoutStatusType status);

    /**
     * Înregistrează începerea unui workout
     */
    @Modifying
    @Query("UPDATE ScheduledWorkout sw SET sw.status = 'IN_PROGRESS', sw.actualStartTime = :startTime " +
            "WHERE sw.scheduledWorkoutId = :workoutId")
    void startWorkout(
            @Param("workoutId") Long workoutId,
            @Param("startTime") LocalDateTime startTime);

    /**
     * Înregistrează finalizarea unui workout
     */
    @Modifying
    @Query("UPDATE ScheduledWorkout sw SET sw.status = 'COMPLETED', sw.actualEndTime = :endTime, " +
            "sw.caloriesBurned = :caloriesBurned, sw.overallRating = :rating " +
            "WHERE sw.scheduledWorkoutId = :workoutId")
    void completeWorkout(
            @Param("workoutId") Long workoutId,
            @Param("endTime") LocalDateTime endTime,
            @Param("caloriesBurned") Integer caloriesBurned,
            @Param("rating") Integer rating);

    /**
     * Găsește ultimele workout-uri completate
     */
    List<ScheduledWorkout> findTop5ByUserUserIdAndStatusOrderByActualEndTimeDesc(
            Long userId, WorkoutStatusType status);
}