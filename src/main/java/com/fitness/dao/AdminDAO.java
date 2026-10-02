package com.fitness.dao;

import com.fitness.model.Admin;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object (DAO) for Admin entity.
 * Handles database queries for administrator authentication.
 */
public class AdminDAO {

    /**
     * Authenticates administrator credentials.
     * Throws SQLException so calling Servlet can capture and report exact database errors.
     */
    public Admin loginAdmin(String email, String password) throws SQLException {
        String sql = "SELECT * FROM admin WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Admin admin = new Admin();
                    admin.setAdminId(rs.getInt("admin_id"));
                    admin.setName(rs.getString("name"));
                    admin.setEmail(rs.getString("email"));
                    admin.setPassword(rs.getString("password"));
                    admin.setCreatedAt(rs.getTimestamp("created_at"));
                    return admin;
                }
            }
        }
        return null;
    }
}
