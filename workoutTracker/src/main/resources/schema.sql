BEGIN;

DROP TABLE IF EXISTS workout_exercise_logs CASCADE;
DROP TABLE IF EXISTS workout_exercise_details CASCADE;
DROP TABLE IF EXISTS scheduled_workouts CASCADE;
DROP TABLE IF EXISTS workout_plans CASCADE;
DROP TABLE IF EXISTS exercises CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS goals;

DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;
DROP FUNCTION IF EXISTS calculate_workout_duration() CASCADE;
DROP FUNCTION IF EXISTS get_user_workout_stats(BIGINT, DATE, DATE) CASCADE;
DROP FUNCTION IF EXISTS create_workout_plan_with_exercises(BIGINT, VARCHAR, TEXT, INTEGER, INTEGER, JSON) CASCADE;
DROP FUNCTION IF EXISTS schedule_workout(BIGINT, BIGINT, DATE, TIME) CASCADE;

DROP TYPE IF EXISTS exercise_category_type CASCADE;
DROP TYPE IF EXISTS muscle_group_type CASCADE;
DROP TYPE IF EXISTS workout_status_type CASCADE;


CREATE TYPE exercise_category_type AS ENUM (
    'CARDIO',
    'STRENGTH',
    'FLEXIBILITY',
    'BALANCE',
    'SPORTS',
    'OTHER'
);

CREATE TYPE muscle_group_type AS ENUM (
    'CHEST',
    'BACK',
    'SHOULDERS',
    'BICEPS',
    'TRICEPS',
    'FOREARMS',
    'ABS',
    'GLUTES',
    'QUADRICEPS',
    'HAMSTRINGS',
    'CALVES',
    'FULL_BODY',
    'CARDIO'
);

CREATE TYPE workout_status_type AS ENUM (
    'PLANNED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED',
    'MISSED'
);

