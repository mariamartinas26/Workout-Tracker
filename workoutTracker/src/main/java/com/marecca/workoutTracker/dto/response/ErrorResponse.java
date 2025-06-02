package com.marecca.workoutTracker.dto.response;

@lombok.Data
@lombok.Builder
public  class ErrorResponse {
    private String error;
    private String message;
}