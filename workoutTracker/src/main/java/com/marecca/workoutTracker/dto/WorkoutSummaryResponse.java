package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;

@lombok.Data
@lombok.Builder
public  class WorkoutSummaryResponse {
    private Long workoutId;
    private WorkoutStatusType status;
    private Integer totalExercises;
    private Integer totalSets;
    private Integer estimatedCalories;
    private Integer elapsedMinutes;
}