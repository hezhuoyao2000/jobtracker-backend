<div align="center">

# ✨ Job Kanban Backend API ✨

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791.svg)

**[📚 Swagger API Docs](http://localhost:8080/swagger-ui/index.html)**

[中文](./README.md) | English

</div>

## 📋 Project Overview

Job Kanban Backend API is a modern RESTful API service built with Spring Boot, providing complete backend support for job application tracking. The project follows a layered architecture and supports user authentication, JWT tokens, and board management.

## ✨ Core Features

- **User Authentication**: Registration, login with JWT Token authentication
- **Board Management**: Create and load user board data
- **Column Management**: Customizable columns (Wish list, Applied, Interviewing, Offered, Rejected)
- **Card Management**: Full CRUD operations with drag-and-drop support
- **Soft Delete**: Cards support soft delete
- **RESTful API**: Complete API endpoints with Swagger documentation

## 🛠 Tech Stack

### Backend
- **Java 17** - Programming Language
- **Spring Boot 3.5.5** - Application Framework
- **Spring Data JPA** - Data Access Layer
- **PostgreSQL** - Database (UUID primary keys, jsonb support)

### Security
- **JWT** - JSON Web Token Authentication
- **Spring Security** - Security Framework

### Documentation & Tools
- **SpringDoc OpenAPI** - API Documentation (Swagger UI)
- **Lombok** - Code Simplification
- **Maven** - Build Tool

## 🚀 Quick Start

### Prerequisites
- Java 17+
- PostgreSQL Database
- Maven

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd myfirstspringboot
```

2. Configure database connection (application.yml):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password
```

3. Build the project:
```bash
mvnw.cmd clean package
# or
./mvnw clean package
```

4. Run the application:
```bash
mvnw.cmd spring-boot:run
# or with database password
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_PASSWORD=your_password"
```

5. Access API documentation:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- API Docs (JSON): http://localhost:8080/v3/api-docs

## 📁 Project Structure

```
src/main/java/com/example/myfirstspringboot/
├── config/           # Configuration classes
├── controller/       # Controller layer
├── dto/             # Data Transfer Objects
│   ├── request/     # Request DTOs
│   └── response/    # Response DTOs
├── entity/          # JPA Entities
├── exception/       # Exception handling
├── repository/      # Data access layer
├── service/         # Business logic layer
│   └── impl/        # Implementations
└── util/            # Utilities
```

## 📖 API Endpoints

### Authentication
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/auth/register | User registration |
| POST | /api/auth/login | User login |

### Board
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/boards | Create board |
| GET | /api/boards/{userId} | Get user board |

### Column
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/columns | Create column |
| PUT | /api/columns/{id} | Update column |
| DELETE | /api/columns/{id} | Delete column |

### Card
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/cards | Create card |
| PUT | /api/cards/{id} | Update card |
| DELETE | /api/cards/{id} | Delete card |
| PUT | /api/cards/{id}/move | Move card |

## 🧪 Testing

```bash
# Run all tests
mvnw.cmd test

# Run specific test class
mvnw.cmd test -Dtest=BoardServiceImplTest
```

## 📈 Project Status

**Current Version**: v1.0.0

### ✅ Completed Features
- User registration and login
- JWT Token authentication
- Board CRUD operations
- Column CRUD operations
- Card CRUD operations
- Card drag-and-drop ordering
- Soft delete functionality
- Global exception handling
- Swagger API documentation
- Response data wrapper

### 🔄 In Progress / Planned
- Batch operation optimization
- Cache support
- Performance monitoring

## 📄 License

This project is licensed under the MIT License.
