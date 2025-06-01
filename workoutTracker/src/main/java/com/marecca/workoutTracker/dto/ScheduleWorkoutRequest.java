package com.marecca.workoutTracker.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@lombok.Data
@lombok.Builder
public  class ScheduleWorkoutRequest {
    @NotNull(message = "You need the id of the user")
    private Long userId;

    @NotNull(message = "You need the id of the plan")
    private Long workoutPlanId;

    @NotNull(message = "You need the scheduling date")
    private LocalDate scheduledDate;

    private LocalTime scheduledTime;
}