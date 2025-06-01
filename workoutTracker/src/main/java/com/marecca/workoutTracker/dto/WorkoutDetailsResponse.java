package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.entity.WorkoutExerciseLog;

import java.util.List;

@lombok.Data
@lombok.Builder
public  class WorkoutDetailsResponse {
    private ScheduledWorkout workout;
    private List<WorkoutExerciseLog> exerciseLogs;
    private Integer totalExercises;
}
