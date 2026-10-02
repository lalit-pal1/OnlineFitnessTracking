package com.fitness.dao;

import com.fitness.model.User;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object (DAO) for User entity.
 * Handles database CRUD operations for user registration, authentication, and profile retrieval.
 */
public class UserDAO {

    /**
     * Checks whether an email is already registered in the users table.
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Returns true if email found
            }
        }
    }

    /**
     * Registers a new user into the users table using PreparedStatement.
     */
    public boolean registerUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, age, gender, height, weight, fitness_goal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPassword());
            ps.setInt(4, user.getAge());
            ps.setString(5, user.getGender());
            ps.setDouble(6, user.getHeight());
            ps.setDouble(7, user.getWeight());
            ps.setString(8, user.getFitnessGoal());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Authenticates user login credentials.
     * Returns User object if valid, null if invalid.
     */
    public User loginUser(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a User by primary key user_id using PreparedStatement.
     */
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Helper method to map a ResultSet row to a User JavaBean.
     */
    private User mapUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setAge(rs.getInt("age"));
        user.setGender(rs.getString("gender"));
        user.setHeight(rs.getDouble("height"));
        user.setWeight(rs.getDouble("weight"));
        user.setFitnessGoal(rs.getString("fitness_goal"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
