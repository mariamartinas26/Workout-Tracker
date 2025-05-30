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
public class WorkoutTrendDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodDate;
    private String periodLabel;
    private Integer workoutCount;
    private Integer totalCalories;
    private BigDecimal avgDuration;
    private BigDecimal avgRating;
}
