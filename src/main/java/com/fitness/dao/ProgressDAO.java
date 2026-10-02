package com.fitness.dao;

import com.fitness.model.Progress;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Progress entity.
 * Handles database CRUD operations for user body weight and BMI progress records.
 */
public class ProgressDAO {

    public ProgressDAO() {
        ensureNotesColumnExists();
    }

    /**
     * Checks database schema and adds optional 'notes' column to progress table if missing.
     */
    private void ensureNotesColumnExists() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "progress", "notes")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE progress ADD COLUMN notes VARCHAR(255) NULL");
                    }
                }
            }
        } catch (SQLException e) {
            // Log warning without breaking application
            System.err.println("⚠️ ProgressDAO schema check warning: " + e.getMessage());
        }
    }

    /**
     * Adds a new progress record for a user using PreparedStatement.
     * Also updates the latest user weight in the users profile table.
     */
    public boolean addProgress(Progress progress) throws SQLException {
        String sql = "INSERT INTO progress (user_id, weight, bmi, progress_date, notes) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, progress.getUserId());
            ps.setDouble(2, progress.getWeight());
            ps.setDouble(3, progress.getBmi());
            ps.setDate(4, progress.getProgressDate());
            ps.setString(5, progress.getNotes());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        progress.setProgressId(rs.getInt(1));
                    }
                }

                // Synchronize latest weight to user profile table
                updateUserProfileWeight(progress.getUserId(), progress.getWeight());
                return true;
            }
            return false;
        }
    }

    /**
     * Updates current weight in the users profile table.
     */
    private void updateUserProfileWeight(int userId, double latestWeight) {
        String sql = "UPDATE users SET weight = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, latestWeight);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ Warning updating user profile weight: " + e.getMessage());
        }
    }

    /**
     * Retrieves all progress records for a user ordered chronologically (for charts and history).
     */
    public List<Progress> getProgressByUserId(int userId) throws SQLException {
        List<Progress> list = new ArrayList<>();
        String sql = "SELECT progress_id, user_id, weight, bmi, progress_date, notes FROM progress " +
                     "WHERE user_id = ? ORDER BY progress_date ASC, progress_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProgress(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves the most recent progress record for a user.
     */
    public Progress getLatestProgressByUserId(int userId) throws SQLException {
        String sql = "SELECT progress_id, user_id, weight, bmi, progress_date, notes FROM progress " +
                     "WHERE user_id = ? ORDER BY progress_date DESC, progress_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProgress(rs);
                }
            }
        }
        return null;
    }

    /**
     * Deletes a progress record belonging to a logged-in user.
     */
    public boolean deleteProgress(int progressId, int userId) throws SQLException {
        String sql = "DELETE FROM progress WHERE progress_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, progressId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                // If records remain, update profile weight to the new latest progress
                Progress latest = getLatestProgressByUserId(userId);
                if (latest != null) {
                    updateUserProfileWeight(userId, latest.getWeight());
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Helper to map ResultSet row to Progress object.
     */
    private Progress mapResultSetToProgress(ResultSet rs) throws SQLException {
        Progress p = new Progress();
        p.setProgressId(rs.getInt("progress_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setWeight(rs.getDouble("weight"));
        p.setBmi(rs.getDouble("bmi"));
        p.setBmiCategory(Progress.calculateBMICategory(p.getBmi()));
        p.setProgressDate(rs.getDate("progress_date"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
