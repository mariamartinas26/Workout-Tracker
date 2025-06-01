package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.entity.Exercise;
import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import com.marecca.workoutTracker.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Controller for exercise management
 */
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExerciseController {

    private final ExerciseService exerciseService;

    /**
     * Create a new exercise
     * POST /api/exercises
     */
    @PostMapping
    public ResponseEntity<Exercise> createExercise(@Valid @RequestBody Exercise exercise) {
        log.info("REST request to create exercise: {}", exercise.getExerciseName());

        try {
            Exercise createdExercise = exerciseService.createExercise(exercise);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdExercise);
        } catch (IllegalArgumentException e) {
            log.error("Error creating exercise: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find an exercise by ID
     * GET /api/exercises/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Exercise> getExerciseById(@PathVariable Long id) {
        log.debug("REST request to get exercise by ID: {}", id);

        Optional<Exercise> exercise = exerciseService.findById(id);
        return exercise.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Find all exercises with pagination and sorting
     * GET /api/exercises
     */
    @GetMapping
    public ResponseEntity<Page<Exercise>> getAllExercises(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "exerciseName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug("REST request to get exercises - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Exercise> exercises = exerciseService.findAll(pageable);

        return ResponseEntity.ok(exercises);
    }

    /**
     * Advanced search with filters
     * GET /api/exercises/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Exercise>> searchExercises(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ExerciseCategoryType category,
            @RequestParam(required = false) MuscleGroupType muscleGroup,
            @RequestParam(required = false) Integer maxDifficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "exerciseName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug("REST request to search exercises with filters");

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        try {
            Page<Exercise> exercises = exerciseService.searchExercises(
                    keyword, category, muscleGroup, maxDifficulty, pageable);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error searching exercises: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find exercises by category
     * GET /api/exercises/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<Exercise>> getExercisesByCategory(
            @PathVariable ExerciseCategoryType category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to get exercises by category: {}", category);

        Pageable pageable = PageRequest.of(page, size, Sort.by("exerciseName"));
        Page<Exercise> exercises = exerciseService.findByCategory(category, pageable);

        return ResponseEntity.ok(exercises);
    }

    /**
     * Find exercises by primary muscle group
     * GET /api/exercises/muscle-group/{muscleGroup}
     */
    @GetMapping("/muscle-group/{muscleGroup}")
    public ResponseEntity<Page<Exercise>> getExercisesByMuscleGroup(
            @PathVariable MuscleGroupType muscleGroup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to get exercises by muscle group: {}", muscleGroup);

        Pageable pageable = PageRequest.of(page, size, Sort.by("exerciseName"));
        Page<Exercise> exercises = exerciseService.findByPrimaryMuscleGroup(muscleGroup, pageable);

        return ResponseEntity.ok(exercises);
    }

    /**
     * Find exercises by any muscle group (primary or secondary)
     * GET /api/exercises/any-muscle-group/{muscleGroup}
     */
    @GetMapping("/any-muscle-group/{muscleGroup}")
    public ResponseEntity<List<Exercise>> getExercisesByAnyMuscleGroup(
            @PathVariable MuscleGroupType muscleGroup) {

        log.debug("REST request to get exercises by any muscle group: {}", muscleGroup);

        List<Exercise> exercises = exerciseService.findByAnyMuscleGroup(muscleGroup);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Find exercises by difficulty level
     * GET /api/exercises/difficulty/{level}
     */
    @GetMapping("/difficulty/{level}")
    public ResponseEntity<List<Exercise>> getExercisesByDifficulty(
            @PathVariable Integer level) {

        log.debug("REST request to get exercises by difficulty level: {}", level);

        try {
            List<Exercise> exercises = exerciseService.findByDifficultyLevel(level);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting exercises by difficulty: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find beginner exercises
     * GET /api/exercises/beginner
     */
    @GetMapping("/beginner")
    public ResponseEntity<List<Exercise>> getBeginnerExercises() {
        log.debug("REST request to get beginner exercises");

        List<Exercise> exercises = exerciseService.findBeginnerExercises();
        return ResponseEntity.ok(exercises);
    }

    /**
     * Find advanced exercises
     * GET /api/exercises/advanced
     */
    @GetMapping("/advanced")
    public ResponseEntity<List<Exercise>> getAdvancedExercises() {
        log.debug("REST request to get advanced exercises");

        List<Exercise> exercises = exerciseService.findAdvancedExercises();
        return ResponseEntity.ok(exercises);
    }

    /**
     * Find exercises without equipment
     * GET /api/exercises/no-equipment
     */
    @GetMapping("/no-equipment")
    public ResponseEntity<List<Exercise>> getExercisesWithoutEquipment() {
        log.debug("REST request to get exercises without equipment");

        List<Exercise> exercises = exerciseService.findExercisesWithoutEquipment();
        return ResponseEntity.ok(exercises);
    }

    /**
     * Find exercises by equipment
     * GET /api/exercises/equipment/{equipment}
     */
    @GetMapping("/equipment/{equipment}")
    public ResponseEntity<List<Exercise>> getExercisesByEquipment(
            @PathVariable String equipment) {

        log.debug("REST request to get exercises by equipment: {}", equipment);

        List<Exercise> exercises = exerciseService.findByEquipment(equipment);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Find similar exercises
     * GET /api/exercises/{id}/similar
     */
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<Exercise>> getSimilarExercises(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit) {

        log.debug("REST request to get similar exercises for ID: {} with limit: {}", id, limit);

        try {
            List<Exercise> exercises = exerciseService.findSimilarExercises(id, limit);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting similar exercises: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find most popular exercises
     * GET /api/exercises/popular
     */
    @GetMapping("/popular")
    public ResponseEntity<List<Exercise>> getMostPopularExercises(
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("REST request to get {} most popular exercises", limit);

        try {
            List<Exercise> exercises = exerciseService.findMostPopularExercises(limit);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting popular exercises: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find popular exercises by category
     * GET /api/exercises/popular/category/{category}
     */
    @GetMapping("/popular/category/{category}")
    public ResponseEntity<List<Exercise>> getPopularExercisesByCategory(
            @PathVariable ExerciseCategoryType category) {

        log.debug("REST request to get popular exercises for category: {}", category);

        List<Exercise> exercises = exerciseService.findPopularExercisesByCategory(category);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Find most logged exercises
     * GET /api/exercises/most-logged
     */
    @GetMapping("/most-logged")
    public ResponseEntity<List<Exercise>> getMostLoggedExercises(
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("REST request to get {} most logged exercises", limit);

        try {
            List<Exercise> exercises = exerciseService.findMostLoggedExercises(limit);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting most logged exercises: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find recently created exercises
     * GET /api/exercises/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Exercise>> getRecentExercises(
            @RequestParam(defaultValue = "30") int days) {

        log.debug("REST request to get exercises created in the last {} days", days);

        try {
            List<Exercise> exercises = exerciseService.findRecentExercises(days);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting recent exercises: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find unused exercises
     * GET /api/exercises/unused
     */
    @GetMapping("/unused")
    public ResponseEntity<List<Exercise>> getUnusedExercises() {
        log.debug("REST request to get unused exercises");

        List<Exercise> exercises = exerciseService.findUnusedExercises();
        return ResponseEntity.ok(exercises);
    }

    /**
     * Update an exercise
     * PUT /api/exercises/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Exercise> updateExercise(
            @PathVariable Long id,
            @Valid @RequestBody Exercise exercise) {

        log.info("REST request to update exercise with ID: {}", id);

        try {
            Exercise updatedExercise = exerciseService.updateExercise(id, exercise);
            return ResponseEntity.ok(updatedExercise);
        } catch (IllegalArgumentException e) {
            log.error("Error updating exercise: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete an exercise
     * DELETE /api/exercises/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExercise(@PathVariable Long id) {
        log.info("REST request to delete exercise with ID: {}", id);

        try {
            exerciseService.deleteExercise(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Exercise not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Error deleting exercise: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Check if exercise name is available
     * GET /api/exercises/check-name/{name}
     */
    @GetMapping("/check-name/{name}")
    public ResponseEntity<Boolean> isExerciseNameAvailable(@PathVariable String name) {
        log.debug("REST request to check if exercise name is available: {}", name);

        boolean available = exerciseService.isExerciseNameAvailable(name);
        return ResponseEntity.ok(available);
    }

    /**
     * Count exercises by category
     * GET /api/exercises/count/category/{category}
     */
    @GetMapping("/count/category/{category}")
    public ResponseEntity<Long> countExercisesByCategory(@PathVariable ExerciseCategoryType category) {
        log.debug("REST request to count exercises by category: {}", category);

        long count = exerciseService.countByCategory(category);
        return ResponseEntity.ok(count);
    }

    /**
     * Count exercises by muscle group
     * GET /api/exercises/count/muscle-group/{muscleGroup}
     */
    @GetMapping("/count/muscle-group/{muscleGroup}")
    public ResponseEntity<Long> countExercisesByMuscleGroup(@PathVariable MuscleGroupType muscleGroup) {
        log.debug("REST request to count exercises by muscle group: {}", muscleGroup);

        long count = exerciseService.countByMuscleGroup(muscleGroup);
        return ResponseEntity.ok(count);
    }

    /**
     * Count how many workout plans use an exercise
     * GET /api/exercises/{id}/usage-count
     */
    @GetMapping("/{id}/usage-count")
    public ResponseEntity<Long> getExerciseUsageCount(@PathVariable Long id) {
        log.debug("REST request to get usage count for exercise ID: {}", id);

        try {
            long count = exerciseService.countWorkoutPlansUsingExercise(id);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            log.error("Exercise not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get exercise statistics
     * GET /api/exercises/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ExerciseService.ExerciseStatistics> getExerciseStatistics() {
        log.debug("REST request to get exercise statistics");

        ExerciseService.ExerciseStatistics statistics = exerciseService.getExerciseStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Find exercises by secondary muscle groups
     * POST /api/exercises/secondary-muscle-groups
     */
    @PostMapping("/secondary-muscle-groups")
    public ResponseEntity<List<Exercise>> getExercisesBySecondaryMuscleGroups(
            @RequestBody List<MuscleGroupType> muscleGroups) {

        log.debug("REST request to get exercises by secondary muscle groups: {}", muscleGroups);

        List<Exercise> exercises = exerciseService.findBySecondaryMuscleGroups(muscleGroups);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Simple search by name
     * GET /api/exercises/search-by-name
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<List<Exercise>> searchExercisesByName(
            @RequestParam String keyword) {

        log.debug("REST request to search exercises by name: {}", keyword);

        List<Exercise> exercises = exerciseService.searchByName(keyword);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Find exercises filtered with multiple criteria
     * GET /api/exercises/filter
     */
    @GetMapping("/filter")
    public ResponseEntity<List<Exercise>> getFilteredExercises(
            @RequestParam(required = false) ExerciseCategoryType category,
            @RequestParam(required = false) MuscleGroupType muscleGroup,
            @RequestParam(required = false) Integer maxDifficulty,
            @RequestParam(required = false) String equipment) {

        log.debug("REST request to get filtered exercises");

        try {
            List<Exercise> exercises = exerciseService.findFilteredExercises(
                    category, muscleGroup, maxDifficulty, equipment);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error filtering exercises: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Exception handlers for error handling
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred");
    }
}