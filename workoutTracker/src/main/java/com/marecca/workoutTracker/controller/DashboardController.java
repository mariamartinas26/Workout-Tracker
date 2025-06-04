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
    @Autowired
    private GoalRepository goalRepository;
    @Autowired
    private ScheduledWorkoutRepository scheduledWorkoutRepository;

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

            // Calculează data de început
            LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);

            List<Map<String, Object>> achievements = new ArrayList<>();

            // 1. Obține goal-urile completate recent
            List<Goal> completedGoals = goalRepository.findCompletedGoalsInDateRange(
                    authenticatedUserId, startDate, LocalDateTime.now());

            for (Goal goal : completedGoals) {
                Map<String, Object> achievement = new HashMap<>();
                achievement.put("id", "goal_" + goal.getGoalId());
                achievement.put("type", "COMPLETED_GOAL");
                achievement.put("title", getGoalAchievementTitle(goal));
                achievement.put("description", getGoalAchievementDescription(goal));
                achievement.put("achievedAt", goal.getCompletedAt());
                achievement.put("goalType", goal.getGoalType().getValue());
                achievement.put("icon", getGoalAchievementIcon(goal.getGoalType()));
                achievement.put("points", calculateAchievementPoints(goal));
                achievements.add(achievement);
            }

            // 2. Obține antrenamentele completate recent
            List<ScheduledWorkout> recentWorkouts = scheduledWorkoutRepository
                    .findCompletedWorkoutsInDateRange(authenticatedUserId, startDate);

            for (ScheduledWorkout workout : recentWorkouts) {
                Map<String, Object> achievement = new HashMap<>();
                achievement.put("id", "workout_" + workout.getScheduledWorkoutId());
                achievement.put("type", "COMPLETED_WORKOUT");
                achievement.put("title", "💪 Workout Completed!");
                achievement.put("description", "You completed a " +
                        (workout.getActualDurationMinutes() != null ? workout.getActualDurationMinutes() + " minute " : "") +
                        "workout session");
                achievement.put("achievedAt", workout.getActualEndTime());
                achievement.put("icon", "🏃‍♂️");
                achievement.put("points", 25);
                achievements.add(achievement);
            }

            // 3. Verifică milestone-uri de greutate
            //achievements.addAll(getWeightMilestones(authenticatedUserId, startDate));

            // Sortează după dată (cel mai recent primul)
            achievements.sort((a, b) -> {
                LocalDateTime dateA = (LocalDateTime) a.get("achievedAt");
                LocalDateTime dateB = (LocalDateTime) b.get("achievedAt");
                return dateB.compareTo(dateA);
            });

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

            // Calculează data de început
            LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);

            List<Map<String, Object>> achievements = new ArrayList<>();

            // 1. Obține goal-urile completate recent
            List<Goal> completedGoals = goalRepository.findCompletedGoalsInDateRange(
                    userId, startDate, LocalDateTime.now());

            for (Goal goal : completedGoals) {
                Map<String, Object> achievement = new HashMap<>();
                achievement.put("id", "goal_" + goal.getGoalId());
                achievement.put("type", "COMPLETED_GOAL");
                achievement.put("title", getGoalAchievementTitle(goal));
                achievement.put("description", getGoalAchievementDescription(goal));
                achievement.put("achievedAt", goal.getCompletedAt());
                achievement.put("goalType", goal.getGoalType().getValue());
                achievement.put("icon", getGoalAchievementIcon(goal.getGoalType()));
                achievement.put("points", calculateAchievementPoints(goal));
                achievements.add(achievement);
            }

            // 2. Obține antrenamentele completate recent
            List<ScheduledWorkout> recentWorkouts = scheduledWorkoutRepository
                    .findCompletedWorkoutsInDateRange(userId, startDate);

            for (ScheduledWorkout workout : recentWorkouts) {
                Map<String, Object> achievement = new HashMap<>();
                achievement.put("id", "workout_" + workout.getScheduledWorkoutId());
                achievement.put("type", "COMPLETED_WORKOUT");
                achievement.put("title", "💪 Workout Completed!");
                achievement.put("description", "You completed a " +
                        (workout.getActualDurationMinutes() != null ? workout.getActualDurationMinutes() + " minute " : "") +
                        "workout session");
                achievement.put("achievedAt", workout.getActualEndTime());
                achievement.put("icon", "🏃‍♂️");
                achievement.put("points", 25);
                achievements.add(achievement);
            }

            // 3. Verifică milestone-uri de greutate
            //achievements.addAll(getWeightMilestones(userId, startDate));

            // Sortează după dată (cel mai recent primul)
            achievements.sort((a, b) -> {
                LocalDateTime dateA = (LocalDateTime) a.get("achievedAt");
                LocalDateTime dateB = (LocalDateTime) b.get("achievedAt");
                return dateB.compareTo(dateA);
            });

            return ResponseEntity.ok(achievements);

        } catch (Exception e) {
            log.error("Error getting recent achievements for user {}: {}", userId, e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get achievements", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

// METODE HELPER (adaugă în clasa controller)

    private String getGoalAchievementTitle(Goal goal) {
        switch (goal.getGoalType()) {
            case LOSE_WEIGHT:
                return "🎯 Weight Loss Goal Achieved!";
            case GAIN_MUSCLE:
                return "💪 Muscle Gain Goal Achieved!";
            case MAINTAIN_HEALTH:
                return "⚖️ Health Maintenance Goal Achieved!";
            default:
                return "🏆 Goal Completed!";
        }
    }

    private String getGoalAchievementDescription(Goal goal) {
        StringBuilder description = new StringBuilder();

        switch (goal.getGoalType()) {
            case LOSE_WEIGHT:
                if (goal.getTargetWeightLoss() != null && goal.getTargetWeightLoss().compareTo(BigDecimal.ZERO) > 0) {
                    description.append("You lost ").append(goal.getTargetWeightLoss()).append(" kg");
                } else {
                    description.append("You achieved your weight loss goal");
                }
                break;
            case GAIN_MUSCLE:
                if (goal.getTargetWeightGain() != null && goal.getTargetWeightGain().compareTo(BigDecimal.ZERO) > 0) {
                    description.append("You gained ").append(goal.getTargetWeightGain()).append(" kg of muscle");
                } else {
                    description.append("You achieved your muscle gain goal");
                }
                break;
            case MAINTAIN_HEALTH:
                description.append("You successfully maintained your health goals");
                break;
            default:
                description.append("You achieved your fitness goal");
        }

        if (goal.getTimeframeMonths() != null) {
            String months = goal.getTimeframeMonths() == 1 ? "month" : "months";
            description.append(" in ").append(goal.getTimeframeMonths()).append(" ").append(months);
        }

        description.append("!");
        return description.toString();
    }

    private String getGoalAchievementIcon(Goal.GoalType goalType) {
        switch (goalType) {
            case LOSE_WEIGHT:
                return "🎯";
            case GAIN_MUSCLE:
                return "💪";
            case MAINTAIN_HEALTH:
                return "⚖️";
            default:
                return "🏆";
        }
    }

    private int calculateAchievementPoints(Goal goal) {
        int basePoints = 100;

        if (goal.getTargetWeightLoss() != null) {
            basePoints += goal.getTargetWeightLoss().multiply(BigDecimal.valueOf(10)).intValue();
        }
        if (goal.getTargetWeightGain() != null) {
            basePoints += goal.getTargetWeightGain().multiply(BigDecimal.valueOf(10)).intValue();
        }

        if (goal.getTimeframeMonths() != null) {
            basePoints += Math.max(0, (12 - goal.getTimeframeMonths()) * 5);
        }

        return basePoints;
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