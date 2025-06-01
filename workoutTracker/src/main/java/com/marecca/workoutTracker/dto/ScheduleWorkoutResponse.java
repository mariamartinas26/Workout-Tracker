package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;

@lombok.Data
@lombok.Builder
public  class ScheduleWorkoutResponse {
    private Long scheduledWorkoutId;
    private WorkoutStatusType status;
    private String message;
    private String nextAction;
}