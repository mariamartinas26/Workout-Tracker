package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.*;
import com.marecca.workoutTracker.service.DashboardService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard controller for fitness tracking analytics - JWT Protected
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Get complete dashboard summary for authenticated user
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            DashboardSummaryDTO summary = dashboardService.getDashboardSummary(authenticatedUserId);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error getting dashboard summary: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get dashboard summary", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get dashboard summary for specific user (with validation)
     */
    @GetMapping("/summary/{userId}")
    public ResponseEntity<?> getDashboardSummaryForUser(@PathVariable Long userId, HttpServletRequest request) {
        // Check user access - returns error response if access denied
        ResponseEntity<?> accessCheck = jwtUtils.checkUserAccess(request, userId);
        if (accessCheck != null) return accessCheck;

        try {
            DashboardSummaryDTO summary = dashboardService.getDashboardSummary(userId);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error getting dashboard summary for user {}: {}", userId, e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get dashboard summary", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout calendar data for authenticated user
     */
    @GetMapping("/calendar")
    public ResponseEntity<?> getWorkoutCalendar(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(365);
            }

            if (ChronoUnit.DAYS.between(startDate, endDate) > 730) {
                return jwtUtils.createBadRequestResponse("Date range cannot exceed 730 days");
            }

            List<WorkoutCalendarDTO> calendar = dashboardService.getWorkoutCalendar(authenticatedUserId, startDate, endDate);
            return ResponseEntity.ok(calendar);

        } catch (Exception e) {
            log.error("Error getting workout calendar: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get workout calendar", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout calendar for specific user (with validation)
     */
    @GetMapping("/calendar/{userId}")
    public ResponseEntity<?> getWorkoutCalendarForUser(
            @PathVariable Long userId,
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Check user access
        ResponseEntity<?> accessCheck = jwtUtils.checkUserAccess(request, userId);
        if (accessCheck != null) return accessCheck;

        try {
            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(365);
            }

            if (ChronoUnit.DAYS.between(startDate, endDate) > 730) {
                return jwtUtils.createBadRequestResponse("Date range cannot exceed 730 days");
            }

            List<WorkoutCalendarDTO> calendar = dashboardService.getWorkoutCalendar(userId, startDate, endDate);
            return ResponseEntity.ok(calendar);

        } catch (Exception e) {
            log.error("Error getting workout calendar for user {}: {}", userId, e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get workout calendar", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout trends for authenticated user
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getWorkoutTrends(
            HttpServletRequest request,
            @RequestParam String period, // 'daily', 'weekly', 'monthly'
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            // Validate period type
            if (!period.matches("daily|weekly|monthly")) {
                return jwtUtils.createBadRequestResponse("Invalid period. Must be 'daily', 'weekly', or 'monthly'");
            }

            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                switch (period) {
                    case "daily":
                        startDate = endDate.minusDays(30);
                        break;
                    case "weekly":
                        startDate = endDate.minusWeeks(12);
                        break;
                    case "monthly":
                        startDate = endDate.minusMonths(12);
                        break;
                }
            }

            List<WorkoutTrendDTO> trends = dashboardService.getWorkoutTrends(authenticatedUserId, period, startDate, endDate);
            return ResponseEntity.ok(trends);

        } catch (Exception e) {
            log.error("Error getting workout trends: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get workout trends", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout trends for specific user (with validation)
     */
    @GetMapping("/trends/{userId}")
    public ResponseEntity<?> getWorkoutTrendsForUser(
            @PathVariable Long userId,
            HttpServletRequest request,
            @RequestParam String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Check user access
        ResponseEntity<?> accessCheck = jwtUtils.checkUserAccess(request, userId);
        if (accessCheck != null) return accessCheck;

        try {
            // Validate period type
            if (!period.matches("daily|weekly|monthly")) {
                return jwtUtils.createBadRequestResponse("Invalid period. Must be 'daily', 'weekly', or 'monthly'");
            }

            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                switch (period) {
                    case "daily":
                        startDate = endDate.minusDays(30);
                        break;
                    case "weekly":
                        startDate = endDate.minusWeeks(12);
                        break;
                    case "monthly":
                        startDate = endDate.minusMonths(12);
                        break;
                }
            }

            List<WorkoutTrendDTO> trends = dashboardService.getWorkoutTrends(userId, period, startDate, endDate);
            return ResponseEntity.ok(trends);

        } catch (Exception e) {
            log.error("Error getting workout trends for user {}: {}", userId, e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get workout trends", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout type breakdown for authenticated user
     */
    @GetMapping("/workout-types")
    public ResponseEntity<?> getWorkoutTypeBreakdown(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(90);
            }

            List<WorkoutTypeBreakdownDTO> breakdown = dashboardService.getWorkoutTypeBreakdown(authenticatedUserId, startDate, endDate);
            return ResponseEntity.ok(breakdown);

        } catch (Exception e) {
            log.error("Error getting workout type breakdown: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get workout type breakdown", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout type breakdown for specific user (with validation)
     */
    @GetMapping("/workout-types/{userId}")
    public ResponseEntity<?> getWorkoutTypeBreakdownForUser(
            @PathVariable Long userId,
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Check user access
        ResponseEntity<?> accessCheck = jwtUtils.checkUserAccess(request, userId);
        if (accessCheck != null) return accessCheck;

        try {
            if (startDate == null || endDate == null) {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(90);
            }

            List<WorkoutTypeBreakdownDTO> breakdown = dashboardService.getWorkoutTypeBreakdown(userId, startDate, endDate);
            return ResponseEntity.ok(breakdown);

        } catch (Exception e) {
            log.error("Error getting workout type breakdown for user {}: {}", userId, e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get workout type breakdown", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get recent achievements for authenticated user
     */
    @GetMapping("/achievements")
    public ResponseEntity<?> getRecentAchievements(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") Integer daysBack) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            log.info("Achievements endpoint for authenticated user {} ", authenticatedUserId);

            List<Object> achievements = new ArrayList<>();
            return ResponseEntity.ok(achievements);

        } catch (Exception e) {
            log.error("Error getting recent achievements: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get achievements", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get recent achievements for specific user (with validation)
     */
    @GetMapping("/achievements/{userId}")
    public ResponseEntity<?> getRecentAchievementsForUser(
            @PathVariable Long userId,
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") Integer daysBack) {

        // Check user access
        ResponseEntity<?> accessCheck = jwtUtils.checkUserAccess(request, userId);
        if (accessCheck != null) return accessCheck;

        try {
            log.info("Achievements endpoint for user {} ", userId);

            List<Object> achievements = new ArrayList<>();
            return ResponseEntity.ok(achievements);

        } catch (Exception e) {
            log.error("Error getting recent achievements for user {}: {}", userId, e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get achievements", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get quick statistics for authenticated user
     */
    @GetMapping("/quick-stats")
    public ResponseEntity<?> getQuickStats(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            DashboardSummaryDTO summary = dashboardService.getDashboardSummary(authenticatedUserId);

            QuickStatsDTO quickStats = QuickStatsDTO.builder()
                    .weeklyWorkouts(summary.getWeeklyWorkouts())
                    .weeklyCalories(summary.getWeeklyCalories())
                    .currentStreak(summary.getCurrentStreak())
                    .totalWorkouts(summary.getTotalWorkouts())
                    .build();

            return ResponseEntity.ok(quickStats);

        } catch (Exception e) {
            log.error("Error getting quick stats: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get quick stats", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get quick stats for specific user (with validation)
     */
    @GetMapping("/quick-stats/{userId}")
    public ResponseEntity<?> getQuickStatsForUser(@PathVariable Long userId, HttpServletRequest request) {
        // Check user access
        ResponseEntity<?> accessCheck = jwtUtils.checkUserAccess(request, userId);
        if (accessCheck != null) return accessCheck;

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
            return jwtUtils.createErrorResponse("Failed to get quick stats", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}