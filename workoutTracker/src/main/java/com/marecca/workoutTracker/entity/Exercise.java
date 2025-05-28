package com.marecca.workoutTracker.entity;

import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    //one-to-many relationship bewtween exercises and muscle groups
    @ElementCollection(targetClass = MuscleGroupType.class)
    @CollectionTable(name = "exercise_secondary_muscles", joinColumns = @JoinColumn(name = "exercise_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group")
    private List<MuscleGroupType> secondaryMuscleGroups;


    @Column(name = "equipment_needed", length = 200)
    private String equipmentNeeded;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel = 1;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "exercise")
    private List<WorkoutExerciseDetail> workoutExerciseDetails = new ArrayList<>();

    @OneToMany(mappedBy = "exercise")
    private List<WorkoutExerciseLog> workoutExerciseLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}