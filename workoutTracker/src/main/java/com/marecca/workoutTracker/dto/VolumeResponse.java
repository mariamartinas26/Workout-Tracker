package com.marecca.workoutTracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

@lombok.Data
@lombok.Builder
public  class VolumeResponse {
    private BigDecimal totalVolume;
    private Long exerciseId;
    private LocalDate startDate;
    private LocalDate endDate;
}