package com.marecca.workoutTracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutCalendarDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workoutDate;
    private Integer workoutCount;
    private Integer totalCalories;
    private Integer totalDuration;
    private BigDecimal avgRating;
    private Integer intensityLevel; // 0-4 for heatmap coloring
}
