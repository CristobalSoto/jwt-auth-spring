package com.example.jwt_auth;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Remove the @SpringBootApplication annotation to avoid conflict
// with the main application class
@ComponentScan(basePackages = "com.example.jwt_auth")
@EntityScan("com.example.jwt_auth.models")
@EnableJpaRepositories("com.example.jwt_auth.repository")
public class TestApplication {
    // Empty class, just for configuration
} 