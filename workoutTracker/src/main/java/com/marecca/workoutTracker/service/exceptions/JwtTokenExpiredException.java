package com.marecca.workoutTracker.service.exceptions;

public class JwtTokenExpiredException extends JwtTokenException {
    public JwtTokenExpiredException(String message) {
        super(message);
    }
}