--user table
CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(50),
                       last_name VARCHAR(50),
                       date_of_birth DATE,
                       height_cm INTEGER CHECK (height_cm > 0 AND height_cm < 300),
                       weight_kg DECIMAL(5,2) CHECK (weight_kg > 0 AND weight_kg < 1000),
                       fitness_level VARCHAR(20) DEFAULT 'BEGINNER' CHECK (fitness_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--exercises table
CREATE TABLE exercises (
                           exercise_id BIGSERIAL PRIMARY KEY,
                           exercise_name VARCHAR(100) NOT NULL UNIQUE,
                           description TEXT,
                           category exercise_category_type NOT NULL,
                           primary_muscle_group muscle_group_type NOT NULL,
                           secondary_muscle_groups muscle_group_type[],
                           equipment_needed VARCHAR(200),
                           difficulty_level INTEGER DEFAULT 1 CHECK (difficulty_level BETWEEN 1 AND 5),
                           instructions TEXT,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--goals table
CREATE TABLE goals (
                       goal_id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       goal_type VARCHAR(50) NOT NULL,
                       target_weight_loss DECIMAL(5,2),
                       target_weight_gain DECIMAL(5,2),
                       current_weight DECIMAL(5,2),
                       timeframe_months INT,
                       daily_calorie_deficit INT,
                       daily_calorie_surplus INT,
                       weekly_weight_change DECIMAL(4,2),
                       target_weight DECIMAL(5,2),
                       status VARCHAR(20) DEFAULT 'ACTIVE',
                       notes TEXT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       completed_at TIMESTAMP NULL,

    --if you delete a user all its goals are also deleted
                       CONSTRAINT fk_goals_user_id
                           FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

--workout plans table
CREATE TABLE workout_plans (
                               workout_plan_id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               plan_name VARCHAR(100) NOT NULL,
                               description TEXT,
                               estimated_duration_minutes INTEGER CHECK (estimated_duration_minutes > 0),
                               difficulty_level INTEGER DEFAULT 1 CHECK (difficulty_level BETWEEN 1 AND 5),
                               goals TEXT,
                               notes TEXT,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_workout_plans_user_id
                                   FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                               CONSTRAINT uk_user_plan_name UNIQUE (user_id, plan_name)
);

--workout exercise details table
CREATE TABLE workout_exercise_details (
                                          workout_exercise_detail_id BIGSERIAL PRIMARY KEY,
                                          workout_plan_id BIGINT NOT NULL,
                                          exercise_id BIGINT NOT NULL,
                                          exercise_order INTEGER NOT NULL,
                                          target_sets INTEGER NOT NULL CHECK (target_sets > 0),
                                          target_reps_min INTEGER CHECK (target_reps_min > 0),
                                          target_reps_max INTEGER CHECK (target_reps_max >= target_reps_min),
                                          target_weight_kg DECIMAL(6,2) CHECK (target_weight_kg >= 0),
                                          target_duration_seconds INTEGER CHECK (target_duration_seconds > 0),
                                          target_distance_meters DECIMAL(8,2) CHECK (target_distance_meters > 0),
                                          rest_time_seconds INTEGER DEFAULT 60 CHECK (rest_time_seconds >= 0),
                                          notes TEXT,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT fk_workout_exercise_details_workout_plan_id
                                              FOREIGN KEY (workout_plan_id) REFERENCES workout_plans(workout_plan_id) ON DELETE CASCADE,
                                          CONSTRAINT fk_workout_exercise_details_exercise_id
                                              FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id) ON DELETE RESTRICT,
                                          CONSTRAINT uk_workout_plan_exercise UNIQUE (workout_plan_id, exercise_id),
                                          CONSTRAINT ck_target_metrics CHECK (
                                              target_reps_min IS NOT NULL OR
                                              target_duration_seconds IS NOT NULL OR
                                              target_distance_meters IS NOT NULL
                                         )
);

--scheduled workots table
CREATE TABLE scheduled_workouts (
                                    scheduled_workout_id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    workout_plan_id BIGINT,
                                    scheduled_date DATE NOT NULL,
                                    scheduled_time TIME,
                                    status workout_status_type DEFAULT 'PLANNED',
                                    actual_start_time TIMESTAMP,
                                    actual_end_time TIMESTAMP,
                                    actual_duration_minutes INTEGER,
                                    calories_burned INTEGER CHECK (calories_burned >= 0),
                                    overall_rating INTEGER CHECK (overall_rating BETWEEN 1 AND 5),
                                    energy_level_before INTEGER CHECK (energy_level_before BETWEEN 1 AND 5),
                                    energy_level_after INTEGER CHECK (energy_level_after BETWEEN 1 AND 5),
                                    notes TEXT,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_scheduled_workouts_user_id
                                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                    CONSTRAINT fk_scheduled_workouts_workout_plan_id
                                        FOREIGN KEY (workout_plan_id) REFERENCES workout_plans(workout_plan_id) ON DELETE SET NULL,
                                    CONSTRAINT ck_actual_times CHECK (
                                        actual_end_time IS NULL OR
                                        actual_start_time IS NULL OR
                                        actual_end_time > actual_start_time
                                        )
);

--workout exercise logs table
CREATE TABLE workout_exercise_logs (
                                       log_id BIGSERIAL PRIMARY KEY,
                                       scheduled_workout_id BIGINT NOT NULL,
                                       exercise_id BIGINT NOT NULL,
                                       exercise_order INTEGER NOT NULL,
                                       sets_completed INTEGER NOT NULL CHECK (sets_completed >= 0),
                                       reps_completed INTEGER CHECK (reps_completed >= 0),
                                       weight_used_kg DECIMAL(6,2) CHECK (weight_used_kg >= 0),
                                       duration_seconds INTEGER CHECK (duration_seconds > 0),
                                       distance_meters DECIMAL(8,2) CHECK (distance_meters > 0),
                                       calories_burned INTEGER CHECK (calories_burned >= 0),
                                       difficulty_rating INTEGER CHECK (difficulty_rating BETWEEN 1 AND 5),
                                       notes TEXT,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_workout_exercise_logs_scheduled_workout_id
                                           FOREIGN KEY (scheduled_workout_id) REFERENCES scheduled_workouts(scheduled_workout_id) ON DELETE CASCADE,
                                       CONSTRAINT fk_workout_exercise_logs_exercise_id
                                           FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id) ON DELETE RESTRICT,
                                       CONSTRAINT uk_scheduled_workout_exercise UNIQUE (scheduled_workout_id, exercise_id)
);


--efficient search after email/username
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

CREATE INDEX idx_exercises_category ON exercises(category);
CREATE INDEX idx_exercises_muscle_group ON exercises(primary_muscle_group);
CREATE INDEX idx_exercises_difficulty ON exercises(difficulty_level);

CREATE INDEX idx_workout_plans_user_id ON workout_plans(user_id);
CREATE INDEX idx_workout_plans_created_at ON workout_plans(created_at);


CREATE INDEX idx_workout_exercise_details_workout_plan_id ON workout_exercise_details(workout_plan_id);
CREATE INDEX idx_workout_exercise_details_exercise_id ON workout_exercise_details(exercise_id);

CREATE INDEX idx_scheduled_workouts_user_id ON scheduled_workouts(user_id);
CREATE INDEX idx_scheduled_workouts_date ON scheduled_workouts(scheduled_date);
CREATE INDEX idx_scheduled_workouts_status ON scheduled_workouts(status);
CREATE INDEX idx_scheduled_workouts_user_date ON scheduled_workouts(user_id, scheduled_date);
CREATE INDEX idx_scheduled_workouts_user_status ON scheduled_workouts(user_id, status);

CREATE INDEX idx_workout_exercise_logs_scheduled_workout_id ON workout_exercise_logs(scheduled_workout_id);
CREATE INDEX idx_workout_exercise_logs_exercise_id ON workout_exercise_logs(exercise_id);


--FUNCTIONS OF TYPE TRIGGER (called by the triggers below)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION calculate_workout_duration()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND
       NEW.actual_start_time IS NOT NULL AND
       NEW.actual_end_time IS NOT NULL THEN
        NEW.actual_duration_minutes = EXTRACT(EPOCH FROM (NEW.actual_end_time - NEW.actual_start_time)) / 60;
END IF;

    IF NEW.actual_start_time IS NOT NULL AND
       NEW.actual_end_time IS NOT NULL AND
       NEW.actual_end_time <= NEW.actual_start_time THEN
        RAISE EXCEPTION 'Actual end time must be after actual start time';
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;


--TRIGGERS THAT INVOKE FUNCTIONS update_updated_at_column() and calculate_workout_duration()
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_workout_plans_updated_at
    BEFORE UPDATE ON workout_plans
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_scheduled_workouts_updated_at
    BEFORE UPDATE ON scheduled_workouts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_calculate_workout_duration
    BEFORE INSERT OR UPDATE ON scheduled_workouts
    FOR EACH ROW
    EXECUTE FUNCTION calculate_workout_duration();


--FUNCTION get_user_workout_stats
CREATE OR REPLACE FUNCTION get_user_workout_stats(
    p_user_id BIGINT,
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL
)
RETURNS TABLE(
    total_workouts INTEGER,
    completed_workouts INTEGER,
    total_duration_minutes INTEGER,
    avg_duration_minutes NUMERIC,
    total_calories_burned INTEGER,
    avg_calories_per_workout NUMERIC,
    completion_rate NUMERIC
) AS $$
BEGIN
RETURN QUERY
SELECT
    COUNT(*)::INTEGER as total_workouts,
        COUNT(CASE WHEN sw.status = 'COMPLETED' THEN 1 END)::INTEGER as completed_workouts,
        COALESCE(SUM(sw.actual_duration_minutes), 0)::INTEGER as total_duration_minutes,
        ROUND(AVG(sw.actual_duration_minutes), 2) as avg_duration_minutes,
    COALESCE(SUM(sw.calories_burned), 0)::INTEGER as total_calories_burned,
        ROUND(AVG(sw.calories_burned), 2) as avg_calories_per_workout,
    ROUND(
            CASE
                WHEN COUNT(*) > 0 THEN
                    (COUNT(CASE WHEN sw.status = 'COMPLETED' THEN 1 END)::NUMERIC / COUNT(*)::NUMERIC) * 100
                ELSE 0
                END, 2
    ) as completion_rate
FROM scheduled_workouts sw
WHERE sw.user_id = p_user_id
  AND (p_start_date IS NULL OR sw.scheduled_date >= p_start_date)
  AND (p_end_date IS NULL OR sw.scheduled_date <= p_end_date);
END;
$$ LANGUAGE plpgsql;



--FUNCTION schedule_workout (ScheduleWorkoutService)!
CREATE OR REPLACE FUNCTION schedule_workout(
    p_user_id BIGINT,
    p_workout_plan_id BIGINT,
    p_scheduled_date DATE,
    p_scheduled_time TIME DEFAULT NULL
) RETURNS BIGINT AS $$
DECLARE
    v_scheduled_workout_id BIGINT;
BEGIN
    --checks if user exists
IF NOT EXISTS (SELECT 1 FROM users WHERE user_id = p_user_id) THEN
        RAISE EXCEPTION 'User with ID % does not exist', p_user_id;
END IF;
    --checks if workout plan exists and is owned by user
IF NOT EXISTS (
        SELECT 1 FROM workout_plans
        WHERE workout_plan_id = p_workout_plan_id AND user_id = p_user_id
    ) THEN
        RAISE EXCEPTION 'Workout plan with ID % does not exist or does not belong to user %',
            p_workout_plan_id, p_user_id;
END IF;

    --checks if user already has a workout scheduled at the same date and hour
    IF EXISTS (
        SELECT 1 FROM scheduled_workouts
        WHERE user_id = p_user_id
          AND scheduled_date = p_scheduled_date
          AND (p_scheduled_time IS NULL OR scheduled_time = p_scheduled_time)
          AND status IN ('PLANNED', 'IN_PROGRESS')
    ) THEN
        RAISE EXCEPTION 'User already has a workout scheduled at % %',
            p_scheduled_date, COALESCE(p_scheduled_time::TEXT, '');
END IF;

--creates workout
INSERT INTO scheduled_workouts (
    user_id, workout_plan_id, scheduled_date, scheduled_time
) VALUES (
    p_user_id, p_workout_plan_id, p_scheduled_date, p_scheduled_time
         ) RETURNING scheduled_workout_id INTO v_scheduled_workout_id;

RETURN v_scheduled_workout_id;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'Error scheduling workout: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;


--user profile: fitness level, weight, workout history in the past 90 days
-- based on this the alg calculates a strength-multiplier (range 0.8x beginners <=5 completed workouts,
--1.3x expert users >=50 completed workouts)
--FUNCTION recommend_workout
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
	--score that reflects how appropriate is an exercise for a specific goal
)
--returns a tabel with recommended workout plan for a user based on his goals

