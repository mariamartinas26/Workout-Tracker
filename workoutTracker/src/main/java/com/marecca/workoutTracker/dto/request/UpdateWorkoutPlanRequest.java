package com.marecca.workoutTracker.dto.request;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public  class UpdateWorkoutPlanRequest {
    private String planName;
    private String description;
    private Integer estimatedDurationMinutes;
    private Integer difficultyLevel;
    private String goals;
    private String notes;
}