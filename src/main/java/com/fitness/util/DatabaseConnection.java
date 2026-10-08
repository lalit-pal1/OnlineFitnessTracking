package com.fitness.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC Connection Utility for the Online Fitness Tracking Application.
 *
 * Database credentials are read from environment variables so that secrets are
 * not stored in source code or committed to the public repository.
 *
 * Supported environment variables:
 * FITNESS_DB_HOST      (default: localhost)
 * FITNESS_DB_PORT      (default: 3306)
 * FITNESS_DB_NAME      (default: fitness_tracking)
 * FITNESS_DB_USER      (required)
 * FITNESS_DB_PASSWORD  (required)
 */
public final class DatabaseConnection {

    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_HOST = getEnvironmentValue("FITNESS_DB_HOST", "localhost");
    private static final String DB_PORT = getEnvironmentValue("FITNESS_DB_PORT", "3306");
    private static final String DB_NAME = getEnvironmentValue("FITNESS_DB_NAME", "fitness_tracking");

    private static final String USER = getEnvironmentValue("FITNESS_DB_USER", null);
    private static final String PASSWORD = getEnvironmentValue("FITNESS_DB_PASSWORD", null);

    private static final String URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    static {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private DatabaseConnection() {
        // Utility class; do not instantiate.
    }

    /**
     * Creates a new JDBC connection using the configured environment variables.
     *
     * @return an open JDBC connection
     * @throws SQLException if credentials are missing or the database cannot be reached
     */
    public static Connection getConnection() throws SQLException {
        if (USER == null || USER.isBlank()) {
            throw new SQLException("FITNESS_DB_USER environment variable is not configured.");
        }
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("FITNESS_DB_PASSWORD environment variable is not configured.");
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String getEnvironmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
