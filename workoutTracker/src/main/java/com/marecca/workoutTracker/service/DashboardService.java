package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.dto.*;
import com.marecca.workoutTracker.entity.UserWorkoutStreak;
import com.marecca.workoutTracker.repository.ScheduledWorkoutRepository;
import com.marecca.workoutTracker.repository.UserWorkoutStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ScheduledWorkoutRepository scheduledWorkoutRepository;
    private final UserWorkoutStreakRepository userWorkoutStreakRepository;

    public DashboardSummaryDTO getDashboardSummary(Long userId) {
        return getDashboardSummary(userId, LocalDate.now());
    }

    /**
     * Get dashboard summary for a specific date
     */
    public DashboardSummaryDTO getDashboardSummary(Long userId, LocalDate currentDate) {
        try {
            log.info("Getting dashboard summary for user {} on date {}", userId, currentDate);

            // Calculate week boundaries (Monday to Sunday)
            LocalDate weekStart, weekEnd;
            if (currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                weekStart = currentDate.minusDays(6);
                weekEnd = currentDate;
            } else {
                weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                weekEnd = weekStart.plusDays(6);
            }

            // Calculate month boundaries
            LocalDate monthStart = currentDate.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate monthEnd = currentDate.with(TemporalAdjusters.lastDayOfMonth());

            // Get weekly stats
            List<Object[]> weeklyResult = scheduledWorkoutRepository.getWorkoutStatsForPeriod(userId, weekStart, weekEnd);
            Object[] weeklyStats = !weeklyResult.isEmpty() ? weeklyResult.get(0) : new Object[5];

            // Get monthly stats
            List<Object[]> monthlyResult = scheduledWorkoutRepository.getWorkoutStatsForPeriod(userId, monthStart, monthEnd);
            Object[] monthlyStats = !monthlyResult.isEmpty() ? monthlyResult.get(0) : new Object[5];

            // Get streak info
            Optional<UserWorkoutStreak> streakInfo = userWorkoutStreakRepository.findByUserId(userId);

            // Get lifetime stats
            List<Object[]> lifetimeResult = scheduledWorkoutRepository.getLifetimeWorkoutStats(userId);
            Object[] lifetimeStats = !lifetimeResult.isEmpty() ? lifetimeResult.get(0) : new Object[5];

            return DashboardSummaryDTO.builder()
                    // Weekly stats
                    .weeklyWorkouts(safeCastToInteger(weeklyStats[0]))
                    .weeklyCalories(safeCastToInteger(weeklyStats[1]))
                    .weeklyAvgDuration(roundToBigDecimal(weeklyStats[2], 1))
                    .weeklyAvgRating(roundToBigDecimal(weeklyStats[3], 1))
                    .weeklyWorkoutDays(safeCastToInteger(weeklyStats[4]))

                    // Monthly stats
                    .monthlyWorkouts(safeCastToInteger(monthlyStats[0]))
                    .monthlyCalories(safeCastToInteger(monthlyStats[1]))
                    .monthlyAvgDuration(roundToBigDecimal(monthlyStats[2], 1))
                    .monthlyAvgRating(roundToBigDecimal(monthlyStats[3], 1))
                    .monthlyWorkoutDays(safeCastToInteger(monthlyStats[4]))

                    // Streak info
                    .currentStreak(streakInfo.map(UserWorkoutStreak::getCurrentStreak).orElse(0))
                    .longestStreak(streakInfo.map(UserWorkoutStreak::getLongestStreak).orElse(0))
                    .lastWorkoutDate(streakInfo.map(UserWorkoutStreak::getLastWorkoutDate).orElse(null))

                    // Lifetime stats
                    .totalWorkouts(safeCastToLong(lifetimeStats[0]))
                    .totalCalories(safeCastToLong(lifetimeStats[1]))
                    .totalWorkoutDays(safeCastToLong(lifetimeStats[2]))
                    .lifetimeAvgDuration(roundToBigDecimal(lifetimeStats[3], 1))
                    .firstWorkoutDate(safeCastToLocalDate(lifetimeStats[4]))
                    .build();

        } catch (Exception e) {
            log.error("Error getting dashboard summary for user {}: {}", userId, e.getMessage(), e);
            return createEmptyDashboard();
        }
    }

    /**
     * method for getting workout calendar
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
            log.error("Error getting workout calendar for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * method for getting workout trends
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
            log.error("Error getting workout trends for user {}: {}", userId, e.getMessage());
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
            log.error("Error getting workout type breakdown for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private BigDecimal roundToBigDecimal(Object value, int scale) {
        if (value == null) return BigDecimal.ZERO;

        BigDecimal bd = safeCastToBigDecimal(value);
        if (bd.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;

        return bd.setScale(scale, RoundingMode.HALF_UP);
    }

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
}