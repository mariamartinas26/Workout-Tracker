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

public class ExerciseController {

    private final ExerciseService exerciseService;
    private final JwtControllerUtils jwtUtils;

    /**
     * Find an exercise by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getExerciseById(@PathVariable Long id) {
        try {
            Optional<Exercise> exercise = exerciseService.findById(id);
            return exercise.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return jwtUtils.createErrorResponse("Failed to get exercise", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Find all exercises
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

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() :
                    Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Exercise> exercises = exerciseService.findAll(pageable);

            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercises");
        }
    }

    /**
     * Find exercises by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getExercisesByCategory(
            @PathVariable ExerciseCategoryType category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            Pageable pageable = PageRequest.of(page, size, Sort.by("exerciseName"));
            Page<Exercise> exercises = exerciseService.findByCategory(category, pageable);

            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercises by category");
        }
    }

    /**
     * Find exercises by primary muscle group
     */
    @GetMapping("/muscle-group/{muscleGroup}")
    public ResponseEntity<?> getExercisesByMuscleGroup(
            @PathVariable MuscleGroupType muscleGroup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            Pageable pageable = PageRequest.of(page, size, Sort.by("exerciseName"));
            Page<Exercise> exercises = exerciseService.findByPrimaryMuscleGroup(muscleGroup, pageable);

            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required to access exercises by muscle group");
        }
    }

    /**
     * Find exercises by any muscle group
     */
    @GetMapping("/any-muscle-group/{muscleGroup}")
    public ResponseEntity<?> getExercisesByAnyMuscleGroup(
            @PathVariable MuscleGroupType muscleGroup,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            List<Exercise> exercises = exerciseService.findByAnyMuscleGroup(muscleGroup);
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    /**
     * Simple search by name
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<?> searchExercisesByName(
            @RequestParam String keyword,
            HttpServletRequest request) {

        try {
            Long authenticatedUserId = jwtUtils.getUserIdFromToken(request);

            List<Exercise> exercises = exerciseService.searchByName(keyword);
            return ResponseEntity.ok(exercises);
        } catch (Exception e) {
            return jwtUtils.createUnauthorizedResponse("Authentication required");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return jwtUtils.createBadRequestResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        return jwtUtils.createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        return jwtUtils.createErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}