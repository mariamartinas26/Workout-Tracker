package com.marecca.workoutTracker.dto;

public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;

    // Constructors
    public ChangePasswordRequest() {}

    // Getters și setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}