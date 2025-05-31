package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.Goal;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.repository.GoalRepository;
import com.marecca.workoutTracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    /**
     * Create a new goal for a user
     */
    public Goal createGoal(Long userId, Goal goal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        goal.setUser(user);

        //calculate additional fields based on goal type
        calculateGoalMetrics(goal);

        return goalRepository.save(goal);
    }

    /**
     * Get all goals for a user
     */
    public List<Goal> getUserGoals(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get active goals for a user
     */
    public List<Goal> getActiveUserGoals(Long userId) {
        return goalRepository.findActiveGoalsByUserId(userId);
    }

    /**
     * Get a specific goal by ID
     */
    public Optional<Goal> getGoalById(Long goalId) {
        return goalRepository.findById(goalId);
    }

    /**
     * Update goal status
     */
    public Goal updateGoalStatus(Long goalId, Goal.GoalStatus status) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + goalId));

        goal.setStatus(status);
        if (status == Goal.GoalStatus.COMPLETED) {
            goal.setCompletedAt(LocalDateTime.now());
        }

        return goalRepository.save(goal);
    }

    /**
     * Delete a goal
     */
    public void deleteGoal(Long goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new RuntimeException("Goal not found with id: " + goalId);
        }
        goalRepository.deleteById(goalId);
    }

    /**
     * Calculate goal metrics (calories, weight change, etc.)
     */
    private void calculateGoalMetrics(Goal goal) {
        if (goal.getGoalType() == Goal.GoalType.LOSE_WEIGHT &&
                goal.getTargetWeightLoss() != null &&
                goal.getCurrentWeight() != null &&
                goal.getTimeframeMonths() != null) {

            // Calculate weight loss metrics
            BigDecimal targetLoss = goal.getTargetWeightLoss();
            BigDecimal timeframeWeeks = BigDecimal.valueOf(goal.getTimeframeMonths() * 4.33);

            // 1 kg fat = approximately 7700 calories
            BigDecimal totalCaloriesNeeded = targetLoss.multiply(BigDecimal.valueOf(7700));
            BigDecimal dailyCalorieDeficit = totalCaloriesNeeded.divide(
                    timeframeWeeks.multiply(BigDecimal.valueOf(7)), 0, RoundingMode.HALF_UP);
            BigDecimal weeklyWeightLoss = targetLoss.divide(timeframeWeeks, 2, RoundingMode.HALF_UP);
            BigDecimal targetWeight = goal.getCurrentWeight().subtract(targetLoss);

            goal.setDailyCalorieDeficit(dailyCalorieDeficit.intValue());
            goal.setWeeklyWeightChange(weeklyWeightLoss.negate()); // negative for loss
            goal.setTargetWeight(targetWeight);

        } else if (goal.getGoalType() == Goal.GoalType.GAIN_MUSCLE &&
                goal.getTargetWeightGain() != null &&
                goal.getTimeframeMonths() != null) {

            // Calculate muscle gain metrics
            BigDecimal targetGain = goal.getTargetWeightGain();
            BigDecimal timeframeWeeks = BigDecimal.valueOf(goal.getTimeframeMonths() * 4.33);

            // 1 kg muscle gain = approximately 5500-6000 calories
            BigDecimal totalCaloriesNeeded = targetGain.multiply(BigDecimal.valueOf(5500));
            BigDecimal dailyCalorieSurplus = totalCaloriesNeeded.divide(
                    timeframeWeeks.multiply(BigDecimal.valueOf(7)), 0, RoundingMode.HALF_UP);
            BigDecimal weeklyWeightGain = targetGain.divide(timeframeWeeks, 2, RoundingMode.HALF_UP);

            goal.setDailyCalorieSurplus(dailyCalorieSurplus.intValue());
            goal.setWeeklyWeightChange(weeklyWeightGain); // positive for gain

            if (goal.getCurrentWeight() != null) {
                BigDecimal targetWeight = goal.getCurrentWeight().add(targetGain);
                goal.setTargetWeight(targetWeight);
            }
        }
    }

    /**
     * Get most recent active goal for a user
     */
    public Optional<Goal> getMostRecentActiveGoal(Long userId) {
        return goalRepository.findMostRecentActiveGoalByUserId(userId);
    }

    /**
     * Count active goals for a user
     */
    public Long countActiveGoals(Long userId) {
        return goalRepository.countActiveGoalsByUserId(userId);
    }

    /**
     * Obține goalurile completed pentru un user într-un anumit interval de timp
     */
    public List<Goal> getCompletedGoalsByUserAndTimeframe(Long userId, Integer daysBack) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);

        // Folosește metoda existentă și filtrează manual
        List<Goal> allUserGoals = getUserGoals(userId);

        return allUserGoals.stream()
                .filter(goal -> goal.getStatus() == Goal.GoalStatus.COMPLETED)
                .filter(goal -> goal.getCompletedAt() != null)
                .filter(goal -> goal.getCompletedAt().isAfter(startDate))
                .sorted((g1, g2) -> g2.getCompletedAt().compareTo(g1.getCompletedAt())) // Sortare descrescătoare
                .collect(Collectors.toList());
    }
}