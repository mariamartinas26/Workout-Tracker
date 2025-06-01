package com.marecca.workoutTracker.service.exceptions;

/**
 * Exception thrown when a workout plan is not found in the database
 */
public class WorkoutPlanNotFoundException extends RuntimeException {

  public WorkoutPlanNotFoundException(String message) {
    super(message);
  }

  public WorkoutPlanNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public WorkoutPlanNotFoundException(Long workoutPlanId) {
    super("Workout plan with ID " + workoutPlanId + " not found");
  }
}