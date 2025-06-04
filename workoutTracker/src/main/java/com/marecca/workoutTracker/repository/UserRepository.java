package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsById(Long userId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    List<User> findByIsActiveTrue();
    List<User> findByFitnessLevel(String fitnessLevel);

    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.userId = :userId")
    void deactivateUser(@Param("userId") Long userId);


    @Query("SELECT u FROM User u WHERE (SELECT COUNT(wp) FROM WorkoutPlan wp WHERE wp.user = u) >= :minPlans")
    List<User> findUsersWithAtLeastMinPlans(@Param("minPlans") Long minPlans);


    @Query(value = "SELECT u.* FROM users u " +
            "JOIN (SELECT user_id, COUNT(*) as completed_count " +
            "FROM scheduled_workouts " +
            "WHERE status = 'COMPLETED' " +
            "GROUP BY user_id " +
            "HAVING COUNT(*) >= :minWorkouts) as sw " +
            "ON u.user_id = sw.user_id",
            nativeQuery = true)
    List<User> findUsersWithMinimumCompletedWorkouts(@Param("minWorkouts") Integer minWorkouts);
}