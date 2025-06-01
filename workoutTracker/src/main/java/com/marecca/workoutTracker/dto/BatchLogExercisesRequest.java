package com.marecca.workoutTracker.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

@lombok.Data
@lombok.Builder
public  class BatchLogExercisesRequest {
    @NotNull(message = "Exercise list is required")
    private List<LogExerciseRequest> exercises;
}
