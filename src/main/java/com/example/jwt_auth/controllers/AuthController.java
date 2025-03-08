package com.example.jwt_auth.controllers;

import com.example.jwt_auth.models.Phone;
import com.example.jwt_auth.models.User;
import com.example.jwt_auth.repository.UserRepository;
import com.example.jwt_auth.security.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    // Password validation regex pattern - requires at least one letter, one number, and minimum 8 characters
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
            User user = userRepository.findByUsername(authRequest.getUsername()).orElseThrow();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(createUserResponse(user, jwt));
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
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        
        // Validate email presence
        if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        // Validate email format using regex
        if (!EMAIL_PATTERN.matcher(registerRequest.getEmail()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
        }
        
        // Check if email is already in use
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use"));
        }
        
        // Validate password format (must contain at least one letter, one number, and be at least 8 characters long)
        if (!PASSWORD_PATTERN.matcher(registerRequest.getPassword()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long and contain at least one letter and one number"));
        }

        // Create a new user with username, email, and password
        User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword())
        );
        
        // Add phones if provided
        if (registerRequest.getPhones() != null && !registerRequest.getPhones().isEmpty()) {
            for (PhoneRequest phoneRequest : registerRequest.getPhones()) {
                Phone phone = new Phone(
                        phoneRequest.getNumber(),
                        phoneRequest.getCityCode(),
                        phoneRequest.getCountryCode()
                );
                user.addPhone(phone);
            }
        }
        
        // Save the user to generate the UUID and timestamps
        user = userRepository.save(user);
        
        // Set lastLogin to createdAt for new users
        user.setLastLogin(user.getCreatedAt());
        user = userRepository.save(user);
        
        // Generate token for the newly registered user
        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        
        return ResponseEntity.ok(createUserResponse(user, jwt));
    }
    
    private Map<String, Object> createUserResponse(User user, String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("createdAt", user.getCreatedAt());
        response.put("updatedAt", user.getUpdatedAt());
        response.put("lastLogin", user.getLastLogin());
        response.put("active", user.getActive());
        
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
        response.put("phones", phonesList);
        
        return response;
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