package com.marecca.workoutTracker.dto;

import java.math.BigDecimal;

public class CreateGoalRequest {
    private Long userId;
    private String goalType;
    private BigDecimal targetWeightLoss;
    private BigDecimal targetWeightGain;
    private BigDecimal currentWeight;
    private Integer timeframe; // months
    private String notes;

    // Getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }
    public BigDecimal getTargetWeightLoss() { return targetWeightLoss; }
    public void setTargetWeightLoss(BigDecimal targetWeightLoss) { this.targetWeightLoss = targetWeightLoss; }
    public BigDecimal getTargetWeightGain() { return targetWeightGain; }
    public void setTargetWeightGain(BigDecimal targetWeightGain) { this.targetWeightGain = targetWeightGain; }
    public BigDecimal getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(BigDecimal currentWeight) { this.currentWeight = currentWeight; }
    public Integer getTimeframe() { return timeframe; }
    public void setTimeframe(Integer timeframe) { this.timeframe = timeframe; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
