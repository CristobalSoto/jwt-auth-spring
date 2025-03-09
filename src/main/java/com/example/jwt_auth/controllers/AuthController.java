package com.example.jwt_auth.controllers;

import com.example.jwt_auth.models.User;
import com.example.jwt_auth.security.JwtUtil;
import com.example.jwt_auth.service.UserService;
import com.example.jwt_auth.service.UserService.PhoneDto;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Operation(summary = "Login a user", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful authentication", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid username or password", 
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            
            // Update last login time
            User user = userService.getUserByUsername(authRequest.getUsername()).orElseThrow();
            user = userService.updateLastLogin(user);
            
            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(userService.convertUserToMap(user, jwt));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid username or password"));
        }
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully", 
                    content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid registration data", 
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        // Validate username
        if (userService.getUserByUsername(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        
        // Validate email
        if (!userService.isEmailValid(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
        }
        
        // Check if email is already in use
        if (userService.isEmailTaken(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use"));
        }
        
        // Validate password
        if (!userService.isPasswordValid(registerRequest.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long and contain at least one letter and one number"));
        }

        // Convert phone requests to DTOs
        List<PhoneDto> phoneDtos = null;
        if (registerRequest.getPhones() != null) {
            phoneDtos = registerRequest.getPhones().stream()
                .map(p -> new PhoneDto(p.getNumber(), p.getCityCode(), p.getCountryCode()))
                .collect(Collectors.toList());
        }
        
        // Create the user
        User user = userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                phoneDtos
        );
        
        // Generate token for the newly registered user
        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        
        return ResponseEntity.ok(userService.convertUserToMap(user, jwt));
    }

    @Data
    public static class AuthRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private List<PhoneRequest> phones;
    }
    
    @Data
    public static class PhoneRequest {
        private String number;
        private String cityCode;
        private String countryCode;
    }
} 