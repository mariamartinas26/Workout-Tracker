package com.marecca.workoutTracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@lombok.Data
@lombok.Builder
public class SessionExerciseLogRequest {
    @NotNull(message = "Exercise ID is required")
    private Long exerciseId;

    @NotNull(message = "Exercise order is required")
    @Min(value = 1, message = "Order must be positive")
    private Integer exerciseOrder;

    @NotNull(message = "Number of sets is required")
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
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer difficultyRating;

    private String notes;
}