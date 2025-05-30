package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.*;
import com.marecca.workoutTracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Dashboard controller
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Obține sumar complet dashboard pentru utilizator
     */
    @GetMapping("/summary/{userId}")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(@PathVariable Long userId) {
        try {
            DashboardSummaryDTO summary = dashboardService.getDashboardSummary(userId);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error getting dashboard summary for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DashboardSummaryDTO.builder().build());
        }
    }

    /**
     * Obține datele pentru calendar workout (heatmap)
     */
    @GetMapping("/calendar/{userId}")
    public ResponseEntity<List<WorkoutCalendarDTO>> getWorkoutCalendar(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Default to last 365 days if no dates provided
            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(365);
            }

            // Validate date range (max 2 years)
            if (ChronoUnit.DAYS.between(startDate, endDate) > 730) {
                return ResponseEntity.badRequest().build();
            }

            List<WorkoutCalendarDTO> calendar = dashboardService.getWorkoutCalendar(userId, startDate, endDate);
            return ResponseEntity.ok(calendar);

        } catch (Exception e) {
            log.error("Error getting workout calendar for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obține tendințe workout pentru grafice
     */
    @GetMapping("/trends/{userId}")
    public ResponseEntity<List<WorkoutTrendDTO>> getWorkoutTrends(
            @PathVariable Long userId,
            @RequestParam String period, // 'daily', 'weekly', 'monthly'
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Validate period type
            if (!period.matches("daily|weekly|monthly")) {
                return ResponseEntity.badRequest().build();
            }

            // Set default date ranges based on period
            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                switch (period) {
                    case "daily":
                        startDate = endDate.minusDays(30); // Last 30 days
                        break;
                    case "weekly":
                        startDate = endDate.minusWeeks(12); // Last 12 weeks
                        break;
                    case "monthly":
                        startDate = endDate.minusMonths(12); // Last 12 months
                        break;
                }
            }

            List<WorkoutTrendDTO> trends = dashboardService.getWorkoutTrends(userId, period, startDate, endDate);
            return ResponseEntity.ok(trends);

        } catch (Exception e) {
            log.error("Error getting workout trends for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obține breakdown tipuri de workout
     */
    @GetMapping("/workout-types/{userId}")
    public ResponseEntity<List<WorkoutTypeBreakdownDTO>> getWorkoutTypeBreakdown(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Default to last 90 days if no dates provided
            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(90);
            }

            List<WorkoutTypeBreakdownDTO> breakdown = dashboardService.getWorkoutTypeBreakdown(userId, startDate, endDate);
            return ResponseEntity.ok(breakdown);

        } catch (Exception e) {
            log.error("Error getting workout type breakdown for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obține realizări recente
     */
    @GetMapping("/achievements/{userId}")
    public ResponseEntity<List<AchievementDTO>> getRecentAchievements(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") Integer daysBack) {

        try {
            // Limit to reasonable range
            if (daysBack < 1 || daysBack > 365) {
                daysBack = 30;
            }

            List<AchievementDTO> achievements = dashboardService.getRecentAchievements(userId, daysBack);
            return ResponseEntity.ok(achievements);

        } catch (Exception e) {
            log.error("Error getting recent achievements for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint pentru statistici rapide (doar cardurile principale)
     */
    @GetMapping("/quick-stats/{userId}")
    public ResponseEntity<QuickStatsDTO> getQuickStats(@PathVariable Long userId) {
        try {
            DashboardSummaryDTO summary = dashboardService.getDashboardSummary(userId);

            QuickStatsDTO quickStats = QuickStatsDTO.builder()
                    .weeklyWorkouts(summary.getWeeklyWorkouts())
                    .weeklyCalories(summary.getWeeklyCalories())
                    .currentStreak(summary.getCurrentStreak())
                    .totalWorkouts(summary.getTotalWorkouts())
                    .build();

            return ResponseEntity.ok(quickStats);

        } catch (Exception e) {
            log.error("Error getting quick stats for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(QuickStatsDTO.builder().build());
        }
    }

    // DTO pentru statistici rapide
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QuickStatsDTO {
        private Integer weeklyWorkouts;
        private Integer weeklyCalories;
        private Integer currentStreak;
        private Long totalWorkouts;
    }
}