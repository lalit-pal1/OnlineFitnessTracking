package com.fitness.controller;

import com.fitness.dao.AdminUserDAO;
import com.fitness.model.Progress;
import com.fitness.model.User;
import com.fitness.util.DatabaseConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller Servlet for Admin Progress Activity Management.
 * Displays all user body weight and BMI progress entries, supports filtering and deletion.
 */
@WebServlet("/admin-progress")
public class AdminProgressServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AdminUserDAO adminUserDAO;

    @Override
    public void init() throws ServletException {
        adminUserDAO = new AdminUserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String userIdStr = request.getParameter("userId");
        String dateStr = request.getParameter("date");
        String search = request.getParameter("search");

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Fetch Users for Filter Dropdown
            List<User> userList = adminUserDAO.getAllUsers(null);

            // 2. Fetch Progress Logs with Filters
            StringBuilder sql = new StringBuilder(
                "SELECT p.progress_id, p.user_id, u.name AS user_name, u.email AS user_email, " +
                "p.weight, p.bmi, p.progress_date, p.notes " +
                "FROM progress p " +
                "JOIN users u ON p.user_id = u.user_id " +
                "WHERE 1=1 "
            );

            Integer filterUserId = null;
            if (userIdStr != null && !userIdStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(userIdStr.trim())) {
                try {
                    filterUserId = Integer.parseInt(userIdStr.trim());
                    sql.append("AND p.user_id = ? ");
                } catch (NumberFormatException ignored) {}
            }

            boolean hasDate = (dateStr != null && !dateStr.trim().isEmpty());
            if (hasDate) {
                sql.append("AND p.progress_date = ? ");
            }

            boolean hasSearch = (search != null && !search.trim().isEmpty());
            if (hasSearch) {
                sql.append("AND (LOWER(u.name) LIKE ? OR LOWER(u.email) LIKE ? OR LOWER(p.notes) LIKE ?) ");
            }

            sql.append("ORDER BY p.progress_date DESC, p.progress_id DESC");

            List<Map<String, Object>> records = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (filterUserId != null) {
                    ps.setInt(idx++, filterUserId);
                }
                if (hasDate) {
                    ps.setDate(idx++, java.sql.Date.valueOf(dateStr.trim()));
                }
                if (hasSearch) {
                    String term = "%" + search.trim().toLowerCase() + "%";
                    ps.setString(idx++, term);
                    ps.setString(idx++, term);
                    ps.setString(idx++, term);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        double bmi = rs.getDouble("bmi");
                        map.put("progressId", rs.getInt("progress_id"));
                        map.put("userId", rs.getInt("user_id"));
                        map.put("userName", rs.getString("user_name"));
                        map.put("userEmail", rs.getString("user_email"));
                        map.put("weight", rs.getDouble("weight"));
                        map.put("bmi", bmi);
                        map.put("bmiCategory", Progress.calculateBMICategory(bmi));
                        map.put("progressDate", rs.getDate("progress_date") != null ? rs.getDate("progress_date").toString() : "");
                        map.put("notes", rs.getString("notes"));
                        records.add(map);
                    }
                }
            }

            // Build JSON Response
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"success\",");
            json.append("\"users\":[");
            for (int i = 0; i < userList.size(); i++) {
                User u = userList.get(i);
                json.append("{");
                json.append("\"userId\":").append(u.getUserId()).append(",");
                json.append("\"name\":\"").append(escapeJson(u.getName())).append("\",");
                json.append("\"email\":\"").append(escapeJson(u.getEmail())).append("\"");
                json.append("}");
                if (i < userList.size() - 1) json.append(",");
            }
            json.append("],");

            json.append("\"progress\":[");
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> p = records.get(i);
                json.append("{");
                json.append("\"progressId\":").append(p.get("progressId")).append(",");
                json.append("\"userId\":").append(p.get("userId")).append(",");
                json.append("\"userName\":\"").append(escapeJson((String) p.get("userName"))).append("\",");
                json.append("\"userEmail\":\"").append(escapeJson((String) p.get("userEmail"))).append("\",");
                json.append("\"weight\":").append(p.get("weight")).append(",");
                json.append("\"bmi\":").append(p.get("bmi")).append(",");
                json.append("\"bmiCategory\":\"").append(escapeJson((String) p.get("bmiCategory"))).append("\",");
                json.append("\"progressDate\":\"").append(p.get("progressDate")).append("\",");
                json.append("\"notes\":\"").append(escapeJson((String) p.get("notes"))).append("\"");
                json.append("}");
                if (i < records.size() - 1) json.append(",");
            }
            json.append("]}");

            out.print(json.toString());

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid date format. Expected YYYY-MM-DD.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        String progressIdStr = request.getParameter("progressId");

        if ("delete".equalsIgnoreCase(action) && progressIdStr != null && !progressIdStr.trim().isEmpty()) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM progress WHERE progress_id = ?")) {

                int progressId = Integer.parseInt(progressIdStr.trim());
                ps.setInt(1, progressId);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    out.print("{\"status\":\"success\",\"message\":\"Progress record deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"Progress record not found or already deleted.\"}");
                }

            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid progress ID format.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid request or missing action/progressId parameter.\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
