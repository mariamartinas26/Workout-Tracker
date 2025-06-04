package com.marecca.workoutTracker.repository;


import com.marecca.workoutTracker.entity.WorkoutPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    @Query("SELECT wp FROM WorkoutPlan wp WHERE wp.user.userId = :userId AND wp.planName = :planName")
    Optional<WorkoutPlan> findByUserAndPlanName(@Param("userId") Long userId, @Param("planName") String planName);

    List<WorkoutPlan> findByUserUserId(Long userId);

    boolean existsByUserUserIdAndPlanName(Long userId, String planName);
}
