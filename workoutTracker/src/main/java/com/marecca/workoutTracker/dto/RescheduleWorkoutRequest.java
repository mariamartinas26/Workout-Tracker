package com.marecca.workoutTracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public  class RescheduleWorkoutRequest {
    @NotNull(message = "Scheduled date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime scheduledTime;
}