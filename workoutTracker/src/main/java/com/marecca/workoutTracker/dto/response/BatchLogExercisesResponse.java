package com.marecca.workoutTracker.dto.response;

import java.util.List;

@lombok.Data
@lombok.Builder
public  class BatchLogExercisesResponse {
    private Integer loggedCount;
    private List<Long> logIds;
    private String message;
}