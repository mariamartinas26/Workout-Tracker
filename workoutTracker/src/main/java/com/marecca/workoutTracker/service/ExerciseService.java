package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.Exercise;
import com.marecca.workoutTracker.entity.enums.ExerciseCategoryType;
import com.marecca.workoutTracker.entity.enums.MuscleGroupType;
import com.marecca.workoutTracker.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    /**
     * finds exercise by id
     */
    @Transactional(readOnly = true)
    public Optional<Exercise> findById(Long exerciseId) {
        return exerciseRepository.findById(exerciseId);
    }


    @Transactional(readOnly = true)
    public Page<Exercise> findAll(Pageable pageable) {
        return exerciseRepository.findAll(pageable);
    }


    @Transactional(readOnly = true)
    public Page<Exercise> findByCategory(ExerciseCategoryType category, Pageable pageable) {
        return exerciseRepository.findByCategory(category, pageable);
    }


    @Transactional(readOnly = true)
    public Page<Exercise> findByPrimaryMuscleGroup(MuscleGroupType muscleGroup, Pageable pageable) {
        return exerciseRepository.findByPrimaryMuscleGroup(muscleGroup, pageable);
    }

    /**
     * find exercises by any type of muscle group
     */
    @Transactional(readOnly = true)
    public List<Exercise> findByAnyMuscleGroup(MuscleGroupType muscleGroup) {
        return exerciseRepository.findByAnyMuscleGroup(muscleGroup.name());
    }

    @Transactional(readOnly = true)
    public List<Exercise> searchByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return exerciseRepository.findByExerciseNameContainingIgnoreCase(keyword.trim());
    }
}