package com.marecca.workoutTracker.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CompleteProfileRequest {
    private Long userId;
    private LocalDate dateOfBirth;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String fitnessLevel;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Integer getHeightCm() { return heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }
}