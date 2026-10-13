package com.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Multi-DataSource Database Connection Manager.
 * Maintains two completely separate, isolated connection pools:
 * 1. LibraryDataSource (Catalog, Circulation, Inventory)
 * 2. UserAuthDataSource (User Management, Authentication, Roles)
 */
public class DatabaseConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);

    private static volatile DatabaseConnectionManager instance;
    private final HikariDataSource libraryDataSource;
    private final HikariDataSource userAuthDataSource;

    private DatabaseConnectionManager() {
        Properties props = loadProperties();

        // 1. Initialize Library DataSource Pool
        HikariConfig libraryConfig = new HikariConfig();
        libraryConfig.setPoolName(props.getProperty("library.db.poolName", "HikariPool-LibraryDB"));
        libraryConfig.setJdbcUrl(props.getProperty("library.db.jdbcUrl", "jdbc:postgresql://localhost:5432/library_db"));
        libraryConfig.setDriverClassName(props.getProperty("library.db.driverClassName", "org.postgresql.Driver"));
        libraryConfig.setUsername(props.getProperty("library.db.username", "library_user"));
        libraryConfig.setPassword(props.getProperty("library.db.password", "library_password_secure"));
        libraryConfig.setMaximumPoolSize(Integer.parseInt(props.getProperty("library.db.maximumPoolSize", "15")));
        libraryConfig.setMinimumIdle(Integer.parseInt(props.getProperty("library.db.minimumIdle", "5")));
        libraryConfig.setIdleTimeout(Long.parseLong(props.getProperty("library.db.idleTimeout", "300000")));
        libraryConfig.setConnectionTimeout(Long.parseLong(props.getProperty("library.db.connectionTimeout", "20000")));
        this.libraryDataSource = new HikariDataSource(libraryConfig);
        logger.info("Initialized Library DB Connection Pool: {}", libraryConfig.getPoolName());

        // 2. Initialize Separate User Management DataSource Pool
        HikariConfig userAuthConfig = new HikariConfig();
        userAuthConfig.setPoolName(props.getProperty("userauth.db.poolName", "HikariPool-UserAuthDB"));
        userAuthConfig.setJdbcUrl(props.getProperty("userauth.db.jdbcUrl", "jdbc:postgresql://localhost:5432/user_auth_db"));
        userAuthConfig.setDriverClassName(props.getProperty("userauth.db.driverClassName", "org.postgresql.Driver"));
        userAuthConfig.setUsername(props.getProperty("userauth.db.username", "userauth_user"));
        userAuthConfig.setPassword(props.getProperty("userauth.db.password", "userauth_password_secure"));
        userAuthConfig.setMaximumPoolSize(Integer.parseInt(props.getProperty("userauth.db.maximumPoolSize", "10")));
        userAuthConfig.setMinimumIdle(Integer.parseInt(props.getProperty("userauth.db.minimumIdle", "3")));
        userAuthConfig.setIdleTimeout(Long.parseLong(props.getProperty("userauth.db.idleTimeout", "300000")));
        userAuthConfig.setConnectionTimeout(Long.parseLong(props.getProperty("userauth.db.connectionTimeout", "20000")));
        this.userAuthDataSource = new HikariDataSource(userAuthConfig);
        logger.info("Initialized User Auth DB Connection Pool: {}", userAuthConfig.getPoolName());
    }

    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionManager.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Obtains an active connection to the Library Database.
     */
    public static Connection getLibraryConnection() throws SQLException {
        return getInstance().libraryDataSource.getConnection();
    }

    /**
     * Obtains an active connection to the separate User Management Database.
     */
    public static Connection getUserAuthConnection() throws SQLException {
        return getInstance().userAuthDataSource.getConnection();
    }

    public DataSource getLibraryDataSource() {
        return libraryDataSource;
    }

    public DataSource getUserAuthDataSource() {
        return userAuthDataSource;
    }

    public void shutdown() {
        if (libraryDataSource != null && !libraryDataSource.isClosed()) {
            libraryDataSource.close();
            logger.info("Closed Library DB connection pool.");
        }
        if (userAuthDataSource != null && !userAuthDataSource.isClosed()) {
            userAuthDataSource.close();
            logger.info("Closed User Auth DB connection pool.");
        }
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                properties.load(in);
            } else {
                logger.warn("db.properties not found in classpath. Using default configuration.");
            }
        } catch (Exception e) {
            logger.error("Failed to load db.properties", e);
        }
        return properties;
    }
}
