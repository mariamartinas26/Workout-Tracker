package com.marecca.workoutTracker.dto;

import lombok.Setter;

import java.math.BigDecimal;

/**
 * class for storing exercise statistics used in workout recommendations
 */
public class ExerciseStats {

    /**
     * Number of times the user has performed this exercise
     */
    @Setter
    private int timesPerformed;

    /**
     * Average weight used by the user for this exercise
     */
    private BigDecimal avgWeightUsed = BigDecimal.ZERO;

    /**
     * Average number of repetitions completed by the user
     */
    @Setter
    private double avgReps;

    /**
     * Average number of sets completed by the user
     */
    @Setter
    private double avgSets;

    /**
     * Average difficulty rating given by the user (1-5 scale)
     */
    @Setter
    private double avgDifficulty = 3.0;

    public ExerciseStats() {}

    public ExerciseStats(int timesPerformed, BigDecimal avgWeightUsed,
                         double avgReps, double avgSets, double avgDifficulty) {
        this.timesPerformed = timesPerformed;
        this.avgWeightUsed = avgWeightUsed != null ? avgWeightUsed : BigDecimal.ZERO;
        this.avgReps = avgReps;
        this.avgSets = avgSets;
        this.avgDifficulty = avgDifficulty;
    }

    public int getTimesPerformed() {
        return timesPerformed;
    }

    public BigDecimal getAvgWeightUsed() {
        return avgWeightUsed;
    }

    public void setAvgWeightUsed(BigDecimal avgWeightUsed) {
        this.avgWeightUsed = avgWeightUsed != null ? avgWeightUsed : BigDecimal.ZERO;
    }

    public double getAvgReps() {
        return avgReps;
    }

    public double getAvgSets() {
        return avgSets;
    }

    public double getAvgDifficulty() {
        return avgDifficulty;
    }


    /**
     * Check if the user has any experience with this exercise
     */
    public boolean hasExperience() {
        return timesPerformed > 0;
    }

    /**
     * Check if the user has weight data for this exercise
     */
    public boolean hasWeightData() {
        return avgWeightUsed != null && avgWeightUsed.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if the user has rep data for this exercise
     */
    public boolean hasRepData() {
        return avgReps > 0;
    }

    /**
     * Get a summary of the exercise experience level
     */
    public String getExperienceLevel() {
        if (timesPerformed == 0) return "No experience";
        if (timesPerformed <= 5) return "Beginner";
        if (timesPerformed <= 15) return "Intermediate";
        return "Experienced";
    }

    @Override
    public String toString() {
        return "ExerciseStats{" +
                "timesPerformed=" + timesPerformed +
                ", avgWeightUsed=" + avgWeightUsed +
                ", avgReps=" + avgReps +
                ", avgSets=" + avgSets +
                ", avgDifficulty=" + avgDifficulty +
                '}';
    }
}