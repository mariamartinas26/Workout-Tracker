package com.marecca.workoutTracker.entity;

import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "exercises")
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

    // PostgreSQL are un tip array pentru coloane, JPA va gestiona acest lucru
    @Column(name = "secondary_muscle_groups")
    private MuscleGroupType[] secondaryMuscleGroups;

    @Column(name = "equipment_needed", length = 200)
    private String equipmentNeeded;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel = 1;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "safety_tips", columnDefinition = "TEXT")
    private String safetyTips;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Relații
    @OneToMany(mappedBy = "exercise")
    private List<WorkoutExerciseDetail> workoutExerciseDetails = new ArrayList<>();

    @OneToMany(mappedBy = "exercise")
    private List<WorkoutExerciseLog> workoutExerciseLogs = new ArrayList<>();
}