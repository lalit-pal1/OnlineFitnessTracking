package com.fitness.dao;

import com.fitness.model.Goal;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Goal entity.
 * Supports active goal retrieval, goal history listing, adding, updating, and deleting goals safely.
 */
public class GoalDAO {

    /**
     * Retrieves the current active (IN_PROGRESS) goal for a user.
     */
    public Goal getActiveGoalByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM goals WHERE user_id = ? AND status = 'IN_PROGRESS' ORDER BY goal_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGoal(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all goals logged by a user ordered by start date.
     */
    public List<Goal> getAllGoalsByUserId(int userId) throws SQLException {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE user_id = ? ORDER BY start_date DESC, goal_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToGoal(rs));
                }
            }
        }
        return list;
    }

    /**
     * Adds a new goal for a user.
     * Marks any previous IN_PROGRESS goal as SUPERSEDED so only one active goal exists.
     * Synchronizes fitness_goal text in users table.
     */
    public boolean addGoal(Goal goal) throws SQLException {
        String sqlSupersede = "UPDATE goals SET status = 'SUPERSEDED' WHERE user_id = ? AND status = 'IN_PROGRESS'";
        String sqlInsert = "INSERT INTO goals (user_id, goal_type, target_value, target_weight, start_date, target_date, daily_calories, daily_protein, notes, status) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Mark previous active goals as superseded
            try (PreparedStatement psSupersede = conn.prepareStatement(sqlSupersede)) {
                psSupersede.setInt(1, goal.getUserId());
                psSupersede.executeUpdate();
            }

            // 2. Insert new active goal
            try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setInt(1, goal.getUserId());
                psInsert.setString(2, goal.getGoalType());
                psInsert.setString(3, goal.getTargetValue() != null ? goal.getTargetValue() : String.format("%.2f kg", goal.getTargetWeight()));
                psInsert.setDouble(4, goal.getTargetWeight());
                psInsert.setDate(5, goal.getStartDate() != null ? goal.getStartDate() : new Date(System.currentTimeMillis()));
                psInsert.setDate(6, goal.getTargetDate());
                psInsert.setDouble(7, goal.getDailyCalories());
                psInsert.setDouble(8, goal.getDailyProtein());
                psInsert.setString(9, goal.getNotes());
                psInsert.setString(10, "IN_PROGRESS");

                int rows = psInsert.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = psInsert.getGeneratedKeys()) {
                        if (rs.next()) {
                            goal.setGoalId(rs.getInt(1));
                        }
                    }
                }
            }

            // 3. Update fitness_goal string in users profile table
            updateUserFitnessGoalText(conn, goal.getUserId(), goal.getGoalType(), goal.getTargetWeight());

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    /**
     * Updates an existing goal record with user ownership check (goal_id = ? AND user_id = ?).
     */
    public boolean updateGoal(Goal goal) throws SQLException {
        String sql = "UPDATE goals SET goal_type = ?, target_value = ?, target_weight = ?, target_date = ?, " +
                     "daily_calories = ?, daily_protein = ?, notes = ?, status = ? WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, goal.getGoalType());
            ps.setString(2, goal.getTargetValue() != null ? goal.getTargetValue() : String.format("%.2f kg", goal.getTargetWeight()));
            ps.setDouble(3, goal.getTargetWeight());
            ps.setDate(4, goal.getTargetDate());
            ps.setDouble(5, goal.getDailyCalories());
            ps.setDouble(6, goal.getDailyProtein());
            ps.setString(7, goal.getNotes());
            ps.setString(8, goal.getStatus() != null ? goal.getStatus() : "IN_PROGRESS");
            ps.setInt(9, goal.getGoalId());
            ps.setInt(10, goal.getUserId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                if ("IN_PROGRESS".equalsIgnoreCase(goal.getStatus())) {
                    try (Connection conn2 = DatabaseConnection.getConnection()) {
                        updateUserFitnessGoalText(conn2, goal.getUserId(), goal.getGoalType(), goal.getTargetWeight());
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Deletes a goal record belonging to the logged-in user.
     */
    public boolean deleteGoal(int goalId, int userId) throws SQLException {
        String sql = "DELETE FROM goals WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, goalId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                // If the user has a recent remaining goal, set the latest as IN_PROGRESS
                Goal active = getActiveGoalByUserId(userId);
                if (active == null) {
                    List<Goal> all = getAllGoalsByUserId(userId);
                    if (!all.isEmpty()) {
                        Goal latest = all.get(0);
                        latest.setStatus("IN_PROGRESS");
                        updateGoal(latest);
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Gets initial user body weight at or around the goal start date.
     */
    public double getInitialWeightForGoal(int userId, Date startDate) throws SQLException {
        // First try to find progress record logged on or before start_date
        String sql = "SELECT weight FROM progress WHERE user_id = ? AND progress_date <= ? ORDER BY progress_date DESC, progress_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setDate(2, startDate != null ? startDate : new Date(System.currentTimeMillis()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("weight");
                }
            }
        }

        // If no progress record on/before start_date, get earliest recorded progress
        String sqlEarliest = "SELECT weight FROM progress WHERE user_id = ? ORDER BY progress_date ASC, progress_id ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlEarliest)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("weight");
                }
            }
        }

        // Fallback to initial weight in user profile table
        UserDAO userDAO = new UserDAO();
        com.fitness.model.User user = userDAO.getUserById(userId);
        return (user != null) ? user.getWeight() : 0.0;
    }

    private void updateUserFitnessGoalText(Connection conn, int userId, String goalType, double targetWeight) {
        String sql = "UPDATE users SET fitness_goal = ? WHERE user_id = ?";
        String goalStr = goalType + " (" + String.format("%.1f kg", targetWeight) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goalStr);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ Warning updating user fitness_goal text: " + e.getMessage());
        }
    }

    private Goal mapResultSetToGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setGoalId(rs.getInt("goal_id"));
        g.setUserId(rs.getInt("user_id"));
        g.setGoalType(rs.getString("goal_type"));
        g.setTargetValue(rs.getString("target_value"));
        
        double tw = rs.getDouble("target_weight");
        if (rs.wasNull() && g.getTargetValue() != null) {
            // Try parsing legacy string e.g. "72.00 kg"
            try {
                String clean = g.getTargetValue().replaceAll("[^0-9.]", "");
                tw = Double.parseDouble(clean);
            } catch (Exception ignored) {}
        }
        g.setTargetWeight(tw);

        g.setStartDate(rs.getDate("start_date"));
        g.setTargetDate(rs.getDate("target_date"));
        g.setDailyCalories(rs.getDouble("daily_calories"));
        g.setDailyProtein(rs.getDouble("daily_protein"));
        g.setNotes(rs.getString("notes"));
        g.setStatus(rs.getString("status"));
        return g;
    }
}
