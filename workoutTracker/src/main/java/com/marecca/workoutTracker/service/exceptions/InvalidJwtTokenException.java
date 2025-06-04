package com.marecca.workoutTracker.service.exceptions;

public class InvalidJwtTokenException extends JwtTokenException {
    public InvalidJwtTokenException(String message) {
        super(message);
    }
}