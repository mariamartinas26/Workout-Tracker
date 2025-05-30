package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.CreateGoalRequest;
import com.marecca.workoutTracker.entity.Goal;
import com.marecca.workoutTracker.service.GoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class GoalController {

    private final GoalService goalService;

    /**
     * Create a new goal
     */
    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody CreateGoalRequest request) {
        try {
            // Validation
            if (request.getUserId() == null) {
                return createErrorResponse("User ID is required", HttpStatus.BAD_REQUEST);
            }
            if (request.getGoalType() == null || request.getGoalType().trim().isEmpty()) {
                return createErrorResponse("Goal type is required", HttpStatus.BAD_REQUEST);
            }

            // Create goal entity
            Goal goal = new Goal();
            goal.setGoalType(Goal.GoalType.fromValue(request.getGoalType()));
            goal.setTargetWeightLoss(request.getTargetWeightLoss());
            goal.setTargetWeightGain(request.getTargetWeightGain());
            goal.setCurrentWeight(request.getCurrentWeight());
            goal.setTimeframeMonths(request.getTimeframe());
            goal.setNotes(request.getNotes());

            Goal savedGoal = goalService.createGoal(request.getUserId(), goal);

            Map<String, Object> response = createGoalResponse(savedGoal);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse("Invalid goal type: " + request.getGoalType(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("Failed to create goal: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all goals for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserGoals(@PathVariable Long userId) {
        try {
            List<Goal> goals = goalService.getUserGoals(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("goals", goals.stream().map(this::createGoalResponse).toList());
            response.put("totalGoals", goals.size());
            response.put("activeGoals", goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Failed to fetch goals: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get active goals for a user
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getActiveUserGoals(@PathVariable Long userId) {
        try {
            List<Goal> activeGoals = goalService.getActiveUserGoals(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("goals", activeGoals.stream().map(this::createGoalResponse).toList());
            response.put("count", activeGoals.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Failed to fetch active goals: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get a specific goal by ID
     */
    @GetMapping("/{goalId}")
    public ResponseEntity<?> getGoal(@PathVariable Long goalId) {
        try {
            Optional<Goal> goalOptional = goalService.getGoalById(goalId);
            if (goalOptional.isEmpty()) {
                return createErrorResponse("Goal not found", HttpStatus.NOT_FOUND);
            }

            Map<String, Object> response = createGoalResponse(goalOptional.get());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Failed to fetch goal: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update goal status
     */
    @PutMapping("/{goalId}/status")
    public ResponseEntity<?> updateGoalStatus(@PathVariable Long goalId, @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("status");
            if (statusStr == null) {
                return createErrorResponse("Status is required", HttpStatus.BAD_REQUEST);
            }

            Goal.GoalStatus status = Goal.GoalStatus.valueOf(statusStr.toUpperCase());
            Goal updatedGoal = goalService.updateGoalStatus(goalId, status);

            Map<String, Object> response = createGoalResponse(updatedGoal);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return createErrorResponse("Invalid status value", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse("Failed to update goal status: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a goal
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long goalId) {
        try {
            goalService.deleteGoal(goalId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Goal deleted successfully");
            response.put("goalId", goalId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Failed to delete goal: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }
}