package com.marecca.workoutTracker.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@lombok.Data
@lombok.Builder
public  class CompleteWorkoutRequest {
    @Min(value = 0, message = "Calories cannot be negative")
    private Integer totalCaloriesBurned;

    @Min(value = 1, message = "Rating must be between 1-5")
    @Max(value = 5, message = "Rating must be between 1-5")
    private Integer overallRating;

    @Min(value = 1, message = "Energy level must be between 1-5")
    @Max(value = 5, message = "Energy level must be between 1-5")
    private Integer energyLevelAfter;

    private String notes;
}