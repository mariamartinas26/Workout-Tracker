package com.marecca.workoutTracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    // Weekly Stats
    private Integer weeklyWorkouts;
    private Integer weeklyCalories;
    private BigDecimal weeklyAvgDuration;
    private BigDecimal weeklyAvgRating;
    private Integer weeklyWorkoutDays;

    // Monthly Stats
    private Integer monthlyWorkouts;
    private Integer monthlyCalories;
    private BigDecimal monthlyAvgDuration;
    private BigDecimal monthlyAvgRating;
    private Integer monthlyWorkoutDays;

    // Streak Info
    private Integer currentStreak;
    private Integer longestStreak;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastWorkoutDate;

    // Lifetime Stats
    private Long totalWorkouts;
    private Long totalCalories;
    private Long totalWorkoutDays;
    private BigDecimal lifetimeAvgDuration;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate firstWorkoutDate;
}