LANGUAGE plpgsql
AS $$
DECLARE
v_user_fitness_level VARCHAR(20);
    v_strength_multiplier DECIMAL(3,2) := 1.0;
    v_avg_user_weight DECIMAL(6,2);
    v_workout_count INTEGER := 0;
BEGIN
    --validate input parameters
    IF p_user_id IS NULL OR p_user_id <= 0 THEN
        RAISE EXCEPTION 'INVALID_USER_ID: User ID must be a positive number, received: %', p_user_id
            USING ERRCODE = '00001';
END IF;

    IF p_goal_type IS NULL THEN
        RAISE EXCEPTION 'NULL_GOAL_TYPE: Goal type cannot be null'
            USING ERRCODE = '00002';
END IF;

    -- Validate goal type
    IF p_goal_type NOT IN ('WEIGHT_LOSS', 'MUSCLE_GAIN', 'MAINTENANCE') THEN
        RAISE EXCEPTION 'INVALID_GOAL_TYPE: Invalid goal type: %. Valid options are: WEIGHT_LOSS, MUSCLE_GAIN, MAINTENANCE', p_goal_type
            USING ERRCODE = '00003';
END IF;

--user info: weight and fitness level
SELECT fitness_level, weight_kg INTO v_user_fitness_level, v_avg_user_weight
FROM users WHERE user_id = p_user_id;

--exception if user does not exist
IF NOT FOUND THEN
        RAISE EXCEPTION 'USER_NOT_FOUND: User not found with ID: %', p_user_id
            USING ERRCODE = '00004';
