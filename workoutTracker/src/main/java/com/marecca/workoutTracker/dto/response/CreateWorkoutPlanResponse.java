package com.marecca.workoutTracker.dto.response;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public  class CreateWorkoutPlanResponse {
    private Long workoutPlanId;
    private String planName;
    private Integer totalExercises;
    private String message;
}