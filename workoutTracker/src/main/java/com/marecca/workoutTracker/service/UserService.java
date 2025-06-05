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
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
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

}