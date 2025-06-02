package com.marecca.workoutTracker.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public class WorkoutRecommendationRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotNull(message = "Goal type is required")
    @Pattern(regexp = "WEIGHT_LOSS|MUSCLE_GAIN|MAINTENANCE",
            message = "Goal type must be WEIGHT_LOSS, MUSCLE_GAIN, or MAINTENANCE")
    private String goalType;

    public WorkoutRecommendationRequest() {}

    public WorkoutRecommendationRequest(Long userId, String goalType) {
        this.userId = userId;
        this.goalType = goalType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGoalType() {
        return goalType;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    @Override
    public String toString() {
        return "WorkoutRecommendationRequest{" +
                "userId=" + userId +
                ", goalType='" + goalType + '\'' +
                '}';
    }
}