package com.example.jwt_auth.service;

import com.example.jwt_auth.models.Phone;
import com.example.jwt_auth.models.User;
import com.example.jwt_auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class UserService {

    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    // Password validation regex pattern - requires at least one letter, one number, and minimum 8 characters
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean isEmailValid(String email) {
        return email != null && !email.trim().isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public boolean isPasswordValid(String password) {
        return password != null && !password.trim().isEmpty() && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean isEmailTakenByOtherUser(String email, UUID currentUserId) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        return existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId);
    }
    
    public boolean isUsernameTakenByOtherUser(String username, UUID currentUserId) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        return existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId);
    }
    
    @Transactional
    public User createUser(String username, String email, String password, List<PhoneDto> phones) {
        User user = new User(
                username,
                email,
                passwordEncoder.encode(password)
        );
        
        // Add phones if provided
        if (phones != null && !phones.isEmpty()) {
            for (PhoneDto phoneDto : phones) {
                Phone phone = new Phone(
                        phoneDto.getNumber(),
                        phoneDto.getCityCode(),
                        phoneDto.getCountryCode()
                );
                user.addPhone(phone);
            }
        }
        
        // Save the user to generate the UUID and timestamps
        user = userRepository.save(user);
        
        // Set lastLogin to createdAt for new users
        user.setLastLogin(user.getCreatedAt());
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateLastLogin(User user) {
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    @Transactional
    public Optional<User> updateUser(UUID id, String username, String email, String password, 
                                    Boolean active, List<PhoneDto> phones) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        
        User user = userOpt.get();
        
        // Update username if provided
        if (username != null && !username.trim().isEmpty()) {
            user.setUsername(username);
        }
        
        // Update email if provided
        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email);
        }
        
        // Update password if provided
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        
        // Update active status if provided
        if (active != null) {
            user.setActive(active);
        }
        
        // Update phones if provided
        if (phones != null) {
            // Remove existing phones
            user.getPhones().clear();
            
            // Add new phones
            for (PhoneDto phoneDto : phones) {
                Phone phone = new Phone(
                        phoneDto.getNumber(),
                        phoneDto.getCityCode(),
                        phoneDto.getCountryCode()
                );
                user.addPhone(phone);
            }
        }
        
        return Optional.of(userRepository.save(user));
    }
    
    @Transactional
    public boolean deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        
        userRepository.deleteById(id);
        return true;
    }
    
    public Map<String, Object> convertUserToMap(User user, String token) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        userMap.put("lastLogin", user.getLastLogin());
        userMap.put("active", user.getActive());
        
        // Add token if provided
        if (token != null) {
            userMap.put("token", token);
        }
        
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
    
    // DTO for phone data
    public static class PhoneDto {
        private String number;
        private String cityCode;
        private String countryCode;
        
        public PhoneDto() {}
        
        public PhoneDto(String number, String cityCode, String countryCode) {
            this.number = number;
            this.cityCode = cityCode;
            this.countryCode = countryCode;
        }
        
        public String getNumber() {
            return number;
        }
        
        public void setNumber(String number) {
            this.number = number;
        }
        
        public String getCityCode() {
            return cityCode;
        }
        
        public void setCityCode(String cityCode) {
            this.cityCode = cityCode;
        }
        
        public String getCountryCode() {
            return countryCode;
        }
        
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }
} 