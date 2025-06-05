package com.marecca.workoutTracker.dto.request;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}