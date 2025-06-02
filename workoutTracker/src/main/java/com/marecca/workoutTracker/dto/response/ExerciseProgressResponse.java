package com.marecca.workoutTracker.dto.response;

import com.marecca.workoutTracker.entity.WorkoutExerciseLog;

import java.math.BigDecimal;
import java.util.List;

@lombok.Data
@lombok.Builder
public  class ExerciseProgressResponse {
    private List<WorkoutExerciseLog> progressLogs;
    private BigDecimal personalBestWeight;
    private Integer personalBestReps;
    private Double progressPercentage;
    private Integer totalSessions;
}