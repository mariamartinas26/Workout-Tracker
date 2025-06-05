package com.marecca.workoutTracker.service.exceptions;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;


public class InvalidWorkoutStatusException extends RuntimeException {

    public InvalidWorkoutStatusException(String message) {
        super(message);
    }

    public InvalidWorkoutStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidWorkoutStatusException(WorkoutStatusType currentStatus, String operation) {
        super(String.format("Cannot %s workout with status %s", operation, currentStatus));
    }

    public InvalidWorkoutStatusException(WorkoutStatusType currentStatus, WorkoutStatusType requiredStatus, String operation) {
        super(String.format("Cannot %s workout. Current status: %s, Required status: %s",
                operation, currentStatus, requiredStatus));
    }
}