package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.entity.Exercise;
import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import com.marecca.workoutTracker.service.ExerciseService;
import com.marecca.workoutTracker.util.JwtControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Slf4j

public class ExerciseController {

    private final ExerciseService exerciseService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Create a new exercise (requires authentication)
     * POST /api/exercises
     */
    @PostMapping
    public ResponseEntity<?> createExercise(@Valid @RequestBody Exercise exercise, HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.info("REST request to create exercise: {} by user: {}", exercise.getExerciseName(), authenticatedUserId);

            Exercise createdExercise = exerciseService.createExercise(exercise);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdExercise);
        } catch (IllegalArgumentException e) {
            log.error("Error creating exercise: {}", e.getMessage());
            return jwtUtils.createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to create exercises");
        }
    }

    /**
     * Find an exercise by ID (public endpoint)
     * GET /api/exercises/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getExerciseById(@PathVariable Long id) {
        log.debug("REST request to get exercise by ID: {}", id);

        try {
            Optional<Exercise> exercise = exerciseService.findById(id);
            return exercise.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting exercise by ID: {}", e.getMessage());
            return jwtUtils.createErrorResponse("Failed to get exercise", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Find all exercises with pagination and sorting (authenticated)
     * GET /api/exercises
     */
    @GetMapping
    public ResponseEntity<?> getAllExercises(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "exerciseName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercises by user: {} - page: {}, size: {}, sortBy: {}, sortDir: {}",
                    authenticatedUserId, page, size, sortBy, sortDir);

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() :
                    Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Exercise> exercises = exerciseService.findAll(pageable);

            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercises");
        }
    }

    /**
     * Advanced search with filters (authenticated)
     * GET /api/exercises/search
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchExercises(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ExerciseCategoryType category,
            @RequestParam(required = false) MuscleGroupType muscleGroup,
            @RequestParam(required = false) Integer maxDifficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "exerciseName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to search exercises by user: {} with filters", authenticatedUserId);

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() :
                    Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Exercise> exercises = exerciseService.searchExercises(
                    keyword, category, muscleGroup, maxDifficulty, pageable);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error searching exercises: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to search exercises");
        }
    }

    /**
     * Find exercises by category (authenticated)
     * GET /api/exercises/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getExercisesByCategory(
            @PathVariable ExerciseCategoryType category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercises by category: {} for user: {}", category, authenticatedUserId);

            Pageable pageable = PageRequest.of(page, size, Sort.by("exerciseName"));
            Page<Exercise> exercises = exerciseService.findByCategory(category, pageable);

            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercises by category");
        }
    }

    /**
     * Find exercises by primary muscle group (authenticated)
     * GET /api/exercises/muscle-group/{muscleGroup}
     */
    @GetMapping("/muscle-group/{muscleGroup}")
    public ResponseEntity<?> getExercisesByMuscleGroup(
            @PathVariable MuscleGroupType muscleGroup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercises by muscle group: {} for user: {}", muscleGroup, authenticatedUserId);

            Pageable pageable = PageRequest.of(page, size, Sort.by("exerciseName"));
            Page<Exercise> exercises = exerciseService.findByPrimaryMuscleGroup(muscleGroup, pageable);

            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercises by muscle group");
        }
    }

    /**
     * Find exercises by any muscle group (authenticated)
     * GET /api/exercises/any-muscle-group/{muscleGroup}
     */
    @GetMapping("/any-muscle-group/{muscleGroup}")
    public ResponseEntity<?> getExercisesByAnyMuscleGroup(
            @PathVariable MuscleGroupType muscleGroup,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercises by any muscle group: {} for user: {}", muscleGroup, authenticatedUserId);

            List<Exercise> exercises = exerciseService.findByAnyMuscleGroup(muscleGroup);
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find exercises by difficulty level (authenticated)
     * GET /api/exercises/difficulty/{level}
     */
    @GetMapping("/difficulty/{level}")
    public ResponseEntity<?> getExercisesByDifficulty(
            @PathVariable Integer level,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercises by difficulty level: {} for user: {}", level, authenticatedUserId);

            List<Exercise> exercises = exerciseService.findByDifficultyLevel(level);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting exercises by difficulty: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find beginner exercises (authenticated)
     * GET /api/exercises/beginner
     */
    @GetMapping("/beginner")
    public ResponseEntity<?> getBeginnerExercises(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get beginner exercises for user: {}", authenticatedUserId);

            List<Exercise> exercises = exerciseService.findBeginnerExercises();
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find advanced exercises (authenticated)
     * GET /api/exercises/advanced
     */
    @GetMapping("/advanced")
    public ResponseEntity<?> getAdvancedExercises(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get advanced exercises for user: {}", authenticatedUserId);

            List<Exercise> exercises = exerciseService.findAdvancedExercises();
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find similar exercises (authenticated)
     * GET /api/exercises/{id}/similar
     */
    @GetMapping("/{id}/similar")
    public ResponseEntity<?> getSimilarExercises(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get similar exercises for ID: {} with limit: {} by user: {}", id, limit, authenticatedUserId);

            List<Exercise> exercises = exerciseService.findSimilarExercises(id, limit);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting similar exercises: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find most popular exercises (authenticated)
     * GET /api/exercises/popular
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getMostPopularExercises(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get {} most popular exercises for user: {}", limit, authenticatedUserId);

            List<Exercise> exercises = exerciseService.findMostPopularExercises(limit);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting popular exercises: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find popular exercises by category (authenticated)
     * GET /api/exercises/popular/category/{category}
     */
    @GetMapping("/popular/category/{category}")
    public ResponseEntity<?> getPopularExercisesByCategory(
            @PathVariable ExerciseCategoryType category,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get popular exercises for category: {} by user: {}", category, authenticatedUserId);

            List<Exercise> exercises = exerciseService.findPopularExercisesByCategory(category);
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find most logged exercises (authenticated)
     * GET /api/exercises/most-logged
     */
    @GetMapping("/most-logged")
    public ResponseEntity<?> getMostLoggedExercises(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get {} most logged exercises for user: {}", limit, authenticatedUserId);

            List<Exercise> exercises = exerciseService.findMostLoggedExercises(limit);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error getting most logged exercises: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find unused exercises (authenticated)
     * GET /api/exercises/unused
     */
    @GetMapping("/unused")
    public ResponseEntity<?> getUnusedExercises(HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get unused exercises for user: {}", authenticatedUserId);

            List<Exercise> exercises = exerciseService.findUnusedExercises();
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Count exercises by category (authenticated)
     * GET /api/exercises/count/category/{category}
     */
    @GetMapping("/count/category/{category}")
    public ResponseEntity<?> countExercisesByCategory(
            @PathVariable ExerciseCategoryType category,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to count exercises by category: {} for user: {}", category, authenticatedUserId);

            long count = exerciseService.countByCategory(category);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Count exercises by muscle group (authenticated)
     * GET /api/exercises/count/muscle-group/{muscleGroup}
     */
    @GetMapping("/count/muscle-group/{muscleGroup}")
    public ResponseEntity<?> countExercisesByMuscleGroup(
            @PathVariable MuscleGroupType muscleGroup,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to count exercises by muscle group: {} for user: {}", muscleGroup, authenticatedUserId);

            long count = exerciseService.countByMuscleGroup(muscleGroup);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Count how many workout plans use an exercise (authenticated)
     * GET /api/exercises/{id}/usage-count
     */
    @GetMapping("/{id}/usage-count")
    public ResponseEntity<?> getExerciseUsageCount(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get usage count for exercise ID: {} by user: {}", id, authenticatedUserId);

            long count = exerciseService.countWorkoutPlansUsingExercise(id);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            log.error("Exercise not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find exercises by secondary muscle groups (authenticated)
     * POST /api/exercises/secondary-muscle-groups
     */
    @PostMapping("/secondary-muscle-groups")
    public ResponseEntity<?> getExercisesBySecondaryMuscleGroups(
            @RequestBody List<MuscleGroupType> muscleGroups,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get exercises by secondary muscle groups: {} for user: {}", muscleGroups, authenticatedUserId);

            List<Exercise> exercises = exerciseService.findBySecondaryMuscleGroups(muscleGroups);
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Simple search by name (authenticated)
     * GET /api/exercises/search-by-name
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<?> searchExercisesByName(
            @RequestParam String keyword,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to search exercises by name: {} for user: {}", keyword, authenticatedUserId);

            List<Exercise> exercises = exerciseService.searchByName(keyword);
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Find exercises filtered with multiple criteria (authenticated)
     * GET /api/exercises/filter
     */
    @GetMapping("/filter")
    public ResponseEntity<?> getFilteredExercises(
            @RequestParam(required = false) ExerciseCategoryType category,
            @RequestParam(required = false) MuscleGroupType muscleGroup,
            @RequestParam(required = false) Integer maxDifficulty,
            @RequestParam(required = false) String equipment,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);
            log.debug("REST request to get filtered exercises for user: {}", authenticatedUserId);

            List<Exercise> exercises = exerciseService.findFilteredExercises(
                    category, muscleGroup, maxDifficulty, equipment);
            return ResponseEntity.ok(exercises);
        } catch (IllegalArgumentException e) {
            log.error("Error filtering exercises: {}", e.getMessage());
            return jwtUtils.createBadRequestResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Exception handlers for error handling
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return jwtUtils.createBadRequestResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage());
        return jwtUtils.createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return jwtUtils.createErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}