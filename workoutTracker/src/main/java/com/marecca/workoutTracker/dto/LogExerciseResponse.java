package com.marecca.workoutTracker.dto;


@lombok.Data
@lombok.Builder
public  class LogExerciseResponse {
    private Long logId;
    private String message;
}