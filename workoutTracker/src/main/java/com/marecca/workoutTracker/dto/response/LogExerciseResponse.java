package com.marecca.workoutTracker.dto.response;


@lombok.Data
@lombok.Builder
public  class LogExerciseResponse {
    private Long logId;
    private String message;
}