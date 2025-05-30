package com.marecca.workoutTracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class SaveWorkoutPlanRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    private Long goalId; // Optional - poate fi null

    @NotEmpty(message = "Recommendations list cannot be empty")
    private List<WorkoutRecommendation> recommendations;

    // Constructors
    public SaveWorkoutPlanRequest() {}

    public SaveWorkoutPlanRequest(Long userId, Long goalId, List<WorkoutRecommendation> recommendations) {
        this.userId = userId;
        this.goalId = goalId;
        this.recommendations = recommendations;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public List<WorkoutRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<WorkoutRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    @Override
    public String toString() {
        return "SaveWorkoutPlanRequest{" +
                "userId=" + userId +
                ", goalId=" + goalId +
                ", recommendationsCount=" + (recommendations != null ? recommendations.size() : 0) +
                '}';
    }
}