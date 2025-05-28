package com.marecca.workoutTracker.entity;

import com.marecca.workoutTracker.entity.enums.WorkoutStatusType;
import com.marecca.workoutTracker.repository.WorkoutPlanRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scheduled_workouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheduled_workout_id")
    private Long scheduledWorkoutId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id")
    private WorkoutPlan workoutPlan;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // This tells Hibernate to treat it as a named DB enum type
    @Column(name = "status", nullable = false) // Ensure this matches your DB schema (e.g., NOT NULL)
    private WorkoutStatusType status = WorkoutStatusType.PLANNED;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "calories_burned")
    private Integer caloriesBurned;

    @Column(name = "overall_rating")
    private Integer overallRating;

    @Column(name = "energy_level_before")
    private Integer energyLevelBefore;

    @Column(name = "energy_level_after")
    private Integer energyLevelAfter;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "scheduledWorkout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutExerciseLog> exerciseLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addExerciseLog(WorkoutExerciseLog log) {
        exerciseLogs.add(log);
        log.setScheduledWorkout(this);
    }

    public void removeExerciseLog(WorkoutExerciseLog log) {
        exerciseLogs.remove(log);
        log.setScheduledWorkout(null);
    }
}