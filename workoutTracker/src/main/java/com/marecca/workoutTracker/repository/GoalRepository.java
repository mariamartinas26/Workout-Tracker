package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.Goal;
import com.marecca.workoutTracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    // Find all goals for a specific user
    List<Goal> findByUserOrderByCreatedAtDesc(User user);

    // Find all goals for a user by user ID
    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId ORDER BY g.createdAt DESC")
    List<Goal> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Find active goals for a user
    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId AND g.status = 'ACTIVE' ORDER BY g.createdAt DESC")
    List<Goal> findActiveGoalsByUserId(@Param("userId") Long userId);

    // Find the most recent active goal for a user
    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId AND g.status = 'ACTIVE' ORDER BY g.createdAt DESC")
    Optional<Goal> findMostRecentActiveGoalByUserId(@Param("userId") Long userId);

    // Find goals by type for a user
    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId AND g.goalType = :goalType ORDER BY g.createdAt DESC")
    List<Goal> findByUserIdAndGoalType(@Param("userId") Long userId, @Param("goalType") Goal.GoalType goalType);

    // Count active goals for a user
    @Query("SELECT COUNT(g) FROM Goal g WHERE g.user.userId = :userId AND g.status = 'ACTIVE'")
    Long countActiveGoalsByUserId(@Param("userId") Long userId);

    // Find completed goals for a user
    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId AND g.status = 'COMPLETED' ORDER BY g.completedAt DESC")
    List<Goal> findCompletedGoalsByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId AND g.status = :status AND g.completedAt > :completedAfter ORDER BY g.completedAt DESC")
    List<Goal> findByUserIdAndStatusAndCompletedAtAfter(@Param("userId") Long userId,
                                                        @Param("status") Goal.GoalStatus status,
                                                        @Param("completedAfter") LocalDateTime completedAfter);
}