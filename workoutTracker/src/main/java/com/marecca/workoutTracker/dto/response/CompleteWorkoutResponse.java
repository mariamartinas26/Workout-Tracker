package com.marecca.workoutTracker.dto.response;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;

import java.time.LocalDateTime;

@lombok.Data
@lombok.Builder
public  class CompleteWorkoutResponse {
    private Long workoutId;
    private WorkoutStatusType status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer totalExercises;
    private Integer totalCaloriesBurned;
    private Integer overallRating;
    private String message;
}