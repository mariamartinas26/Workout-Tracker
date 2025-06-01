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

-- Add indexes for performance
CREATE INDEX idx_user_workout_streaks_user_id ON user_workout_streaks(user_id);
CREATE INDEX idx_scheduled_workouts_completed_date ON scheduled_workouts(user_id, scheduled_date)
    WHERE status = 'COMPLETED';

-- Trigger for streak table updates
CREATE TRIGGER trigger_user_workout_streaks_updated_at
    BEFORE UPDATE ON user_workout_streaks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to get comprehensive dashboard stats
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
    -- FIXED: Calculate week properly (Monday to Sunday including current day)
    -- PostgreSQL DOW: 0=Sunday, 1=Monday, 2=Tuesday, etc.

    IF EXTRACT(DOW FROM p_current_date) = 0 THEN
        -- If today is Sunday, week started last Monday
        v_week_start := p_current_date - 6;
        v_week_end := p_current_date;
ELSE
        -- Otherwise, calculate Monday of current week
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


-- Function to update workout streaks
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

    -- Get or create streak record
SELECT * INTO v_streak_record
FROM user_workout_streaks
WHERE user_id = p_user_id;

IF NOT FOUND THEN
        -- First workout ever
        INSERT INTO user_workout_streaks (
            user_id, current_streak, longest_streak,
            last_workout_date, streak_start_date
        ) VALUES (
            p_user_id, 1, 1,
            p_workout_date, p_workout_date
        ) RETURNING * INTO v_streak_record;

        v_is_new_record := TRUE;
ELSE
        -- Check if this is the same day (no change needed)
        IF v_streak_record.last_workout_date = p_workout_date THEN
            -- Same day, no streak update needed
            RETURN QUERY SELECT
                             v_streak_record.current_streak,
                             v_streak_record.longest_streak,
                             FALSE;
RETURN;
END IF;

        -- Check if consecutive day
        IF v_streak_record.last_workout_date = v_yesterday THEN
            -- Consecutive day - increment streak
            v_streak_record.current_streak := v_streak_record.current_streak + 1;
ELSE
            -- Gap in workouts - reset streak
            v_streak_record.current_streak := 1;
            v_streak_record.streak_start_date := p_workout_date;
END IF;

        -- Update longest streak if current is longer
        IF v_streak_record.current_streak > v_streak_record.longest_streak THEN
            v_streak_record.longest_streak := v_streak_record.current_streak;
            v_is_new_record := TRUE;
END IF;

        -- Update the record
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

-- Function to get workout calendar data (for heatmap)
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
    -- Intensity level for heatmap (0-4)
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

-- Function to get workout trends (for charts)
-- Fixed function to get workout trends (for charts)
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
        TO_CHAR(DATE_TRUNC('month', sw.scheduled_date), 'Mon YYYY')::VARCHAR(20) as period_label, -- Cast to VARCHAR
        COUNT(*)::INTEGER as workout_count,
        COALESCE(SUM(sw.calories_burned), 0)::INTEGER as total_calories,
        ROUND(AVG(sw.actual_duration_minutes), 1) as avg_duration,
    ROUND(AVG(sw.overall_rating), 1) as avg_rating
FROM scheduled_workouts sw
WHERE sw.user_id = p_user_id
  AND sw.status = 'COMPLETED'
  AND sw.scheduled_date BETWEEN p_start_date AND p_end_date
GROUP BY DATE_TRUNC('month', sw.scheduled_date)cke
ORDER BY DATE_TRUNC('month', sw.scheduled_date);
END IF;
END;
$$;

-- Function to get workout type breakdown
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
    -- Get total workouts for percentage calculation
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
        v_total_workouts := 1; -- Prevent division by zero
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

-- Trigger to automatically update streaks when workout is completed
CREATE OR REPLACE FUNCTION trigger_update_streak_on_workout_completion()
RETURNS TRIGGER AS $$
BEGIN
    -- Only update streak when status changes to COMPLETED
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

-- Function to get recent achievements/milestones
CREATE OR REPLACE FUNCTION get_recent_achievements(
    p_user_id BIGINT,
    p_days_back INTEGER DEFAULT 30
)
RETURNS TABLE(
    achievement_type VARCHAR(50),
    achievement_title VARCHAR(100),
    achievement_description TEXT,
    achieved_date DATE,
    metric_value INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
v_start_date DATE;
    v_total_workouts INTEGER;
    v_current_streak INTEGER;
    v_total_calories INTEGER;
BEGIN
    v_start_date := CURRENT_DATE - p_days_back;

    -- Get current stats
SELECT
    COUNT(*),
    COALESCE(SUM(calories_burned), 0)
INTO v_total_workouts, v_total_calories
FROM scheduled_workouts
WHERE user_id = p_user_id AND status = 'COMPLETED';

SELECT COALESCE(current_streak, 0)
INTO v_current_streak
FROM user_workout_streaks
WHERE user_id = p_user_id;

RETURN QUERY
-- Streak achievements
SELECT
    'STREAK'::VARCHAR(50),
        ('🔥 ' || v_current_streak || ' Day Streak!')::VARCHAR(100),
        ('You have completed workouts for ' || v_current_streak || ' consecutive days!')::TEXT,
        CURRENT_DATE,
    v_current_streak
    WHERE v_current_streak >= 7 AND v_current_streak % 7 = 0

UNION ALL

-- Workout milestones
SELECT
    'WORKOUT_COUNT'::VARCHAR(50),
        ('🎯 ' || v_total_workouts || ' Workouts Completed!')::VARCHAR(100),
        ('Congratulations on completing ' || v_total_workouts || ' workouts!')::TEXT,
        CURRENT_DATE,
    v_total_workouts
    WHERE v_total_workouts > 0 AND (
        v_total_workouts IN (1, 5, 10, 25, 50, 100, 250, 500) OR
        v_total_workouts % 100 = 0
    )

UNION ALL

-- Calorie milestones
SELECT
    'CALORIES'::VARCHAR(50),
        ('🔥 ' || v_total_calories || ' Calories Burned!')::VARCHAR(100),
        ('You have burned a total of ' || v_total_calories || ' calories!')::TEXT,
        CURRENT_DATE,
    v_total_calories
    WHERE v_total_calories > 0 AND (
        v_total_calories >= 1000 AND
        (v_total_calories % 5000 = 0 OR v_total_calories IN (1000, 2500))
    )

ORDER BY achieved_date DESC, metric_value DESC;
END;
$$;