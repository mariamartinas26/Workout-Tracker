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
     * Finds user by email
     * @param email
     * @return
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    /**
     * Update user info
     * @param userId
     * @param updatedUser
     * @return
     * @throws RuntimeException if user does not exist
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

        return userRepository.save(existingUser);
    }

    /**
     * Deactivates user
     * @param userId
     * @throws IllegalArgumentException if user does not exist
     */
    public void deactivateUser(Long userId) {
        validateUserExists(userId);
        userRepository.deactivateUser(userId);
    }

    /**
     * Activates user
     * @param userId
     */
    public void activateUser(Long userId) {
        User user = findUserById(userId);
        user.setIsActive(true);

        userRepository.save(user);
    }

    /**
     * Register a new user with encrypted password
     * @param user
     * @param plainPassword
     * @return
     */
    @Transactional
    public User registerUser(User user, String plainPassword) {
        // Encrypt password
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setIsActive(true);

        return userRepository.save(user);
    }

    /**
     * Authenticates user based on email and password
     * @param email
     * @param plainPassword
     * @return
     */
    public Optional<User> authenticateUser(String email, String plainPassword) {
        Optional<User> userOptional = userRepository.findByEmail(email.toLowerCase());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Check password
            if (passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Finds all active users
     */
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Finds user by fitness level
     */
    @Transactional(readOnly = true)
    public List<User> findByFitnessLevel(String fitnessLevel) {
        return userRepository.findByFitnessLevel(fitnessLevel);
    }

    /**
     * Find Users With Minimum workout Plans
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithMinimumPlans(Long minPlans) {
        return userRepository.findUsersWithAtLeastMinPlans(minPlans);
    }

    /**
     * Find Users With Minimum Completed Workouts
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithMinimumCompletedWorkouts(Integer minWorkouts) {
        return userRepository.findUsersWithMinimumCompletedWorkouts(minWorkouts);
    }

    /**
     * Checks if a user exists with a given email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase());
    }

    /**
     * Checks if a user exists with a given username
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
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