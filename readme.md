# JWT Authentication API

A secure RESTful API for user authentication and management using JWT (JSON Web Tokens).

## 📋 Overview

This application provides a robust authentication system with user management capabilities. It implements industry-standard security practices including password hashing, JWT-based authentication, and proper validation.

Key features:

- User registration and authentication
- JWT token generation and validation
- User profile management
- Phone number management for users
- Comprehensive validation and error handling

## 🛠️ Technology Stack

- Java 17
- Spring Boot 3.x
- Spring Security
- JWT (JSON Web Tokens)
- JPA / Hibernate
- Maven
- JUnit 5 & Mockito (for testing)

## 🔐 Security Features

- Password encryption using BCrypt
- JWT token-based authentication
- Token expiration and validation
- Protection against common security vulnerabilities
  
## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+

### Configuration

- Clone the repository:
  
```cmd
git clone https://github.com/yourusername/jwt-auth-api.git
cd jwt-auth
```

- Configure the database in application.properties:

```cmd
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
```

- Configure JWT properties:

```cmd
jwt.secret=your_jwt_secret
jwt.expiration=3600000
```

Building and Running

```cmd
mvn clean install
mvn spring-boot:run
```

The API will be available at <http://localhost:8080>

### 📝 API Documentation

Register a new user

```markdown
POST /api/auth/register
```

Request body:

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "Password123",
  "phones": [
    {
      "number": "1234567",
      "cityCode": "1",
      "countryCode": "57"
    }
  ]
}
```
Login

```markdown
POST /api/auth/login
```

Request body: 

```json
{
  "username": "johndoe",
  "password": "Password123"
}
```

User Management Endpoints

Get all users

```markdown
GET /api/users
``` 

Get user by ID

```markdown
GET /api/users/{id}
```

Update user

```markdown
PUT /api/users/{id}
```

```markdown
PATCH /api/users/{id}
```

Delete user

```markdown
DELETE /api/users/{id}
```

Also, you can find the swagger generated documentation at <http://localhost:8080/swagger-ui/index.html>

### 🧪 Testing

The application includes comprehensive unit tests for all layers. Run the tests with:

```cmd
mvn test
```

## 📊 Database Schema

The application uses two main entities:

### User

- id (UUID)
- username (String, unique)
- email (String, unique)
- password (String, encrypted)
- role (String)
- active (Boolean)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)
- lastLogin (LocalDateTime)

### Phone

- id (UUID)
- number (String)
- cityCode (String)
- countryCode (String)
- user_id (UUID, foreign key)

## 🔄 Authentication Flow

1. Client registers or logs in
2. Server validates credentials and returns a JWT token
3. Client includes the token in the Authorization header for subsequent requests
4. Server validates the token and processes the request if valid


Made by Cristobal Soto