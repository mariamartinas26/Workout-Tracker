-- Script pentru popularea tabelei exercises cu date de test
BEGIN;

-- Exerciții pentru CHEST (Piept)
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Push-ups', 'Classic bodyweight exercise for chest development', 'STRENGTH', 'CHEST', ARRAY['TRICEPS', 'SHOULDERS']::muscle_group_type[], NULL, 2,
 'Start in plank position. Lower body until chest nearly touches ground. Push back up to starting position.'),

('Bench Press', 'Fundamental chest exercise using barbell', 'STRENGTH', 'CHEST', ARRAY['TRICEPS', 'SHOULDERS']::muscle_group_type[], 'Barbell, Bench', 3,
 'Lie on bench, grip barbell wider than shoulders. Lower to chest, then press up explosively.'),

('Dumbbell Flyes', 'Isolation exercise for chest muscles', 'STRENGTH', 'CHEST', ARRAY['SHOULDERS']::muscle_group_type[], 'Dumbbells, Bench', 3,
 'Lie on bench with dumbbells. Lower weights in arc motion, then bring back together above chest.'),

('Incline Dumbbell Press', 'Upper chest focused pressing movement', 'STRENGTH', 'CHEST', ARRAY['TRICEPS', 'SHOULDERS']::muscle_group_type[], 'Dumbbells, Incline Bench', 4,
 'Set bench to 30-45 degrees. Press dumbbells from chest level to overhead.'),

('Chest Dips', 'Advanced bodyweight chest exercise', 'STRENGTH', 'CHEST', ARRAY['TRICEPS', 'SHOULDERS']::muscle_group_type[], 'Dip Bars', 4,
 'Support body on dip bars. Lower until shoulders below elbows, then push back up.');

-- Exerciții pentru BACK (Spate)
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Pull-ups', 'Classic back and bicep exercise', 'STRENGTH', 'BACK', ARRAY['BICEPS', 'SHOULDERS']::muscle_group_type[], 'Pull-up Bar', 4,
 'Hang from bar with overhand grip. Pull body up until chin over bar, lower with control.'),

('Bent-over Rows', 'Compound back exercise with barbell', 'STRENGTH', 'BACK', ARRAY['BICEPS', 'SHOULDERS']::muscle_group_type[], 'Barbell', 3,
 'Bend at hips holding barbell. Pull bar to lower chest, squeeze shoulder blades together.'),

('Lat Pulldowns', 'Machine-based back exercise', 'STRENGTH', 'BACK', ARRAY['BICEPS']::muscle_group_type[], 'Cable Machine', 2,
 'Sit at lat pulldown machine. Pull bar down to chest while leaning slightly back.'),

('Deadlifts', 'Full-body compound movement', 'STRENGTH', 'BACK', ARRAY['GLUTES', 'HAMSTRINGS']::muscle_group_type[], 'Barbell', 5,
 'Stand with feet hip-width apart. Lift barbell from ground by extending hips and knees.'),

('T-Bar Rows', 'Thick grip rowing exercise', 'STRENGTH', 'BACK', ARRAY['BICEPS', 'SHOULDERS']::muscle_group_type[], 'T-Bar, Plates', 3,
 'Straddle T-bar, bend at hips. Pull weight to chest, focus on squeezing shoulder blades.');

-- Exerciții pentru LEGS (Picioare)
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Squats', 'Fundamental leg exercise', 'STRENGTH', 'QUADRICEPS', ARRAY['GLUTES', 'HAMSTRINGS']::muscle_group_type[], 'Barbell, Squat Rack', 3,
 'Stand with feet shoulder-width apart. Lower into squat position, then drive through heels to stand.'),

('Lunges', 'Single-leg functional movement', 'STRENGTH', 'QUADRICEPS', ARRAY['GLUTES', 'HAMSTRINGS']::muscle_group_type[], NULL, 2,
 'Step forward into lunge position. Lower until both knees at 90 degrees, return to standing.'),

('Romanian Deadlifts', 'Hip-hinge movement for hamstrings', 'STRENGTH', 'HAMSTRINGS', ARRAY['GLUTES', 'BACK']::muscle_group_type[], 'Barbell', 3,
 'Hold barbell at hip level. Hinge at hips, lower weight while keeping back straight.'),

