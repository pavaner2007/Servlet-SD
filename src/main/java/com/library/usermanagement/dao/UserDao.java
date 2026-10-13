package com.library.usermanagement.dao;

import com.library.config.DatabaseConnectionManager;
import com.library.usermanagement.model.User;
import com.library.usermanagement.model.UserRole;
import com.library.usermanagement.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

/**
 * Data Access Object for User Management.
 * Explicitly connects to the isolated User Management Database via DatabaseConnectionManager.getUserAuthConnection().
 */
public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    private static final String SELECT_BY_ID = 
        "SELECT user_id, username, password_hash, email, first_name, last_name, role, status, " +
        "failed_login_count, lockout_until, last_login_at, created_at, updated_at " +
        "FROM users WHERE user_id = ?";

    private static final String SELECT_BY_USERNAME = 
        "SELECT user_id, username, password_hash, email, first_name, last_name, role, status, " +
        "failed_login_count, lockout_until, last_login_at, created_at, updated_at " +
        "FROM users WHERE username = ?";

    private static final String INSERT_USER = 
        "INSERT INTO users (username, password_hash, email, first_name, last_name, role, status) " +
        "VALUES (?, ?, ?, ?, ?, ?::user_role_enum, ?::user_status_enum) RETURNING user_id";

    private static final String INSERT_AUDIT = 
        "INSERT INTO user_audit_logs (user_id, action, ip_address, user_agent) VALUES (?, ?, ?, ?)";

    public Optional<User> findById(Long userId) throws SQLException {
        try (Connection conn = DatabaseConnectionManager.getUserAuthConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection conn = DatabaseConnectionManager.getUserAuthConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Long createUser(User user) throws SQLException {
        try (Connection conn = DatabaseConnectionManager.getUserAuthConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_USER)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFirstName());
            ps.setString(5, user.getLastName());
            ps.setString(6, user.getRole().name());
            ps.setString(7, user.getStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long generatedId = rs.getLong(1);
                    user.setUserId(generatedId);
                    logAuditEvent(generatedId, "USER_CREATED", "127.0.0.1", "System");
                    return generatedId;
                }
            }
        }
        throw new SQLException("Failed to create user, no ID obtained.");
    }

    public void logAuditEvent(Long userId, String action, String ipAddress, String userAgent) {
        try (Connection conn = DatabaseConnectionManager.getUserAuthConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_AUDIT)) {
            if (userId != null) {
                ps.setLong(1, userId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, action);
            ps.setString(3, ipAddress);
            ps.setString(4, userAgent);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log audit event for user: {}", userId, e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setFailedLoginCount(rs.getInt("failed_login_count"));
        Timestamp lockout = rs.getTimestamp("lockout_until");
        if (lockout != null) user.setLockoutUntil(lockout.toInstant());
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) user.setLastLoginAt(lastLogin.toInstant());
        user.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return user;
    }
}
