-- ============================================================================
-- User Management Module - MySQL Database & Table Setup
-- Database: user_auth_db or library_db
-- ============================================================================

CREATE DATABASE IF NOT EXISTS user_auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_auth_db;

-- Table for UserServlet CRUD operations
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Optional initial seed record for testing
INSERT INTO users (id, name, email) VALUES 
(1, 'John Doe', 'john.doe@example.com')
ON DUPLICATE KEY UPDATE name=VALUES(name);
