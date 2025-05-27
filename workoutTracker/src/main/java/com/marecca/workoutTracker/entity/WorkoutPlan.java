package com.marecca.workoutTracker.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "workout_plans")
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workout_plan_id")
    private Long workoutPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_name", length = 100, nullable = false)
    private String planName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel = 1;

    @Column(name = "goals", columnDefinition = "TEXT")
    private String goals;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_template")
    private Boolean isTemplate = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relații
    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutExerciseDetail> exerciseDetails = new ArrayList<>();

    @OneToMany(mappedBy = "workoutPlan")
    private List<ScheduledWorkout> scheduledWorkouts = new ArrayList<>();
}