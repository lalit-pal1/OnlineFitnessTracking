package com.fitness.controller;

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
 * Controller Servlet for Admin Dashboard metrics & recent activity streams.
 * Protected by AuthenticationFilter (requires role = ADMIN).
 */
@WebServlet("/admin-dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = DatabaseConnection.getConnection()) {
            Map<String, Object> stats = new HashMap<>();

            // Total Users
            stats.put("totalUsers", getCount(conn, "SELECT COUNT(*) FROM users"));

            // Total Exercises
            stats.put("totalExercises", getCount(conn, "SELECT COUNT(*) FROM exercises"));

            // Total Workouts
            stats.put("totalWorkouts", getCount(conn, "SELECT COUNT(*) FROM workouts"));

            // Total Nutrition
            stats.put("totalNutrition", getCount(conn, "SELECT COUNT(*) FROM nutrition"));

            // Total Progress
            stats.put("totalProgress", getCount(conn, "SELECT COUNT(*) FROM progress"));

            // Workouts Today
            stats.put("workoutsToday", getCount(conn, "SELECT COUNT(*) FROM workouts WHERE workout_date = CURRENT_DATE()"));

            // Nutrition Today
            stats.put("nutritionToday", getCount(conn, "SELECT COUNT(*) FROM nutrition WHERE nutrition_date = CURRENT_DATE()"));

            // Progress This Month
            stats.put("progressThisMonth", getCount(conn, "SELECT COUNT(*) FROM progress WHERE YEAR(progress_date) = YEAR(CURRENT_DATE()) AND MONTH(progress_date) = MONTH(CURRENT_DATE())"));

            // Recent Users Stream (5 latest)
            List<Map<String, Object>> recentUsers = getRecentUsers(conn, 5);

            // Recent Workouts Stream (5 latest)
            List<Map<String, Object>> recentWorkouts = getRecentWorkouts(conn, 5);

            // Recent Nutrition Stream (5 latest)
            List<Map<String, Object>> recentNutrition = getRecentNutrition(conn, 5);

            // Build JSON response
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"success\",");
            json.append("\"stats\":{");
            json.append("\"totalUsers\":").append(stats.get("totalUsers")).append(",");
            json.append("\"totalExercises\":").append(stats.get("totalExercises")).append(",");
            json.append("\"totalWorkouts\":").append(stats.get("totalWorkouts")).append(",");
            json.append("\"totalNutrition\":").append(stats.get("totalNutrition")).append(",");
            json.append("\"totalProgress\":").append(stats.get("totalProgress")).append(",");
            json.append("\"workoutsToday\":").append(stats.get("workoutsToday")).append(",");
            json.append("\"nutritionToday\":").append(stats.get("nutritionToday")).append(",");
            json.append("\"progressThisMonth\":").append(stats.get("progressThisMonth"));
            json.append("},");

            json.append("\"recentUsers\":").append(toJsonList(recentUsers)).append(",");
            json.append("\"recentWorkouts\":").append(toJsonList(recentWorkouts)).append(",");
            json.append("\"recentNutrition\":").append(toJsonList(recentNutrition));
            json.append("}");

            out.print(json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
        }
    }

    private int getCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private List<Map<String, Object>> getRecentUsers(Connection conn, int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT user_id, name, email, created_at FROM users ORDER BY created_at DESC, user_id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", rs.getInt("user_id"));
                    map.put("name", rs.getString("name"));
                    map.put("email", rs.getString("email"));
                    map.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "");
                    list.add(map);
                }
            }
        }
        return list;
    }

    private List<Map<String, Object>> getRecentWorkouts(Connection conn, int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT w.workout_id, w.user_id, u.name AS user_name, u.email AS user_email, " +
                     "COALESCE(e.exercise_name, w.custom_exercise_name) AS exercise_name, w.workout_date " +
                     "FROM workouts w " +
                     "JOIN users u ON w.user_id = u.user_id " +
                     "LEFT JOIN exercises e ON w.exercise_id = e.exercise_id " +
                     "ORDER BY w.workout_date DESC, w.workout_id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("workoutId", rs.getInt("workout_id"));
                    map.put("userId", rs.getInt("user_id"));
                    map.put("userName", rs.getString("user_name"));
                    map.put("userEmail", rs.getString("user_email"));
                    map.put("exerciseName", rs.getString("exercise_name"));
                    map.put("workoutDate", rs.getDate("workout_date") != null ? rs.getDate("workout_date").toString() : "");
                    list.add(map);
                }
            }
        }
        return list;
    }

    private List<Map<String, Object>> getRecentNutrition(Connection conn, int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT n.nutrition_id, n.user_id, u.name AS user_name, u.email AS user_email, " +
                     "n.food_name, n.calories, n.nutrition_date, n.meal_type " +
                     "FROM nutrition n " +
                     "JOIN users u ON n.user_id = u.user_id " +
                     "ORDER BY n.nutrition_date DESC, n.nutrition_id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("nutritionId", rs.getInt("nutrition_id"));
                    map.put("userId", rs.getInt("user_id"));
                    map.put("userName", rs.getString("user_name"));
                    map.put("userEmail", rs.getString("user_email"));
                    map.put("foodName", rs.getString("food_name"));
                    map.put("calories", rs.getDouble("calories"));
                    map.put("nutritionDate", rs.getDate("nutrition_date") != null ? rs.getDate("nutrition_date").toString() : "");
                    map.put("mealType", rs.getString("meal_type"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    private String toJsonList(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            sb.append("{");
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\":");
                Object val = entry.getValue();
                if (val == null) {
                    sb.append("null");
                } else if (val instanceof Number || val instanceof Boolean) {
                    sb.append(val);
                } else {
                    sb.append("\"").append(escapeJson(val.toString())).append("\"");
                }
                if (++j < map.size()) sb.append(",");
            }
            sb.append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
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
