package com.example.jwt_auth.controllers;

import com.example.jwt_auth.models.Phone;
import com.example.jwt_auth.models.User;
import com.example.jwt_auth.repository.PhoneRepository;
import com.example.jwt_auth.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management API")
public class UserController {

    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    // Password validation regex pattern - requires at least one letter, one number, and minimum 8 characters
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PhoneRepository phoneRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "Get all users", description = "Returns a list of all users", 
               security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of users retrieved successfully", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", 
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream()
                .map(this::convertUserToMap)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(convertUserToMap(userOpt.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest updateRequest) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        
        // Validate and update username if provided
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().trim().isEmpty()) {
            // Check if the new username is already taken by another user
            Optional<User> existingUser = userRepository.findByUsername(updateRequest.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
            user.setUsername(updateRequest.getUsername());
        }
        
        // Validate and update email if provided
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().trim().isEmpty()) {
            // Validate email format
            if (!EMAIL_PATTERN.matcher(updateRequest.getEmail()).matches()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
            }
            
            // Check if the new email is already taken by another user
            boolean emailExists = userRepository.existsByEmail(updateRequest.getEmail());
            Optional<User> existingUser = userRepository.findByEmail(updateRequest.getEmail());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use"));
            }
            
            user.setEmail(updateRequest.getEmail());
        }
        
        // Validate and update password if provided
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().trim().isEmpty()) {
            // Validate password format
            if (!PASSWORD_PATTERN.matcher(updateRequest.getPassword()).matches()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long and contain at least one letter and one number"));
            }
            
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        
        // Update active status if provided
        if (updateRequest.getActive() != null) {
            user.setActive(updateRequest.getActive());
        }
        
        // Update phones if provided
        if (updateRequest.getPhones() != null) {
            // Remove existing phones
            user.getPhones().clear();
            
            // Add new phones
            for (PhoneRequest phoneRequest : updateRequest.getPhones()) {
                Phone phone = new Phone(
                        phoneRequest.getNumber(),
                        phoneRequest.getCityCode(),
                        phoneRequest.getCountryCode()
                );
                user.addPhone(phone);
            }
        }
        
        // Save the updated user
        user = userRepository.save(user);
        
        return ResponseEntity.ok(convertUserToMap(user));
    }
    
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> partialUpdateUser(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        
        // Handle username update
        if (updates.containsKey("username")) {
            String username = (String) updates.get("username");
            if (username != null && !username.trim().isEmpty()) {
                // Check if the new username is already taken by another user
                Optional<User> existingUser = userRepository.findByUsername(username);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
                }
                user.setUsername(username);
            }
        }
        
        // Handle email update
        if (updates.containsKey("email")) {
            String email = (String) updates.get("email");
            if (email != null && !email.trim().isEmpty()) {
                // Validate email format
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
                }
                
                // Check if the new email is already taken by another user
                Optional<User> existingUser = userRepository.findByEmail(email);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use"));
                }
                
                user.setEmail(email);
            }
        }
        
        // Handle password update
        if (updates.containsKey("password")) {
            String password = (String) updates.get("password");
            if (password != null && !password.trim().isEmpty()) {
                // Validate password format
                if (!PASSWORD_PATTERN.matcher(password).matches()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long and contain at least one letter and one number"));
                }
                
                user.setPassword(passwordEncoder.encode(password));
            }
        }
        
        // Handle active status update
        if (updates.containsKey("active")) {
            Boolean active = (Boolean) updates.get("active");
            if (active != null) {
                user.setActive(active);
            }
        }
        
        // Handle phones update (more complex, so we'll skip it in PATCH for simplicity)
        // For phones updates, recommend using the PUT endpoint
        
        // Save the updated user
        user = userRepository.save(user);
        
        return ResponseEntity.ok(convertUserToMap(user));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        userRepository.deleteById(id);
        
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
    
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        userMap.put("lastLogin", user.getLastLogin());
        userMap.put("active", user.getActive());
        
        // Add phones to the response
        List<Map<String, String>> phonesList = new ArrayList<>();
        for (Phone phone : user.getPhones()) {
            Map<String, String> phoneMap = new HashMap<>();
            phoneMap.put("id", phone.getId().toString());
            phoneMap.put("number", phone.getNumber());
            phoneMap.put("cityCode", phone.getCityCode());
            phoneMap.put("countryCode", phone.getCountryCode());
            phonesList.add(phoneMap);
        }
        userMap.put("phones", phonesList);
        
        return userMap;
    }
    
    @Data
    public static class UpdateUserRequest {
        private String username;
        private String email;
        private String password;
        private Boolean active;
        private List<PhoneRequest> phones;
    }
    
    @Data
    public static class PhoneRequest {
        private String number;
        private String cityCode;
        private String countryCode;
    }
} 