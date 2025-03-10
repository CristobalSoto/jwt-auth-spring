package com.example.jwt_auth.controllers;

import com.example.jwt_auth.models.User;
import com.example.jwt_auth.service.UserService;
import com.example.jwt_auth.dto.PhoneDto;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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
        List<User> users = userService.getAllUsers();
        List<Map<String, Object>> response = users.stream()
                .map(user -> userService.convertUserToMap(user, null))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get user by ID", description = "Returns a specific user by ID", 
               security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "User not found", 
                    content = @Content(schema = @Schema(implementation = Void.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", 
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        Optional<User> userOpt = userService.getUserById(id);
        
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userService.convertUserToMap(userOpt.get(), null));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Usuario no encontrado"));
        }
    }
    
    @Operation(summary = "Update user", description = "Updates a user completely", 
               security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid update data", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "User not found", 
                    content = @Content(schema = @Schema(implementation = Void.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", 
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest updateRequest) {
        // Validate username if provided
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().trim().isEmpty()) {
            if (userService.isUsernameTakenByOtherUser(updateRequest.getUsername(), id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
        }
        
        // Validate email if provided
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().trim().isEmpty()) {
            if (!userService.isEmailValid(updateRequest.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
            }
            
            if (userService.isEmailTakenByOtherUser(updateRequest.getEmail(), id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use"));
            }
        }
        
        // Validate password if provided
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().trim().isEmpty()) {
            if (!userService.isPasswordValid(updateRequest.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long and contain at least one letter and one number"));
            }
        }
        
        // Convert phone requests to DTOs
        List<PhoneDto> phoneDtos = null;
        if (updateRequest.getPhones() != null) {
            phoneDtos = updateRequest.getPhones().stream()
                .map(p -> new PhoneDto(p.getNumber(), p.getCityCode(), p.getCountryCode()))
                .collect(Collectors.toList());
        }
        
        // Update the user
        Optional<User> updatedUserOpt = userService.updateUser(
                id,
                updateRequest.getUsername(),
                updateRequest.getEmail(),
                updateRequest.getPassword(),
                updateRequest.getActive(),
                phoneDtos
        );
        
        if (updatedUserOpt.isPresent()) {
            return ResponseEntity.ok(userService.convertUserToMap(updatedUserOpt.get(), null));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(summary = "Partially update user", description = "Updates specific fields of a user", 
               security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid update data", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "User not found", 
                    content = @Content(schema = @Schema(implementation = Void.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", 
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> partialUpdateUser(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        Optional<User> userOpt = userService.getUserById(id);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        String username = null;
        String email = null;
        String password = null;
        Boolean active = null;
        
        // Handle username update
        if (updates.containsKey("username")) {
            username = (String) updates.get("username");
            if (username != null && !username.trim().isEmpty()) {
                if (userService.isUsernameTakenByOtherUser(username, id)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
                }
            }
        }
        
        // Handle email update
        if (updates.containsKey("email")) {
            email = (String) updates.get("email");
            if (email != null && !email.trim().isEmpty()) {
                if (!userService.isEmailValid(email)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
                }
                
                if (userService.isEmailTakenByOtherUser(email, id)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use"));
                }
            }
        }
        
        // Handle password update
        if (updates.containsKey("password")) {
            password = (String) updates.get("password");
            if (password != null && !password.trim().isEmpty()) {
                if (!userService.isPasswordValid(password)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long and contain at least one letter and one number"));
                }
            }
        }
        
        // Handle active status update
        if (updates.containsKey("active")) {
            active = (Boolean) updates.get("active");
        }
        
        // Update the user (without changing phones in PATCH)
        Optional<User> updatedUserOpt = userService.updateUser(
                id,
                username,
                email,
                password,
                active,
                null // Don't update phones in PATCH
        );
        
        return ResponseEntity.ok(userService.convertUserToMap(updatedUserOpt.get(), null));
    }
    
    @Operation(summary = "Delete user", description = "Deletes a user by ID", 
               security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "User not found", 
                    content = @Content(schema = @Schema(implementation = Void.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", 
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        boolean deleted = userService.deleteUser(id);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Usuario no encontrado"));
        }
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