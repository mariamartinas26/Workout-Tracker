package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    // DTOs
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;

        // Getters și setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class CompleteProfileRequest {
        private Long userId;
        private LocalDate dateOfBirth;
        private Integer heightCm;
        private BigDecimal weightKg;
        private String fitnessLevel;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
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
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Auth controller is working!");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "WorkoutTracker Authentication API");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            log.info("Register attempt for email: {}", request.getEmail());

            // Validare
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return createErrorResponse("Email is required", HttpStatus.BAD_REQUEST);
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return createErrorResponse("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
            }
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                return createErrorResponse("First name is required", HttpStatus.BAD_REQUEST);
            }
            if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                return createErrorResponse("Last name is required", HttpStatus.BAD_REQUEST);
            }

            // Verifică dacă utilizatorul există deja
            if (userService.existsByEmail(request.getEmail())) {
                return createErrorResponse("Email already exists", HttpStatus.BAD_REQUEST);
            }

            // Generează username dacă nu este furnizat
            String username = request.getUsername();
            if (username == null || username.trim().isEmpty()) {
                // Generează username din email (partea dinaintea @)
                username = request.getEmail().split("@")[0];

                // Dacă username-ul generat există deja, adaugă un număr
                String baseUsername = username;
                int counter = 1;
                while (userService.existsByUsername(username)) {
                    username = baseUsername + counter;
                    counter++;
                }
                log.info("Generated username: {}", username);
            } else {
                // Verifică dacă username-ul furnizat există
                if (userService.existsByUsername(username)) {
                    return createErrorResponse("Username already exists", HttpStatus.BAD_REQUEST);
                }
            }

            // Creează utilizatorul
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(request.getEmail().trim());
            newUser.setFirstName(request.getFirstName().trim());
            newUser.setLastName(request.getLastName().trim());

            User savedUser = userService.registerUser(newUser, request.getPassword());

            Map<String, Object> response = createUserResponse(savedUser);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during registration", e);
            return createErrorResponse("Registration failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Login utilizator
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for email: {}", request.getEmail());

            Optional<User> userOptional = userService.authenticateUser(request.getEmail(), request.getPassword());

            if (userOptional.isEmpty()) {
                return createErrorResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
            }

            User user = userOptional.get();
            if (!user.getIsActive()) {
                return createErrorResponse("Account is deactivated", HttpStatus.UNAUTHORIZED);
            }

            Map<String, Object> response = createUserResponse(user);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during login", e);
            return createErrorResponse("Login failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Completează profilul utilizatorului
     */
    @PutMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@RequestBody CompleteProfileRequest request) {
        try {
            log.info("Complete profile request for user ID: {}", request.getUserId());

            if (request.getUserId() == null) {
                return createErrorResponse("User ID is required", HttpStatus.BAD_REQUEST);
            }

            Optional<User> userOptional = userService.findById(request.getUserId());
            if (userOptional.isEmpty()) {
                return createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            // Actualizează profilul
            if (request.getDateOfBirth() != null) {
                user.setDateOfBirth(request.getDateOfBirth());
            }
            if (request.getHeightCm() != null) {
                user.setHeightCm(request.getHeightCm());
            }
            if (request.getWeightKg() != null) {
                user.setWeightKg(request.getWeightKg());
            }
            if (request.getFitnessLevel() != null) {
                user.setFitnessLevel(request.getFitnessLevel());
            }

            User savedUser = userService.updateUser(request.getUserId(), user);

            Map<String, Object> response = createUserResponse(savedUser);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error completing profile for user ID: {}", request.getUserId(), e);
            return createErrorResponse("Error completing profile: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Metode helper
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId()); // Asigură-te că este userId, nu id
        response.put("id", user.getUserId()); // Adaugă și id pentru compatibilitate
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("dateOfBirth", user.getDateOfBirth());
        response.put("heightCm", user.getHeightCm());
        response.put("weightKg", user.getWeightKg());
        response.put("fitnessLevel", user.getFitnessLevel());
        response.put("isActive", user.getIsActive());
        return response;
    }

    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }
}