package com.marecca.workoutTracker.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_exercise_logs")
@Getter
@Setter
@NoArgsConstructor
public class WorkoutExerciseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_workout_id", nullable = false)
    private ScheduledWorkout scheduledWorkout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "exercise_order", nullable = false)
    private Integer exerciseOrder;

    @Column(name = "sets_completed", nullable = false)
    private Integer setsCompleted;

    @Column(name = "reps_completed")
    private Integer repsCompleted;

    @Column(name = "weight_used_kg", precision = 6, scale = 2)
    private BigDecimal weightUsedKg;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters", precision = 8, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "calories_burned")
    private Integer caloriesBurned;

    @Column(name = "difficulty_rating")
    private Integer difficultyRating;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
