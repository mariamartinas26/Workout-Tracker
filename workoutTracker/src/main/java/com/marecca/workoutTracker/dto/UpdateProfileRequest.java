package com.marecca.workoutTracker.dto;


import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateProfileRequest {
    private Long userId; // Opțional, pentru identificare
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate dateOfBirth;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String fitnessLevel;

    // Getters și setters pentru toate câmpurile
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public BigDecimal  getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal  weightKg) { this.weightKg = weightKg; }

    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }
}