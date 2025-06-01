package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;

import java.time.LocalDateTime;

@lombok.Data
@lombok.Builder
public  class FreeWorkoutResponse {
    private Long workoutId;
    private WorkoutStatusType status;
    private LocalDateTime startTime;
    private String message;
    private String nextAction;
}
