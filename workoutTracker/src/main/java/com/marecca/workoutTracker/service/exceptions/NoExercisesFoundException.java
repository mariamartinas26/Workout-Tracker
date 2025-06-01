package com.marecca.workoutTracker.service.exceptions;

public class NoExercisesFoundException extends RuntimeException {
    public NoExercisesFoundException(String message) {
        super(message);
    }
}