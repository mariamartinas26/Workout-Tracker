package com.marecca.workoutTracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class WorkoutRecommendationDTO {

    @JsonProperty("exerciseId")
    private Long exerciseId;

    @JsonProperty("exerciseName")
    private String exerciseName;

    @JsonProperty("recommendedSets")
    private Integer recommendedSets;

    @JsonProperty("recommendedRepsMin")
    private Integer recommendedRepsMin;

    @JsonProperty("recommendedRepsMax")
    private Integer recommendedRepsMax;

    @JsonProperty("recommendedWeightPercentage")
    private BigDecimal recommendedWeightPercentage;

    @JsonProperty("restTimeSeconds")
    private Integer restTimeSeconds;

    @JsonProperty("priorityScore")
    private BigDecimal priorityScore;

    public WorkoutRecommendationDTO() {}

    public WorkoutRecommendationDTO(Long exerciseId, String exerciseName,
                                    Integer recommendedSets, Integer recommendedRepsMin,
                                    Integer recommendedRepsMax, BigDecimal recommendedWeightPercentage,
                                    Integer restTimeSeconds, BigDecimal priorityScore) {
                    this.exerciseId = exerciseId;
                    this.exerciseName = exerciseName;
                    this.recommendedSets = recommendedSets;
                    this.recommendedRepsMin = recommendedRepsMin;
                    this.recommendedRepsMax = recommendedRepsMax;
                    this.recommendedWeightPercentage = recommendedWeightPercentage;
                    this.restTimeSeconds = restTimeSeconds;
                    this.priorityScore = priorityScore;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public Integer getRecommendedSets() {
        return recommendedSets;
    }

    public void setRecommendedSets(Integer recommendedSets) {
        this.recommendedSets = recommendedSets;
    }

    public Integer getRecommendedRepsMin() {
        return recommendedRepsMin;
    }

    public void setRecommendedRepsMin(Integer recommendedRepsMin) {
        this.recommendedRepsMin = recommendedRepsMin;
    }

    public Integer getRecommendedRepsMax() {
        return recommendedRepsMax;
    }

    public void setRecommendedRepsMax(Integer recommendedRepsMax) {
        this.recommendedRepsMax = recommendedRepsMax;
    }

    public BigDecimal getRecommendedWeightPercentage() {
        return recommendedWeightPercentage;
    }

    public void setRecommendedWeightPercentage(BigDecimal recommendedWeightPercentage) {
        this.recommendedWeightPercentage = recommendedWeightPercentage;
    }

    public Integer getRestTimeSeconds() {
        return restTimeSeconds;
    }

    public void setRestTimeSeconds(Integer restTimeSeconds) {
        this.restTimeSeconds = restTimeSeconds;
    }

    public BigDecimal getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(BigDecimal priorityScore) {
        this.priorityScore = priorityScore;
    }

    @Override
    public String toString() {
        return "WorkoutRecommendation{" +
                "exerciseId=" + exerciseId +
                ", exerciseName='" + exerciseName + '\'' +
                ", recommendedSets=" + recommendedSets +
                ", recommendedRepsMin=" + recommendedRepsMin +
                ", recommendedRepsMax=" + recommendedRepsMax +
                ", recommendedWeightPercentage=" + recommendedWeightPercentage +
                ", restTimeSeconds=" + restTimeSeconds +
                ", priorityScore=" + priorityScore +
                '}';
    }
}