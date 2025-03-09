package com.example.jwt_auth.controllers;

import com.example.jwt_auth.controllers.AuthController.AuthRequest;
import com.example.jwt_auth.controllers.AuthController.PhoneRequest;
import com.example.jwt_auth.controllers.AuthController.RegisterRequest;
import com.example.jwt_auth.models.User;
import com.example.jwt_auth.security.JwtUtil;
import com.example.jwt_auth.service.UserService;
import com.example.jwt_auth.service.UserService.PhoneDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;
    private String testToken;
    private Map<String, Object> userResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        
        testUser = new User("testuser", "test@example.com", "encodedPassword");
        testUser.setId(UUID.randomUUID());
        testToken = "test-jwt-token";
        
        userResponse = new HashMap<>();
        userResponse.put("id", testUser.getId());
        userResponse.put("username", testUser.getUsername());
        userResponse.put("email", testUser.getEmail());
        userResponse.put("token", testToken);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUserWithToken() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userService.updateLastLogin(testUser)).thenReturn(testUser);
        when(jwtUtil.generateToken(userDetails)).thenReturn(testToken);
        when(userService.convertUserToMap(testUser, testToken)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value(testToken));
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByUsername("testuser");
        verify(userService).updateLastLogin(testUser);
        verify(jwtUtil).generateToken(userDetails);
        verify(userService).convertUserToMap(testUser, testToken);
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("wrongpassword");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, never()).getUserByUsername(anyString());
        verify(userService, never()).updateLastLogin(any(User.class));
    }

    @Test
    void register_WithValidData_ShouldReturnCreatedUser() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("Password123");
        
        PhoneRequest phoneRequest = new PhoneRequest();
        phoneRequest.setNumber("1234567");
        phoneRequest.setCityCode("1");
        phoneRequest.setCountryCode("57");
        registerRequest.setPhones(List.of(phoneRequest));

        User newUser = new User("newuser", "new@example.com", "encodedPassword");
        newUser.setId(UUID.randomUUID());
        UserDetails userDetails = mock(UserDetails.class);
        
        when(userService.getUserByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.isEmailValid("new@example.com")).thenReturn(true);
        when(userService.isEmailTaken("new@example.com")).thenReturn(false);
        when(userService.isPasswordValid("Password123")).thenReturn(true);
        when(userService.createUser(eq("newuser"), eq("new@example.com"), eq("Password123"), anyList()))
            .thenReturn(newUser);
        when(userDetailsService.loadUserByUsername("newuser")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn(testToken);
        
        Map<String, Object> newUserResponse = new HashMap<>();
        newUserResponse.put("id", newUser.getId());
        newUserResponse.put("username", newUser.getUsername());
        newUserResponse.put("email", newUser.getEmail());
        newUserResponse.put("token", testToken);
        
        when(userService.convertUserToMap(newUser, testToken)).thenReturn(newUserResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.token").value(testToken));
        
        verify(userService).getUserByUsername("newuser");
        verify(userService).isEmailValid("new@example.com");
        verify(userService).isEmailTaken("new@example.com");
        verify(userService).isPasswordValid("Password123");
        verify(userService).createUser(eq("newuser"), eq("new@example.com"), eq("Password123"), anyList());
        verify(userDetailsService).loadUserByUsername("newuser");
        verify(jwtUtil).generateToken(userDetails);
        verify(userService).convertUserToMap(newUser, testToken);
    }

    @Test
    void register_WithExistingUsername_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("existinguser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("Password123");

        when(userService.getUserByUsername("existinguser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
        
        verify(userService).getUserByUsername("existinguser");
        verify(userService, never()).createUser(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void register_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("invalid-email");
        registerRequest.setPassword("Password123");

        when(userService.getUserByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.isEmailValid("invalid-email")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid email format"));
        
        verify(userService).getUserByUsername("newuser");
        verify(userService).isEmailValid("invalid-email");
        verify(userService, never()).createUser(anyString(), anyString(), anyString(), anyList());
    }
} 