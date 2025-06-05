package com.marecca.workoutTracker.service.exceptions;


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