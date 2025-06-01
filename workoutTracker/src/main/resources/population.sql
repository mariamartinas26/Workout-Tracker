-- =================================================================
-- FITNESS DATABASE POPULATION SCRIPT
-- =================================================================

BEGIN;

-- =================================================================
-- POPULATE USERS TABLE
-- =================================================================

INSERT INTO users (username, email, password_hash, first_name, last_name, date_of_birth, height_cm, weight_kg, fitness_level, is_active) VALUES
                                                                                                                                             ('john_doe', 'john.doe@email.com', '$2b$12$K8nzQ2rJ5X7vL9wP3mA4sO.hF6gT8nY2kM1pR7vB9cX4jL3qS8uE6', 'John', 'Doe', '1990-05-15', 180, 75.5, 'INTERMEDIATE', true),
                                                                                                                                             ('sarah_smith', 'sarah.smith@email.com', '$2b$12$L9oA3rK6Y8wQ0nP4mB5tP.iG7hU9oZ3lN2qS8wC0dY5kM4rT9vF7', 'Sarah', 'Smith', '1988-03-22', 165, 62.0, 'BEGINNER', true),
                                                                                                                                             ('mike_wilson', 'mike.wilson@email.com', '$2b$12$M0pB4sL7Z9xR1oQ5nC6uQ.jH8iV0pA4mO3rT9xD1eZ6lN5sU0wG8', 'Mike', 'Wilson', '1985-11-08', 190, 85.0, 'ADVANCED', true);


-- =================================================================
-- POPULATE GOALS TABLE
-- =================================================================

INSERT INTO goals (user_id, goal_type, target_weight_loss, current_weight, timeframe_months, daily_calorie_deficit, target_weight, status, notes) VALUES
                                                                                                                                                      (1, 'WEIGHT_LOSS', 8.0, 75.5, 4, 500, 67.5, 'ACTIVE', 'Goal is to lose weight for summer beach season'),
                                                                                                                                                      (2, 'FITNESS_IMPROVEMENT', NULL, 62.0, 6, NULL, NULL, 'ACTIVE', 'Focus on building strength and endurance'),
                                                                                                                                                      (3, 'MUSCLE_GAIN', NULL, 85.0, 8, NULL, 90.0, 'ACTIVE', 'Bulk phase - increase muscle mass and strength');

INSERT INTO goals (user_id, goal_type, target_weight_gain, current_weight, timeframe_months, daily_calorie_surplus, target_weight, status, notes) VALUES
    (3, 'WEIGHT_GAIN', 5.0, 85.0, 6, 300, 90.0, 'ACTIVE', 'Clean bulk with focus on lean muscle mass');

-- =================================================================
-- POPULATE EXERCISES TABLE
-- =================================================================

INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
                                                                                                                                                                  ('Push-ups', 'Classic bodyweight chest exercise', 'STRENGTH', 'CHEST', ARRAY['TRICEPS', 'SHOULDERS']::muscle_group_type[], 'None', 2, 'Start in plank position, lower body until chest nearly touches floor, push back up'),
                                                                                                                                                                  ('Squats', 'Fundamental lower body compound movement', 'STRENGTH', 'QUADRICEPS', ARRAY['GLUTES', 'HAMSTRINGS']::muscle_group_type[], 'None or Barbell', 2, 'Stand with feet shoulder-width apart, lower hips back and down, return to standing'),
                                                                                                                                                                  ('Running', 'Cardiovascular endurance exercise', 'CARDIO', 'CARDIO', ARRAY['CALVES', 'QUADRICEPS']::muscle_group_type[], 'Running shoes', 1, 'Maintain steady pace, focus on breathing rhythm and proper form');

INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
                                                                                                                                                                  ('Deadlift', 'Compound movement targeting posterior chain', 'STRENGTH', 'BACK', ARRAY['GLUTES', 'HAMSTRINGS']::muscle_group_type[], 'Barbell', 4, 'Hip hinge movement, keep bar close to body, drive through heels'),
                                                                                                                                                                  ('Plank', 'Isometric core strengthening exercise', 'STRENGTH', 'ABS', ARRAY['SHOULDERS', 'BACK']::muscle_group_type[], 'None', 2, 'Hold straight line from head to heels, engage core muscles'),
                                                                                                                                                                  ('Yoga Flow', 'Flexibility and balance practice', 'FLEXIBILITY', 'FULL_BODY', NULL, 'Yoga mat', 3, 'Flow through various poses focusing on breath and flexibility');
-- =================================================================
-- POPULATE WORKOUT_PLANS TABLE
-- =================================================================

INSERT INTO workout_plans (user_id, plan_name, description, estimated_duration_minutes, difficulty_level, goals, notes) VALUES
                                                                                                                            (1, 'Beginner Full Body', 'Complete full body workout for beginners', 45, 2, 'Build strength and lose weight', 'Perfect for starting fitness journey'),
                                                                                                                            (2, 'Cardio Blast', 'High intensity cardio focused workout', 30, 3, 'Improve cardiovascular health', 'Great for burning calories quickly'),
                                                                                                                            (3, 'Strength Builder', 'Advanced strength training program', 60, 4, 'Build muscle mass and strength', 'Focus on compound movements with progressive overload');

-- =================================================================
-- POPULATE WORKOUT_EXERCISE_DETAILS TABLE
-- =================================================================

-- Beginner Full Body Plan (Plan ID 1)
INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_reps_min, target_reps_max, rest_time_seconds, notes) VALUES
                                                                                                                                                                 (1, 1, 1, 3, 8, 12, 60, 'Modify on knees if needed'),
                                                                                                                                                                 (1, 2, 2, 3, 10, 15, 90, 'Focus on proper form over speed');

INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_duration_seconds, rest_time_seconds, notes) VALUES
    (1, 5, 3, 2, 45, 45, 'Hold for 45 seconds per set');

-- Cardio Blast Plan (Plan ID 2)
INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_duration_seconds, rest_time_seconds, notes) VALUES
    (2, 3, 1, 1, 1200, 120, '20 minute steady run');

-- Strength Builder Plan (Plan ID 3)
INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_reps_min, target_reps_max, target_weight_kg, rest_time_seconds, notes) VALUES
                                                                                                                                                                                   (3, 4, 1, 4, 5, 8, 100.0, 180, 'Progressive overload each week'),
                                                                                                                                                                                   (3, 2, 2, 4, 8, 12, 80.0, 120, 'Back squats with proper depth');

-- =================================================================
-- POPULATE SCHEDULED_WORKOUTS TABLE
-- =================================================================

INSERT INTO scheduled_workouts (user_id, workout_plan_id, scheduled_date, scheduled_time, status, actual_start_time, actual_end_time, calories_burned, overall_rating, energy_level_before, energy_level_after, notes) VALUES
                                                                                                                                                                                                                           (1, 1, '2025-05-28', '08:00:00', 'COMPLETED', '2025-05-28 08:05:00', '2025-05-28 08:50:00', 320, 4, 3, 4, 'Great first workout, felt energized'),
                                                                                                                                                                                                                           (2, 2, '2025-05-29', '18:30:00', 'COMPLETED', '2025-05-29 18:35:00', '2025-05-29 19:10:00', 280, 5, 4, 3, 'Excellent cardio session, really pushed myself'),
                                                                                                                                                                                                                           (3, 3, '2025-05-30', '07:00:00', 'COMPLETED', '2025-05-30 07:10:00', '2025-05-30 08:15:00', 450, 4, 4, 4, 'Solid strength session, hit all target weights');

INSERT INTO scheduled_workouts (user_id, workout_plan_id, scheduled_date, scheduled_time, status, notes) VALUES
                                                                                                             (1, 1, '2025-06-02', '08:00:00', 'PLANNED', 'Looking forward to second workout'),
                                                                                                             (2, 2, '2025-06-02', '19:00:00', 'PLANNED', 'Evening cardio session planned');

