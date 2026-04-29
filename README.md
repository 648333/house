spring:
  datasource:
    url: jdbc:mysql://localhost:3306/housing_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: your_username  # Update thisspring:
  datasource:
    url: jdbc:mysql://localhost:3306/housing_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: your_username  # Update this
    password: your_password  # Update this
    password: your_password  # Update this# Housing Transaction Platform

## Project Overview
This is a housing transaction platform built with:
- **Frontend**: Vue 3 + Vite + Element Plus
- **Backend**: Spring Boot 3 + Spring Security + JWT + JPA/Hibernate + MySQL

## Prerequisites
- Java 17+
- Node.js 18+
- MySQL Server

## Setup Instructions

### Backend
1. Navigate to `backend` directory.
2. Update `src/main/resources/application.yml` with your MySQL database credentials.
   ```yaml
   spring:
     datasource:
       username: your_username
       password: your_password
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Frontend
1. Navigate to `frontend` directory.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```

## Features Implemented
- **User Authentication**: Register and Login with JWT.
- **Property Management**:
  - List all properties.
  - View property details.
  - Search properties by title.
  - Create new property listings (protected route).
- **Security**:
  - Password encryption (BCrypt).
  - Stateless authentication with JWT.
  - Role-based access control (foundation laid).

## API Endpoints
- `POST /api/auth/register`: Register a new user.
- `POST /api/auth/login`: Login and receive JWT.
- `GET /api/properties`: List all properties.
- `GET /api/properties/{id}`: Get property details.
- `POST /api/properties`: Create a new property (Auth required).
- `GET /api/properties/search?title={query}`: Search properties.
