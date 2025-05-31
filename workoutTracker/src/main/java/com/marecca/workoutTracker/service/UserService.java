package com.marecca.workoutTracker.service;

import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * business logic for operations with users
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }


    /**
     * finds user by email
     * @param email
     * @return
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Update user info
     * @param userId
     * @param updatedUser
     * @return
     * @throws IllegalArgumentException if user does not exist or invalid data
     */
    public User updateUser(Long userId, User updatedUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updatedUser.getDateOfBirth() != null) {
            existingUser.setDateOfBirth(updatedUser.getDateOfBirth());
        }
        if (updatedUser.getHeightCm() != null) {
            existingUser.setHeightCm(updatedUser.getHeightCm());
        }
        if (updatedUser.getWeightKg() != null) {
            existingUser.setWeightKg(updatedUser.getWeightKg());
        }
        if (updatedUser.getFitnessLevel() != null) {
            existingUser.setFitnessLevel(updatedUser.getFitnessLevel());
        }

        existingUser.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existingUser);
    }

    /**
     * Deactivates user
     * @param userId
     * @throws IllegalArgumentException if user does not exist
     */
    public void deactivateUser(Long userId) {
        log.info("Deactivating user with ID: {}", userId);

        validateUserExists(userId);
        userRepository.deactivateUser(userId);

        log.info("User deactivated successfully: {}", userId);
    }

    /**
     * Activates user
     */
    public void activateUser(Long userId) {
        log.info("Activating user with ID: {}", userId);

        User user = findUserById(userId);
        // user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("User activated successfully: {}", userId);
    }

    /**
     * finds all active users
     */
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /**
     * finds user by fitness level
     */
    @Transactional(readOnly = true)
    public List<User> findByFitnessLevel(String fitnessLevel) {
        return userRepository.findByFitnessLevel(fitnessLevel);
    }

    /**
     * find Users With Minimum workout Plans
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithMinimumPlans(Long minPlans) {
        return userRepository.findUsersWithAtLeastMinPlans(minPlans);
    }

    /**
     * find Users With Minimum Completed Workouts
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithMinimumCompletedWorkouts(Integer minWorkouts) {
        return userRepository.findUsersWithMinimumCompletedWorkouts(minWorkouts);
    }


    /**
     * checks if a user exists with a given email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase());
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * register a new user with encrpited password
     */
    @Transactional
    public User registerUser(User user, String plainPassword) {
        //encripts password
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Logs and user based on email and password
     */
    public Optional<User> authenticateUser(String email, String plainPassword) {
        Optional<User> userOptional = userRepository.findByEmail(email.toLowerCase());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            //checks password
            if (passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists:  " + email);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found with ID: " + userId));
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User was not found with ID: " + userId);
        }
    }
}