('Calf Raises', 'Isolation exercise for calves', 'STRENGTH', 'CALVES', NULL, 'Dumbbells or Machine', 1,
 'Stand on balls of feet. Raise heels as high as possible, lower with control.'),

('Bulgarian Split Squats', 'Advanced single-leg exercise', 'STRENGTH', 'QUADRICEPS', ARRAY['GLUTES']::muscle_group_type[], 'Bench', 4,
 'Place rear foot on bench. Lower into single-leg squat, drive through front heel to return.');

-- Exerciții pentru SHOULDERS (Umeri)
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Shoulder Press', 'Overhead pressing movement', 'STRENGTH', 'SHOULDERS', ARRAY['TRICEPS']::muscle_group_type[], 'Dumbbells or Barbell', 3,
 'Press weights from shoulder level to overhead. Lower with control to starting position.'),

('Lateral Raises', 'Isolation exercise for side delts', 'STRENGTH', 'SHOULDERS', NULL, 'Dumbbells', 2,
 'Hold dumbbells at sides. Raise arms to shoulder height, lower slowly.'),

('Front Raises', 'Isolation for front deltoids', 'STRENGTH', 'SHOULDERS', NULL, 'Dumbbells', 2,
 'Hold dumbbells in front of thighs. Raise arms forward to shoulder height.'),

('Reverse Flyes', 'Rear deltoid isolation exercise', 'STRENGTH', 'SHOULDERS', ARRAY['BACK']::muscle_group_type[], 'Dumbbells', 2,
 'Bend forward slightly. Raise dumbbells out to sides, squeezing shoulder blades.'),

('Handstand Push-ups', 'Advanced bodyweight shoulder exercise', 'STRENGTH', 'SHOULDERS', ARRAY['TRICEPS']::muscle_group_type[], 'Wall', 5,
 'In handstand position against wall. Lower head toward ground, press back to starting position.');

-- Exerciții pentru ARMS (Brațe)
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Bicep Curls', 'Classic bicep isolation exercise', 'STRENGTH', 'BICEPS', NULL, 'Dumbbells or Barbell', 1,
 'Hold weights with arms extended. Curl weights toward shoulders, lower slowly.'),

('Tricep Dips', 'Bodyweight tricep exercise', 'STRENGTH', 'TRICEPS', ARRAY['SHOULDERS']::muscle_group_type[], 'Bench or Chair', 2,
 'Support body weight on bench. Lower body by bending elbows, push back up.'),

('Hammer Curls', 'Neutral grip bicep exercise', 'STRENGTH', 'BICEPS', ARRAY['FOREARMS']::muscle_group_type[], 'Dumbbells', 2,
 'Hold dumbbells with neutral grip. Curl weights keeping thumbs up throughout movement.'),

('Tricep Extensions', 'Overhead tricep isolation', 'STRENGTH', 'TRICEPS', NULL, 'Dumbbell', 2,
 'Hold dumbbell overhead with both hands. Lower behind head, extend back to starting position.'),

('Wrist Curls', 'Forearm strengthening exercise', 'STRENGTH', 'FOREARMS', NULL, 'Light Dumbbells', 1,
 'Rest forearms on bench with wrists hanging over. Curl wrists up and down.');

-- Exerciții pentru ABS (Abdomen)
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Crunches', 'Basic abdominal exercise', 'STRENGTH', 'ABS', NULL, NULL, 1,
 'Lie on back with knees bent. Lift shoulders off ground, contract abs, lower slowly.'),

('Planks', 'Isometric core strengthening', 'STRENGTH', 'ABS', ARRAY['BACK', 'SHOULDERS']::muscle_group_type[], NULL, 2,
 'Hold push-up position with forearms on ground. Keep body straight, engage core.'),

('Russian Twists', 'Rotational core exercise', 'STRENGTH', 'ABS', NULL, 'Medicine Ball (optional)', 2,
 'Sit with knees bent, lean back slightly. Rotate torso side to side, touching ground.'),

('Mountain Climbers', 'Dynamic core and cardio exercise', 'CARDIO', 'ABS', ARRAY['SHOULDERS']::muscle_group_type[], NULL, 3,
 'Start in plank position. Alternate bringing knees toward chest rapidly.'),

('Dead Bug', 'Core stability exercise', 'STRENGTH', 'ABS', NULL, NULL, 2,
 'Lie on back with arms up and knees bent. Lower opposite arm and leg, return to start.');

