package com.bytebite.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database Configuration
 * Centralizes database configuration constants and utilities
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    // Database connection parameters
    public static final String DB_HOST = getEnv("DB_HOST", "localhost");
    public static final String DB_PORT = getEnv("DB_PORT", "5432");
    public static final String DB_NAME = getEnv("DB_NAME", "bytebite_db");
    public static final String DB_USER = getEnv("DB_USER", "bytebite");
    public static final String DB_PASSWORD = getEnv("DB_PASSWORD", "bytebite_secure_pwd_123");

    // Connection pool parameters
    public static final int POOL_MAX_SIZE = 20;
    public static final int POOL_MIN_IDLE = 5;
    public static final int CONNECTION_TIMEOUT_MS = 30000;
    public static final int IDLE_TIMEOUT_MS = 600000;
    public static final int MAX_LIFETIME_MS = 1800000;

    // JDBC URL
    public static final String JDBC_URL = String.format(
        "jdbc:postgresql://%s:%s/%s",
        DB_HOST, DB_PORT, DB_NAME
    );

    static {
        logger.info("Database Configuration loaded:");
        logger.info("  Host: {}", DB_HOST);
        logger.info("  Port: {}", DB_PORT);
        logger.info("  Database: {}", DB_NAME);
        logger.info("  JDBC URL: {}", JDBC_URL);
        logger.info("  Pool size: {} (min: {}, max: {})", POOL_MIN_IDLE, POOL_MIN_IDLE, POOL_MAX_SIZE);
    }

    public static String getJdbcUrl() {
        return JDBC_URL;
    }

    public static String getUsername() {
        return DB_USER;
    }

    public static String getPassword() {
        return DB_PASSWORD;
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
