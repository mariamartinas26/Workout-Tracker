package com.marecca.workoutTracker.dto;

import jakarta.validation.constraints.NotNull;

@lombok.Data
@lombok.Builder
public  class StartFreeWorkoutRequest {
    @NotNull(message = "You need the id of the user")
    private Long userId;

    private String notes;
}
