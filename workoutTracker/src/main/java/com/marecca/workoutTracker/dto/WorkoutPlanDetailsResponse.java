package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.WorkoutPlan;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public  class WorkoutPlanDetailsResponse {
    private WorkoutPlan workoutPlan;
    private Integer totalExercises;
}