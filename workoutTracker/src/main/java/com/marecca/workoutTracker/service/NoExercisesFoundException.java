package com.marecca.workoutTracker.service;

public class NoExercisesFoundException extends RuntimeException {
    public NoExercisesFoundException(String message) {
        super(message);
    }
}