-- =================================================================
-- POPULATE WORKOUT_EXERCISE_LOGS TABLE
-- =================================================================

-- Logs for John's completed workout (scheduled_workout_id 1)
INSERT INTO workout_exercise_logs (scheduled_workout_id, exercise_id, exercise_order, sets_completed, reps_completed, calories_burned, difficulty_rating, notes) VALUES
                                                                                                                                                                     (1, 1, 1, 3, 10, 80, 3, 'Did full push-ups, form was good'),
                                                                                                                                                                     (1, 2, 2, 3, 12, 120, 3, 'Squats felt comfortable'),
                                                                                                                                                                     (1, 5, 3, 2, NULL, 40, 2, 'Held plank for 45 seconds each set');

-- Logs for Sarah's completed workout (scheduled_workout_id 2)
INSERT INTO workout_exercise_logs (scheduled_workout_id, exercise_id, exercise_order, sets_completed, duration_seconds, calories_burned, difficulty_rating, notes) VALUES
                                                                                                                                                                       (2, 3, 1, 1, 1200, 200, 4, 'Maintained good pace throughout'),
                                                                                                                                                                       (2, 1, 2, 3, NULL, 80, 3, 'Quick push-up sets between running');

-- Logs for Mike's completed workout (scheduled_workout_id 3)
INSERT INTO workout_exercise_logs (scheduled_workout_id, exercise_id, exercise_order, sets_completed, reps_completed, weight_used_kg, calories_burned, difficulty_rating, notes) VALUES
                                                                                                                                                                                     (3, 4, 1, 4, 6, 100.0, 250, 4, 'Hit target weight, form was solid'),
                                                                                                                                                                                     (3, 2, 2, 4, 10, 80.0, 200, 3, 'Back squats went to parallel depth');

-- =================================================================
-- USER_WORKOUT_STREAKS TABLE
-- =================================================================
-- Note: This table is automatically populated by triggers when workouts are completed
-- The trigger 'trigger_workout_completion_streak_update' calls update_workout_streak()
-- when a workout status changes to 'COMPLETED'

COMMIT;

-- =================================================================
-- VERIFICATION QUERIES
-- =================================================================

-- Check record counts for each table
SELECT 'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL
SELECT 'goals', COUNT(*) FROM goals
UNION ALL
SELECT 'exercises', COUNT(*) FROM exercises
UNION ALL
SELECT 'workout_plans', COUNT(*) FROM workout_plans
UNION ALL
SELECT 'workout_exercise_details', COUNT(*) FROM workout_exercise_details
UNION ALL
SELECT 'scheduled_workouts', COUNT(*) FROM scheduled_workouts
UNION ALL
SELECT 'workout_exercise_logs', COUNT(*) FROM workout_exercise_logs
UNION ALL
SELECT 'user_workout_streaks', COUNT(*) FROM user_workout_streaks
ORDER BY table_name;

-- Test some of the custom functions
SELECT 'Dashboard Summary for John Doe:' as test_description;
SELECT * FROM get_dashboard_summary(1, '2025-06-01');

SELECT 'Workout Stats for Sarah Smith:' as test_description;
SELECT * FROM get_user_workout_stats(2, '2025-05-01', '2025-06-01');

-- Show sample data from views
SELECT 'Sample from workout_plan_details view:' as test_description;
SELECT * FROM workout_plan_details WHERE user_id = 1 LIMIT 3;

SELECT 'Sample from user_workout_history view:' as test_description;
SELECT username, plan_name, scheduled_date, status, calories_burned, overall_rating
FROM user_workout_history
WHERE status = 'COMPLETED'
ORDER BY scheduled_date DESC
    LIMIT 3;

SELECT 'Database populated successfully with sample data!' as status;