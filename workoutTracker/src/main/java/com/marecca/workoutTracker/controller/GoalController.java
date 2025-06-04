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

/**
 * Controller for managing goals - JWT Protected
 */
@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class GoalController {

    private final GoalService goalService;
    private final JwtControllerUtils jwtUtils;

    @Autowired
    private GoalRepository goalRepository;

    /**
     * Create a new goal (requires authentication)
     */
    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody CreateGoalRequest request, HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to create goal by user: {}", authenticatedUserId);

            // Validation
            if (request.getUserId() == null) {
                return jwtUtils.createBadRequestResponse("User ID is required");
            }

            // Verify user can only create goals for themselves
            if (!request.getUserId().equals(authenticatedUserId)) {
                log.warn("User {} attempted to create goal for user {}", authenticatedUserId, request.getUserId());
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
            log.error("Invalid goal type: {}", request.getGoalType());
            return jwtUtils.createBadRequestResponse("Invalid goal type: " + request.getGoalType());
        } catch (Exception e) {
            log.error("Authentication error or unexpected error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to create goals");
        }
    }

    /**
     * Get all goals for a user (requires authentication)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserGoals(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get goals for user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only access their own goals
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access goals for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own goals", HttpStatus.FORBIDDEN);
            }

            List<Goal> goals = goalService.getUserGoals(authenticatedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("goals", goals.stream().map(this::createGoalResponse).toList());
            response.put("totalGoals", goals.size());
            response.put("activeGoals", goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access goals");
        }
    }

    /**
     * Get active goals for a user (requires authentication)
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getActiveUserGoals(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get active goals for user: {} by authenticated user: {}", userId, authenticatedUserId);

            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access active goals for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own goals", HttpStatus.FORBIDDEN);
            }

            List<Goal> activeGoals = goalService.getActiveUserGoals(authenticatedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("goals", activeGoals.stream().map(this::createGoalResponse).toList());
            response.put("count", activeGoals.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access active goals");
        }
    }

    /**
     * Get a specific goal by ID (requires authentication)
     */
    @GetMapping("/{goalId}")
    public ResponseEntity<?> getGoal(@PathVariable Long goalId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get goal: {} by user: {}", goalId, authenticatedUserId);

            Optional<Goal> goalOptional = goalService.getGoalById(goalId);
            if (goalOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("Goal not found", HttpStatus.NOT_FOUND);
            }

            Goal goal = goalOptional.get();

            // Verify goal ownership - need to check if this goal belongs to the authenticated user
            // This assumes Goal entity has a way to get the user ID (you might need to add this)
            List<Goal> userGoals = goalService.getUserGoals(authenticatedUserId);
            boolean isUserGoal = userGoals.stream().anyMatch(g -> g.getGoalId().equals(goalId));

            if (!isUserGoal) {
                log.warn("User {} attempted to access goal {} that they don't own", authenticatedUserId, goalId);
                return jwtUtils.createErrorResponse("You can only access your own goals", HttpStatus.FORBIDDEN);
            }

            Map<String, Object> response = createGoalResponse(goal);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access goals");
        }
    }

    /**
     * Update goal status (requires authentication)
     */
    @PutMapping("/{goalId}/status")
    public ResponseEntity<?> updateGoalStatus(@PathVariable Long goalId,
                                              @RequestBody Map<String, String> request,
                                              HttpServletRequest httpRequest) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(httpRequest);
            log.info("REST request to update goal status: {} by user: {}", goalId, authenticatedUserId);

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
                log.warn("User {} attempted to update goal {} that they don't own", authenticatedUserId, goalId);
                return jwtUtils.createErrorResponse("You can only update your own goals", HttpStatus.FORBIDDEN);
            }

            Goal.GoalStatus status = Goal.GoalStatus.valueOf(statusStr.toUpperCase());
            Goal updatedGoal = goalService.updateGoalStatus(goalId, status);

            Map<String, Object> response = createGoalResponse(updatedGoal);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", request.get("status"));
            return jwtUtils.createBadRequestResponse("Invalid status value");
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to update goals");
        }
    }

    /**
     * Delete a goal (requires authentication)
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long goalId, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to delete goal: {} by user: {}", goalId, authenticatedUserId);

            // Verify goal ownership before deleting
            Optional<Goal> goalOptional = goalService.getGoalById(goalId);
            if (goalOptional.isEmpty()) {
                return jwtUtils.createErrorResponse("Goal not found", HttpStatus.NOT_FOUND);
            }

            // Check ownership
            List<Goal> userGoals = goalService.getUserGoals(authenticatedUserId);
            boolean isUserGoal = userGoals.stream().anyMatch(g -> g.getGoalId().equals(goalId));

            if (!isUserGoal) {
                log.warn("User {} attempted to delete goal {} that they don't own", authenticatedUserId, goalId);
                return jwtUtils.createErrorResponse("You can only delete your own goals", HttpStatus.FORBIDDEN);
            }

            goalService.deleteGoal(goalId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Goal deleted successfully");
            response.put("goalId", goalId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to delete goals");
        }
    }

    /**
     * Get completed goals as achievements for authenticated user
     * Endpoint: GET /api/goals/achievements/completed-goals
     */
    @GetMapping("/achievements/completed-goals")
    public ResponseEntity<?> getCompletedGoalsAchievements(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") Integer daysBack) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("Getting completed goals achievements for user {} for last {} days",
                    authenticatedUserId, daysBack);

            // Calculează data de început
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
                log.warn("Could not fetch completed goals: {}", e.getMessage());
            }

            // Returnează în formatul așteptat de frontend
            Map<String, Object> response = new HashMap<>();
            response.put("completedGoals", achievements);

            log.info("Found {} completed goals for user {}", achievements.size(), authenticatedUserId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting completed goals achievements: {}", e.getMessage());
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
     * Endpoint for completed goals (requires authentication)
     */
    @GetMapping("/achievements/{userId}/completed-goals")
    public ResponseEntity<?> getCompletedGoalsAsAchievements(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") Integer daysBack,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get completed goals for user: {} by authenticated user: {}", userId, authenticatedUserId);

            // Users can only access their own achievements
            if (!userId.equals(authenticatedUserId)) {
                log.warn("User {} attempted to access achievements for user {}", authenticatedUserId, userId);
                return jwtUtils.createErrorResponse("You can only access your own achievements", HttpStatus.FORBIDDEN);
            }

            if (daysBack < 1 || daysBack > 365) {
                daysBack = 30;
            }

            List<Goal> completedGoals = goalService.getCompletedGoalsByUserAndTimeframe(authenticatedUserId, daysBack);

            Map<String, Object> response = new HashMap<>();
            response.put("completedGoals", completedGoals.stream().map(this::createGoalAchievementResponse).toList());
            response.put("count", completedGoals.size());
            response.put("daysBack", daysBack);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access achievements");
        }
    }

    /**
     * Get current user's goals (convenience endpoint)
     */
    @GetMapping("/my-goals")
    public ResponseEntity<?> getMyGoals(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my goals for user: {}", authenticatedUserId);

            return getUserGoals(authenticatedUserId, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access goals");
        }
    }

    /**
     * Get current user's active goals (convenience endpoint)
     */
    @GetMapping("/my-active")
    public ResponseEntity<?> getMyActiveGoals(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my active goals for user: {}", authenticatedUserId);

            return getActiveUserGoals(authenticatedUserId, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access active goals");
        }
    }

    /**
     * Get current user's achievements (convenience endpoint)
     */
    @GetMapping("/my-achievements")
    public ResponseEntity<?> getMyAchievements(@RequestParam(defaultValue = "30") Integer daysBack, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get my achievements for user: {}", authenticatedUserId);

            return getCompletedGoalsAsAchievements(authenticatedUserId, daysBack, request);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access achievements");
        }
    }

    /**
     * Creates answer for completed goals as achievements
     */
    private Map<String, Object> createGoalAchievementResponse(Goal goal) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", goal.getGoalId());
        response.put("type", "COMPLETED_GOAL");
        response.put("title", getGoalAchievementTitle(goal));
        response.put("description", getGoalAchievementDescription(goal));
        response.put("achievedAt", goal.getCompletedAt());
        response.put("goalType", goal.getGoalType().getValue());
        response.put("originalGoal", createGoalResponse(goal));
        response.put("icon", getGoalAchievementIcon(goal.getGoalType()));
        response.put("points", calculateAchievementPoints(goal));
        return response;
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
        log.error("Illegal argument: {}", e.getMessage());
        return jwtUtils.createBadRequestResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage());
        return jwtUtils.createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return jwtUtils.createErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}