-- Exerciții CARDIO
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Running', 'Classic cardiovascular exercise', 'CARDIO', 'CARDIO', ARRAY['QUADRICEPS', 'HAMSTRINGS', 'CALVES']::muscle_group_type[], 'Running Shoes', 2,
 'Maintain steady pace for desired duration. Focus on proper breathing and form.'),

('Burpees', 'Full-body explosive exercise', 'CARDIO', 'FULL_BODY', ARRAY['CHEST', 'SHOULDERS', 'ABS']::muscle_group_type[], NULL, 4,
 'Drop to push-up, perform push-up, jump feet to hands, jump up with arms overhead.'),

('Jumping Jacks', 'Simple cardio movement', 'CARDIO', 'CARDIO', ARRAY['SHOULDERS']::muscle_group_type[], NULL, 1,
 'Jump feet apart while raising arms overhead. Jump back to starting position.'),

('High Knees', 'Running in place variation', 'CARDIO', 'CARDIO', ARRAY['ABS', 'QUADRICEPS']::muscle_group_type[], NULL, 2,
 'Run in place bringing knees up toward chest. Maintain rapid pace.'),

('Cycling', 'Low-impact cardiovascular exercise', 'CARDIO', 'CARDIO', ARRAY['QUADRICEPS', 'HAMSTRINGS']::muscle_group_type[], 'Bicycle or Stationary Bike', 2,
 'Maintain steady cycling pace. Adjust resistance or incline for intensity.');

-- Exerciții pentru FLEXIBILITY
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Forward Fold', 'Hamstring and back stretch', 'FLEXIBILITY', 'HAMSTRINGS', ARRAY['BACK']::muscle_group_type[], NULL, 1,
 'Stand with feet hip-width apart. Fold forward from hips, reaching toward ground.'),

('Downward Dog', 'Full-body yoga stretch', 'FLEXIBILITY', 'SHOULDERS', ARRAY['HAMSTRINGS', 'CALVES']::muscle_group_type[], 'Yoga Mat', 2,
 'Start on hands and knees. Push hips up forming inverted V shape.'),

('Child''s Pose', 'Relaxing yoga stretch', 'FLEXIBILITY', 'BACK', ARRAY['SHOULDERS']::muscle_group_type[], 'Yoga Mat', 1,
 'Kneel and sit back on heels. Fold forward with arms extended in front.'),

('Pigeon Pose', 'Hip opening stretch', 'FLEXIBILITY', 'GLUTES', NULL, 'Yoga Mat', 3,
 'From downward dog, bring one knee forward. Extend back leg and fold forward.'),

('Cat-Cow Stretch', 'Spinal mobility exercise', 'FLEXIBILITY', 'BACK', NULL, 'Yoga Mat', 1,
 'On hands and knees, alternate between arching and rounding the spine.');

-- Exerciții pentru BALANCE
INSERT INTO exercises (exercise_name, description, category, primary_muscle_group, secondary_muscle_groups, equipment_needed, difficulty_level, instructions) VALUES
('Single Leg Stand', 'Basic balance exercise', 'BALANCE', 'CALVES', ARRAY['ABS']::muscle_group_type[], NULL, 1,
 'Stand on one leg for specified time. Focus on a fixed point ahead.'),

('Tree Pose', 'Yoga balance posture', 'BALANCE', 'CALVES', ARRAY['ABS']::muscle_group_type[], NULL, 2,
 'Stand on one leg, place other foot on inner thigh. Hands in prayer position.'),

('Warrior III', 'Advanced yoga balance pose', 'BALANCE', 'GLUTES', ARRAY['ABS', 'BACK']::muscle_group_type[], NULL, 4,
 'Balance on one leg with torso parallel to ground. Extend arms and back leg.'),

('Bosu Ball Squats', 'Unstable surface squats', 'BALANCE', 'QUADRICEPS', ARRAY['ABS', 'GLUTES']::muscle_group_type[], 'BOSU Ball', 3,
 'Perform squats while standing on BOSU ball. Focus on maintaining balance.'),

('Single Leg Deadlifts', 'Balance and strength combination', 'BALANCE', 'HAMSTRINGS', ARRAY['GLUTES', 'ABS']::muscle_group_type[], 'Light Dumbbells', 3,
 'Balance on one leg while hinging at hip. Touch weight toward ground and return.');

COMMIT;