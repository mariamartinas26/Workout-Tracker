package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.controller.WorkoutPlanController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import com.marecca.workoutTracker.dto.*;
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public  class CreateWorkoutPlanRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Plan name is required")
    private String planName;

    private String description;

    @Min(value = 1, message = "Estimated duration must be positive")
    private Integer estimatedDurationMinutes;

    @Min(value = 1, message = "Difficulty level must be between 1 and 5")
    @Max(value = 5, message = "Difficulty level must be between 1 and 5")
    private Integer difficultyLevel;

    private String goals;
    private String notes;
    private List<ExerciseDetailRequest> exercises;
}