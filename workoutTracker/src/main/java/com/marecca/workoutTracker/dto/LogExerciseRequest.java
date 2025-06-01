package com.marecca.workoutTracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@lombok.Data
@lombok.Builder
public  class LogExerciseRequest {
    @NotNull(message = "Scheduled workout ID is required")
    private Long scheduledWorkoutId;

    @NotNull(message = "Exercise ID is required")
    private Long exerciseId;

    @NotNull(message = "Exercise order is required")
    @Positive(message = "Exercise order must be positive")
    private Integer exerciseOrder;

    @NotNull(message = "Number of completed sets is required")
    @Min(value = 0, message = "Number of sets cannot be negative")
    private Integer setsCompleted;

    @Min(value = 0, message = "Number of reps cannot be negative")
    private Integer repsCompleted;

    private BigDecimal weightUsedKg;

    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationSeconds;

    private BigDecimal distanceMeters;

    @Min(value = 0, message = "Calories cannot be negative")
    private Integer caloriesBurned;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @jakarta.validation.constraints.Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer difficultyRating;

    private String notes;
}