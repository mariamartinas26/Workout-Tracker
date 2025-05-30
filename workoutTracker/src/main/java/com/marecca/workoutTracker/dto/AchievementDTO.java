package com.marecca.workoutTracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {
    private String achievementType;
    private String achievementTitle;
    private String achievementDescription;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate achievedDate;
    private Integer metricValue;
}
