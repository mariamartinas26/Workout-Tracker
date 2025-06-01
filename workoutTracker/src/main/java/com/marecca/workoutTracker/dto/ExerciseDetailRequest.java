package com.marecca.workoutTracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public  class ExerciseDetailRequest {
    @NotNull(message = "Exercise ID is required")
    private Long exerciseId;

    private Integer exerciseOrder;

    @NotNull(message = "Number of sets is required")
    @Min(value = 1, message = "Number of sets must be positive")
    private Integer targetSets;

    @Min(value = 1, message = "Minimum reps must be positive")
    private Integer targetRepsMin;

    private Integer targetRepsMax;
    private BigDecimal targetWeightKg;
    private Integer targetDurationSeconds;
    private BigDecimal targetDistanceMeters;

    @Min(value = 0, message = "Rest time cannot be negative")
    private Integer restTimeSeconds = 60;

    private String notes;
}