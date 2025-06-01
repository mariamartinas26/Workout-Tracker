package com.marecca.workoutTracker.service.exceptions;

/**
 * Exception thrown when a scheduled workout is not found in the database
 */
public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(String message) {
        super(message);
    }

    public WorkoutNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkoutNotFoundException(Long scheduledWorkoutId) {
        super("Scheduled workout with ID " + scheduledWorkoutId + " not found");
    }
}