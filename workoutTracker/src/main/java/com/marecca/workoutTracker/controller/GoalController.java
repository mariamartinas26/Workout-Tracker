package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.request.CreateGoalRequest;
import com.marecca.workoutTracker.entity.Goal;
import com.marecca.workoutTracker.repository.GoalRepository;
import com.marecca.workoutTracker.service.GoalService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final JwtControllerUtils jwtUtils;

    @Autowired
    private GoalRepository goalRepository;

    /**
     * Create a new goal
     */
    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody CreateGoalRequest request, HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            // Validation
            if (request.getUserId() == null) {
                return jwtUtils.createBadRequestResponse("User ID is required");
            }

            // Verify user can only create goals for themselves
            if (!request.getUserId().equals(authenticatedUserId)) {
                return jwtUtils.createErrorResponse("You can only create goals for yourself", HttpStatus.FORBIDDEN);
            }

            if (request.getGoalType() == null || request.getGoalType().trim().isEmpty()) {
                return jwtUtils.createBadRequestResponse("Goal type is required");
            }

            // Create goal entity
            Goal goal = new Goal();
            goal.setGoalType(Goal.GoalType.fromValue(request.getGoalType()));
            goal.setTargetWeightLoss(request.getTargetWeightLoss());
            goal.setTargetWeightGain(request.getTargetWeightGain());
            goal.setCurrentWeight(request.getCurrentWeight());
            goal.setTimeframeMonths(request.getTimeframe());
            goal.setNotes(request.getNotes());

            Goal savedGoal = goalService.createGoal(authenticatedUserId, goal); // Use authenticated user ID

            Map<String, Object> response = createGoalResponse(savedGoal);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse("Invalid goal type: " + request.getGoalType());
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to create goals");
        }
    }

    /**
     * Get a specific goal by ID
     */
    @GetMapping("/{goalId}")
    public ResponseEntity<?> getGoal(@PathVariable Long goalId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            Optional<Goal> goalOptional = goalService.getGoalById(goalId);
            if (goalOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("Goal not found", HttpStatus.NOT_FOUND);
            }

            Goal goal = goalOptional.get();

            List<Goal> userGoals = goalService.getUserGoals(authenticatedUserId);
            boolean isUserGoal = userGoals.stream().anyMatch(g -> g.getGoalId().equals(goalId));

            if (!isUserGoal) {
                return jwtUtils.createErrorResponse("You can only access your own goals", HttpStatus.FORBIDDEN);
            }

            Map<String, Object> response = createGoalResponse(goal);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access goals");
        }
    }

    /**
     * Update goal status
     */
    @PutMapping("/{goalId}/status")
    public ResponseEntity<?> updateGoalStatus(@PathVariable Long goalId,
                                              @RequestBody Map<String, String> request,
                                              HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);

            String statusStr = request.get("status");
            if (statusStr == null) {
                return jwtUtils.createBadRequestResponse("Status is required");
            }

            // Verify goal ownership before updating
            Optional<Goal> goalOptional = goalService.getGoalById(goalId);
            if (goalOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("Goal not found", HttpStatus.NOT_FOUND);
            }

            // Check ownership
            List<Goal> userGoals = goalService.getUserGoals(authenticatedUserId);
            boolean isUserGoal = userGoals.stream().anyMatch(g -> g.getGoalId().equals(goalId));

            if (!isUserGoal) {
                return jwtUtils.createErrorResponse("You can only update your own goals", HttpStatus.FORBIDDEN);
            }

            Goal.GoalStatus status = Goal.GoalStatus.valueOf(statusStr.toUpperCase());
            Goal updatedGoal = goalService.updateGoalStatus(goalId, status);

            Map<String, Object> response = createGoalResponse(updatedGoal);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return jwtUtils.createBadRequestResponse("Invalid status value");
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to update goals");
        }
    }

    /**
     * Delete a goal
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long goalId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            // Verify goal ownership before deleting
            Optional<Goal> goalOptional = goalService.getGoalById(goalId);
            if (goalOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("Goal not found", HttpStatus.NOT_FOUND);
            }

            // Check ownership
            List<Goal> userGoals = goalService.getUserGoals(authenticatedUserId);
            boolean isUserGoal = userGoals.stream().anyMatch(g -> g.getGoalId().equals(goalId));

            if (!isUserGoal) {

                return jwtUtils.createErrorResponse("You can only delete your own goals", HttpStatus.FORBIDDEN);
            }

            goalService.deleteGoal(goalId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Goal deleted successfully");
            response.put("goalId", goalId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to delete goals");
        }
    }

    /**
     * Get completed goals as achievements for authenticated user
     */
    @GetMapping("/achievements/completed-goals")
    public ResponseEntity<?> getCompletedGoalsAchievements(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") Integer daysBack) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);

            List<Map<String, Object>> achievements = new ArrayList<>();

            try {
                List<Goal> completedGoals = goalRepository.findCompletedGoalsInDateRange(
                        authenticatedUserId, startDate, LocalDateTime.now());

                for (Goal goal : completedGoals) {
                    Map<String, Object> achievement = new HashMap<>();
                    achievement.put("id", goal.getGoalId());
                    achievement.put("type", "COMPLETED_GOAL");
                    achievement.put("title", getGoalAchievementTitle(goal));
                    achievement.put("description", getGoalAchievementDescription(goal));
                    achievement.put("achievedAt", goal.getCompletedAt());
                    achievement.put("goalType", goal.getGoalType().getValue());
                    achievement.put("originalGoal", convertGoalToMap(goal));
                    achievement.put("icon", getGoalAchievementIcon(goal.getGoalType()));
                    achievement.put("points", calculateAchievementPoints(goal));
                    achievements.add(achievement);
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }


            Map<String, Object> response = new HashMap<>();
            response.put("completedGoals", achievements);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get completed goals achievements"));
        }
    }

    private Map<String, Object> convertGoalToMap(Goal goal) {
        Map<String, Object> goalMap = new HashMap<>();
        goalMap.put("goalId", goal.getGoalId());
        goalMap.put("goalType", goal.getGoalType().getValue());
        goalMap.put("targetWeightLoss", goal.getTargetWeightLoss());
        goalMap.put("targetWeightGain", goal.getTargetWeightGain());
        goalMap.put("currentWeight", goal.getCurrentWeight());
        goalMap.put("timeframeMonths", goal.getTimeframeMonths());
        goalMap.put("status", goal.getStatus().toString());
        goalMap.put("createdAt", goal.getCreatedAt());
        goalMap.put("completedAt", goal.getCompletedAt());
        return goalMap;
    }


    /**
     * Get current user's goals
     */
    @GetMapping("/my-goals")
    public ResponseEntity<?> getMyGoals(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            List<Goal> goals = goalService.getUserGoals(authenticatedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("goals", goals.stream().map(this::createGoalResponse).toList());
            response.put("totalGoals", goals.size());
            response.put("activeGoals", goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access goals");
        }
    }

    /**
     * Generates achievements title
     */
    private String getGoalAchievementTitle(Goal goal) {
        String goalTypeValue = goal.getGoalType().getValue().toLowerCase();

        if (goalTypeValue.contains("lose_weight")) {
            return "Weight Loss Goal Achieved!";
        } else if (goalTypeValue.contains("gain_muscle")) {
            return "Muscle Gain Goal Achieved!";
        } else if (goalTypeValue.contains("maintain_health")) {
            return "Health Maintenance Goal Achieved!";
        } else {
            return "Goal Completed!";
        }
    }

    /**
     * Generates description for achievement
     */
    private String getGoalAchievementDescription(Goal goal) {
        String goalTypeValue = goal.getGoalType().getValue().toLowerCase();
        StringBuilder description = new StringBuilder();

        if (goalTypeValue.contains("lose_weight") && goal.getTargetWeightLoss() != null && goal.getTargetWeightLoss().compareTo(BigDecimal.ZERO) > 0) {
            description.append("You lost ").append(goal.getTargetWeightLoss()).append(" kg");
        } else if (goalTypeValue.contains("gain_muscle") && goal.getTargetWeightGain() != null && goal.getTargetWeightGain().compareTo(BigDecimal.ZERO) > 0) {
            description.append("You gained ").append(goal.getTargetWeightGain()).append(" kg of muscle");
        } else if (goalTypeValue.contains("maintain_health")) {
            description.append("You successfully maintained your health goals");
        } else {
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
        String goalTypeValue = goalType.getValue().toLowerCase();

        if (goalTypeValue.contains("lose_weight")) {
            return "🎯";
        } else if (goalTypeValue.contains("gain_muscle")) {
            return "💪";
        } else if (goalTypeValue.contains("maintain_health")) {
            return "⚖️";
        } else {
            return "🏆";
        }
    }

    /**
     * Calculates points for achievement based on goal difficulty
     */
    private Integer calculateAchievementPoints(Goal goal) {
        int basePoints = 100;

        if (goal.getTargetWeightLoss() != null) {
            basePoints += goal.getTargetWeightLoss().intValue() * 10;
        }
        if (goal.getTargetWeightGain() != null) {
            basePoints += goal.getTargetWeightGain().intValue() * 10;
        }

        // Points based on duration
        if (goal.getTimeframeMonths() != null) {
            basePoints += Math.max(0, (12 - goal.getTimeframeMonths()) * 5); // More points for faster goals
        }

        return basePoints;
    }

    private Map<String, Object> createGoalResponse(Goal goal) {
        Map<String, Object> response = new HashMap<>();
        response.put("goalId", goal.getGoalId());
        response.put("goalType", goal.getGoalType().getValue());
        response.put("targetWeightLoss", goal.getTargetWeightLoss());
        response.put("targetWeightGain", goal.getTargetWeightGain());
        response.put("currentWeight", goal.getCurrentWeight());
        response.put("timeframeMonths", goal.getTimeframeMonths());
        response.put("dailyCalorieDeficit", goal.getDailyCalorieDeficit());
        response.put("dailyCalorieSurplus", goal.getDailyCalorieSurplus());
        response.put("weeklyWeightChange", goal.getWeeklyWeightChange());
        response.put("targetWeight", goal.getTargetWeight());
        response.put("status", goal.getStatus());
        response.put("createdAt", goal.getCreatedAt());
        response.put("updatedAt", goal.getUpdatedAt());
        response.put("completedAt", goal.getCompletedAt());
        response.put("notes", goal.getNotes());
        return response;
    }

    /**
     * Exception handlers for error handling
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return jwtUtils.createBadRequestResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        return jwtUtils.createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        return jwtUtils.createErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}