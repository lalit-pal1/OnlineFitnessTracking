package com.fitness.dao;

import com.fitness.model.User;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) for Admin User Management.
 * Handles listing, searching, profile metrics retrieval, and user account deletion with safety checks.
 */
public class AdminUserDAO {

    /**
     * Retrieves all registered users, optionally filtered by search query matching name or email.
     * Passwords are not returned for security.
     */
    public List<User> getAllUsers(String search) throws SQLException {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT user_id, name, email, age, gender, height, weight, fitness_goal, created_at FROM users ");
        
        boolean hasSearch = (search != null && !search.trim().isEmpty());
        if (hasSearch) {
            sql.append("WHERE LOWER(name) LIKE ? OR LOWER(email) LIKE ? ");
        }
        sql.append("ORDER BY user_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (hasSearch) {
                String term = "%" + search.trim().toLowerCase() + "%";
                ps.setString(1, term);
                ps.setString(2, term);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setAge(rs.getInt("age"));
                    u.setGender(rs.getString("gender"));
                    u.setHeight(rs.getDouble("height"));
                    u.setWeight(rs.getDouble("weight"));
                    u.setFitnessGoal(rs.getString("fitness_goal"));
                    u.setCreatedAt(rs.getTimestamp("created_at"));
                    users.add(u);
                }
            }
        }
        return users;
    }

    /**
     * Retrieves detailed profile metrics for a specific user ID.
     */
    public Map<String, Object> getUserProfileDetails(int userId) throws SQLException {
        Map<String, Object> details = new HashMap<>();

        // 1. Basic User Info
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserById(userId);
        if (user == null) {
            return null;
        }
        // Sanitize password before returning
        user.setPassword(null);
        details.put("user", user);

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 2. Count Workouts
            String sqlWorkouts = "SELECT COUNT(*) AS total_workouts, MAX(workout_date) AS last_workout FROM workouts WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlWorkouts)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        details.put("totalWorkouts", rs.getInt("total_workouts"));
                        details.put("lastWorkoutDate", rs.getDate("last_workout"));
                    }
                }
            }

            // 3. Count Nutrition Records
            String sqlNutrition = "SELECT COUNT(*) AS total_nutrition, MAX(nutrition_date) AS last_nutrition FROM nutrition WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlNutrition)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        details.put("totalNutrition", rs.getInt("total_nutrition"));
                        details.put("lastNutritionDate", rs.getDate("last_nutrition"));
                    }
                }
            }

            // 4. Count Progress Entries and Get Latest Weight/BMI
            String sqlProgress = "SELECT COUNT(*) AS total_progress FROM progress WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlProgress)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        details.put("totalProgress", rs.getInt("total_progress"));
                    }
                }
            }

            String sqlLatestProgress = "SELECT weight, bmi, progress_date FROM progress WHERE user_id = ? ORDER BY progress_date DESC, progress_id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlLatestProgress)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        details.put("latestWeight", rs.getDouble("weight"));
                        details.put("latestBmi", rs.getDouble("bmi"));
                        details.put("latestProgressDate", rs.getDate("progress_date"));
                    }
                }
            }
        }

        return details;
    }

    /**
     * Safely deletes a user account from the database by user ID using PreparedStatement.
     * Foreign keys with ON DELETE CASCADE will clean up child records automatically.
     */
    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }
}
