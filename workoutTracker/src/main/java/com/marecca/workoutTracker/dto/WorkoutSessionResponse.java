package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;

import java.time.LocalDateTime;

@lombok.Data
@lombok.Builder
public  class WorkoutSessionResponse {
    private Long workoutId;
    private WorkoutStatusType status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;
    private String nextAction;
}