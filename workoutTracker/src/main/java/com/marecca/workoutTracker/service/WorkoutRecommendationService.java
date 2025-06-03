package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.dto.WorkoutRecommendationDTO;
import com.marecca.workoutTracker.service.exceptions.InvalidGoalTypeException;
import com.marecca.workoutTracker.service.exceptions.InvalidUserDataException;
import com.marecca.workoutTracker.service.exceptions.NoExercisesFoundException;
import com.marecca.workoutTracker.service.exceptions.UserNotFoundException;
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
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Workout recommendation
     */
    public List<WorkoutRecommendationDTO> getRecommendations(Long userId, String goalType) {
        try {
            String sql = "SELECT * FROM recommend_workout(?, ?)";

            List<WorkoutRecommendationDTO> recommendations = jdbcTemplate.query(
                    sql,
                    new Object[]{userId, goalType},
                    new WorkoutRecommendationRowMapper()
            );

            return recommendations;

        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            String sqlState = extractSQLState(e);

            //hadle exceptions
            if (sqlState != null) {
                switch (sqlState) {
                    case "00001":
                        // INVALID_USER_ID
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "INVALID_USER_ID"));

                    case "00002":
                        // NULL_GOAL_TYPE
                        throw new IllegalArgumentException(
                                extractCustomErrorMessage(errorMessage, "NULL_GOAL_TYPE"));

                    case "00003":
                        // INVALID_GOAL_TYPE
                        throw new InvalidGoalTypeException(
                                extractCustomErrorMessage(errorMessage, "INVALID_GOAL_TYPE"));

                    case "00004":
                        // USER_NOT_FOUND
                        throw new UserNotFoundException(
                                extractCustomErrorMessage(errorMessage, "USER_NOT_FOUND"));

                    case "00005":
                    case "00006":
                        // INVALID_USER_DATA
                        throw new InvalidUserDataException(
                                extractCustomErrorMessage(errorMessage, "INVALID_USER_DATA"));

                    case "00007":
                        // NO_EXERCISES_FOUND
                        throw new NoExercisesFoundException(
                                extractCustomErrorMessage(errorMessage, "NO_EXERCISES_FOUND"));

                    case "00999":
                        // DATABASE_ERROR
                        throw new RuntimeException(
                                "Database error occurred: " +
                                        extractCustomErrorMessage(errorMessage, "DATABASE_ERROR"), e);

                    default:
                        break;
                }
            }

            if (errorMessage.contains("USER_NOT_FOUND")) {
                throw new UserNotFoundException(
                        extractUserIdFromError(errorMessage, "USER_NOT_FOUND"));
            } else if (errorMessage.contains("INVALID_GOAL_TYPE")) {
                throw new InvalidGoalTypeException(
                        extractGoalTypeFromError(errorMessage, "INVALID_GOAL_TYPE"));
            } else if (errorMessage.contains("INVALID_USER_DATA")) {
                throw new InvalidUserDataException(
                        extractCustomErrorMessage(errorMessage, "INVALID_USER_DATA"));
            } else if (errorMessage.contains("NO_EXERCISES_FOUND")) {
                throw new NoExercisesFoundException(
                        extractCustomErrorMessage(errorMessage, "NO_EXERCISES_FOUND"));
            }

            throw new RuntimeException("Failed to retrieve workout recommendations: " + errorMessage, e);

        } catch (UserNotFoundException | InvalidGoalTypeException |
                 InvalidUserDataException | NoExercisesFoundException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while generating recommendations", e);
        }
    }

    /**
     * Extracts the SQL state from DataAccessException
     */
    private String extractSQLState(DataAccessException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.sql.SQLException) {
                return ((java.sql.SQLException) cause).getSQLState();
            }
            cause = cause.getCause();
        }
        return null;
    }

    /**
     * Extracts the custom error message from PL/SQL exception
     */
    private String extractCustomErrorMessage(String fullErrorMessage, String errorPrefix) {
        if (fullErrorMessage == null) return "Unknown error";

        int prefixIndex = fullErrorMessage.indexOf(errorPrefix + ":");
        if (prefixIndex != -1) {
            String message = fullErrorMessage.substring(prefixIndex + errorPrefix.length() + 1).trim();
            int whereIndex = message.indexOf(" Where:");
            if (whereIndex != -1) {
                message = message.substring(0, whereIndex).trim();
            }
            return message;
        }

        return fullErrorMessage;
    }

    /**
     * Extracts user ID from error message
     */
    private String extractUserIdFromError(String errorMessage, String prefix) {
        String cleanMessage = extractCustomErrorMessage(errorMessage, prefix);
        // Extract the user ID using regex
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*ID:? (\\d+).*");
        java.util.regex.Matcher matcher = pattern.matcher(cleanMessage);
        if (matcher.matches()) {
            return "User with ID " + matcher.group(1) + " does not exist";
        }
        return cleanMessage;
    }

    /**
     * Extracts goal type from error message
     */
    private String extractGoalTypeFromError(String errorMessage, String prefix) {
        String cleanMessage = extractCustomErrorMessage(errorMessage, prefix);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*goal type:? ([A-Z_]+).*");
        java.util.regex.Matcher matcher = pattern.matcher(cleanMessage);
        if (matcher.matches()) {
            return "Invalid goal type: " + matcher.group(1) +
                    ". Valid options are: WEIGHT_LOSS, MUSCLE_GAIN, MAINTENANCE";
        }
        return cleanMessage;
    }

    /**
     * Checks if a workout plan exists for a given user and plan name
     */
    private boolean workoutPlanExists(Long userId, String planName) {
        try {
            String sql = "SELECT COUNT(*) FROM workout_plans WHERE user_id = ? AND plan_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, planName);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            return false;
        }
    }

    /**
     * saves a recommended workout plan
     * @param userId
     * @param recommendations
     * @param goalId
     * @param planName
     * @return
     */
    public Map<String, Object> saveWorkoutPlan(Long userId, List<WorkoutRecommendationDTO> recommendations, Long goalId, String planName) {
        try {
            if (!userExists(userId)) {
                throw new IllegalArgumentException("User with ID " + userId + " does not exist");
            }

            if (recommendations == null || recommendations.isEmpty()) {
                throw new IllegalArgumentException("Cannot save workout plan without recommendations");
            }

            // Generates plan name based on goal type
            if (planName == null || planName.trim().isEmpty()) {
                planName = generatePlanNameFromGoal(goalId);
            }

            // Check if workout plan already exists
            if (workoutPlanExists(userId, planName)) {
                // Update existing workout plan
                return updateExistingWorkoutPlan(userId, planName, recommendations, goalId);
            }

            // Calculates estimated duration
            int estimatedDuration = calculateEstimatedDuration(recommendations);

            // Inserts workout plan into workout_plans table
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

            // Inserts exercises details
            insertWorkoutExerciseDetails(workoutPlanId, recommendations);

            Map<String, Object> result = new HashMap<>();
            result.put("workoutPlanId", workoutPlanId);
            result.put("planName", planName);
            result.put("description", description);
            result.put("estimatedDurationMinutes", estimatedDuration);
            result.put("exerciseCount", recommendations.size());
            result.put("createdAt", now);
            result.put("userId", userId);

            return result;

        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save workout plan: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while saving the workout plan", e);
        }
    }
    /**
     * Generates plan name based on goal type
     */
    private String generatePlanNameFromGoal(Long goalId) {
        if (goalId == null) {
            return "Recommended Workout";
        }

        try {
            //query for obtaining goal type fron db
            String getGoalTypeSql = "SELECT goal_type FROM goals WHERE goal_id = ?";
            String goalType = jdbcTemplate.queryForObject(getGoalTypeSql, String.class, goalId);

            String goalTypeDisplay = mapGoalTypeToDisplay(goalType);

            return "Recommended Workout for " + goalTypeDisplay;

        } catch (Exception e) {
            return "Recommended Workout";
        }
    }

    /**
     * maps goal type from db to a user-frendly name
     */
    private String mapGoalTypeToDisplay(String goalType) {
        if (goalType == null) {
            return "Fitness";
        }

        switch (goalType.toLowerCase()) {
            case "lose_weight":
                return "Weight Loss";
            case "gain_muscle":
                return "Muscle Gain";
            case "maintain_health":
                return "Health Maintenance";
            default:
                return "Fitness";
        }
    }
    /**
     * Statistics about user workouts
     */
    public Map<String, Object> getUserWorkoutStats(Long userId) {
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

            stats.put("userId", userId);
            stats.put("periodDays", 90);
            stats.put("workoutFrequencyPerWeek",
                    calculateWorkoutFrequency((Number) stats.get("completed_workouts")));

            return stats;

        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to retrieve user workout stats: " + e.getMessage(), e);
        } catch (Exception e) {
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
            return false;
        }
    }


    /**
     * Calculates duration of workout
     */
    private int calculateEstimatedDuration(List<WorkoutRecommendationDTO> recommendations) {
        int totalMinutes = 0;

        for (WorkoutRecommendationDTO rec : recommendations) {
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
    private int calculateAverageDifficulty(List<WorkoutRecommendationDTO> recommendations) {
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
    private void insertWorkoutExerciseDetails(Long workoutPlanId, List<WorkoutRecommendationDTO> recommendations) {
        String sql = """
            INSERT INTO workout_exercise_details 
            (workout_plan_id, exercise_id, exercise_order, target_sets, target_reps_min, 
             target_reps_max, target_weight_kg, rest_time_seconds, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        for (int i = 0; i < recommendations.size(); i++) {
            WorkoutRecommendationDTO rec = recommendations.get(i);

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
    }

    /**
     * Updates an existing workout plan with new recommendations
     */
    private Map<String, Object> updateExistingWorkoutPlan(Long userId, String planName, List<WorkoutRecommendationDTO> recommendations, Long goalId) {
        try {
            // Get the existing workout plan id
            String getWorkoutPlanIdSql = "SELECT workout_plan_id FROM workout_plans WHERE user_id = ? AND plan_name = ?";
            Long workoutPlanId = jdbcTemplate.queryForObject(getWorkoutPlanIdSql, Long.class, userId, planName);

            // Calculate new values
            int estimatedDuration = calculateEstimatedDuration(recommendations);
            String description = "Updated workout plan based on personalized recommendations";
            String goals = goalId != null ? "Goal ID: " + goalId : "General fitness improvement";
            String notes = "Updated with " + recommendations.size() + " exercises";
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            // Update the existing workout plan
            String updatePlanSql = """
            UPDATE workout_plans 
            SET description = ?, 
                estimated_duration_minutes = ?, 
                difficulty_level = ?, 
                goals = ?, 
                notes = ?, 
                updated_at = ?
            WHERE workout_plan_id = ?
            """;

            jdbcTemplate.update(updatePlanSql,
                    description,
                    estimatedDuration,
                    calculateAverageDifficulty(recommendations),
                    goals,
                    notes,
                    now,
                    workoutPlanId);

            // Delete existing exercise details for this workout plan
            String deleteExercisesSql = "DELETE FROM workout_exercise_details WHERE workout_plan_id = ?";
            jdbcTemplate.update(deleteExercisesSql, workoutPlanId);

            // Insert new exercise details
            insertWorkoutExerciseDetails(workoutPlanId, recommendations);

            //result
            Map<String, Object> result = new HashMap<>();
            result.put("workoutPlanId", workoutPlanId);
            result.put("planName", planName);
            result.put("description", description);
            result.put("estimatedDurationMinutes", estimatedDuration);
            result.put("exerciseCount", recommendations.size());
            result.put("updatedAt", now);
            result.put("userId", userId);
            result.put("isUpdated", true); //flag that indicates that is an update not a creation

            return result;

        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to update existing workout plan: " + e.getMessage(), e);
        }
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
    private static class WorkoutRecommendationRowMapper implements RowMapper<WorkoutRecommendationDTO> {
        @Override
        public WorkoutRecommendationDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkoutRecommendationDTO recommendation = new WorkoutRecommendationDTO();

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