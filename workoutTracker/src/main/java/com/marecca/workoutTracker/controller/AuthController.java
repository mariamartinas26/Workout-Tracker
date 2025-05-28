package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller pentru autentificare și înregistrare utilizatori
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * DTO pentru cererea de înregistrare
     */
    public static class RegisterRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String password;

        // Constructors
        public RegisterRequest() {}

        // Getters și setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * DTO pentru cererea de login
     */
    public static class LoginRequest {
        private String email;
        private String password;

        // Constructors
        public LoginRequest() {}

        // Getters și setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * DTO pentru răspunsul utilizatorului (fără parolă)
     */
    public static class UserResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String username;
        private String fitnessLevel;
        private LocalDateTime createdAt;

        // Constructor din User entity
        public UserResponse(User user) {
            this.id = user.getUserId();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.email = user.getEmail();
            this.username = user.getUsername();
            this.fitnessLevel = user.getFitnessLevel();
            this.createdAt = user.getCreatedAt();
        }

        // Getters și setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFitnessLevel() { return fitnessLevel; }
        public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Endpoint pentru înregistrarea unui utilizator nou
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            log.info("Registration attempt for email: {}", request.getEmail());

            // Validare input
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                return createErrorResponse("First name is required", HttpStatus.BAD_REQUEST);
            }

            if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                return createErrorResponse("Last name is required", HttpStatus.BAD_REQUEST);
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return createErrorResponse("Email is required", HttpStatus.BAD_REQUEST);
            }

            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return createErrorResponse("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
            }

            // Verifică dacă email-ul este disponibil
            if (!userService.isEmailAvailable(request.getEmail())) {
                return createErrorResponse("Email already exists", HttpStatus.BAD_REQUEST);
            }

            // Creează User entity
            User user = new User();
            user.setFirstName(request.getFirstName().trim());
            user.setLastName(request.getLastName().trim());
            user.setEmail(request.getEmail().trim().toLowerCase());
            user.setPasswordHash(request.getPassword()); // Va fi criptat în service

            // Generează username din first name + last name
            String username = generateUsername(request.getFirstName(), request.getLastName());
            user.setUsername(username);

            // Setează valori default
            user.setFitnessLevel("BEGINNER");
            user.setIsActive(true);

            // Salvează utilizatorul
            User savedUser = userService.createUser(user);

            log.info("User registered successfully with ID: {}", savedUser.getUserId());

            // Returnează răspunsul fără parolă
            UserResponse userResponse = new UserResponse(savedUser);
            return ResponseEntity.ok(userResponse);

        } catch (IllegalArgumentException e) {
            log.error("Registration validation error: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Registration error for email: {}", request.getEmail(), e);
            return createErrorResponse("Registration failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint pentru autentificarea utilizatorului
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for email: {}", request.getEmail());

            // Validare input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return createErrorResponse("Email is required", HttpStatus.BAD_REQUEST);
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return createErrorResponse("Password is required", HttpStatus.BAD_REQUEST);
            }

            // Găsește utilizatorul după email
            Optional<User> userOptional = userService.findByEmail(request.getEmail().trim().toLowerCase());

            if (userOptional.isEmpty()) {
                log.warn("Login failed - user not found: {}", request.getEmail());
                return createErrorResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
            }

            User user = userOptional.get();

            // Verifică dacă utilizatorul este activ
            if (!user.getIsActive()) {
                log.warn("Login failed - user is inactive: {}", request.getEmail());
                return createErrorResponse("Account is inactive", HttpStatus.UNAUTHORIZED);
            }

            // Verifică parola
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                log.warn("Login failed - invalid password for: {}", request.getEmail());
                return createErrorResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
            }

            log.info("User logged in successfully: {}", user.getEmail());

            // Returnează răspunsul fără parolă
            UserResponse userResponse = new UserResponse(user);
            return ResponseEntity.ok(userResponse);

        } catch (Exception e) {
            log.error("Login error for email: {}", request.getEmail(), e);
            return createErrorResponse("Login failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint pentru verificarea disponibilității email-ului
     * GET /api/auth/check-email?email=test@example.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailAvailability(@RequestParam String email) {
        try {
            boolean available = userService.isEmailAvailable(email.trim().toLowerCase());

            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("available", available);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking email availability: {}", email, e);
            return createErrorResponse("Error checking email availability", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint pentru verificarea disponibilității username-ului
     * GET /api/auth/check-username?username=john_doe
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsernameAvailability(@RequestParam String username) {
        try {
            boolean available = userService.isUsernameAvailable(username.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("available", available);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking username availability: {}", username, e);
            return createErrorResponse("Error checking username availability", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint de test pentru verificarea funcționării
     * GET /api/auth/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Auth endpoints are working!");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "WorkoutTracker Authentication API");
        response.put("version", "1.0.0");

        return ResponseEntity.ok(response);
    }

    // Metode utilitare private

    /**
     * Generează un username unic din numele și prenumele utilizatorului
     */
    private String generateUsername(String firstName, String lastName) {
        String baseUsername = (firstName + "_" + lastName)
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "")
                .replaceAll("_+", "_");

        String username = baseUsername;
        int counter = 1;

        // Dacă username-ul există, adaugă un număr la sfârșit
        while (!userService.isUsernameAvailable(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

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