package com.marecca.workoutTracker.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marecca.workoutTracker.entity.ScheduledWorkout;
import com.marecca.workoutTracker.service.ScheduledWorkoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller pentru gestionarea workout-urilor programate
 * Oferă endpoint-uri REST pentru programarea și gestionarea workout-urilor
 */
@RestController
@RequestMapping("/api/scheduled-workouts")
@RequiredArgsConstructor
@Slf4j
public class ScheduledWorkoutController {

    private final ScheduledWorkoutService scheduledWorkoutService;

    /**
     * Programează un workout nou
     */
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleWorkout(@Valid @RequestBody ScheduleWorkoutRequest request) {
        try {
            Long scheduledWorkoutId = scheduledWorkoutService.scheduleWorkoutWithValidation(
                    request.getUserId(),
                    request.getWorkoutPlanId(),
                    request.getScheduledDate(),
                    request.getScheduledTime()
            );

            ScheduleWorkoutResponse response = ScheduleWorkoutResponse.builder()
                    .scheduledWorkoutId(scheduledWorkoutId)
                    .message("Workout programat cu succes")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error while scheduling workout: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Unexpected error while scheduling workout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Eroare internă")
                            .message("A apărut o eroare neașteptată")
                            .build());
        }
    }

    /**
     * Find all scheduled workouts for a user
*/
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ScheduledWorkout>> getUserWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findByUserId(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Găsește workout-urile pentru o perioadă specificată

    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<ScheduledWorkout>> getUserWorkoutsBetweenDates(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<ScheduledWorkout> workouts = scheduledWorkoutService.findUserWorkoutsBetweenDates(
                userId, startDate, endDate);
        return ResponseEntity.ok(workouts);
    }
     */
    /**
     * Shows the schedueled workouts for today for a user
     */
    @GetMapping("/user/{userId}/today")
    public ResponseEntity<List<ScheduledWorkout>> getTodaysWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findTodaysWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Începe un workout
     */
    @PutMapping("/{workoutId}/start")
    public ResponseEntity<?> startWorkout(@PathVariable Long workoutId) {
        try {
            scheduledWorkoutService.startWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout început cu succes")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Finalizează un workout
     */
    @PutMapping("/{workoutId}/complete")
    public ResponseEntity<?> completeWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody CompleteWorkoutRequest request) {
        try {
            scheduledWorkoutService.completeWorkout(
                    workoutId,
                    request.getCaloriesBurned(),
                    request.getRating()
            );

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout finalizat cu succes")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Anulează un workout
     */
    @PutMapping("/{workoutId}/cancel")
    public ResponseEntity<?> cancelWorkout(@PathVariable Long workoutId) {
        try {
            scheduledWorkoutService.cancelWorkout(workoutId);
            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout anulat cu succes")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Verifică dacă un utilizator poate programa un workout la o anumită dată/oră
     */
    @GetMapping("/user/{userId}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {

        boolean available = scheduledWorkoutService.canScheduleWorkoutAt(userId, date, time);

        AvailabilityResponse response = AvailabilityResponse.builder()
                .available(available)
                .date(date)
                .time(time)
                .message(available ? "Slot disponibil" : "Slot ocupat")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Shows the recently completed workouts
     */
    @GetMapping("/user/{userId}/recent-completed")
    public ResponseEntity<List<ScheduledWorkout>> getRecentCompletedWorkouts(@PathVariable Long userId) {
        List<ScheduledWorkout> workouts = scheduledWorkoutService.findRecentCompletedWorkouts(userId);
        return ResponseEntity.ok(workouts);
    }

    /**
     * Shows how many completed workout are for a user
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<ScheduledWorkoutService.WorkoutStatistics> getUserStatistics(@PathVariable Long userId) {
        ScheduledWorkoutService.WorkoutStatistics stats = scheduledWorkoutService.getUserWorkoutStatistics(userId);
        return ResponseEntity.ok(stats);
    }
    /**
     * Reprogramează un workout existent
     */
    @PutMapping("/{workoutId}/reschedule")
    public ResponseEntity<?> rescheduleWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody RescheduleWorkoutRequest request) {
        try {
            scheduledWorkoutService.rescheduleWorkout(
                    workoutId,
                    request.getScheduledDate(),
                    request.getScheduledTime()
            );

            return ResponseEntity.ok()
                    .body(SuccessResponse.builder()
                            .message("Workout reprogramat cu succes")
                            .build());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .error("Eroare de validare")
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error while rescheduling workout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .error("Eroare internă")
                            .message("A apărut o eroare neașteptată")
                            .build());
        }
    }


    // DTO Classes pentru request/response

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RescheduleWorkoutRequest {
        @NotNull(message = "Data programată este obligatorie")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate scheduledDate;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime scheduledTime;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor  // Add this line
    @lombok.AllArgsConstructor // Add this line
    public static class ScheduleWorkoutRequest {
        @NotNull(message = "ID-ul utilizatorului este obligatoriu")
        @Positive(message = "ID-ul utilizatorului trebuie să fie pozitiv")
        private Long userId;

        @NotNull(message = "ID-ul planului de workout este obligatoriu")
        @Positive(message = "ID-ul planului de workout trebuie să fie pozitiv")
        private Long workoutPlanId;

        @NotNull(message = "Data programată este obligatorie")
        @JsonFormat(pattern = "yyyy-MM-dd") // Add this to help with date parsing
        private LocalDate scheduledDate;

        @JsonFormat(pattern = "HH:mm") // Add this to help with time parsing
        private LocalTime scheduledTime;
    }


    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor  // Add this annotation
    @lombok.AllArgsConstructor // Add this annotation
    public static class CompleteWorkoutRequest {
        private Integer caloriesBurned;
        private Integer rating;
    }

    @lombok.Data
    @lombok.Builder
    public static class ScheduleWorkoutResponse {
        private Long scheduledWorkoutId;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class AvailabilityResponse {
        private boolean available;
        private LocalDate date;
        private LocalTime time;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class SuccessResponse {
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private String error;
        private String message;
    }
}