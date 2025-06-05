package com.marecca.workoutTracker.controller;

import com.marecca.workoutTracker.dto.request.CompleteProfileRequest;
import com.marecca.workoutTracker.dto.request.LoginRequest;
import com.marecca.workoutTracker.dto.request.RegisterRequest;
import com.marecca.workoutTracker.entity.User;
import com.marecca.workoutTracker.service.UserService;
import com.marecca.workoutTracker.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
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

            if (userService.existsByEmail(request.getEmail())) {
                return createErrorResponse("Email already exists", HttpStatus.BAD_REQUEST);
            }

            //generating username
            String username = request.getUsername();
            if (username == null || username.trim().isEmpty()) {
                username = request.getEmail().split("@")[0];
                String baseUsername = username;
                int counter = 1;
                while (userService.existsByUsername(username)) {
                    username = baseUsername + counter;
                    counter++;
                }
            } else {
                if (userService.existsByUsername(username)) {
                    return createErrorResponse("Username already exists", HttpStatus.BAD_REQUEST);
                }
            }

            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(request.getEmail().trim());
            newUser.setFirstName(request.getFirstName().trim());
            newUser.setLastName(request.getLastName().trim());

            User savedUser = userService.registerUser(newUser, request.getPassword());

            //generate JWT token
            String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getUserId());

            Map<String, Object> response = createUserResponse(savedUser);
            response.put("token", token);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Registration failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Optional<User> userOptional = userService.authenticateUser(request.getEmail(), request.getPassword());

            if (userOptional.isEmpty()) {
                return createErrorResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
            }

            User user = userOptional.get();
            if (!user.getIsActive()) {
                return createErrorResponse("Account is deactivated", HttpStatus.UNAUTHORIZED);
            }

            //generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getUserId());

            Map<String, Object> response = createUserResponse(user);
            response.put("token", token);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Login failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@RequestBody CompleteProfileRequest request) {
        try {
            if (request.getUserId() == null) {
                return createErrorResponse("User ID is required", HttpStatus.BAD_REQUEST);
            }

            Optional<User> userOptional = userService.findById(request.getUserId());
            if (userOptional.isEmpty()) {
                return createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

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
            return createErrorResponse("Error completing profile: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return createErrorResponse("Invalid token format", HttpStatus.BAD_REQUEST);
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return createErrorResponse("Invalid token", HttpStatus.UNAUTHORIZED);
            }

            String email = jwtUtil.getEmailFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);

            String newToken = jwtUtil.generateToken(email, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Token refresh failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("id", user.getUserId());
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