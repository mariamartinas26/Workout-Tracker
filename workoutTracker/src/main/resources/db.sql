BEGIN;

DROP TABLE IF EXISTS workout_exercise_logs CASCADE;
DROP TABLE IF EXISTS workout_exercise_details CASCADE;
DROP TABLE IF EXISTS scheduled_workouts CASCADE;
DROP TABLE IF EXISTS workout_plans CASCADE;
DROP TABLE IF EXISTS exercises CASCADE;
DROP TABLE IF EXISTS users CASCADE;

DROP VIEW IF EXISTS workout_plan_details CASCADE;
DROP VIEW IF EXISTS user_workout_history CASCADE;

DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;
DROP FUNCTION IF EXISTS calculate_workout_duration() CASCADE;
DROP FUNCTION IF EXISTS get_user_workout_stats(BIGINT, DATE, DATE) CASCADE;
DROP FUNCTION IF EXISTS create_workout_plan_with_exercises(BIGINT, VARCHAR, TEXT, INTEGER, INTEGER, JSON) CASCADE;
DROP FUNCTION IF EXISTS schedule_workout(BIGINT, BIGINT, DATE, TIME) CASCADE;
DROP FUNCTION IF EXISTS cleanup_test_data() CASCADE;

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

-- Funcție pentru programarea unui workout
CREATE OR REPLACE FUNCTION schedule_workout(
    p_user_id BIGINT,
    p_workout_plan_id BIGINT,
    p_scheduled_date DATE,
    p_scheduled_time TIME DEFAULT NULL
) RETURNS BIGINT AS $$
DECLARE
v_scheduled_workout_id BIGINT;
BEGIN
    -- Validează că utilizatorul există
    IF NOT EXISTS (SELECT 1 FROM users WHERE user_id = p_user_id) THEN
        RAISE EXCEPTION 'User with ID % does not exist', p_user_id;
END IF;

    -- Validează că planul de workout există și aparține utilizatorului
    IF NOT EXISTS (
        SELECT 1 FROM workout_plans
        WHERE workout_plan_id = p_workout_plan_id AND user_id = p_user_id
    ) THEN
        RAISE EXCEPTION 'Workout plan with ID % does not exist or does not belong to user %',
            p_workout_plan_id, p_user_id;
END IF;

    -- Verifică dacă utilizatorul are deja un workout programat la aceeași dată și oră
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

    -- Creează workout-ul programat
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

-- Trigger pentru actualizare automată timestamp updated_at
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

-- Trigger pentru calcularea automată a duratei workout-ului
CREATE TRIGGER trigger_calculate_workout_duration
    BEFORE INSERT OR UPDATE ON scheduled_workouts
                         FOR EACH ROW
                         EXECUTE FUNCTION calculate_workout_duration();

CREATE OR REPLACE VIEW workout_plan_details AS
SELECT
    wp.workout_plan_id,
    wp.user_id,
    u.username,
    wp.plan_name,
    wp.description,
    wp.estimated_duration_minutes,
    wp.difficulty_level,
    wp.goals,
    wp.notes,
    wp.created_at,
    wed.exercise_order,
    e.exercise_name,
    e.category,
    e.primary_muscle_group,
    e.equipment_needed,
    e.difficulty_level as exercise_difficulty,
    wed.target_sets,
    wed.target_reps_min,
    wed.target_reps_max,
    wed.target_weight_kg,
    wed.target_duration_seconds,
    wed.target_distance_meters,
    wed.rest_time_seconds,
    wed.notes as exercise_notes
FROM workout_plans wp
         JOIN users u ON wp.user_id = u.user_id
         JOIN workout_exercise_details wed ON wp.workout_plan_id = wed.workout_plan_id
         JOIN exercises e ON wed.exercise_id = e.exercise_id
ORDER BY wp.workout_plan_id, wed.exercise_order;

CREATE OR REPLACE VIEW user_workout_history AS
SELECT
    sw.scheduled_workout_id,
    u.username,
    u.user_id,
    u.first_name,
    u.last_name,
    wp.plan_name,
    wp.difficulty_level as plan_difficulty,
    sw.scheduled_date,
    sw.scheduled_time,
    sw.status,
    sw.actual_start_time,
    sw.actual_end_time,
    sw.actual_duration_minutes,
    sw.calories_burned,
    sw.overall_rating,
    sw.energy_level_before,
    sw.energy_level_after,
    sw.notes,
    COUNT(wel.log_id) as exercises_logged,
    sw.created_at
FROM scheduled_workouts sw
         JOIN users u ON sw.user_id = u.user_id
         LEFT JOIN workout_plans wp ON sw.workout_plan_id = wp.workout_plan_id
         LEFT JOIN workout_exercise_logs wel ON sw.scheduled_workout_id = wel.scheduled_workout_id
GROUP BY sw.scheduled_workout_id, u.username, u.user_id, u.first_name, u.last_name,
         wp.plan_name, wp.difficulty_level, sw.scheduled_date, sw.scheduled_time,
         sw.status, sw.actual_start_time, sw.actual_end_time, sw.actual_duration_minutes,
         sw.calories_burned, sw.overall_rating, sw.energy_level_before, sw.energy_level_after,
         sw.notes, sw.created_at
ORDER BY sw.scheduled_date DESC, sw.scheduled_time DESC;

COMMIT;