END IF;

IF v_user_fitness_level IS NULL THEN
        RAISE EXCEPTION 'INVALID_USER_DATA: User fitness level is null for user ID: %', p_user_id
            USING ERRCODE = '00005';
END IF;

IF v_avg_user_weight IS NULL OR v_avg_user_weight <= 0 THEN
        RAISE EXCEPTION 'INVALID_USER_DATA: User weight is invalid for user ID: %. Weight: %', p_user_id, v_avg_user_weight
            USING ERRCODE = '00006';
END IF;

--counts nr of workouts done in the past 90 days
SELECT COUNT(DISTINCT sw.scheduled_workout_id) INTO v_workout_count
FROM scheduled_workouts sw
WHERE sw.user_id = p_user_id AND sw.status = 'COMPLETED' AND sw.actual_start_time >= CURRENT_DATE - INTERVAL '90 days';

--strength multiplier based on v_workout_count (reflects user experience)
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
    		-- Calculates the average weight used across workout logs; defaults to 0 if no data
			COALESCE(AVG(wel.weight_used_kg), 0) as avg_weight_used,
            -- Calculates the average number of reps completed; defaults to 0 if no data
			COALESCE(AVG(wel.reps_completed), 0) as avg_reps,
			-- Calculates the average number of sets completed; defaults to 0 if no data
			COALESCE(AVG(wel.sets_completed), 0) as avg_sets,
			-- Calculates the average difficulty rating given by the user; defaults to 3 if no data
			COALESCE(AVG(wel.difficulty_rating), 3) as avg_difficulty,
			-- Counts how many times the exercise has been performed
			COUNT(wel.log_id) as times_performed,

			--Estimated calories per minute
            CASE e.category
                WHEN 'CARDIO' THEN 12.0 --12 calories per minute (cardio exercises are the best)
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
            -- Cardio effectiveness (cardiovascular health)
            CASE e.category
                WHEN 'CARDIO' THEN 5
                WHEN 'STRENGTH' THEN
                    CASE
                        WHEN e.primary_muscle_group = 'FULL_BODY' THEN 3
                        ELSE 2
                    END
                ELSE 3 --general exercises(streching)
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
            --we compute calculated_priority_score based on goal, efficency and penalty
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
            GREATEST(1, ROUND(s.avg_reps * 0.8))
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
            ROUND(s.avg_reps * 1.2)
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

    -- Check if no exercises were found
    IF NOT FOUND THEN
        RAISE EXCEPTION 'NO_EXERCISES_FOUND: No suitable exercises found for user % with goal type % and fitness level %',
            p_user_id, p_goal_type, v_user_fitness_level
            USING ERRCODE = '00007';
END IF;

EXCEPTION
    WHEN OTHERS THEN
        -- Re-raise custom exceptions
        IF SQLSTATE IN ('00001', '00002', '00003', '00004', '00005', '00006', '00007') THEN
            RAISE;
ELSE
            -- Handle unexpected database errors
            RAISE EXCEPTION 'DATABASE_ERROR: Unexpected database error occurred: %', SQLERRM
                USING ERRCODE = '00999';
END IF;
END;
$$;



-- Add streak tracking table
CREATE TABLE user_workout_streaks (
                                      streak_id BIGSERIAL PRIMARY KEY,
                                      user_id BIGINT NOT NULL,
                                      current_streak INTEGER DEFAULT 0,
                                      longest_streak INTEGER DEFAULT 0,
                                      last_workout_date DATE,
                                      streak_start_date DATE,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_user_workout_streaks_user_id
                                          FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                      CONSTRAINT uk_user_streak UNIQUE (user_id)
);

CREATE INDEX idx_user_workout_streaks_user_id ON user_workout_streaks(user_id);
CREATE INDEX idx_scheduled_workouts_completed_date ON scheduled_workouts(user_id, scheduled_date)
    WHERE status = 'COMPLETED';

CREATE TRIGGER trigger_user_workout_streaks_updated_at
    BEFORE UPDATE ON user_workout_streaks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


--FUNCTION get_dashboard_summary
CREATE OR REPLACE FUNCTION get_dashboard_summary(
    p_user_id BIGINT,
    p_current_date DATE DEFAULT CURRENT_DATE
)
RETURNS TABLE(
    -- Weekly stats (Monday to Sunday)
    weekly_workouts INTEGER,
    weekly_calories INTEGER,
    weekly_avg_duration NUMERIC,
    weekly_avg_rating NUMERIC,
    weekly_workout_days INTEGER,

    -- Monthly stats (1st to end of month)
    monthly_workouts INTEGER,
    monthly_calories INTEGER,
    monthly_avg_duration NUMERIC,
    monthly_avg_rating NUMERIC,
    monthly_workout_days INTEGER,

    -- Streak info
    current_streak INTEGER,
    longest_streak INTEGER,
    last_workout_date DATE,

    -- Lifetime stats
    total_workouts BIGINT,
    total_calories BIGINT,
    total_workout_days BIGINT,
    lifetime_avg_duration NUMERIC,
    first_workout_date DATE
)
LANGUAGE plpgsql
AS $$
DECLARE
v_week_start DATE;
    v_week_end DATE;
    v_month_start DATE;
    v_month_end DATE;
BEGIN
    IF EXTRACT(DOW FROM p_current_date) = 0 THEN
        v_week_start := p_current_date - 6;
        v_week_end := p_current_date;
