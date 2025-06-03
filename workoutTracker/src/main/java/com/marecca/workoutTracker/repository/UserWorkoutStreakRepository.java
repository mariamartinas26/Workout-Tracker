package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.UserWorkoutStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWorkoutStreakRepository extends JpaRepository<UserWorkoutStreak, Long> {
    Optional<UserWorkoutStreak> findByUserId(Long userId);
}