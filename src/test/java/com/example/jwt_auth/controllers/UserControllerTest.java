package com.example.jwt_auth.controllers;

import com.example.jwt_auth.controllers.UserController.UpdateUserRequest;
import com.example.jwt_auth.controllers.UserController.PhoneRequest;
import com.example.jwt_auth.models.Phone;
import com.example.jwt_auth.models.User;
import com.example.jwt_auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;
    private UUID userId;
    private Map<String, Object> userResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        
        userId = UUID.randomUUID();
        testUser = new User("testuser", "test@example.com", "encodedPassword");
        testUser.setId(userId);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setLastLogin(LocalDateTime.now());
        testUser.setActive(true);
        testUser.setRole("USER");

        // Create a real Phone object with ID to avoid NullPointerException
        Phone phone = new Phone("1234567", "1", "57");
        // Set ID manually using reflection to avoid modifying your Phone class
        try {
            java.lang.reflect.Field idField = Phone.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(phone, UUID.randomUUID());
        } catch (Exception e) {
            e.printStackTrace();
        }
        testUser.addPhone(phone);
        
        userResponse = new HashMap<>();
        userResponse.put("id", testUser.getId());
        userResponse.put("username", testUser.getUsername());
        userResponse.put("email", testUser.getEmail());
        userResponse.put("role", testUser.getRole());
        userResponse.put("active", testUser.getActive());
        
        List<Map<String, String>> phonesList = new ArrayList<>();
        Map<String, String> phoneMap = new HashMap<>();
        phoneMap.put("number", "1234567");
        phoneMap.put("cityCode", "1");
        phoneMap.put("countryCode", "57");
        phoneMap.put("id", UUID.randomUUID().toString());
        phonesList.add(phoneMap);
        userResponse.put("phones", phonesList);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() throws Exception {
        // Arrange
        when(userService.getAllUsers()).thenReturn(List.of(testUser));
        when(userService.convertUserToMap(eq(testUser), isNull())).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"));
        
        verify(userService).getAllUsers();
        verify(userService).convertUserToMap(eq(testUser), isNull());
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() throws Exception {
        // Arrange
        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        when(userService.convertUserToMap(eq(testUser), isNull())).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
        
        verify(userService).getUserById(userId);
        verify(userService).convertUserToMap(eq(testUser), isNull());
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
        
        verify(userService).getUserById(userId);
        verify(userService, never()).convertUserToMap(any(User.class), any());
    }

    @Test
    void updateUser_WhenUserExists_ShouldReturnUpdatedUser() throws Exception {
        // Arrange
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setUsername("updateduser");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setPassword("Password123");
        updateRequest.setActive(true);
        
        PhoneRequest phoneRequest = new PhoneRequest();
        phoneRequest.setNumber("9876543");
        phoneRequest.setCityCode("2");
        phoneRequest.setCountryCode("58");
        updateRequest.setPhones(List.of(phoneRequest));

        User updatedUser = new User("updateduser", "updated@example.com", "encodedPassword");
        updatedUser.setId(userId);
        
        Map<String, Object> updatedUserResponse = new HashMap<>();
        updatedUserResponse.put("id", updatedUser.getId());
        updatedUserResponse.put("username", updatedUser.getUsername());
        updatedUserResponse.put("email", updatedUser.getEmail());

        when(userService.isUsernameTakenByOtherUser("updateduser", userId)).thenReturn(false);
        when(userService.isEmailValid("updated@example.com")).thenReturn(true);
        when(userService.isEmailTakenByOtherUser("updated@example.com", userId)).thenReturn(false);
        when(userService.isPasswordValid("Password123")).thenReturn(true);
        when(userService.updateUser(eq(userId), eq("updateduser"), eq("updated@example.com"), 
                                   eq("Password123"), eq(true), anyList()))
            .thenReturn(Optional.of(updatedUser));
        when(userService.convertUserToMap(eq(updatedUser), isNull())).thenReturn(updatedUserResponse);

        // Act & Assert
        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
        
        verify(userService).isUsernameTakenByOtherUser("updateduser", userId);
        verify(userService).isEmailValid("updated@example.com");
        verify(userService).isEmailTakenByOtherUser("updated@example.com", userId);
        verify(userService).isPasswordValid("Password123");
        verify(userService).updateUser(eq(userId), eq("updateduser"), eq("updated@example.com"), 
                                      eq("Password123"), eq(true), anyList());
        verify(userService).convertUserToMap(eq(updatedUser), isNull());
    }

    @Test
    void deleteUser_WhenUserExists_ShouldReturnSuccess() throws Exception {
        // Arrange
        when(userService.deleteUser(userId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
        
        verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUser_WhenUserDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(userService.deleteUser(userId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
        
        verify(userService).deleteUser(userId);
    }
} 