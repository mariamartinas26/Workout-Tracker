package com.marecca.workoutTracker.service.exceptions;

import java.time.LocalDate;
import java.time.LocalTime;

public class WorkoutAlreadyScheduledException extends RuntimeException {

    public WorkoutAlreadyScheduledException(String message) {
        super(message);
    }

    public WorkoutAlreadyScheduledException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkoutAlreadyScheduledException(Long userId, LocalDate date, LocalTime time) {
        super(String.format("User %d already has a workout scheduled on %s at %s",
                userId, date, time != null ? time : "unspecified time"));
    }

    public WorkoutAlreadyScheduledException(Long userId, LocalDate date) {
        super(String.format("User %d already has a workout scheduled on %s", userId, date));
    }
}