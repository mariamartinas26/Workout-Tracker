package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.dto.ExerciseStats;
import com.marecca.workoutTracker.dto.WorkoutRecommendationDTO;
import com.marecca.workoutTracker.entity.*;
import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import com.marecca.workoutTracker.repository.*;
import com.marecca.workoutTracker.service.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkoutRecommendationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ScheduledWorkoutRepository scheduledWorkoutRepository;

    @Autowired
    private WorkoutExerciseLogRepository workoutExerciseLogRepository;

    @Autowired
    private WorkoutPlanRepository workoutPlanRepository;

    @Autowired
    private WorkoutExerciseDetailRepository workoutExerciseDetailRepository;

    @Autowired
    private GoalRepository goalRepository;

    /**
     * Workout recommendation
     */
    public List<WorkoutRecommendationDTO> getRecommendations(Long userId, String goalType) {
        try {
            // Validate input parameters
            validateInputParameters(userId, goalType);

            //Get user data
            User user = getUserAndValidate(userId);

            //Count completed workouts in the last 90 days
            int workoutCount = getCompletedWorkoutCount(userId);

            //Calculate strength multiplier based on experience
            BigDecimal strengthMultiplier = calculateStrengthMultiplier(workoutCount);

            //Get exercises suitable for user's fitness level
            List<Exercise> suitableExercises = getSuitableExercises(user.getFitnessLevel());

            //Calculate exercise statistics and scores
            List<WorkoutRecommendationDTO> recommendations = calculateRecommendations(userId, goalType, user, strengthMultiplier, suitableExercises);

            //Filter and sort by priority score
            List<WorkoutRecommendationDTO> filteredRecommendations = recommendations.stream()
                    .filter(rec -> rec.getPriorityScore().compareTo(BigDecimal.valueOf(1.0)) > 0) //keep recommendations with a priority score >1
                    .sorted((r1, r2) -> r2.getPriorityScore().compareTo(r1.getPriorityScore())) //sort in descending order
                    .limit(8) //take the first 8 exercises
                    .collect(Collectors.toList()); //collect the final results into a list

            if (filteredRecommendations.isEmpty()) {
                throw new NoExercisesFoundException("No suitable exercises found for user " + userId + " with goal type " + goalType + " and fitness level " + user.getFitnessLevel());
            }
            return filteredRecommendations;

        } catch (UserNotFoundException | InvalidGoalTypeException |
                 InvalidUserDataException | NoExercisesFoundException |
                 IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while generating recommendations", e);
        }
    }

    private void validateInputParameters(Long userId, String goalType) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number, received: " + userId);
        }

        if (goalType == null) {
            throw new IllegalArgumentException("Goal type cannot be null");
        }

        if (!Arrays.asList("WEIGHT_LOSS", "MUSCLE_GAIN", "MAINTENANCE").contains(goalType)) {
            throw new InvalidGoalTypeException("Invalid goal type: " + goalType + ". Valid options are: WEIGHT_LOSS, MUSCLE_GAIN, MAINTENANCE");
        }
    }

    private User getUserAndValidate(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (user.getFitnessLevel() == null) {
            throw new InvalidUserDataException("User fitness level is null for user ID: " + userId);
        }

        if (user.getWeightKg() == null || user.getWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidUserDataException("User weight is invalid for user ID: " + userId + ". Weight: " + user.getWeightKg());
        }
        return user;
    }

    private int getCompletedWorkoutCount(Long userId) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(90);
        return (int) scheduledWorkoutRepository.countWorkoutsByUserStatusAndDate(
                userId, WorkoutStatusType.COMPLETED, startDate);
    }

    private BigDecimal calculateStrengthMultiplier(int workoutCount) {
        if (workoutCount > 50) return BigDecimal.valueOf(1.3); // very advanced
        if (workoutCount > 20) return BigDecimal.valueOf(1.15); // advanced
        if (workoutCount > 5) return BigDecimal.valueOf(1.0); // medium
        return BigDecimal.valueOf(0.8); // beginner
    }

    private List<Exercise> getSuitableExercises(String fitnessLevel) {
        int maxDifficulty;
        switch (fitnessLevel) {
            case "BEGINNER":
                maxDifficulty = 3;
                break;
            case "INTERMEDIATE":
                maxDifficulty = 4;
                break;
            default:
                maxDifficulty = 5;
                break;
        }

        return exerciseRepository.findByDifficultyLevelLessThanEqual(maxDifficulty);
    }

    /**
     * for each suitable exercise, computes user stats for that exercise
     * checks if the exersise was done in the last 7 days
     * calculates priority score
     * gets final recommendation
     */
    private List<WorkoutRecommendationDTO> calculateRecommendations(Long userId, String goalType, User user, BigDecimal strengthMultiplier, List<Exercise> exercises) {

        List<WorkoutRecommendationDTO> recommendations = new ArrayList<>();
        LocalDateTime recentDate = LocalDateTime.now().minusDays(7);

        for (Exercise exercise : exercises) {
            //Get exercise statistics
            ExerciseStats stats = calculateExerciseStats(userId, exercise);

            // Check if exercise was done recently
            boolean doneRecently = workoutExerciseLogRepository.existsRecentExerciseLog(userId, exercise.getExerciseId(), recentDate);


            // Calculate priority score
            BigDecimal priorityScore = calculatePriorityScore(goalType, exercise, stats, doneRecently);

            // Create recommendation
            WorkoutRecommendationDTO recommendation = createRecommendation(exercise, stats, goalType, user, strengthMultiplier, priorityScore);

            recommendations.add(recommendation);
        }

        return recommendations;
    }

    private ExerciseStats calculateExerciseStats(Long userId, Exercise exercise) {
        List<WorkoutExerciseLog> logs = workoutExerciseLogRepository.findLogsByUserExerciseAndStatus(userId, exercise.getExerciseId(), WorkoutStatusType.COMPLETED);

        ExerciseStats stats = new ExerciseStats();
        stats.setTimesPerformed(logs.size());

        if (!logs.isEmpty()) {
            //avg weight used
            stats.setAvgWeightUsed(logs.stream()
                    .filter(log -> log.getWeightUsedKg() != null) //eliminates null values
                    .map(WorkoutExerciseLog::getWeightUsedKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)//adds all weights into one value
                    .divide(BigDecimal.valueOf(logs.size()), 2, RoundingMode.HALF_UP)); //avg

            //avg reps
            stats.setAvgReps(logs.stream()
                    .filter(log -> log.getRepsCompleted() != null)
                    .mapToInt(WorkoutExerciseLog::getRepsCompleted)
                    .average()
                    .orElse(0.0));

            //avg sets
            stats.setAvgSets(logs.stream()
                    .filter(log -> log.getSetsCompleted() != null)
                    .mapToInt(WorkoutExerciseLog::getSetsCompleted)
                    .average()
                    .orElse(0.0));

            //avg difficulty
            stats.setAvgDifficulty(logs.stream()
                    .filter(log -> log.getDifficultyRating() != null)
                    .mapToInt(WorkoutExerciseLog::getDifficultyRating)
                    .average()
                    .orElse(3.0)); //default difficulty 3
        } else {
            stats.setAvgWeightUsed(BigDecimal.ZERO);
            stats.setAvgReps(0.0);
            stats.setAvgSets(0.0);
            stats.setAvgDifficulty(3.0);
        }

        return stats;
    }

    /**
     * method for calculating priority score
     */
    private BigDecimal calculatePriorityScore(String goalType, Exercise exercise, ExerciseStats stats, boolean doneRecently) {
        // Calculate estimated calories per minute
        BigDecimal caloriesPerMinute = calculateCaloriesPerMinute(exercise);

        // Calculate muscle building potential
        int muscleBuildingPotential = calculateMuscleBuildingPotential(exercise);

        // Calculate cardio effectiveness
        int cardioEffectiveness = calculateCardioEffectiveness(exercise);

        //if the exercise was done in the past 7 days -> -1
        BigDecimal recencyPenalty = doneRecently ? BigDecimal.valueOf(-1.0) : BigDecimal.ZERO;

        //priority score based on goal type
        BigDecimal priorityScore;
        switch (goalType) {
            case "WEIGHT_LOSS":
                priorityScore = BigDecimal.valueOf(cardioEffectiveness)
                        .multiply(BigDecimal.valueOf(0.6))
                        .add(caloriesPerMinute.multiply(BigDecimal.valueOf(0.3)))
                        .add(ExerciseCategoryType.CARDIO.equals(exercise.getCategory()) ? BigDecimal.valueOf(2.0) : BigDecimal.ZERO)
                        .add(recencyPenalty);
                break;

            case "MUSCLE_GAIN":
                priorityScore = BigDecimal.valueOf(muscleBuildingPotential)
                        .multiply(BigDecimal.valueOf(0.7))
                        .add(ExerciseCategoryType.STRENGTH.equals(exercise.getCategory()) ? BigDecimal.valueOf(2.0) : BigDecimal.ZERO)
                        .add(Arrays.asList(MuscleGroupType.FULL_BODY, MuscleGroupType.BACK, MuscleGroupType.CHEST, MuscleGroupType.QUADRICEPS)
                                .contains(exercise.getPrimaryMuscleGroup()) ? BigDecimal.valueOf(1.0) : BigDecimal.ZERO)
                        .add(recencyPenalty);
                break;

            default: // MAINTENANCE
                priorityScore = BigDecimal.valueOf(cardioEffectiveness + muscleBuildingPotential)
                        .multiply(BigDecimal.valueOf(0.4))
                        .add(exercise.getDifficultyLevel() <= 3 ? BigDecimal.valueOf(1.0) : BigDecimal.ZERO)
                        .add(recencyPenalty);
                break;
        }

        return priorityScore;
    }

    /**
     * estimates how many calories are burned per minute for a given exercise
     *
     * @param exercise
     * @return
     */
    private BigDecimal calculateCaloriesPerMinute(Exercise exercise) {
        switch (exercise.getCategory()) {
            case CARDIO:
                return BigDecimal.valueOf(12.0); //12 calories per minute
            case STRENGTH:
                MuscleGroupType muscleGroup = exercise.getPrimaryMuscleGroup();
                if (muscleGroup == MuscleGroupType.FULL_BODY) {
                    return BigDecimal.valueOf(8.0);
                } else if (muscleGroup == MuscleGroupType.BACK) {
                    return BigDecimal.valueOf(6.0);
                } else if (muscleGroup == MuscleGroupType.QUADRICEPS) {
                    return BigDecimal.valueOf(7.0);
                } else {
                    return BigDecimal.valueOf(5.0);
                }
            default:
                return BigDecimal.valueOf(4.0);
        }
    }

    /**
     * calculates a muscle-building score
     *
     * @param exercise
     * @return
     */
    private int calculateMuscleBuildingPotential(Exercise exercise) {
        if (ExerciseCategoryType.STRENGTH.equals(exercise.getCategory())) {
            MuscleGroupType muscleGroup = exercise.getPrimaryMuscleGroup();
            if (muscleGroup == MuscleGroupType.FULL_BODY) {
                return 5;
            } else if (muscleGroup == MuscleGroupType.BACK ||
                    muscleGroup == MuscleGroupType.CHEST ||
                    muscleGroup == MuscleGroupType.QUADRICEPS) {
                return 4;
            } else {
                return 3;
            }
        }
        return 2;
    }

    private int calculateCardioEffectiveness(Exercise exercise) {
        switch (exercise.getCategory()) {
            case CARDIO:
                return 5;
            case STRENGTH:
                return MuscleGroupType.FULL_BODY.equals(exercise.getPrimaryMuscleGroup()) ? 3 : 2;
            default:
                return 3;
        }
    }

    private WorkoutRecommendationDTO createRecommendation(Exercise exercise, ExerciseStats stats, String goalType, User user, BigDecimal strengthMultiplier, BigDecimal priorityScore) {
        WorkoutRecommendationDTO recommendation = new WorkoutRecommendationDTO();

        recommendation.setExerciseId(exercise.getExerciseId());
        recommendation.setExerciseName(exercise.getExerciseName());
        recommendation.setPriorityScore(priorityScore);

        //calculate recommended sets
        recommendation.setRecommendedSets(calculateRecommendedSets(goalType, exercise, strengthMultiplier));

        //calculate recommended reps
        int[] repsRange = calculateRecommendedReps(goalType, exercise, stats);
        recommendation.setRecommendedRepsMin(repsRange[0]);
        recommendation.setRecommendedRepsMax(repsRange[1]);

        //calculate recommended weight percentage
        recommendation.setRecommendedWeightPercentage(calculateRecommendedWeightPercentage(goalType, stats, user, strengthMultiplier));

        //calculate rest time
        recommendation.setRestTimeSeconds(calculateRestTime(goalType, exercise));

        return recommendation;
    }

    private Integer calculateRecommendedSets(String goalType, Exercise exercise, BigDecimal strengthMultiplier) {
        switch (goalType) {
            case "WEIGHT_LOSS":
                if (exercise.getCategory() == ExerciseCategoryType.CARDIO) {
                    return 1;
                } else if (exercise.getCategory() == ExerciseCategoryType.STRENGTH) {
                    return Math.max(2, strengthMultiplier.multiply(BigDecimal.valueOf(3)).intValue());
                } else {
                    return 2;
                }
            case "MUSCLE_GAIN":
                if (exercise.getCategory() == ExerciseCategoryType.STRENGTH) {
                    return Math.max(3, strengthMultiplier.multiply(BigDecimal.valueOf(4)).intValue());
                } else if (exercise.getCategory() == ExerciseCategoryType.CARDIO) {
                    return 1;
                } else {
                    return 3;
                }
            default:
                return 3;
        }
    }

    private int[] calculateRecommendedReps(String goalType, Exercise exercise, ExerciseStats stats) {
        int minReps, maxReps;

        if (stats.getTimesPerformed() > 0 && stats.getAvgReps() > 0) {
            minReps = Math.max(1, (int) Math.round(stats.getAvgReps() * 0.8));
            maxReps = (int) Math.round(stats.getAvgReps() * 1.2);
        } else {
            switch (goalType) {
                case "WEIGHT_LOSS":
                    if (exercise.getCategory() == ExerciseCategoryType.CARDIO) {
                        minReps = maxReps = 1;
                    } else if (exercise.getCategory() == ExerciseCategoryType.STRENGTH) {
                        minReps = 12;
                        maxReps = 15;
                    } else {
                        minReps = 10;
                        maxReps = 15;
                    }
                    break;
                case "MUSCLE_GAIN":
                    if (exercise.getCategory() == ExerciseCategoryType.STRENGTH) {
                        minReps = 6;
                        maxReps = 12;
                    } else if (exercise.getCategory() == ExerciseCategoryType.CARDIO) {
                        minReps = maxReps = 1;
                    } else {
                        minReps = 8;
                        maxReps = 12;
                    }
                    break;
                default:
                    minReps = 10;
                    maxReps = 15;
                    break;
            }
        }

        return new int[]{minReps, maxReps};
    }

    private BigDecimal calculateRecommendedWeightPercentage(String goalType, ExerciseStats stats, User user, BigDecimal strengthMultiplier) {
        if (stats.getTimesPerformed() > 0 && stats.getAvgWeightUsed().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal userWeight = user.getWeightKg().max(BigDecimal.valueOf(50));
            BigDecimal basePercentage = stats.getAvgWeightUsed()
                    .divide(userWeight, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            switch (goalType) {
                case "MUSCLE_GAIN":
                    return basePercentage.multiply(BigDecimal.valueOf(1.1)).min(BigDecimal.valueOf(100.0));
                case "WEIGHT_LOSS":
                    return basePercentage.multiply(BigDecimal.valueOf(0.9)).min(BigDecimal.valueOf(90.0));
                default:
                    return basePercentage.min(BigDecimal.valueOf(95.0));
            }
        } else {
            switch (goalType) {
                case "MUSCLE_GAIN":
                    return BigDecimal.valueOf(80).multiply(strengthMultiplier);
                case "WEIGHT_LOSS":
                    return BigDecimal.valueOf(65).multiply(strengthMultiplier);
                default:
                    return BigDecimal.valueOf(70).multiply(strengthMultiplier);
            }
        }
    }

    private Integer calculateRestTime(String goalType, Exercise exercise) {
        if (exercise.getCategory() == ExerciseCategoryType.CARDIO) {
            return 30;
        } else if (exercise.getCategory() == ExerciseCategoryType.STRENGTH) {
            switch (goalType) {
                case "MUSCLE_GAIN":
                    return 120;
                case "WEIGHT_LOSS":
                    return 45;
                default:
                    return 90;
            }
        } else {
            return 60;
        }
    }

    public Map<String, Object> saveWorkoutPlan(Long userId, List<WorkoutRecommendationDTO> recommendations, Long goalId, String planName) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " does not exist"));

            if (recommendations == null || recommendations.isEmpty()) {
                throw new IllegalArgumentException("Cannot save workout plan without recommendations");
            }

            // Generate plan name based on goal type
            if (planName == null || planName.trim().isEmpty()) {
                planName = generatePlanNameFromGoal(goalId);
            }

            // Check if workout plan already exists
            Optional<WorkoutPlan> existingPlan = workoutPlanRepository.findByUserAndPlanName(userId, planName);
            if (existingPlan.isPresent()) {
                return updateExistingWorkoutPlan(existingPlan.get(), recommendations, goalId);
            }

            // Calculate estimated duration
            int estimatedDuration = calculateEstimatedDuration(recommendations);

            // Create and save workout plan
            WorkoutPlan workoutPlan = WorkoutPlan.builder()
                    .user(user)
                    .planName(planName)
                    .description("Generated workout plan based on personalized recommendations")
                    .estimatedDurationMinutes(estimatedDuration)
                    .difficultyLevel(calculateAverageDifficulty(recommendations))
                    .goals(goalId != null ? "Goal ID: " + goalId : "General fitness improvement")
                    .notes("Generated with " + recommendations.size() + " exercises")
                    .build();

            workoutPlan = workoutPlanRepository.save(workoutPlan);

            // Insert exercise details
            insertWorkoutExerciseDetails(workoutPlan, recommendations);

            Map<String, Object> result = new HashMap<>();
            result.put("workoutPlanId", workoutPlan.getWorkoutPlanId());
            result.put("planName", planName);
            result.put("description", workoutPlan.getDescription());
            result.put("estimatedDurationMinutes", estimatedDuration);
            result.put("exerciseCount", recommendations.size());
            result.put("createdAt", workoutPlan.getCreatedAt());
            result.put("userId", userId);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save workout plan: " + e.getMessage(), e);
        }
    }

    private String generatePlanNameFromGoal(Long goalId) {
        if (goalId == null) {
            return "Recommended Workout";
        }

        try {
            Optional<Goal> goal = goalRepository.findById(goalId);
            if (goal.isPresent()) {
                String goalTypeDisplay = mapGoalTypeToDisplay(goal.get().getGoalType().getValue());
                return "Recommended Workout for " + goalTypeDisplay;
            }
            return "Recommended Workout";
        } catch (Exception e) {
            return "Recommended Workout";
        }
    }

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


    private int calculateEstimatedDuration(List<WorkoutRecommendationDTO> recommendations) {
        int totalMinutes = 0;

        for (WorkoutRecommendationDTO rec : recommendations) {
            // 2 minutes per set + rest time
            int setsTime = (rec.getRecommendedSets() != null ? rec.getRecommendedSets() : 3) * 2;
            int restTime = (rec.getRestTimeSeconds() != null ? rec.getRestTimeSeconds() : 60) * (rec.getRecommendedSets() != null ? rec.getRecommendedSets() : 3) / 60;
            totalMinutes += setsTime + restTime;
        }

        // Add 10 minutes for warm up
        return Math.max(totalMinutes + 10, 30); // minimum 30 mins
    }

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

    private void insertWorkoutExerciseDetails(WorkoutPlan workoutPlan, List<WorkoutRecommendationDTO> recommendations) {
        for (int i = 0; i < recommendations.size(); i++) {
            WorkoutRecommendationDTO rec = recommendations.get(i);

            Exercise exercise = exerciseRepository.findById(rec.getExerciseId())
                    .orElseThrow(() -> new RuntimeException("Exercise not found with ID: " + rec.getExerciseId()));

            String notes = String.format("Recommendation - Priority Score: %.2f, Weight: %s%%",
                    rec.getPriorityScore() != null ? rec.getPriorityScore().doubleValue() : 0.0,
                    rec.getRecommendedWeightPercentage() != null ? rec.getRecommendedWeightPercentage().toString() : "N/A");

            WorkoutExerciseDetail detail = WorkoutExerciseDetail.builder()
                    .workoutPlan(workoutPlan)
                    .exercise(exercise)
                    .exerciseOrder(i + 1)
                    .targetSets(rec.getRecommendedSets())
                    .targetRepsMin(rec.getRecommendedRepsMin())
                    .targetRepsMax(rec.getRecommendedRepsMax())
                    .targetWeightKg(null)
                    .restTimeSeconds(rec.getRestTimeSeconds())
                    .notes(notes)
                    .build();

            workoutExerciseDetailRepository.save(detail);
        }
    }

    private Map<String, Object> updateExistingWorkoutPlan(WorkoutPlan workoutPlan,
                                                          List<WorkoutRecommendationDTO> recommendations, Long goalId) {
        try {
            // Calculate new values
            int estimatedDuration = calculateEstimatedDuration(recommendations);
            String description = "Updated workout plan based on personalized recommendations";
            String goals = goalId != null ? "Goal ID: " + goalId : "General fitness improvement";
            String notes = "Updated with " + recommendations.size() + " exercises";

            // Update the existing workout plan
            workoutPlan.setDescription(description);
            workoutPlan.setEstimatedDurationMinutes(estimatedDuration);
            workoutPlan.setDifficultyLevel(calculateAverageDifficulty(recommendations));
            workoutPlan.setGoals(goals);
            workoutPlan.setNotes(notes);

            workoutPlan = workoutPlanRepository.save(workoutPlan);

            // Delete existing exercise details for this workout plan
            workoutExerciseDetailRepository.deleteByWorkoutPlanId(workoutPlan.getWorkoutPlanId());

            // Insert new exercise details
            insertWorkoutExerciseDetails(workoutPlan, recommendations);

            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("workoutPlanId", workoutPlan.getWorkoutPlanId());
            result.put("planName", workoutPlan.getPlanName());
            result.put("description", description);
            result.put("estimatedDurationMinutes", estimatedDuration);
            result.put("exerciseCount", recommendations.size());
            result.put("updatedAt", workoutPlan.getUpdatedAt());
            result.put("userId", workoutPlan.getUser().getUserId());
            result.put("isUpdated", true);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update existing workout plan: " + e.getMessage(), e);
        }
    }
}