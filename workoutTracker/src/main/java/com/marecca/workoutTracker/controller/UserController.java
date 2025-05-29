package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller pentru operațiunile CRUD cu utilizatorii
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * DTO pentru actualizarea profilului utilizatorului
     */
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String email;
        private LocalDate dateOfBirth;
        private Integer heightCm;
        private BigDecimal weightKg;
        private String fitnessLevel;

        // Constructors
        public UpdateProfileRequest() {}

        // Getters și setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public Integer getHeightCm() { return heightCm; }
        public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

        public String getFitnessLevel() { return fitnessLevel; }
        public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }
    }

    /**
     * DTO pentru schimbarea parolei
     */
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        // Constructors
        public ChangePasswordRequest() {}

        // Getters și setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /**
     * DTO pentru răspunsul detaliat al utilizatorului
     */
    public static class DetailedUserResponse {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private LocalDate dateOfBirth;
        private Integer heightCm;
        private BigDecimal weightKg;
        private String fitnessLevel;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Integer workoutPlansCount;
        private Integer scheduledWorkoutsCount;

        // Constructor din User entity
        public DetailedUserResponse(User user) {
            this.id = user.getUserId();
            this.username = user.getUsername();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.email = user.getEmail();
            this.dateOfBirth = user.getDateOfBirth();
            this.heightCm = user.getHeightCm();
            this.weightKg = user.getWeightKg();
            this.fitnessLevel = user.getFitnessLevel();
            this.isActive = user.getIsActive();
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
            this.workoutPlansCount = user.getWorkoutPlans() != null ? user.getWorkoutPlans().size() : 0;
            this.scheduledWorkoutsCount = user.getScheduledWorkouts() != null ? user.getScheduledWorkouts().size() : 0;
        }

        // Getters și setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public Integer getHeightCm() { return heightCm; }
        public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

        public String getFitnessLevel() { return fitnessLevel; }
        public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public Integer getWorkoutPlansCount() { return workoutPlansCount; }
        public void setWorkoutPlansCount(Integer workoutPlansCount) { this.workoutPlansCount = workoutPlansCount; }

        public Integer getScheduledWorkoutsCount() { return scheduledWorkoutsCount; }
        public void setScheduledWorkoutsCount(Integer scheduledWorkoutsCount) { this.scheduledWorkoutsCount = scheduledWorkoutsCount; }
    }
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateCompleteProfile(@PathVariable Long userId, @RequestBody UpdateProfileRequest request) {
        try {
            log.info("Updating complete profile for user ID: {}", userId);

            // Verifică dacă utilizatorul există
            Optional<User> existingUserOptional = userService.findById(userId);
            if (existingUserOptional.isEmpty()) {
                return createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User existingUser = existingUserOptional.get();

            // Validare input doar pentru câmpurile obligatorii
            if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
                existingUser.setFirstName(request.getFirstName().trim());
            }

            if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
                existingUser.setLastName(request.getLastName().trim());
            }

            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                // Verifică dacă email-ul nu este deja folosit de alt utilizator
                Optional<User> userWithEmail = userService.findByEmail(request.getEmail().trim());
                if (userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(userId)) {
                    return createErrorResponse("Email already exists", HttpStatus.BAD_REQUEST);
                }
                existingUser.setEmail(request.getEmail().trim().toLowerCase());
            }

            // Actualizează câmpurile opționale
            if (request.getDateOfBirth() != null) {
                existingUser.setDateOfBirth(request.getDateOfBirth());
            }

            if (request.getHeightCm() != null) {
                if (request.getHeightCm() < 50 || request.getHeightCm() > 300) {
                    return createErrorResponse("Height must be between 50 and 300 cm", HttpStatus.BAD_REQUEST);
                }
                existingUser.setHeightCm(request.getHeightCm());
            }

            if (request.getWeightKg() != null) {
                if (request.getWeightKg().compareTo(BigDecimal.valueOf(20)) < 0 ||
                        request.getWeightKg().compareTo(BigDecimal.valueOf(1000)) > 0) {
                    return createErrorResponse("Weight must be between 20 and 1000 kg", HttpStatus.BAD_REQUEST);
                }
                existingUser.setWeightKg(request.getWeightKg());
            }

            if (request.getFitnessLevel() != null) {
                if (!List.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(request.getFitnessLevel())) {
                    return createErrorResponse("Invalid fitness level. Must be BEGINNER, INTERMEDIATE, or ADVANCED", HttpStatus.BAD_REQUEST);
                }
                existingUser.setFitnessLevel(request.getFitnessLevel());
            }

            // Actualizează utilizatorul
            User savedUser = userService.updateUser(userId, existingUser);

            log.info("Complete profile updated successfully for user ID: {}", userId);

            // Returnează răspuns compatibil cu frontend (ca AuthController)
            Map<String, Object> response = new HashMap<>();
            response.put("userId", savedUser.getUserId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("firstName", savedUser.getFirstName());
            response.put("lastName", savedUser.getLastName());
            response.put("dateOfBirth", savedUser.getDateOfBirth());
            response.put("heightCm", savedUser.getHeightCm());
            response.put("weightKg", savedUser.getWeightKg());
            response.put("fitnessLevel", savedUser.getFitnessLevel());
            response.put("isActive", savedUser.getIsActive());
            response.put("createdAt", savedUser.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating complete profile for user ID: {}", userId, e);
            return createErrorResponse("Error updating profile", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * Obține profilul utilizatorului după ID
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        try {
            log.debug("Getting user profile for ID: {}", userId);

            Optional<User> userOptional = userService.findById(userId);

            if (userOptional.isEmpty()) {
                return createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            DetailedUserResponse userResponse = new DetailedUserResponse(userOptional.get());
            return ResponseEntity.ok(userResponse);

        } catch (Exception e) {
            log.error("Error getting user profile for ID: {}", userId, e);
            return createErrorResponse("Error retrieving user profile", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/complete-profile")
    public ResponseEntity<?> completeProfileUser(@RequestBody Map<String, Object> request) {
        try {
            log.info("Complete profile request: {}", request);

            // Verifică și extrage userId cu validare
            Object userIdObj = request.get("userId");
            if (userIdObj == null) {
                return createErrorResponse("User ID is required", HttpStatus.BAD_REQUEST);
            }

            Long userId;
            try {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else {
                    userId = Long.valueOf(userIdObj.toString());
                }
            } catch (NumberFormatException e) {
                return createErrorResponse("Invalid user ID format", HttpStatus.BAD_REQUEST);
            }

            Optional<User> userOptional = userService.findById(userId);
            if (userOptional.isEmpty()) {
                return createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            // Actualizează câmpurile dacă sunt prezente și nu sunt null
            Object dateOfBirthObj = request.get("dateOfBirth");
            if (dateOfBirthObj != null && !dateOfBirthObj.toString().trim().isEmpty()) {
                try {
                    user.setDateOfBirth(LocalDate.parse(dateOfBirthObj.toString()));
                } catch (Exception e) {
                    return createErrorResponse("Invalid date format for dateOfBirth", HttpStatus.BAD_REQUEST);
                }
            }

            Object heightCmObj = request.get("heightCm");
            if (heightCmObj != null) {
                try {
                    Integer height = heightCmObj instanceof Number ?
                            ((Number) heightCmObj).intValue() :
                            Integer.valueOf(heightCmObj.toString());

                    if (height < 50 || height > 300) {
                        return createErrorResponse("Height must be between 50 and 300 cm", HttpStatus.BAD_REQUEST);
                    }
                    user.setHeightCm(height);
                } catch (NumberFormatException e) {
                    return createErrorResponse("Invalid height format", HttpStatus.BAD_REQUEST);
                }
            }

            Object weightKgObj = request.get("weightKg");
            if (weightKgObj != null) {
                try {
                    BigDecimal weight = weightKgObj instanceof Number ?
                            BigDecimal.valueOf(((Number) weightKgObj).doubleValue()) :
                            new BigDecimal(weightKgObj.toString());

                    if (weight.compareTo(BigDecimal.valueOf(20)) < 0 ||
                            weight.compareTo(BigDecimal.valueOf(1000)) > 0) {
                        return createErrorResponse("Weight must be between 20 and 1000 kg", HttpStatus.BAD_REQUEST);
                    }
                    user.setWeightKg(weight);
                } catch (NumberFormatException e) {
                    return createErrorResponse("Invalid weight format", HttpStatus.BAD_REQUEST);
                }
            }

            Object fitnessLevelObj = request.get("fitnessLevel");
            if (fitnessLevelObj != null && !fitnessLevelObj.toString().trim().isEmpty()) {
                String fitnessLevel = fitnessLevelObj.toString().toUpperCase();
                if (!List.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(fitnessLevel)) {
                    return createErrorResponse("Invalid fitness level. Must be BEGINNER, INTERMEDIATE, or ADVANCED", HttpStatus.BAD_REQUEST);
                }
                user.setFitnessLevel(fitnessLevel);
            }

            User savedUser = userService.updateUser(userId, user);

            // Returnează răspuns compatibil cu frontend
            Map<String, Object> response = new HashMap<>();
            response.put("userId", savedUser.getUserId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("firstName", savedUser.getFirstName());
            response.put("lastName", savedUser.getLastName());
            response.put("dateOfBirth", savedUser.getDateOfBirth());
            response.put("heightCm", savedUser.getHeightCm());
            response.put("weightKg", savedUser.getWeightKg());
            response.put("fitnessLevel", savedUser.getFitnessLevel());
            response.put("isActive", savedUser.getIsActive());

            log.info("Profile completed successfully for user ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error completing profile", e);
            return createErrorResponse("Error completing profile: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * Schimbă parola utilizatorului
     * POST /api/users/{userId}/change-password
     */
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable Long userId, @RequestBody ChangePasswordRequest request) {
        try {
            log.info("Changing password for user ID: {}", userId);

            // Validare input
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                return createErrorResponse("Current password is required", HttpStatus.BAD_REQUEST);
            }

            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return createErrorResponse("New password must be at least 6 characters", HttpStatus.BAD_REQUEST);
            }

            // Schimbă parola
            userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

            log.info("Password changed successfully for user ID: {}", userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password changed successfully");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Password change validation error for user ID {}: {}", userId, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error changing password for user ID: {}", userId, e);
            return createErrorResponse("Error changing password", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obține toți utilizatorii activi
     * GET /api/users/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveUsers() {
        try {
            log.debug("Getting all active users");

            List<User> activeUsers = userService.findActiveUsers();

            List<DetailedUserResponse> userResponses = activeUsers.stream()
                    .map(DetailedUserResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userResponses);

        } catch (Exception e) {
            log.error("Error getting active users", e);
            return createErrorResponse("Error retrieving active users", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obține utilizatori după nivelul de fitness
     * GET /api/users/by-fitness-level/{fitnessLevel}
     */
    @GetMapping("/by-fitness-level/{fitnessLevel}")
    public ResponseEntity<?> getUsersByFitnessLevel(@PathVariable String fitnessLevel) {
        try {
            log.debug("Getting users by fitness level: {}", fitnessLevel);

            // Validare fitness level
            if (!List.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(fitnessLevel.toUpperCase())) {
                return createErrorResponse("Invalid fitness level. Must be BEGINNER, INTERMEDIATE, or ADVANCED", HttpStatus.BAD_REQUEST);
            }

            List<User> users = userService.findByFitnessLevel(fitnessLevel.toUpperCase());

            List<DetailedUserResponse> userResponses = users.stream()
                    .map(DetailedUserResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userResponses);

        } catch (Exception e) {
            log.error("Error getting users by fitness level: {}", fitnessLevel, e);
            return createErrorResponse("Error retrieving users by fitness level", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Dezactivează un utilizator
     * POST /api/users/{userId}/deactivate
     */
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        try {
            log.info("Deactivating user ID: {}", userId);

            userService.deactivateUser(userId);

            log.info("User deactivated successfully: {}", userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deactivated successfully");
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Deactivation validation error for user ID {}: {}", userId, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error deactivating user ID: {}", userId, e);
            return createErrorResponse("Error deactivating user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Activează un utilizator
     * POST /api/users/{userId}/activate
     */
    @PostMapping("/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId) {
        try {
            log.info("Activating user ID: {}", userId);

            userService.activateUser(userId);

            log.info("User activated successfully: {}", userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User activated successfully");
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Activation validation error for user ID {}: {}", userId, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error activating user ID: {}", userId, e);
            return createErrorResponse("Error activating user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obține statistici despre utilizatori
     * GET /api/users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        try {
            log.debug("Getting user statistics");

            List<User> allActiveUsers = userService.findActiveUsers();

            long beginnerCount = allActiveUsers.stream()
                    .filter(u -> "BEGINNER".equals(u.getFitnessLevel()))
                    .count();

            long intermediateCount = allActiveUsers.stream()
                    .filter(u -> "INTERMEDIATE".equals(u.getFitnessLevel()))
                    .count();

            long advancedCount = allActiveUsers.stream()
                    .filter(u -> "ADVANCED".equals(u.getFitnessLevel()))
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalActiveUsers", allActiveUsers.size());
            stats.put("beginnerUsers", beginnerCount);
            stats.put("intermediateUsers", intermediateCount);
            stats.put("advancedUsers", advancedCount);
            stats.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            return createErrorResponse("Error retrieving user statistics", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Caută utilizatori după username sau email
     * GET /api/users/search?query=john
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        try {
            log.debug("Searching users with query: {}", query);

            if (query == null || query.trim().length() < 2) {
                return createErrorResponse("Search query must be at least 2 characters", HttpStatus.BAD_REQUEST);
            }

            String searchTerm = query.trim().toLowerCase();
            List<User> allActiveUsers = userService.findActiveUsers();

            // Filtrează utilizatorii care conțin termenul de căutare în username sau email
            List<User> filteredUsers = allActiveUsers.stream()
                    .filter(user ->
                            user.getUsername().toLowerCase().contains(searchTerm) ||
                                    user.getEmail().toLowerCase().contains(searchTerm) ||
                                    (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(searchTerm)) ||
                                    (user.getLastName() != null && user.getLastName().toLowerCase().contains(searchTerm))
                    )
                    .collect(Collectors.toList());

            List<DetailedUserResponse> userResponses = filteredUsers.stream()
                    .map(DetailedUserResponse::new)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("totalResults", userResponses.size());
            response.put("users", userResponses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching users with query: {}", query, e);
            return createErrorResponse("Error searching users", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obține utilizatori cu cel puțin un număr minim de planuri de workout
     * GET /api/users/with-min-plans?minPlans=5
     */
    @GetMapping("/with-min-plans")
    public ResponseEntity<?> getUsersWithMinimumPlans(@RequestParam(defaultValue = "1") Long minPlans) {
        try {
            log.debug("Getting users with at least {} workout plans", minPlans);

            if (minPlans < 0) {
                return createErrorResponse("Minimum plans must be non-negative", HttpStatus.BAD_REQUEST);
            }

            List<User> users = userService.findUsersWithMinimumPlans(minPlans);

            List<DetailedUserResponse> userResponses = users.stream()
                    .map(DetailedUserResponse::new)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("minPlans", minPlans);
            response.put("totalResults", userResponses.size());
            response.put("users", userResponses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting users with minimum plans: {}", minPlans, e);
            return createErrorResponse("Error retrieving users with minimum plans", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obține utilizatori cu cel puțin un număr minim de workout-uri completate
     * GET /api/users/with-min-workouts?minWorkouts=10
     */
    @GetMapping("/with-min-workouts")
    public ResponseEntity<?> getUsersWithMinimumWorkouts(@RequestParam(defaultValue = "1") Integer minWorkouts) {
        try {
            log.debug("Getting users with at least {} completed workouts", minWorkouts);

            if (minWorkouts < 0) {
                return createErrorResponse("Minimum workouts must be non-negative", HttpStatus.BAD_REQUEST);
            }

            List<User> users = userService.findUsersWithMinimumCompletedWorkouts(minWorkouts);

            List<DetailedUserResponse> userResponses = users.stream()
                    .map(DetailedUserResponse::new)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("minWorkouts", minWorkouts);
            response.put("totalResults", userResponses.size());
            response.put("users", userResponses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting users with minimum workouts: {}", minWorkouts, e);
            return createErrorResponse("Error retrieving users with minimum workouts", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint de test pentru verificarea funcționării controller-ului
     * GET /api/users/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User controller is working!");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "WorkoutTracker User Management API");
        response.put("version", "1.0.0");

        return ResponseEntity.ok(response);
    }

    // Metode utilitare private

    /**
     * Creează un răspuns de eroare standardizat
     */
    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());

        return ResponseEntity.status(status).body(errorResponse);
    }
}