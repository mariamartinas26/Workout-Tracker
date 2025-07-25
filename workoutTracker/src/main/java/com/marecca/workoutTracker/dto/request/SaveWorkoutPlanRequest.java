package com.marecca.workoutTracker.dto.request;

import com.marecca.workoutTracker.dto.WorkoutRecommendationDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class SaveWorkoutPlanRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    private Long goalId;

    private String planName;

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    @NotEmpty(message = "Recommendations list cannot be empty")
    private List<WorkoutRecommendationDTO> recommendations;

    public SaveWorkoutPlanRequest() {}

    public SaveWorkoutPlanRequest(Long userId, Long goalId, List<WorkoutRecommendationDTO> recommendations) {
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

    public List<WorkoutRecommendationDTO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<WorkoutRecommendationDTO> recommendations) {
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