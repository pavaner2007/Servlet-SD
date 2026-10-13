-- ============================================================================
-- User Management Module - PostgreSQL Relational Schema
-- Database: user_auth_db (Separate Database Connection)
-- ============================================================================

CREATE TYPE user_role_enum AS ENUM ('ADMIN', 'LIBRARIAN', 'MEMBER', 'AUDITOR');
CREATE TYPE user_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFICATION');

-- ----------------------------------------------------------------------------
-- Table: Users
-- Dedicated security principal and authentication store.
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    user_id             BIGSERIAL PRIMARY KEY,
    username            VARCHAR(50) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL, -- Argon2 / BCrypt / PBKDF2 hash
    email               VARCHAR(255) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    role                user_role_enum NOT NULL DEFAULT 'MEMBER',
    status              user_status_enum NOT NULL DEFAULT 'ACTIVE',
    failed_login_count  INT NOT NULL DEFAULT 0,
    lockout_until       TIMESTAMP WITH TIME ZONE,
    last_login_at       TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- ----------------------------------------------------------------------------
-- Table: User Audit Logs
-- Security logging and access history.
-- ----------------------------------------------------------------------------
CREATE TABLE user_audit_logs (
    audit_id            BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(user_id) ON DELETE SET NULL,
    action              VARCHAR(100) NOT NULL, -- e.g. "LOGIN_SUCCESS", "PASSWORD_RESET", "ROLE_CHANGE"
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(255),
    details             JSONB,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user_id ON user_audit_logs(user_id);
CREATE INDEX idx_audit_action ON user_audit_logs(action);
CREATE INDEX idx_audit_created_at ON user_audit_logs(created_at);
