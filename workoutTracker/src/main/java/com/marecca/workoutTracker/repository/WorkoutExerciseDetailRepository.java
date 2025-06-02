package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.WorkoutExerciseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;


@Repository
public interface WorkoutExerciseDetailRepository extends JpaRepository<WorkoutExerciseDetail, Long> {

    @Modifying
    @Query("DELETE FROM WorkoutExerciseDetail wed WHERE wed.workoutPlan.workoutPlanId = :workoutPlanId")
    void deleteByWorkoutPlanId(@Param("workoutPlanId") Long workoutPlanId);
}