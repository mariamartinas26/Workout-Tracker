package com.marecca.workoutTracker.dto;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class QuickStatsDTO {
    private Integer weeklyWorkouts;
    private Integer weeklyCalories;
    private Integer currentStreak;
    private Long totalWorkouts;
}