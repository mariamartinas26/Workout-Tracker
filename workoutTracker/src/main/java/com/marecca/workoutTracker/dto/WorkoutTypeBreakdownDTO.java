package com.marecca.workoutTracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutTypeBreakdownDTO {
    private String category;
    private Integer workoutCount;
    private Integer totalDuration;
    private Integer totalCalories;
    private BigDecimal avgRating;
    private BigDecimal percentage;
}
