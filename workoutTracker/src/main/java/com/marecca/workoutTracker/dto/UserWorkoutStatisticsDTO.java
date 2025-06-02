package com.marecca.workoutTracker.dto;

@lombok.Data
@lombok.Builder
public  class UserWorkoutStatisticsDTO {
    private Long totalCompletedWorkouts;
    private Double averageDurationMinutes;
    private Integer totalCaloriesBurned;
    private Long uniqueExercisesTrained;
}