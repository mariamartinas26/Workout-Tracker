package com.marecca.workoutTracker.service;
import com.marecca.workoutTracker.dto.*;
import com.marecca.workoutTracker.repository.ScheduledWorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ScheduledWorkoutRepository scheduledWorkoutRepository;

    /**
     * method for getting workout calendar
     * @param userId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<WorkoutCalendarDTO> getWorkoutCalendar(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<Object[]> result = scheduledWorkoutRepository.getWorkoutCalendar(userId, startDate, endDate);
            List<WorkoutCalendarDTO> calendar = new ArrayList<>();

            for (Object[] row : result) {
                calendar.add(WorkoutCalendarDTO.builder()
                        .workoutDate(safeCastToLocalDate(row[0]))
                        .workoutCount(safeCastToInteger(row[1]))
                        .totalCalories(safeCastToInteger(row[2]))
                        .totalDuration(safeCastToInteger(row[3]))
                        .avgRating(safeCastToBigDecimal(row[4]))
                        .intensityLevel(safeCastToInteger(row[5]))
                        .build());
            }

            return calendar;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * method for getting workout trends
     * @param userId
     * @param periodType
     * @param startDate
     * @param endDate
     * @return
     */
    public List<WorkoutTrendDTO> getWorkoutTrends(Long userId, String periodType, LocalDate startDate, LocalDate endDate) {
        try {
            List<Object[]> result = scheduledWorkoutRepository.getWorkoutTrends(userId, periodType, startDate, endDate);
            List<WorkoutTrendDTO> trends = new ArrayList<>();

            for (Object[] row : result) {
                trends.add(WorkoutTrendDTO.builder()
                        .periodDate(safeCastToLocalDate(row[0]))
                        .periodLabel(safeCastToString(row[1]))
                        .workoutCount(safeCastToInteger(row[2]))
                        .totalCalories(safeCastToInteger(row[3]))
                        .avgDuration(safeCastToBigDecimal(row[4]))
                        .avgRating(safeCastToBigDecimal(row[5]))
                        .build());
            }

            return trends;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<WorkoutTypeBreakdownDTO> getWorkoutTypeBreakdown(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<Object[]> result = scheduledWorkoutRepository.getWorkoutTypeBreakdown(userId, startDate, endDate);
            List<WorkoutTypeBreakdownDTO> breakdown = new ArrayList<>();

            for (Object[] row : result) {
                breakdown.add(WorkoutTypeBreakdownDTO.builder()
                        .category(safeCastToString(row[0]))
                        .workoutCount(safeCastToInteger(row[1]))
                        .totalDuration(safeCastToInteger(row[2]))
                        .totalCalories(safeCastToInteger(row[3]))
                        .avgRating(safeCastToBigDecimal(row[4]))
                        .percentage(safeCastToBigDecimal(row[5]))
                        .build());
            }

            return breakdown;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    //method to create empty dashboard
    private DashboardSummaryDTO createEmptyDashboard() {
        return DashboardSummaryDTO.builder()
                .weeklyWorkouts(0)
                .weeklyCalories(0)
                .weeklyAvgDuration(BigDecimal.ZERO)
                .weeklyAvgRating(BigDecimal.ZERO)
                .weeklyWorkoutDays(0)
                .monthlyWorkouts(0)
                .monthlyCalories(0)
                .monthlyAvgDuration(BigDecimal.ZERO)
                .monthlyAvgRating(BigDecimal.ZERO)
                .monthlyWorkoutDays(0)
                .currentStreak(0)
                .longestStreak(0)
                .lastWorkoutDate(null)
                .totalWorkouts(0L)
                .totalCalories(0L)
                .totalWorkoutDays(0L)
                .lifetimeAvgDuration(BigDecimal.ZERO)
                .firstWorkoutDate(null)
                .build();
    }

    private Integer safeCastToInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    private Long safeCastToLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private BigDecimal safeCastToBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }

    private String safeCastToString(Object value) {
        return value != null ? value.toString() : "";
    }

    private LocalDate safeCastToLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof Date) return ((Date) value).toLocalDate();
        if (value instanceof java.util.Date) return new Date(((java.util.Date) value).getTime()).toLocalDate();
        return null;
    }

    public DashboardSummaryDTO getDashboardSummary(Long userId) {
        try {
            List<Object[]> result = scheduledWorkoutRepository.getDashboardSummary(userId, LocalDate.now());

            if (!result.isEmpty()) {
                Object[] row = result.get(0);
                return DashboardSummaryDTO.builder()
                        .weeklyWorkouts(safeCastToInteger(row[0]))
                        .weeklyCalories(safeCastToInteger(row[1]))
                        .weeklyAvgDuration(safeCastToBigDecimal(row[2]))
                        .weeklyAvgRating(safeCastToBigDecimal(row[3]))
                        .weeklyWorkoutDays(safeCastToInteger(row[4]))
                        .monthlyWorkouts(safeCastToInteger(row[5]))
                        .monthlyCalories(safeCastToInteger(row[6]))
                        .monthlyAvgDuration(safeCastToBigDecimal(row[7]))
                        .monthlyAvgRating(safeCastToBigDecimal(row[8]))
                        .monthlyWorkoutDays(safeCastToInteger(row[9]))
                        .currentStreak(safeCastToInteger(row[10]))
                        .longestStreak(safeCastToInteger(row[11]))
                        .lastWorkoutDate(safeCastToLocalDate(row[12]))
                        .totalWorkouts(safeCastToLong(row[13]))
                        .totalCalories(safeCastToLong(row[14]))
                        .totalWorkoutDays(safeCastToLong(row[15]))
                        .lifetimeAvgDuration(safeCastToBigDecimal(row[16]))
                        .firstWorkoutDate(safeCastToLocalDate(row[17]))
                        .build();
            }

            // Return empty dashboard
            return createEmptyDashboard();
        } catch (Exception e) {
            return createEmptyDashboard();
        }
    }
}