package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pentru entitatea User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Găsește un utilizator după numele de utilizator
     */
    Optional<User> findByUsername(String username);

    /**
     * Găsește un utilizator după adresa de email
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifică dacă există un utilizator cu numele dat
     */
    boolean existsByUsername(String username);

    /**
     * Verifică dacă există un utilizator cu emailul dat
     */
    boolean existsByEmail(String email);

    /**
     * Găsește toți utilizatorii activi
     */
    List<User> findByIsActiveTrue();

    /**
     * Găsește utilizatori după nivelul de fitness
     */
    List<User> findByFitnessLevel(String fitnessLevel);

    /**
     * Dezactivează un utilizator
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.userId = :userId")
    void deactivateUser(@Param("userId") Long userId);

    /**
     * Găsește utilizatori care au creat un număr minim de planuri de workout
     */
    @Query("SELECT u FROM User u WHERE (SELECT COUNT(wp) FROM WorkoutPlan wp WHERE wp.user = u) >= :minPlans")
    List<User> findUsersWithAtLeastMinPlans(@Param("minPlans") Long minPlans);

    /**
     * Găsește utilizatori care au terminat un număr minim de workout-uri
     */
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