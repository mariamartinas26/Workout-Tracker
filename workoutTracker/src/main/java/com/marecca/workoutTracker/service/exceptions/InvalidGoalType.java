package com.marecca.workoutTracker.service.exceptions;

public class InvalidGoalTypeException extends RuntimeException {
    public InvalidGoalTypeException(String message) {
        super(message);
    }
}