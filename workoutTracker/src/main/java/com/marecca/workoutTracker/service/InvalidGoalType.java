package com.marecca.workoutTracker.service;

public class InvalidGoalTypeException extends RuntimeException {
    public InvalidGoalTypeException(String message) {
        super(message);
    }
}