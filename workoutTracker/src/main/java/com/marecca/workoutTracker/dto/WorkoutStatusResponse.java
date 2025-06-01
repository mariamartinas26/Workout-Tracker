package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@lombok.Data
@lombok.Builder
public  class WorkoutStatusResponse {
    private Long workoutId;
    private WorkoutStatusType status;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer exercisesLogged;
    private Integer caloriesBurned;
    private Integer overallRating;
}
