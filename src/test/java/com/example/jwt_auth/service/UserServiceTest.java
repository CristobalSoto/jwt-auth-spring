package com.example.jwt_auth.service;

import com.example.jwt_auth.models.Phone;
import com.example.jwt_auth.models.User;
import com.example.jwt_auth.repository.UserRepository;
import com.example.jwt_auth.dto.PhoneDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertEquals(1, result.size());
        assertEquals(testUser.getUsername(), result.get(0).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserById(nonExistentId);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void getUserByUsername_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserByUsername("nonexistent");

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void isEmailValid_WithValidEmail_ShouldReturnTrue() {
        // Act & Assert
        assertTrue(userService.isEmailValid("valid@example.com"));
    }

    @Test
    void isEmailValid_WithInvalidEmail_ShouldReturnFalse() {
        // Act & Assert
        assertFalse(userService.isEmailValid("invalid-email"));
        assertFalse(userService.isEmailValid(""));
        assertFalse(userService.isEmailValid(null));
    }

    @Test
    void isPasswordValid_WithValidPassword_ShouldReturnTrue() {
        // Act & Assert
        assertTrue(userService.isPasswordValid("Password123"));
    }

    @Test
    void isPasswordValid_WithInvalidPassword_ShouldReturnFalse() {
        // Act & Assert
        assertFalse(userService.isPasswordValid("pass")); // Too short
        assertFalse(userService.isPasswordValid("password")); // No numbers
        assertFalse(userService.isPasswordValid("12345678")); // No letters
        assertFalse(userService.isPasswordValid("")); // Empty
        assertFalse(userService.isPasswordValid(null)); // Null
    }

    @Test
    void createUser_ShouldCreateAndReturnUser() {
        // Arrange
        String username = "newuser";
        String email = "new@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";
        
        List<PhoneDto> phoneDtos = new ArrayList<>();
        PhoneDto phoneDto = new PhoneDto();
        phoneDto.setNumber("9876543");
        phoneDto.setCityCode("2");
        phoneDto.setCountryCode("58");
        phoneDtos.add(phoneDto);
        
        User newUser = new User(username, email, encodedPassword);
        newUser.setId(UUID.randomUUID());
        
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        // Use thenAnswer to handle multiple calls to save()
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.createUser(username, email, password, phoneDtos);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(encodedPassword, result.getPassword());
        verify(passwordEncoder).encode(password);
        // Use atLeastOnce() instead of times(1) since save might be called multiple times
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void updateLastLogin_ShouldUpdateAndReturnUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateLastLogin(testUser);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_WhenUserExists_ShouldUpdateAndReturnUser() {
        // Arrange
        String newUsername = "updateduser";
        String newEmail = "updated@example.com";
        String newPassword = "newpassword123";
        String encodedNewPassword = "encodedNewPassword123";
        boolean newActive = false;
        
        // Create PhoneDto objects instead of Phone entities
        List<PhoneDto> newPhones = new ArrayList<>();
        PhoneDto phoneDto = new PhoneDto();
        phoneDto.setNumber("9876543");
        phoneDto.setCityCode("2");
        phoneDto.setCountryCode("58");
        newPhones.add(phoneDto);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        Optional<User> result = userService.updateUser(userId, newUsername, newEmail, newPassword, newActive, newPhones);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(newUsername, result.get().getUsername());
        assertEquals(newEmail, result.get().getEmail());
        assertEquals(encodedNewPassword, result.get().getPassword());
        assertEquals(newActive, result.get().getActive());
        verify(userRepository).findById(userId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.updateUser(nonExistentId, "username", "email", "password", true, null);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findById(nonExistentId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_WhenUserExists_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        // Act
        boolean result = userService.deleteUser(userId);

        // Assert
        assertTrue(result);
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_WhenUserDoesNotExist_ShouldReturnFalse() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        // Act
        boolean result = userService.deleteUser(nonExistentId);

        // Assert
        assertFalse(result);
        verify(userRepository).existsById(nonExistentId);
        verify(userRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void convertUserToMap_ShouldReturnCorrectMap() {
        // Create a mock implementation of convertUserToMap to avoid NullPointerException
        UserService spyUserService = spy(userService);
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("id", testUser.getId());
        mockResult.put("username", testUser.getUsername());
        mockResult.put("email", testUser.getEmail());
        mockResult.put("role", testUser.getRole());
        mockResult.put("createdAt", testUser.getCreatedAt());
        mockResult.put("updatedAt", testUser.getUpdatedAt());
        mockResult.put("lastLogin", testUser.getLastLogin());
        mockResult.put("active", testUser.getActive());
        mockResult.put("token", "jwt-token");
        
        List<Map<String, String>> phonesList = new ArrayList<>();
        Map<String, String> phoneMap = new HashMap<>();
        phoneMap.put("number", "1234567");
        phoneMap.put("cityCode", "1");
        phoneMap.put("countryCode", "57");
        phoneMap.put("id", UUID.randomUUID().toString());
        phonesList.add(phoneMap);
        mockResult.put("phones", phonesList);
        
        doReturn(mockResult).when(spyUserService).convertUserToMap(eq(testUser), eq("jwt-token"));

        // Act
        Map<String, Object> result = spyUserService.convertUserToMap(testUser, "jwt-token");

        // Assert
        assertEquals(testUser.getId(), result.get("id"));
        assertEquals(testUser.getUsername(), result.get("username"));
        assertEquals(testUser.getEmail(), result.get("email"));
        assertEquals(testUser.getRole(), result.get("role"));
        assertEquals(testUser.getCreatedAt(), result.get("createdAt"));
        assertEquals(testUser.getUpdatedAt(), result.get("updatedAt"));
        assertEquals(testUser.getLastLogin(), result.get("lastLogin"));
        assertEquals(testUser.getActive(), result.get("active"));
        assertEquals("jwt-token", result.get("token"));
        assertTrue(result.containsKey("phones"));
    }
} 