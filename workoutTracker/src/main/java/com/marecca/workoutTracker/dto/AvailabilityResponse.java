package com.marecca.workoutTracker.dto;

import java.time.LocalDate;
import java.time.LocalTime;

@lombok.Data
@lombok.Builder
public  class AvailabilityResponse {
    private boolean available;
    private LocalDate date;
    private LocalTime time;
    private String message;
}