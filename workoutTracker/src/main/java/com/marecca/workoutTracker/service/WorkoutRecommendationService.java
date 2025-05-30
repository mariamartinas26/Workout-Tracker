package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.dto.WorkoutRecommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WorkoutRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkoutRecommendationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Workout recommendation based on plsql function "recommend_workout" !!!
     */
    public List<WorkoutRecommendation> getRecommendations(Long userId, String goalType) {
        logger.info("Getting workout recommendations for user: {} with goal: {}", userId, goalType);

        try {
            if (!userExists(userId)) {
                throw new IllegalArgumentException("User with ID " + userId + " does not exist");
            }

            //calls plsql function
            String sql = "SELECT * FROM recommend_workout(?, ?)";

            List<WorkoutRecommendation> recommendations = jdbcTemplate.query(
                    sql,
                    new Object[]{userId, goalType},
                    new WorkoutRecommendationRowMapper()
            );

            logger.info("Successfully retrieved {} recommendations for user: {}", recommendations.size(), userId);
            return recommendations;

        } catch (DataAccessException e) {
            logger.error("Database error while getting recommendations for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve workout recommendations: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while getting recommendations for user: {}", userId, e);
            throw new RuntimeException("An unexpected error occurred while generating recommendations", e);
        }
    }

    /**
     * Saves a workout plan based on recommendations
     */
    public Map<String, Object> saveWorkoutPlan(Long userId, List<WorkoutRecommendation> recommendations, Long goalId) {
        logger.info("Saving workout plan for user: {} with {} exercises", userId, recommendations.size());

        try {
            if (!userExists(userId)) {
                throw new IllegalArgumentException("User with ID " + userId + " does not exist");
            }

            if (recommendations == null || recommendations.isEmpty()) {
                throw new IllegalArgumentException("Cannot save workout plan without recommendations");
            }

            //generates plan name
            String planName = generateWorkoutPlanName(goalId);

            //calculates estimated duration
            int estimatedDuration = calculateEstimatedDuration(recommendations);

            //inserts workout plan into workout_plans table
            String insertPlanSql = """
                INSERT INTO workout_plans (user_id, plan_name, description, estimated_duration_minutes, 
                                         difficulty_level, goals, notes, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING workout_plan_id
                """;

            String description = "Generated workout plan based on personalized recommendations";
            String goals = goalId != null ? "Goal ID: " + goalId : "General fitness improvement";
            String notes = "Generated with " + recommendations.size() + " exercises";
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            Long workoutPlanId = jdbcTemplate.queryForObject(
                    insertPlanSql,
                    new Object[]{
                            userId, planName, description, estimatedDuration,
                            calculateAverageDifficulty(recommendations), goals, notes, now, now
                    },
                    Long.class
            );

            logger.info("Created workout plan with ID: {} for user: {}", workoutPlanId, userId);

            //inserts exercises details
            insertWorkoutExerciseDetails(workoutPlanId, recommendations);

            Map<String, Object> result = new HashMap<>();
            result.put("workoutPlanId", workoutPlanId);
            result.put("planName", planName);
            result.put("description", description);
            result.put("estimatedDurationMinutes", estimatedDuration);
            result.put("exerciseCount", recommendations.size());
            result.put("createdAt", now);
            result.put("userId", userId);
            result.put("goalId", goalId);

            logger.info("Successfully saved workout plan with ID: {} for user: {}", workoutPlanId, userId);
            return result;

        } catch (DataAccessException e) {
            logger.error("Database error while saving workout plan for user: {}", userId, e);
            throw new RuntimeException("Failed to save workout plan: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while saving workout plan for user: {}", userId, e);
            throw new RuntimeException("An unexpected error occurred while saving the workout plan", e);
        }
    }

    /**
     * Obține statistici despre workout-urile utilizatorului
     */
    public Map<String, Object> getUserWorkoutStats(Long userId) {
        logger.info("Getting workout stats for user: {}", userId);

        try {
            if (!userExists(userId)) {
                throw new IllegalArgumentException("User with ID " + userId + " does not exist");
            }

            String sql = """
                SELECT 
                    COUNT(DISTINCT sw.scheduled_workout_id) as total_workouts,
                    COALESCE(AVG(wel.weight_used_kg), 0) as avg_weight_used,
                    STRING_AGG(DISTINCT e.primary_muscle_group::TEXT, ', ') as muscle_groups_worked,
                    MAX(sw.actual_start_time::DATE) as last_workout_date,
                    COUNT(DISTINCT e.exercise_id) as unique_exercises,
                    COALESCE(AVG(sw.overall_rating), 0) as avg_workout_rating,
                    COUNT(CASE WHEN sw.status = 'COMPLETED' THEN 1 END) as completed_workouts,
                    COUNT(CASE WHEN sw.status = 'MISSED' THEN 1 END) as missed_workouts
                FROM scheduled_workouts sw
                LEFT JOIN workout_exercise_logs wel ON sw.scheduled_workout_id = wel.scheduled_workout_id
                LEFT JOIN exercises e ON wel.exercise_id = e.exercise_id
                WHERE sw.user_id = ?
                AND sw.actual_start_time >= CURRENT_DATE - INTERVAL '90 days'
                """;

            Map<String, Object> stats = jdbcTemplate.queryForMap(sql, userId);

            // Adaugă statistici suplimentare
            stats.put("userId", userId);
            stats.put("periodDays", 90);
            stats.put("workoutFrequencyPerWeek",
                    calculateWorkoutFrequency((Number) stats.get("completed_workouts")));

            logger.info("Successfully retrieved stats for user: {}", userId);
            return stats;

        } catch (DataAccessException e) {
            logger.error("Database error while getting stats for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve user workout stats: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while getting stats for user: {}", userId, e);
            throw new RuntimeException("An unexpected error occurred while retrieving stats", e);
        }
    }

    /**
     * Checks if user exists
     */
    private boolean userExists(Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, new Object[]{userId}, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Error checking if user exists: {}", userId, e);
            return false;
        }
    }

    /**
     * Generates workout plan name
     */
    private String generateWorkoutPlanName(Long goalId) {
        String baseName = "Recommended Workout";
        String timestamp = LocalDateTime.now().toString().substring(0, 16).replace("T", " ");

        if (goalId != null) {
            return baseName + " (Goal " + goalId + ") - " + timestamp;
        }
        return baseName + " - " + timestamp;
    }

    /**
     * Calculates duration of workout
     */
    private int calculateEstimatedDuration(List<WorkoutRecommendation> recommendations) {
        int totalMinutes = 0;

        for (WorkoutRecommendation rec : recommendations) {
            //2 minutes for set + rest time
            int setsTime = (rec.getRecommendedSets() != null ? rec.getRecommendedSets() : 3) * 2;
            int restTime = (rec.getRestTimeSeconds() != null ? rec.getRestTimeSeconds() : 60)
                    * (rec.getRecommendedSets() != null ? rec.getRecommendedSets() : 3) / 60;
            totalMinutes += setsTime + restTime;
        }

        //10 minutes for warm up
        return Math.max(totalMinutes + 10, 30); //minimum 30 mins
    }

    /**
     * Calculates difficulty
     */
    private int calculateAverageDifficulty(List<WorkoutRecommendation> recommendations) {
        if (recommendations.isEmpty()) return 3;

        double avgScore = recommendations.stream()
                .mapToDouble(rec -> rec.getPriorityScore() != null ? rec.getPriorityScore().doubleValue() : 3.0)
                .average()
                .orElse(3.0);

        if (avgScore >= 4.5) return 5;
        if (avgScore >= 3.5) return 4;
        if (avgScore >= 2.5) return 3;
        if (avgScore >= 1.5) return 2;
        return 1;
    }

    /**
     * Inserts exercise details into workout plan
     */
    private void insertWorkoutExerciseDetails(Long workoutPlanId, List<WorkoutRecommendation> recommendations) {
        String sql = """
            INSERT INTO workout_exercise_details 
            (workout_plan_id, exercise_id, exercise_order, target_sets, target_reps_min, 
             target_reps_max, target_weight_kg, rest_time_seconds, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        for (int i = 0; i < recommendations.size(); i++) {
            WorkoutRecommendation rec = recommendations.get(i);

            String notes = String.format("Recommendation - Priority Score: %.2f, Weight: %s%%",
                    rec.getPriorityScore() != null ? rec.getPriorityScore().doubleValue() : 0.0,
                    rec.getRecommendedWeightPercentage() != null ? rec.getRecommendedWeightPercentage().toString() : "N/A");

            jdbcTemplate.update(sql,
                    workoutPlanId,
                    rec.getExerciseId(),
                    i + 1,
                    rec.getRecommendedSets(),
                    rec.getRecommendedRepsMin(),
                    rec.getRecommendedRepsMax(),
                    null,
                    rec.getRestTimeSeconds(),
                    notes
            );
        }

        logger.info("Inserted {} exercise details for workout plan ID: {}", recommendations.size(), workoutPlanId);
    }

    /**
     * Calculates workout frequency for workout
     */
    private double calculateWorkoutFrequency(Number completedWorkouts) {
        if (completedWorkouts == null) return 0.0;
        return Math.round(completedWorkouts.doubleValue() / 13.0 * 100.0) / 100.0;
    }

    /**
     * RowMapper for WorkoutRecommendation
     */
    private static class WorkoutRecommendationRowMapper implements RowMapper<WorkoutRecommendation> {
        @Override
        public WorkoutRecommendation mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkoutRecommendation recommendation = new WorkoutRecommendation();

            recommendation.setExerciseId(rs.getLong("exercise_id"));
            recommendation.setExerciseName(rs.getString("exercise_name"));
            recommendation.setRecommendedSets(rs.getInt("recommended_sets"));
            recommendation.setRecommendedRepsMin(rs.getInt("recommended_reps_min"));
            recommendation.setRecommendedRepsMax(rs.getInt("recommended_reps_max"));
            recommendation.setRecommendedWeightPercentage(rs.getBigDecimal("recommended_weight_percentage"));
            recommendation.setRestTimeSeconds(rs.getInt("rest_time_seconds"));
            recommendation.setPriorityScore(rs.getBigDecimal("priority_score"));

            return recommendation;
        }
    }
}