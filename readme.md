# JWT Authentication API

A secure RESTful API for user authentication and management using JWT (JSON Web Tokens).
📋 Overview
This application provides a robust authentication system with user management capabilities. It implements industry-standard security practices including password hashing, JWT-based authentication, and proper validation.
Key features:

- User registration and authentication
- JWT token generation and validation
- User profile management
- Phone number management for users
- Comprehensive validation and error handling

🛠️ Technology Stack

- Java 17
- Spring Boot 3.x
- Spring Security
- JWT (JSON Web Tokens)
- JPA / Hibernate
- PostgreSQL
- Maven
- JUnit 5 & Mockito (for testing)
🔐 Security Features

- Password encryption using BCrypt
- JWT token-based authentication
- Role-based authorization
- Token expiration and validation
- Protection against common security vulnerabilities
  
🚀 Getting Started

Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+

Configuration
- Clone the repository:
- Configure the database in application.properties:
- Configure JWT properties:

Building and Running
The API will be available at http://localhost:8080

📝 API Documentation

Authentication Endpoints
Register a new user
Request body:
Login
Request body:
User Management Endpoints
Get all users
Get user by ID
Update user
Request body:
Delete user
🧪 Testing
The application includes comprehensive unit tests for all layers. Run the tests with:
📊 Database Schema
The application uses two main entities:
User
id (UUID)
username (String, unique)
email (String, unique)
password (String, encrypted)
role (String)
active (Boolean)
createdAt (LocalDateTime)
updatedAt (LocalDateTime)
lastLogin (LocalDateTime)
Phone
id (UUID)
number (String)
cityCode (String)
countryCode (String)
user_id (UUID, foreign key)
🔄 Authentication Flow
Client registers or logs in
Server validates credentials and returns a JWT token
Client includes the token in the Authorization header for subsequent requests
Server validates the token and processes the request if valid
🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.
1. Fork the repository
Create your feature branch (git checkout -b feature/amazing-feature)
Commit your changes (git commit -m 'Add some amazing feature')
Push to the branch (git push origin feature/amazing-feature)
Open a Pull Request
📜 License
This project is licensed under the MIT License - see the LICENSE file for details.
---
Made with by Cristobal Soto