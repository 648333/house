-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS housing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE housing_db;

-- Create Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) DEFAULT 'USER'
);

-- Create Properties table
CREATE TABLE IF NOT EXISTS properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19, 2) NOT NULL,
    address VARCHAR(255) NOT NULL,
    image_url VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    owner_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Insert Sample Users (Password is '648666' hashed with BCrypt)
-- Note: current runtime DataInitializer resets sample accounts to password 'password'
INSERT INTO users (username, password, email, role) VALUES 
('admin', '$2a$10$piAAl47AcD.I85tVAQd3uuHxWwAK0UqCFCii9sHltVBJctDtmH9Mi', 'admin@example.com', 'ADMIN'),
('user1', '$2a$10$piAAl47AcD.I85tVAQd3uuHxWwAK0UqCFCii9sHltVBJctDtmH9Mi', 'user1@example.com', 'USER'),
('agent1', '$2a$10$piAAl47AcD.I85tVAQd3uuHxWwAK0UqCFCii9sHltVBJctDtmH9Mi', 'agent1@example.com', 'AGENT');

-- Insert Sample Properties
INSERT INTO properties (title, description, price, address, image_url, status, owner_id, created_at) VALUES 
('Modern Apartment in City Center', 'Beautiful 2-bedroom apartment with city views.', 350000.00, '123 Main St, New York, NY', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 'APPROVED', 2, NOW()),
('Cozy Cottage near Lake', 'Charming 3-bedroom cottage perfect for weekends.', 275000.00, '456 Lakeview Dr, Austin, TX', 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 'APPROVED', 3, NOW()),
('Luxury Villa with Pool', 'Spacious villa with private pool and garden.', 1200000.00, '789 Ocean Blvd, Miami, FL', 'https://images.unsplash.com/photo-1613977257363-707ba9348227?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 'PENDING', 2, NOW());
