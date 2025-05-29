package com.marecca.workoutTracker.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Long goalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false)
    private GoalType goalType;

    @Column(name = "target_weight_loss")
    private BigDecimal targetWeightLoss;

    @Column(name = "target_weight_gain")
    private BigDecimal targetWeightGain;

    @Column(name = "current_weight")
    private BigDecimal currentWeight;

    @Column(name = "timeframe_months")
    private Integer timeframeMonths;

    @Column(name = "daily_calorie_deficit")
    private Integer dailyCalorieDeficit;

    @Column(name = "daily_calorie_surplus")
    private Integer dailyCalorieSurplus;

    @Column(name = "weekly_weight_change")
    private BigDecimal weeklyWeightChange;

    @Column(name = "target_weight")
    private BigDecimal targetWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum GoalType {
        LOSE_WEIGHT("lose_weight"),
        GAIN_MUSCLE("gain_muscle"),
        MAINTAIN_HEALTH("maintain_health");

        private final String value;

        GoalType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static GoalType fromValue(String value) {
            for (GoalType type : GoalType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown goal type: " + value);
        }
    }

    public enum GoalStatus {
        ACTIVE,
        COMPLETED,
        PAUSED,
        CANCELLED
    }
}
