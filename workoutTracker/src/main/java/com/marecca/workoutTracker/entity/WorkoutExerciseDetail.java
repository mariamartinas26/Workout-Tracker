package com.marecca.workoutTracker.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_exercise_details")
@Getter
@Setter
@NoArgsConstructor
public class WorkoutExerciseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workout_exercise_detail_id")
    private Long workoutExerciseDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    private WorkoutPlan workoutPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "exercise_order", nullable = false)
    private Integer exerciseOrder;

    @Column(name = "target_sets", nullable = false)
    private Integer targetSets;

    @Column(name = "target_reps_min")
    private Integer targetRepsMin;

    @Column(name = "target_reps_max")
    private Integer targetRepsMax;

    @Column(name = "target_weight_kg", precision = 6, scale = 2)
    private BigDecimal targetWeightKg;

    @Column(name = "target_duration_seconds")
    private Integer targetDurationSeconds;

    @Column(name = "target_distance_meters", precision = 8, scale = 2)
    private BigDecimal targetDistanceMeters;

    @Column(name = "rest_time_seconds")
    private Integer restTimeSeconds = 60;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}