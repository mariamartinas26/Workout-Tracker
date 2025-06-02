package com.marecca.workoutTracker.dto.response;

@lombok.Data
@lombok.Builder
public  class SessionExerciseLogResponse {
    private Long logId;
    private Integer exerciseOrder;
    private Integer totalExercisesLogged;
    private String message;
    private String nextAction;
}