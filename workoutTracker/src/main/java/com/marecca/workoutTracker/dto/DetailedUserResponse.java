package com.marecca.workoutTracker.dto;

import com.marecca.workoutTracker.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DetailedUserResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate dateOfBirth;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String fitnessLevel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer workoutPlansCount;
    private Integer scheduledWorkoutsCount;

    // Constructor din User entity
    public DetailedUserResponse(User user) {
        this.id = user.getUserId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.dateOfBirth = user.getDateOfBirth();
        this.heightCm = user.getHeightCm();
        this.weightKg = user.getWeightKg();
        this.fitnessLevel = user.getFitnessLevel();
        this.isActive = user.getIsActive();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.workoutPlansCount = user.getWorkoutPlans() != null ? user.getWorkoutPlans().size() : 0;
        this.scheduledWorkoutsCount = user.getScheduledWorkouts() != null ? user.getScheduledWorkouts().size() : 0;
    }

    // Getters și setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getWorkoutPlansCount() { return workoutPlansCount; }
    public void setWorkoutPlansCount(Integer workoutPlansCount) { this.workoutPlansCount = workoutPlansCount; }

    public Integer getScheduledWorkoutsCount() { return scheduledWorkoutsCount; }
    public void setScheduledWorkoutsCount(Integer scheduledWorkoutsCount) { this.scheduledWorkoutsCount = scheduledWorkoutsCount; }
}