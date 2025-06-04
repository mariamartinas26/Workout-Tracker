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

    //find all goals for a user by id
    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId ORDER BY g.createdAt DESC")
    List<Goal> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    //find active goals for a user
    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId AND g.status = 'ACTIVE' ORDER BY g.createdAt DESC")
    List<Goal> findActiveGoalsByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM Goal g WHERE g.user.userId = :userId " +
            "AND g.status = 'COMPLETED' " +
            "AND g.completedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY g.completedAt DESC")
    List<Goal> findCompletedGoalsInDateRange(@Param("userId") Long userId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

}