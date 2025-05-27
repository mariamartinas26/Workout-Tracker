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
 * Service pentru gestionarea utilizatorilor
 * Conține logica de business pentru operațiile cu utilizatori
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creează un utilizator nou
     * @param user obiectul User cu datele de intrare
     * @return utilizatorul creat
     * @throws IllegalArgumentException dacă username-ul sau email-ul există deja
     */
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getUsername());

        validateUniqueUsername(user.getUsername());
        validateUniqueEmail(user.getEmail());

        // Criptează parola
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        //user.setActive(true);

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getUserId());

        return savedUser;
    }

    /**
     * Găsește un utilizator după ID
     * @param userId ID-ul utilizatorului
     * @return Optional cu utilizatorul găsit
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        log.debug("Finding user by ID: {}", userId);
        return userRepository.findById(userId);
    }

    /**
     * Găsește un utilizator după username
     * @param username numele de utilizator
     * @return Optional cu utilizatorul găsit
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Găsește un utilizator după email
     * @param email adresa de email
     * @return Optional cu utilizatorul găsit
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Actualizează informațiile utilizatorului
     * @param userId ID-ul utilizatorului de actualizat
     * @param updatedUser obiectul cu noile date
     * @return utilizatorul actualizat
     * @throws IllegalArgumentException dacă utilizatorul nu există sau datele nu sunt valide
     */
    public User updateUser(Long userId, User updatedUser) {
        log.info("Updating user with ID: {}", userId);

        User existingUser = findUserById(userId);

        // Verifică unicitatea username și email doar dacă au fost schimbate
        validateUsernameChange(existingUser, updatedUser);
        validateEmailChange(existingUser, updatedUser);

        // Actualizează câmpurile
        updateUserFields(existingUser, updatedUser);
        existingUser.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(existingUser);
        log.info("User updated successfully: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Dezactivează un utilizator
     * @param userId ID-ul utilizatorului de dezactivat
     * @throws IllegalArgumentException dacă utilizatorul nu există
     */
    public void deactivateUser(Long userId) {
        log.info("Deactivating user with ID: {}", userId);

        validateUserExists(userId);
        userRepository.deactivateUser(userId);

        log.info("User deactivated successfully: {}", userId);
    }

    /**
     * Activează un utilizator
     * @param userId ID-ul utilizatorului de activat
     * @throws IllegalArgumentException dacă utilizatorul nu există
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
     * Găsește toți utilizatorii activi
     * @return lista utilizatorilor activi
     */
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        log.debug("Finding all active users");
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Găsește utilizatori după nivelul de fitness
     * @param fitnessLevel nivelul de fitness (BEGINNER, INTERMEDIATE, ADVANCED)
     * @return lista utilizatorilor cu nivelul specificat
     */
    @Transactional(readOnly = true)
    public List<User> findByFitnessLevel(String fitnessLevel) {
        log.debug("Finding users by fitness level: {}", fitnessLevel);
        return userRepository.findByFitnessLevel(fitnessLevel);
    }

    /**
     * Găsește utilizatori cu un număr minim de planuri de workout
     * @param minPlans numărul minim de planuri
     * @return lista utilizatorilor care îndeplinesc criteriul
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithMinimumPlans(Long minPlans) {
        log.debug("Finding users with at least {} workout plans", minPlans);
        return userRepository.findUsersWithAtLeastMinPlans(minPlans);
    }

    /**
     * Găsește utilizatori cu un număr minim de workout-uri completate
     * @param minWorkouts numărul minim de workout-uri completate
     * @return lista utilizatorilor care îndeplinesc criteriul
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithMinimumCompletedWorkouts(Integer minWorkouts) {
        log.debug("Finding users with at least {} completed workouts", minWorkouts);
        return userRepository.findUsersWithMinimumCompletedWorkouts(minWorkouts);
    }

    /**
     * Schimbă parola utilizatorului
     * @param userId ID-ul utilizatorului
     * @param oldPassword parola actuală
     * @param newPassword noua parolă
     * @throws IllegalArgumentException dacă parola veche nu este corectă
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user ID: {}", userId);

        User user = findUserById(userId);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Parola actuală este incorectă");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Password changed successfully for user ID: {}", userId);
    }

    /**
     * Resetează parola utilizatorului (pentru admin)
     * @param userId ID-ul utilizatorului
     * @param newPassword noua parolă
     */
    public void resetPassword(Long userId, String newPassword) {
        log.info("Resetting password for user ID: {}", userId);

        User user = findUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Password reset successfully for user ID: {}", userId);
    }

    /**
     * Verifică dacă username-ul este disponibil
     * @param username numele de utilizator de verificat
     * @return true dacă este disponibil, false altfel
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Verifică dacă email-ul este disponibil
     * @param email adresa de email de verificat
     * @return true dacă este disponibil, false altfel
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Șterge permanent un utilizator (doar pentru admin)
     * @param userId ID-ul utilizatorului de șters
     * @throws IllegalArgumentException dacă utilizatorul nu există
     */
    public void deleteUser(Long userId) {
        log.warn("Permanently deleting user with ID: {}", userId);

        validateUserExists(userId);
        userRepository.deleteById(userId);

        log.warn("User permanently deleted: {}", userId);
    }

    // Metode private pentru validare și utilități

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username-ul există deja: " + username);
        }
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email-ul există deja: " + email);
        }
    }

    private void validateUsernameChange(User existing, User updated) {
        if (!existing.getUsername().equals(updated.getUsername()) &&
                userRepository.existsByUsername(updated.getUsername())) {
            throw new IllegalArgumentException("Username-ul există deja: " + updated.getUsername());
        }
    }

    private void validateEmailChange(User existing, User updated) {
        if (!existing.getEmail().equals(updated.getEmail()) &&
                userRepository.existsByEmail(updated.getEmail())) {
            throw new IllegalArgumentException("Email-ul există deja: " + updated.getEmail());
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost găsit cu ID-ul: " + userId));
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Utilizatorul nu a fost găsit cu ID-ul: " + userId);
        }
    }

    private void updateUserFields(User existing, User updated) {
        existing.setUsername(updated.getUsername());
        existing.setEmail(updated.getEmail());
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setHeightCm(updated.getHeightCm());
        existing.setWeightKg(updated.getWeightKg());
        existing.setFitnessLevel(updated.getFitnessLevel());
    }
}