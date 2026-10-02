package com.fitness.controller;

import com.fitness.dao.AdminUserDAO;
import com.fitness.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller Servlet for Admin User Management.
 * Supports listing all users, searching users, fetching user profile details, and deleting users safely.
 */
@WebServlet("/admin-users")
public class AdminUserServlet extends HttpServlet {
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

        String userIdParam = request.getParameter("userId");
        String searchParam = request.getParameter("search");

        try {
            if (userIdParam != null && !userIdParam.trim().isEmpty()) {
                // Fetch Detailed User Profile
                int userId = Integer.parseInt(userIdParam.trim());
                Map<String, Object> profileDetails = adminUserDAO.getUserProfileDetails(userId);

                if (profileDetails == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"User not found.\"}");
                    return;
                }

                User user = (User) profileDetails.get("user");

                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"status\":\"success\",");
                json.append("\"user\":{");
                json.append("\"userId\":").append(user.getUserId()).append(",");
                json.append("\"name\":\"").append(escapeJson(user.getName())).append("\",");
                json.append("\"email\":\"").append(escapeJson(user.getEmail())).append("\",");
                json.append("\"age\":").append(user.getAge()).append(",");
                json.append("\"gender\":\"").append(escapeJson(user.getGender())).append("\",");
                json.append("\"height\":").append(user.getHeight()).append(",");
                json.append("\"weight\":").append(user.getWeight()).append(",");
                json.append("\"fitnessGoal\":\"").append(escapeJson(user.getFitnessGoal())).append("\",");
                json.append("\"createdAt\":\"").append(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "").append("\"");
                json.append("},");
                json.append("\"totalWorkouts\":").append(profileDetails.getOrDefault("totalWorkouts", 0)).append(",");
                json.append("\"totalNutrition\":").append(profileDetails.getOrDefault("totalNutrition", 0)).append(",");
                json.append("\"totalProgress\":").append(profileDetails.getOrDefault("totalProgress", 0)).append(",");
                json.append("\"lastWorkoutDate\":\"").append(profileDetails.get("lastWorkoutDate") != null ? profileDetails.get("lastWorkoutDate").toString() : "None").append("\",");
                json.append("\"lastNutritionDate\":\"").append(profileDetails.get("lastNutritionDate") != null ? profileDetails.get("lastNutritionDate").toString() : "None").append("\",");
                json.append("\"latestWeight\":").append(profileDetails.getOrDefault("latestWeight", 0.0)).append(",");
                json.append("\"latestBmi\":").append(profileDetails.getOrDefault("latestBmi", 0.0)).append(",");
                json.append("\"latestProgressDate\":\"").append(profileDetails.get("latestProgressDate") != null ? profileDetails.get("latestProgressDate").toString() : "None").append("\"");
                json.append("}");

                out.print(json.toString());
            } else {
                // Fetch All Users List
                List<User> users = adminUserDAO.getAllUsers(searchParam);

                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"status\":\"success\",");
                json.append("\"users\":[");

                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    json.append("{");
                    json.append("\"userId\":").append(u.getUserId()).append(",");
                    json.append("\"name\":\"").append(escapeJson(u.getName())).append("\",");
                    json.append("\"email\":\"").append(escapeJson(u.getEmail())).append("\",");
                    json.append("\"age\":").append(u.getAge()).append(",");
                    json.append("\"gender\":\"").append(escapeJson(u.getGender())).append("\",");
                    json.append("\"height\":").append(u.getHeight()).append(",");
                    json.append("\"weight\":").append(u.getWeight()).append(",");
                    json.append("\"fitnessGoal\":\"").append(escapeJson(u.getFitnessGoal())).append("\",");
                    json.append("\"createdAt\":\"").append(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "").append("\"");
                    json.append("}");
                    if (i < users.size() - 1) json.append(",");
                }

                json.append("]}");
                out.print(json.toString());
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid user ID format.\"}");
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
        String userIdParam = request.getParameter("userId");

        if ("delete".equalsIgnoreCase(action) && userIdParam != null && !userIdParam.trim().isEmpty()) {
            try {
                int userId = Integer.parseInt(userIdParam.trim());
                boolean deleted = adminUserDAO.deleteUser(userId);

                if (deleted) {
                    out.print("{\"status\":\"success\",\"message\":\"User account deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"User account not found or already deleted.\"}");
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid user ID format.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Failed to delete user: " + escapeJson(e.getMessage()) + "\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid request or missing action/userId parameter.\"}");
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