ELSE
        v_week_start := p_current_date - (EXTRACT(DOW FROM p_current_date)::INTEGER - 1);
        v_week_end := v_week_start + 6;
END IF;

    v_month_start := DATE_TRUNC('month', p_current_date)::DATE;
    v_month_end := (DATE_TRUNC('month', p_current_date) + INTERVAL '1 month - 1 day')::DATE;

RETURN QUERY
    WITH weekly_stats AS (
        SELECT
            COUNT(*)::INTEGER as w_workouts,
            COALESCE(SUM(sw.calories_burned), 0)::INTEGER as w_calories,
            ROUND(AVG(NULLIF(sw.actual_duration_minutes, 0)), 1) as w_avg_duration,
            ROUND(AVG(sw.overall_rating), 1) as w_avg_rating,
            COUNT(DISTINCT sw.scheduled_date)::INTEGER as w_workout_days
        FROM scheduled_workouts sw
        WHERE sw.user_id = p_user_id
          AND sw.status = 'COMPLETED'
          AND sw.scheduled_date BETWEEN v_week_start AND v_week_end
    ),
    monthly_stats AS (
        SELECT
            COUNT(*)::INTEGER as m_workouts,
            COALESCE(SUM(sw.calories_burned), 0)::INTEGER as m_calories,
            ROUND(AVG(NULLIF(sw.actual_duration_minutes, 0)), 1) as m_avg_duration,
            ROUND(AVG(sw.overall_rating), 1) as m_avg_rating,
            COUNT(DISTINCT sw.scheduled_date)::INTEGER as m_workout_days
        FROM scheduled_workouts sw
        WHERE sw.user_id = p_user_id
          AND sw.status = 'COMPLETED'
          AND sw.scheduled_date BETWEEN v_month_start AND v_month_end
    ),
    streak_info AS (
        SELECT
            COALESCE(uws.current_streak, 0) as curr_streak,
            COALESCE(uws.longest_streak, 0) as long_streak,
            uws.last_workout_date as last_date
        FROM user_workout_streaks uws
        WHERE uws.user_id = p_user_id
        UNION ALL
        SELECT 0, 0, NULL::DATE
        LIMIT 1
    ),
    lifetime_stats AS (
        SELECT
            COUNT(*)::BIGINT as total_workouts,
            COALESCE(SUM(sw.calories_burned), 0)::BIGINT as total_calories,
            COUNT(DISTINCT sw.scheduled_date)::BIGINT as total_workout_days,
            ROUND(AVG(NULLIF(sw.actual_duration_minutes, 0)), 1) as lifetime_avg_duration,
            MIN(sw.scheduled_date) as first_workout_date
        FROM scheduled_workouts sw
        WHERE sw.user_id = p_user_id
          AND sw.status = 'COMPLETED'
    )
SELECT
    ws.w_workouts,
    ws.w_calories,
    ws.w_avg_duration,
    ws.w_avg_rating,
    ws.w_workout_days,

    ms.m_workouts,
    ms.m_calories,
    ms.m_avg_duration,
    ms.m_avg_rating,
    ms.m_workout_days,

    si.curr_streak,
    si.long_streak,
    si.last_date,

    ls.total_workouts,
    ls.total_calories,
    ls.total_workout_days,
    ls.lifetime_avg_duration,
    ls.first_workout_date

FROM weekly_stats ws
         CROSS JOIN monthly_stats ms
         CROSS JOIN streak_info si
         CROSS JOIN lifetime_stats ls;
END;
$$;

