package com.fitness.controller;

import com.fitness.dao.ProgressDAO;
import com.fitness.dao.UserDAO;
import com.fitness.model.Progress;
import com.fitness.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Controller Servlet for Progress Tracking module.
 * Endpoint: /progress
 */
@WebServlet("/progress")
public class ProgressServlet extends HttpServlet {

    private ProgressDAO progressDAO;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        progressDAO = new ProgressDAO();
        userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || !"USER".equals(session.getAttribute("role")) || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in first.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        try {
            User user = userDAO.getUserById(userId);
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.write("{\"status\":\"error\",\"message\":\"User profile not found.\"}");
                return;
            }

            List<Progress> logs = progressDAO.getProgressByUserId(userId);
            Progress latest = progressDAO.getLatestProgressByUserId(userId);

            double heightCm = user.getHeight();
            double currentWeight = 0.0;
            double currentBmi = 0.0;
            String bmiCategory = "N/A";

            if (latest != null) {
                currentWeight = latest.getWeight();
                currentBmi = latest.getBmi();
                bmiCategory = latest.getBmiCategory();
            } else if (user.getWeight() > 0) {
                currentWeight = user.getWeight();
                currentBmi = Progress.calculateBMI(currentWeight, heightCm);
                bmiCategory = Progress.calculateBMICategory(currentBmi);
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"success\",");
            json.append("\"userId\":").append(userId).append(",");
            json.append("\"userName\":\"").append(escapeJson(user.getName())).append("\",");
            json.append("\"height\":").append(formatDouble(heightCm)).append(",");
            json.append("\"currentWeight\":").append(formatDouble(currentWeight)).append(",");
            json.append("\"currentBmi\":").append(formatDouble(currentBmi)).append(",");
            json.append("\"bmiCategory\":\"").append(escapeJson(bmiCategory)).append("\",");
            json.append("\"fitnessGoal\":\"").append(escapeJson(user.getFitnessGoal())).append("\",");
            json.append("\"totalEntries\":").append(logs.size()).append(",");
            json.append("\"logs\":[");

            for (int i = 0; i < logs.size(); i++) {
                Progress p = logs.get(i);
                json.append("{");
                json.append("\"progressId\":").append(p.getProgressId()).append(",");
                json.append("\"weight\":").append(formatDouble(p.getWeight())).append(",");
                json.append("\"bmi\":").append(formatDouble(p.getBmi())).append(",");
                json.append("\"bmiCategory\":\"").append(escapeJson(p.getBmiCategory())).append("\",");
                json.append("\"progressDate\":\"").append(p.getProgressDate().toString()).append("\",");
                json.append("\"notes\":\"").append(escapeJson(p.getNotes() != null ? p.getNotes() : "")).append("\"");
                json.append("}");
                if (i < logs.size() - 1) json.append(",");
            }

            json.append("]}");
            out.write(json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || !"USER".equals(session.getAttribute("role")) || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in first.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");

        try {
            // ACTION: ADD PROGRESS ENTRY
            if ("add".equalsIgnoreCase(action)) {
                String weightStr = request.getParameter("weight");
                String dateStr = request.getParameter("progressDate");
                String notes = request.getParameter("notes");

                if (weightStr == null || weightStr.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Weight is required.\"}");
                    return;
                }

                if (dateStr == null || dateStr.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Progress date is required.\"}");
                    return;
                }

                double weight;
                Date progressDate;

                try {
                    weight = Double.parseDouble(weightStr.trim());
                } catch (NumberFormatException e) {
                    out.write("{\"status\":\"error\",\"message\":\"Weight must be a valid positive number.\"}");
                    return;
                }

                if (weight < 20.0 || weight > 300.0) {
                    out.write("{\"status\":\"error\",\"message\":\"Weight must be between 20 kg and 300 kg.\"}");
                    return;
                }

                try {
                    progressDate = Date.valueOf(dateStr.trim());
                } catch (IllegalArgumentException e) {
                    out.write("{\"status\":\"error\",\"message\":\"Invalid date format. Use YYYY-MM-DD.\"}");
                    return;
                }

                User user = userDAO.getUserById(userId);
                if (user == null) {
                    out.write("{\"status\":\"error\",\"message\":\"User profile not found.\"}");
                    return;
                }

                // Calculate BMI from user's profile height and entered weight
                double bmi = Progress.calculateBMI(weight, user.getHeight());
                String bmiCategory = Progress.calculateBMICategory(bmi);

                Progress p = new Progress(0, userId, weight, bmi, bmiCategory, progressDate, notes != null ? notes.trim() : "");
                boolean success = progressDAO.addProgress(p);

                if (success) {
                    out.write("{\"status\":\"success\",\"message\":\"Progress logged successfully!\",\"progressId\":" + p.getProgressId() + "}");
                } else {
                    out.write("{\"status\":\"error\",\"message\":\"Failed to save progress entry.\"}");
                }
                return;
            }

            // ACTION: DELETE PROGRESS ENTRY
            if ("delete".equalsIgnoreCase(action)) {
                String idStr = request.getParameter("progressId");
                if (idStr == null || idStr.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Progress ID is required for deletion.\"}");
                    return;
                }

                int progressId;
                try {
                    progressId = Integer.parseInt(idStr.trim());
                } catch (NumberFormatException e) {
                    out.write("{\"status\":\"error\",\"message\":\"Invalid progress ID.\"}");
                    return;
                }

                boolean deleted = progressDAO.deleteProgress(progressId, userId);
                if (deleted) {
                    out.write("{\"status\":\"success\",\"message\":\"Progress record deleted successfully.\"}");
                } else {
                    out.write("{\"status\":\"error\",\"message\":\"Unable to delete record. Entry not found or unauthorized.\"}");
                }
                return;
            }

            out.write("{\"status\":\"error\",\"message\":\"Invalid action parameter.\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String formatDouble(Double val) {
        if (val == null) return "0.0";
        return String.format(Locale.US, "%.2f", val);
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
