--user profile: fitness level, weight, workout history in the past 90 days
-- based on this the alg calculates an strength-multiplier (range 0.8x begginers <=5 completed workouts,
--1.3x expert users >=50 completed workouts)
--

CREATE OR REPLACE FUNCTION recommend_workout(
    p_user_id BIGINT,
    p_goal_type VARCHAR(50)
) RETURNS TABLE(
    exercise_id BIGINT,
    exercise_name VARCHAR(100),
    recommended_sets INTEGER,
    recommended_reps_min INTEGER,
    recommended_reps_max INTEGER,
    recommended_weight_percentage DECIMAL(5,2),
    rest_time_seconds INTEGER,
    priority_score DECIMAL(5,2)
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_user_fitness_level VARCHAR(20);
    v_strength_multiplier DECIMAL(3,2) := 1.0;
    v_avg_user_weight DECIMAL(6,2);
    v_workout_count INTEGER := 0;
BEGIN
    --user info: weight and fitness level
    SELECT fitness_level, weight_kg
    INTO v_user_fitness_level, v_avg_user_weight
    FROM users WHERE user_id = p_user_id;
    --exception if user does not exist
    IF NOT FOUND THEN
        RAISE EXCEPTION 'User not found with ID: %', p_user_id;
    END IF;

    --counts nr of workouts done in the past 90 days
    SELECT COUNT(DISTINCT sw.scheduled_workout_id) INTO v_workout_count
    FROM scheduled_workouts sw
    WHERE sw.user_id = p_user_id
    AND sw.status = 'COMPLETED'
    AND sw.actual_start_time >= CURRENT_DATE - INTERVAL '90 days';

    --strength multiplier based on experience
    v_strength_multiplier := CASE
        WHEN v_workout_count > 50 THEN 1.3 --very advanced
        WHEN v_workout_count > 20 THEN 1.15 --advanced
        WHEN v_workout_count > 5 THEN 1.0 --medium
        ELSE 0.8 --beginner
    END;

    RETURN QUERY
    WITH exercise_stats AS (
        -- Calculate stats for each exercise based on user's history
        SELECT
            e.exercise_id,
            e.exercise_name,
            e.category,
            e.primary_muscle_group,
            e.difficulty_level,
            -- Average performance metrics from user's history
            COALESCE(AVG(wel.weight_used_kg), 0) as avg_weight_used,
            COALESCE(AVG(wel.reps_completed), 0) as avg_reps,
            COALESCE(AVG(wel.sets_completed), 0) as avg_sets,
            COALESCE(AVG(wel.difficulty_rating), 3) as avg_difficulty,
            COUNT(wel.log_id) as times_performed,
            -- Calories estimation based on category and muscle groups
            CASE e.category
                WHEN 'CARDIO' THEN 12.0 --12 calories per minute
                WHEN 'STRENGTH' THEN
                    CASE
                        WHEN e.primary_muscle_group = 'FULL_BODY' THEN 8.0 --8 cal/min
                        WHEN e.primary_muscle_group = 'BACK' THEN 6.0 --6 cal/min
                        WHEN e.primary_muscle_group = 'QUADRICEPS' THEN 7.0
                        ELSE 5.0
                    END
                ELSE 4.0 --general activities 4cal/min
            END as estimated_calories_per_minute,
            -- Muscle building potential
            CASE e.category
                WHEN 'STRENGTH' THEN
                    CASE
                        WHEN e.primary_muscle_group = 'FULL_BODY' THEN 5 --the most efficient
                        WHEN e.primary_muscle_group IN ('BACK', 'CHEST', 'QUADRICEPS') THEN 4
                        ELSE 3
                    END
                ELSE 2 --minimal impact
            END as muscle_building_potential,
            -- Cardio effectiveness
            CASE e.category
                WHEN 'CARDIO' THEN 5
                WHEN 'STRENGTH' THEN
                    CASE
                        WHEN e.primary_muscle_group = 'FULL_BODY' THEN 3
                        ELSE 2
                    END
                ELSE 3
            END as cardio_effectiveness,
            -- Recently performed penalty
            -- if the exercise was done in the las 7 days->-1points
            --else, 0 points
            --purpose: avoids exercise repetition and promotes variety for muscle recovery
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM workout_exercise_logs wel2
                    JOIN scheduled_workouts sw2 ON wel2.scheduled_workout_id = sw2.scheduled_workout_id
                    WHERE sw2.user_id = p_user_id
                    AND wel2.exercise_id = e.exercise_id
                    AND sw2.actual_start_time >= CURRENT_DATE - INTERVAL '7 days'
                ) THEN -1.0
                ELSE 0.0
            END as recency_penalty
        FROM exercises e
        LEFT JOIN workout_exercise_logs wel ON e.exercise_id = wel.exercise_id
        LEFT JOIN scheduled_workouts sw ON wel.scheduled_workout_id = sw.scheduled_workout_id
            AND sw.user_id = p_user_id AND sw.status = 'COMPLETED'
        WHERE e.difficulty_level <=
            CASE v_user_fitness_level
                WHEN 'BEGINNER' THEN 3
                WHEN 'INTERMEDIATE' THEN 4
                ELSE 5
            END
        GROUP BY e.exercise_id, e.exercise_name, e.category, e.primary_muscle_group, e.difficulty_level
    ),
    scored_exercises AS (
        SELECT
            *,
            -- Calculate priority score based on goal type
            CASE p_goal_type
                WHEN 'WEIGHT_LOSS' THEN
                    (cardio_effectiveness * 0.6 +
                     estimated_calories_per_minute * 0.3 +
                     CASE WHEN category = 'CARDIO' THEN 2.0 ELSE 0.0 END +
                     recency_penalty)
                WHEN 'MUSCLE_GAIN' THEN
                    (muscle_building_potential * 0.7 +
                     CASE WHEN category = 'STRENGTH' THEN 2.0 ELSE 0.0 END +
                     CASE WHEN primary_muscle_group IN ('FULL_BODY', 'BACK', 'CHEST', 'QUADRICEPS') THEN 1.0 ELSE 0.0 END +
                     recency_penalty)
                ELSE -- MAINTENANCE
                    ((cardio_effectiveness + muscle_building_potential) * 0.4 +
                     CASE WHEN difficulty_level <= 3 THEN 1.0 ELSE 0.0 END +
                     recency_penalty)
            END as calculated_priority_score
        FROM exercise_stats
    )
    SELECT
        s.exercise_id,
        s.exercise_name,
        -- Recommended sets based on goal and user experience
        CASE p_goal_type
            WHEN 'WEIGHT_LOSS' THEN
                CASE s.category
                    WHEN 'CARDIO' THEN 1
                    WHEN 'STRENGTH' THEN GREATEST(2, ROUND(3 * v_strength_multiplier)::INTEGER)
                    ELSE 2
                END
            WHEN 'MUSCLE_GAIN' THEN
                CASE s.category
                    WHEN 'STRENGTH' THEN GREATEST(3, ROUND(4 * v_strength_multiplier)::INTEGER)
                    WHEN 'CARDIO' THEN 1
                    ELSE 3
                END
            ELSE 3
        END as recommended_sets,

        -- Recommended reps min (based on user's history if available)
        CASE
            WHEN s.times_performed > 0 AND s.avg_reps > 0 THEN
                GREATEST(1, ROUND(s.avg_reps * 0.8)::INTEGER)
            ELSE
                CASE p_goal_type
                    WHEN 'WEIGHT_LOSS' THEN
                        CASE s.category WHEN 'CARDIO' THEN 1 WHEN 'STRENGTH' THEN 12 ELSE 10 END
                    WHEN 'MUSCLE_GAIN' THEN
                        CASE s.category WHEN 'STRENGTH' THEN 6 WHEN 'CARDIO' THEN 1 ELSE 8 END
                    ELSE 10
                END
        END as recommended_reps_min,

        -- Recommended reps max
        CASE
            WHEN s.times_performed > 0 AND s.avg_reps > 0 THEN
                ROUND(s.avg_reps * 1.2)::INTEGER
            ELSE
                CASE p_goal_type
                    WHEN 'WEIGHT_LOSS' THEN
                        CASE s.category WHEN 'CARDIO' THEN 1 WHEN 'STRENGTH' THEN 15 ELSE 15 END
                    WHEN 'MUSCLE_GAIN' THEN
                        CASE s.category WHEN 'STRENGTH' THEN 12 WHEN 'CARDIO' THEN 1 ELSE 12 END
                    ELSE 15
                END
        END as recommended_reps_max,

        -- Weight percentage (based on user's history if available)
        CASE
            WHEN s.times_performed > 0 AND s.avg_weight_used > 0 THEN
                CASE p_goal_type
                    WHEN 'MUSCLE_GAIN' THEN LEAST(100.0, (s.avg_weight_used / GREATEST(v_avg_user_weight, 50) * 100 * 1.1))
                    WHEN 'WEIGHT_LOSS' THEN LEAST(90.0, (s.avg_weight_used / GREATEST(v_avg_user_weight, 50) * 100 * 0.9))
                    ELSE LEAST(95.0, (s.avg_weight_used / GREATEST(v_avg_user_weight, 50) * 100))
                END
            ELSE
                CASE p_goal_type
                    WHEN 'MUSCLE_GAIN' THEN (80 * v_strength_multiplier)::DECIMAL(5,2)
                    WHEN 'WEIGHT_LOSS' THEN (65 * v_strength_multiplier)::DECIMAL(5,2)
                    ELSE (70 * v_strength_multiplier)::DECIMAL(5,2)
                END
        END as recommended_weight_percentage,

        -- Rest time
        CASE s.category
            WHEN 'CARDIO' THEN 30 --30 sec
            WHEN 'STRENGTH' THEN
                CASE p_goal_type
                    WHEN 'MUSCLE_GAIN' THEN 120
                    WHEN 'WEIGHT_LOSS' THEN 45
                    ELSE 90
                END
            ELSE 60
        END as rest_time_seconds,

        s.calculated_priority_score as priority_score

    FROM scored_exercises s
    WHERE s.calculated_priority_score > 1.0  -- Only return exercises with good scores
    ORDER BY s.calculated_priority_score DESC, RANDOM()
    LIMIT 8;
END;
$$;
