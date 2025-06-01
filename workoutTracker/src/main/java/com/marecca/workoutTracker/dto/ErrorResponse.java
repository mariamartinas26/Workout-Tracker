package com.marecca.workoutTracker.dto;

@lombok.Data
@lombok.Builder
public  class ErrorResponse {
    private String error;
    private String message;
}