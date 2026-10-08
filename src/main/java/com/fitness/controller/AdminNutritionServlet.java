package com.fitness.controller;

import com.fitness.dao.AdminUserDAO;
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
 * Controller Servlet for Admin Nutrition Activity Management.
 * Displays all user nutrition logs, supports filtering by user and date, search, and deleting records.
 */
@WebServlet("/admin-nutrition")
public class AdminNutritionServlet extends HttpServlet {
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

            // 2. Fetch Nutrition Logs with Filters
            StringBuilder sql = new StringBuilder(
                "SELECT n.nutrition_id, n.user_id, u.name AS user_name, u.email AS user_email, " +
                "n.food_name, n.calories, n.protein, n.carbs, n.fats, n.nutrition_date, n.meal_type, n.meal_time, n.image_name " +
                "FROM nutrition n " +
                "JOIN users u ON n.user_id = u.user_id " +
                "WHERE 1=1 "
            );

            Integer filterUserId = null;
            if (userIdStr != null && !userIdStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(userIdStr.trim())) {
                try {
                    filterUserId = Integer.parseInt(userIdStr.trim());
                    sql.append("AND n.user_id = ? ");
                } catch (NumberFormatException ignored) {}
            }

            boolean hasDate = (dateStr != null && !dateStr.trim().isEmpty());
            if (hasDate) {
                sql.append("AND n.nutrition_date = ? ");
            }

            boolean hasSearch = (search != null && !search.trim().isEmpty());
            if (hasSearch) {
                sql.append("AND (LOWER(n.food_name) LIKE ? OR LOWER(u.name) LIKE ? OR LOWER(u.email) LIKE ?) ");
            }

            sql.append("ORDER BY n.nutrition_date DESC, n.nutrition_id DESC");

            List<Map<String, Object>> logs = new ArrayList<>();
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
                        map.put("nutritionId", rs.getInt("nutrition_id"));
                        map.put("userId", rs.getInt("user_id"));
                        map.put("userName", rs.getString("user_name"));
                        map.put("userEmail", rs.getString("user_email"));
                        map.put("foodName", rs.getString("food_name"));
                        map.put("calories", rs.getDouble("calories"));
                        map.put("protein", rs.getDouble("protein"));
                        map.put("carbs", rs.getDouble("carbs"));
                        map.put("fats", rs.getDouble("fats"));
                        map.put("nutritionDate", rs.getDate("nutrition_date") != null ? rs.getDate("nutrition_date").toString() : "");
                        map.put("mealType", rs.getString("meal_type"));
                        map.put("mealTime", rs.getTime("meal_time") != null ? rs.getTime("meal_time").toString() : "");
                        map.put("imageName", rs.getString("image_name"));
                        logs.add(map);
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

            json.append("\"logs\":[");
            for (int i = 0; i < logs.size(); i++) {
                Map<String, Object> log = logs.get(i);
                json.append("{");
                json.append("\"nutritionId\":").append(log.get("nutritionId")).append(",");
                json.append("\"userId\":").append(log.get("userId")).append(",");
                json.append("\"userName\":\"").append(escapeJson((String) log.get("userName"))).append("\",");
                json.append("\"userEmail\":\"").append(escapeJson((String) log.get("userEmail"))).append("\",");
                json.append("\"foodName\":\"").append(escapeJson((String) log.get("foodName"))).append("\",");
                json.append("\"calories\":").append(log.get("calories")).append(",");
                json.append("\"protein\":").append(log.get("protein")).append(",");
                json.append("\"carbs\":").append(log.get("carbs")).append(",");
                json.append("\"fats\":").append(log.get("fats")).append(",");
                json.append("\"nutritionDate\":\"").append(log.get("nutritionDate")).append("\",");
                json.append("\"mealType\":\"").append(escapeJson((String) log.get("mealType"))).append("\",");
                json.append("\"mealTime\":\"").append(escapeJson((String) log.get("mealTime"))).append("\",");
                json.append("\"imageName\":\"").append(escapeJson((String) log.get("imageName"))).append("\"");
                json.append("}");
                if (i < logs.size() - 1) json.append(",");
            }
            json.append("]}");

            out.print(json.toString());

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid date format. Expected YYYY-MM-DD.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        String nutritionIdStr = request.getParameter("nutritionId");

        if ("delete".equalsIgnoreCase(action) && nutritionIdStr != null && !nutritionIdStr.trim().isEmpty()) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM nutrition WHERE nutrition_id = ?")) {

                int nutritionId = Integer.parseInt(nutritionIdStr.trim());
                ps.setInt(1, nutritionId);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    out.print("{\"status\":\"success\",\"message\":\"Nutrition record deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"Nutrition record not found or already deleted.\"}");
                }

            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid nutrition ID format.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid request or missing action/nutritionId parameter.\"}");
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
