-- ============================================================================
-- User Management Module - MySQL Relational Schema (8.0+)
-- Database: user_auth_db (Separate Database Connection)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS user_auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_auth_db;

CREATE TABLE IF NOT EXISTS users (
    user_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(50) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    role                ENUM('ADMIN', 'LIBRARIAN', 'MEMBER', 'AUDITOR') NOT NULL DEFAULT 'MEMBER',
    status              ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFICATION') NOT NULL DEFAULT 'ACTIVE',
    failed_login_count  INT NOT NULL DEFAULT 0,
    lockout_until       DATETIME NULL,
    last_login_at       DATETIME NULL,
    password_changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_audit_logs (
    audit_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NULL,
    action              VARCHAR(100) NOT NULL,
    ip_address          VARCHAR(45) NULL,
    user_agent          VARCHAR(255) NULL,
    details             JSON NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB;
