package com.marecca.workoutTracker.entity;

import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Exercise entity coresponding to the exercises table in the database
 */
@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(name = "exercise_name", length = 100, nullable = false, unique = true)
    private String exerciseName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExerciseCategoryType category;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_muscle_group", nullable = false)
    private MuscleGroupType primaryMuscleGroup;

    /**
     * This field maps to a SQL ARRAY type in the database
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "secondary_muscle_groups", columnDefinition = "muscle_group_type[]")
    @Builder.Default
    private List<MuscleGroupType> secondaryMuscleGroups = new ArrayList<>();


    @Column(name = "equipment_needed", length = 200)
    private String equipment;

    @Column(name = "difficulty_level")
    @Builder.Default
    private Integer difficultyLevel = 1;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<WorkoutExerciseDetail> workoutExerciseDetails = new ArrayList<>();

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<WorkoutExerciseLog> workoutExerciseLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.secondaryMuscleGroups == null) {
            this.secondaryMuscleGroups = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.secondaryMuscleGroups == null) {
            this.secondaryMuscleGroups = new ArrayList<>();
        }
    }

    public String getEquipmentNeeded() {
        return this.equipment;
    }

    public void setEquipmentNeeded(String equipment) {
        this.equipment = equipment;
    }
}