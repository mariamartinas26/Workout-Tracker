package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.*;
import com.marecca.workoutTracker.entity.Goal;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.repository.GoalRepository;
import com.marecca.workoutTracker.repository.ScheduledWorkoutRepository;
import com.marecca.workoutTracker.service.DashboardService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtControllerUtils jwtUtils;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private ScheduledWorkoutRepository scheduledWorkoutRepository;

    /**
     * Get complete dashboard summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            DashboardSummaryDTO summary = dashboardService.getDashboardSummary(authenticatedUserId);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return jwtUtils.createErrorResponse("Failed to get dashboard summary", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout calendar data
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
            return jwtUtils.createErrorResponse("Failed to get workout calendar", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout trends
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getWorkoutTrends(
            HttpServletRequest request,
            @RequestParam String period, // daily, weekly, monthly
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            //validate period type
            if (!period.matches("daily|weekly|monthly")) {
                return jwtUtils.createBadRequestResponse("Invalid period");
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
            return jwtUtils.createErrorResponse("Failed to get workout trends", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get workout type breakdown
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
            return jwtUtils.createErrorResponse("Failed to get workout type breakdown", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get recent achievements
     */
    @GetMapping("/achievements")
    public ResponseEntity<?> getRecentAchievements(HttpServletRequest request, @RequestParam(defaultValue = "30") Integer daysBack) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            //computes startDate
            LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);

            List<Map<String, Object>> achievements = new ArrayList<>();

            //recently completed workouts
            try {
                List<ScheduledWorkout> recentWorkouts = scheduledWorkoutRepository.findCompletedWorkoutsInDateRange(authenticatedUserId, startDate);

                for (ScheduledWorkout workout : recentWorkouts) {
                    Map<String, Object> achievement = new HashMap<>();
                    achievement.put("id", "workout_" + workout.getScheduledWorkoutId());
                    achievement.put("type", "COMPLETED_WORKOUT");
                    achievement.put("title", "Workout Completed!");
                    achievement.put("description", "You completed a " +
                            (workout.getActualDurationMinutes() != null ? workout.getActualDurationMinutes() + " minute " : "") +
                            "workout session");
                    achievement.put("achievedAt", workout.getActualEndTime());
                    achievement.put("points", 25);
                    achievements.add(achievement);
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }

            //sorting after recency
            achievements.sort((a, b) -> {
                LocalDateTime dateA = (LocalDateTime) a.get("achievedAt");
                LocalDateTime dateB = (LocalDateTime) b.get("achievedAt");

                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;

                return dateB.compareTo(dateA);
            });

            return ResponseEntity.ok(achievements);

        } catch (Exception e) {
            return jwtUtils.createErrorResponse("Failed to get achievements", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get quick statistics
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
            return jwtUtils.createErrorResponse("Failed to get quick stats", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}