--FUNCTION update_workout_streak
CREATE OR REPLACE FUNCTION update_workout_streak(
    p_user_id BIGINT,
    p_workout_date DATE
)
RETURNS TABLE(
    current_streak INTEGER,
    longest_streak INTEGER,
    is_new_record BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
v_streak_record user_workout_streaks%ROWTYPE;
    v_yesterday DATE;
    v_is_new_record BOOLEAN := FALSE;
BEGIN
    v_yesterday := p_workout_date - INTERVAL '1 day';

SELECT * INTO v_streak_record
FROM user_workout_streaks
WHERE user_id = p_user_id;

IF NOT FOUND THEN
        INSERT INTO user_workout_streaks (
            user_id, current_streak, longest_streak,
            last_workout_date, streak_start_date
        ) VALUES (
            p_user_id, 1, 1,
            p_workout_date, p_workout_date
        ) RETURNING * INTO v_streak_record;

        v_is_new_record := TRUE;
ELSE
        IF v_streak_record.last_workout_date = p_workout_date THEN
            RETURN QUERY SELECT
                             v_streak_record.current_streak,
                             v_streak_record.longest_streak,
                             FALSE;
RETURN;
END IF;

IF v_streak_record.last_workout_date = v_yesterday THEN
            v_streak_record.current_streak := v_streak_record.current_streak + 1;
ELSE
            v_streak_record.current_streak := 1;
            v_streak_record.streak_start_date := p_workout_date;
END IF;
IF v_streak_record.current_streak > v_streak_record.longest_streak THEN
            v_streak_record.longest_streak := v_streak_record.current_streak;
            v_is_new_record := TRUE;
END IF;


UPDATE user_workout_streaks
SET
    current_streak = v_streak_record.current_streak,
    longest_streak = v_streak_record.longest_streak,
    last_workout_date = p_workout_date,
    streak_start_date = v_streak_record.streak_start_date,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = p_user_id;
END IF;

RETURN QUERY SELECT
        v_streak_record.current_streak,
        v_streak_record.longest_streak,
        v_is_new_record;
END;
$$;



--FUNCTION get_workout_calendar
CREATE OR REPLACE FUNCTION get_workout_calendar(
    p_user_id BIGINT,
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE(
    workout_date DATE,
    workout_count INTEGER,
    total_calories INTEGER,
    total_duration INTEGER,
    avg_rating NUMERIC,
    intensity_level INTEGER -- 0-4 scale for heatmap coloring
)
LANGUAGE plpgsql
AS $$
BEGIN
RETURN QUERY
    WITH date_series AS (
        SELECT generate_series(p_start_date, p_end_date, '1 day'::interval)::DATE as date
    ),
    daily_workouts AS (
        SELECT
            sw.scheduled_date,
            COUNT(*)::INTEGER as workout_count,
            COALESCE(SUM(sw.calories_burned), 0)::INTEGER as total_calories,
            COALESCE(SUM(sw.actual_duration_minutes), 0)::INTEGER as total_duration,
            ROUND(AVG(sw.overall_rating), 1) as avg_rating
        FROM scheduled_workouts sw
        WHERE sw.user_id = p_user_id
          AND sw.status = 'COMPLETED'
          AND sw.scheduled_date BETWEEN p_start_date AND p_end_date
        GROUP BY sw.scheduled_date
    )
SELECT
    ds.date as workout_date,
    COALESCE(dw.workout_count, 0) as workout_count,
    COALESCE(dw.total_calories, 0) as total_calories,
    COALESCE(dw.total_duration, 0) as total_duration,
    COALESCE(dw.avg_rating, 0) as avg_rating,
    CASE
        WHEN dw.workout_count IS NULL THEN 0
        WHEN dw.workout_count = 1 AND dw.total_duration < 30 THEN 1
        WHEN dw.workout_count = 1 AND dw.total_duration < 60 THEN 2
        WHEN dw.workout_count = 1 THEN 3
        WHEN dw.workout_count >= 2 THEN 4
        ELSE 0
        END as intensity_level
FROM date_series ds
         LEFT JOIN daily_workouts dw ON ds.date = dw.scheduled_date
ORDER BY ds.date;
END;
$$;



--FUNCTION get_workout_trends
CREATE OR REPLACE FUNCTION get_workout_trends(
    p_user_id BIGINT,
    p_period_type VARCHAR(10), -- 'daily', 'weekly', 'monthly'
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE(
    period_date DATE,
    period_label VARCHAR(20),
    workout_count INTEGER,
    total_calories INTEGER,
    avg_duration NUMERIC,
    avg_rating NUMERIC
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_period_type = 'daily' THEN
        RETURN QUERY
SELECT
    sw.scheduled_date as period_date,
    TO_CHAR(sw.scheduled_date, 'Mon DD')::VARCHAR(20) as period_label, -- Cast to VARCHAR
        COUNT(*)::INTEGER as workout_count,
        COALESCE(SUM(sw.calories_burned), 0)::INTEGER as total_calories,
        ROUND(AVG(sw.actual_duration_minutes), 1) as avg_duration,
    ROUND(AVG(sw.overall_rating), 1) as avg_rating
FROM scheduled_workouts sw
WHERE sw.user_id = p_user_id
  AND sw.status = 'COMPLETED'
  AND sw.scheduled_date BETWEEN p_start_date AND p_end_date
GROUP BY sw.scheduled_date
ORDER BY sw.scheduled_date;

ELSIF p_period_type = 'weekly' THEN
        RETURN QUERY
SELECT
    DATE_TRUNC('week', sw.scheduled_date)::DATE as period_date,
        ('Week ' || TO_CHAR(DATE_TRUNC('week', sw.scheduled_date), 'MM/DD'))::VARCHAR(20) as period_label, -- Cast to VARCHAR
        COUNT(*)::INTEGER as workout_count,
        COALESCE(SUM(sw.calories_burned), 0)::INTEGER as total_calories,
        ROUND(AVG(sw.actual_duration_minutes), 1) as avg_duration,
    ROUND(AVG(sw.overall_rating), 1) as avg_rating
FROM scheduled_workouts sw
WHERE sw.user_id = p_user_id
  AND sw.status = 'COMPLETED'
  AND sw.scheduled_date BETWEEN p_start_date AND p_end_date
GROUP BY DATE_TRUNC('week', sw.scheduled_date)
ORDER BY DATE_TRUNC('week', sw.scheduled_date);

ELSIF p_period_type = 'monthly' THEN
        RETURN QUERY
SELECT
    DATE_TRUNC('month', sw.scheduled_date)::DATE as period_date,
        TO_CHAR(DATE_TRUNC('month', sw.scheduled_date), 'Mon YYYY')::VARCHAR(20) as period_label,
        COUNT(*)::INTEGER as workout_count,
        COALESCE(SUM(sw.calories_burned), 0)::INTEGER as total_calories,
        ROUND(AVG(sw.actual_duration_minutes), 1) as avg_duration,
    ROUND(AVG(sw.overall_rating), 1) as avg_rating
FROM scheduled_workouts sw
WHERE sw.user_id = p_user_id
  AND sw.status = 'COMPLETED'
  AND sw.scheduled_date BETWEEN p_start_date AND p_end_date
GROUP BY DATE_TRUNC('month', sw.scheduled_date)
ORDER BY DATE_TRUNC('month', sw.scheduled_date);
END IF;
END;
$$;


--FUNCTION get_workout_type_breakdown
CREATE OR REPLACE FUNCTION get_workout_type_breakdown(
    p_user_id BIGINT,
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL
)
RETURNS TABLE(
    category exercise_category_type,
    workout_count INTEGER,
    total_duration INTEGER,
    total_calories INTEGER,
    avg_rating NUMERIC,
    percentage NUMERIC
)
LANGUAGE plpgsql
AS $$
DECLARE
v_total_workouts INTEGER;
BEGIN

SELECT COUNT(*) INTO v_total_workouts
FROM scheduled_workouts sw
         JOIN workout_plans wp ON sw.workout_plan_id = wp.workout_plan_id
         JOIN workout_exercise_details wed ON wp.workout_plan_id = wed.workout_plan_id
         JOIN exercises e ON wed.exercise_id = e.exercise_id
WHERE sw.user_id = p_user_id
  AND sw.status = 'COMPLETED'
  AND (p_start_date IS NULL OR sw.scheduled_date >= p_start_date)
  AND (p_end_date IS NULL OR sw.scheduled_date <= p_end_date);

IF v_total_workouts = 0 THEN
        v_total_workouts := 1;
END IF;

RETURN QUERY
SELECT
    e.category,
    COUNT(DISTINCT sw.scheduled_workout_id)::INTEGER as workout_count,
        COALESCE(SUM(sw.actual_duration_minutes), 0)::INTEGER as total_duration,
        COALESCE(SUM(sw.calories_burned), 0)::INTEGER as total_calories,
        ROUND(AVG(sw.overall_rating), 1) as avg_rating,
    ROUND((COUNT(DISTINCT sw.scheduled_workout_id)::NUMERIC / v_total_workouts::NUMERIC) * 100, 1) as percentage
FROM scheduled_workouts sw
         JOIN workout_plans wp ON sw.workout_plan_id = wp.workout_plan_id
         JOIN workout_exercise_details wed ON wp.workout_plan_id = wed.workout_plan_id
         JOIN exercises e ON wed.exercise_id = e.exercise_id
WHERE sw.user_id = p_user_id
  AND sw.status = 'COMPLETED'
  AND (p_start_date IS NULL OR sw.scheduled_date >= p_start_date)
  AND (p_end_date IS NULL OR sw.scheduled_date <= p_end_date)
GROUP BY e.category
ORDER BY workout_count DESC;
END;
$$;

CREATE OR REPLACE FUNCTION trigger_update_streak_on_workout_completion()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND (OLD.status IS NULL OR OLD.status != 'COMPLETED') THEN
        PERFORM update_workout_streak(NEW.user_id, NEW.scheduled_date);
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_workout_completion_streak_update
    AFTER INSERT OR UPDATE ON scheduled_workouts
                        FOR EACH ROW
                        EXECUTE FUNCTION trigger_update_streak_on_workout_completion();

COMMIT;



--DATABASE POPULATION

BEGIN;

--populate user tables

INSERT INTO users (username, email, password_hash, first_name, last_name,
                   date_of_birth, height_cm, weight_kg, fitness_level, is_active) VALUES
                                                                                      ('Maria Martinas', 'mariamartinas@gmail.com', '$2b$12$K8nzQ2rJ5X7vL9wP3mA4sO.hF6gT8nY2kM1pR7vB9cX4jL3qS8uE6', 'Maria', 'Martinas', '1990-05-15', 180, 75.5, 'INTERMEDIATE', true),
                                                                                      ('Rebecca Sacarescu', 'rebeccasacaraescu@gmail.com', '$2b$12$L9oA3rK6Y8wQ0nP4mB5tP.iG7hU9oZ3lN2qS8wC0dY5kM4rT9vF7', 'Rebecca', 'Sacarescu', '1988-03-22', 165, 62.0, 'BEGINNER', true),
                                                                                      ('Ana Stefan', 'anastefan@gmail.com', '$2b$12$M0pB4sL7Z9xR1oQ5nC6uQ.jH8iV0pA4mO3rT9xD1eZ6lN5sU0wG8', 'Ana', 'Stefan', '1985-11-08', 190, 85.0, 'ADVANCED', true);


-- POPULATE GOALS TABLE
INSERT INTO goals (user_id, goal_type, target_weight_loss, current_weight, timeframe_months,
                   daily_calorie_deficit, target_weight, status, notes) VALUES
                                                                            (1, 'WEIGHT_LOSS', 8.0, 75.5, 4, 500, 67.5, 'ACTIVE', 'Goal is to lose weight for summer beach season'),
                                                                            (2, 'FITNESS_IMPROVEMENT', NULL, 62.0, 6, NULL, NULL, 'ACTIVE', 'Focus on building strength and endurance'),
                                                                            (3, 'MUSCLE_GAIN', NULL, 85.0, 8, NULL, 90.0, 'ACTIVE', 'Bulk phase - increase muscle mass and strength');

INSERT INTO goals (user_id, goal_type, target_weight_gain, current_weight,
                   timeframe_months, daily_calorie_surplus, target_weight, status, notes) VALUES
    (3, 'WEIGHT_GAIN', 5.0, 85.0, 6, 300, 90.0, 'ACTIVE', 'Clean bulk with focus on lean muscle mass');


-- POPULATE EXERCISES TABLE

INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups,
                       equipment_needed, difficulty_level, instructions) VALUES
                                                                             ('Push-ups', 'Classic bodyweight chest exercise', 'STRENGTH', 'CHEST', ARRAY['TRICEPS', 'SHOULDERS']::muscle_group_type[], 'None', 2, 'Start in plank position, lower body until chest nearly touches floor, push back up'),
                                                                             ('Squats', 'Fundamental lower body compound movement', 'STRENGTH', 'QUADRICEPS', ARRAY['GLUTES', 'HAMSTRINGS']::muscle_group_type[], 'None or Barbell', 2, 'Stand with feet shoulder-width apart, lower hips back and down, return to standing'),
                                                                             ('Running', 'Cardiovascular endurance exercise', 'CARDIO', 'CARDIO', ARRAY['CALVES', 'QUADRICEPS']::muscle_group_type[], 'Running shoes', 1, 'Maintain steady pace, focus on breathing rhythm and proper form');

INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
                                                                                                                                                                  ('Deadlift', 'Compound movement targeting posterior chain', 'STRENGTH', 'BACK', ARRAY['GLUTES', 'HAMSTRINGS']::muscle_group_type[], 'Barbell', 4, 'Hip hinge movement, keep bar close to body, drive through heels'),
                                                                                                                                                                  ('Plank', 'Isometric core strengthening exercise', 'STRENGTH', 'ABS', ARRAY['SHOULDERS', 'BACK']::muscle_group_type[], 'None', 2, 'Hold straight line from head to heels, engage core muscles'),
                                                                                                                                                                  ('Yoga Flow', 'Flexibility and balance practice', 'FLEXIBILITY', 'FULL_BODY', NULL, 'Yoga mat', 3, 'Flow through various poses focusing on breath and flexibility');
-- POPULATE WORKOUT_PLANS TABLE
INSERT INTO workout_plans (user_id, plan_name, description, estimated_duration_minutes, difficulty_level, goals, notes) VALUES
                                                                                                                            (1, 'Beginner Full Body', 'Complete full body workout for beginners', 45, 2, 'Build strength and lose weight', 'Perfect for starting fitness journey'),
                                                                                                                            (2, 'Cardio Blast', 'High intensity cardio focused workout', 30, 3, 'Improve cardiovascular health', 'Great for burning calories quickly'),
                                                                                                                            (3, 'Strength Builder', 'Advanced strength training program', 60, 4, 'Build muscle mass and strength', 'Focus on compound movements with progressive overload');

-- POPULATE WORKOUT_EXERCISE_DETAILS TABLE

-- Beginner Full Body Plan (Plan ID 1)
INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_reps_min, target_reps_max, rest_time_seconds, notes) VALUES
                                                                                                                                                                 (1, 1, 1, 3, 8, 12, 60, 'Modify on knees if needed'),
                                                                                                                                                                 (1, 2, 2, 3, 10, 15, 90, 'Focus on proper form over speed');

INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_duration_seconds, rest_time_seconds, notes) VALUES
    (1, 5, 3, 2, 45, 45, 'Hold for 45 seconds per set');

-- Cardio Blast Plan (Plan ID 2)
INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_duration_seconds, rest_time_seconds, notes) VALUES
    (2, 3, 1, 1, 1200, 120, '20 minute steady run');

INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_reps_min, target_reps_max, rest_time_seconds, notes) VALUES
    (2, 1, 2, 3, 10, 15, 30, 'Quick bursts between runs');

-- Strength Builder Plan (Plan ID 3)
INSERT INTO workout_exercise_details (workout_plan_id, exercise_id, exercise_order, target_sets, target_reps_min, target_reps_max, target_weight_kg, rest_time_seconds, notes) VALUES
                                                                                                                                                                                   (3, 4, 1, 4, 5, 8, 100.0, 180, 'Progressive overload each week'),
                                                                                                                                                                                   (3, 2, 2, 4, 8, 12, 80.0, 120, 'Back squats with proper depth');
-- POPULATE SCHEDULED_WORKOUTS TABLE

INSERT INTO scheduled_workouts (user_id, workout_plan_id, scheduled_date, scheduled_time, status, actual_start_time, actual_end_time, calories_burned, overall_rating, energy_level_before, energy_level_after, notes) VALUES
                                                                                                                                                                                                                           (1, 1, '2025-05-28', '08:00:00', 'COMPLETED', '2025-05-28 08:05:00', '2025-05-28 08:50:00', 320, 4, 3, 4, 'Great first workout, felt energized'),
                                                                                                                                                                                                                           (2, 2, '2025-05-29', '18:30:00', 'COMPLETED', '2025-05-29 18:35:00', '2025-05-29 19:10:00', 280, 5, 4, 3, 'Excellent cardio session, really pushed myself'),
                                                                                                                                                                                                                           (3, 3, '2025-05-30', '07:00:00', 'COMPLETED', '2025-05-30 07:10:00', '2025-05-30 08:15:00', 450, 4, 4, 4, 'Solid strength session, hit all target weights');

INSERT INTO scheduled_workouts (user_id, workout_plan_id, scheduled_date, scheduled_time, status, notes) VALUES
                                                                                                             (1, 1, '2025-06-02', '08:00:00', 'PLANNED', 'Looking forward to second workout'),
                                                                                                             (2, 2, '2025-06-02', '19:00:00', 'PLANNED', 'Evening cardio session planned');

-- POPULATE WORKOUT_EXERCISE_LOGS TABLE
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


-- USER_WORKOUT_STREAKS TABLE

-- This table is automatically populated by triggers when workouts are completed
-- The trigger 'trigger_workout_completion_streak_update' calls update_workout_streak()
-- when a workout status changes to 'COMPLETED'

COMMIT;
