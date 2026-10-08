package com.fitness.controller;

import com.fitness.dao.GoalDAO;
import com.fitness.dao.ProgressDAO;
import com.fitness.dao.UserDAO;
import com.fitness.model.Goal;
import com.fitness.model.Progress;
import com.fitness.model.User;
import com.fitness.util.DatabaseConnection;
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

/**
 * Controller Servlet for User My Goals Module.
 * Manages adding goals, updating active goals, deleting goals, and calculating progress toward target weight.
 * Protected by AuthenticationFilter (requires role = USER).
 */
@WebServlet({"/goals", "/goal"})
public class GoalServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private GoalDAO goalDAO;
    private UserDAO userDAO;
    private ProgressDAO progressDAO;

    @Override
    public void init() throws ServletException {
        goalDAO = new GoalDAO();
        userDAO = new UserDAO();
        progressDAO = new ProgressDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in.\"}");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        try {
            Goal activeGoal = goalDAO.getActiveGoalByUserId(userId);
            List<Goal> history = goalDAO.getAllGoalsByUserId(userId);

            // Fetch current user weight
            double currentWeight = 0.0;
            Progress latestProgress = progressDAO.getLatestProgressByUserId(userId);
            if (latestProgress != null) {
                currentWeight = latestProgress.getWeight();
            } else {
                User user = userDAO.getUserById(userId);
                if (user != null) currentWeight = user.getWeight();
            }

            // Calculate progress metrics if active goal exists
            double startWeight = 0.0;
            double progressPercentage = 0.0;
            double weightRemaining = 0.0;
            String statusMsg = "";

            if (activeGoal != null) {
                startWeight = goalDAO.getInitialWeightForGoal(userId, activeGoal.getStartDate());
                if (startWeight <= 0) startWeight = currentWeight;

                double targetWeight = activeGoal.getTargetWeight();
                String type = activeGoal.getGoalType() != null ? activeGoal.getGoalType().trim() : "Weight Loss";

                if ("Muscle Gain".equalsIgnoreCase(type)) {
                    double targetDelta = targetWeight - startWeight;
                    double currentGain = currentWeight - startWeight;
                    if (targetDelta > 0) {
                        progressPercentage = (currentGain / targetDelta) * 100.0;
                    } else {
                        progressPercentage = (currentWeight >= targetWeight) ? 100.0 : 0.0;
                    }

                    if (currentWeight >= targetWeight) {
                        progressPercentage = 100.0;
                        statusMsg = "🎉 Goal Achieved! Target weight reached.";
                    } else {
                        weightRemaining = targetWeight - currentWeight;
                        statusMsg = String.format("%.1f kg to gain", weightRemaining);
                    }

                } else if ("Weight Loss".equalsIgnoreCase(type)) {
                    double targetLoss = startWeight - targetWeight;
                    double currentLoss = startWeight - currentWeight;
                    if (targetLoss > 0) {
                        progressPercentage = (currentLoss / targetLoss) * 100.0;
                    } else {
                        progressPercentage = (currentWeight <= targetWeight) ? 100.0 : 0.0;
                    }

                    if (currentWeight <= targetWeight) {
                        progressPercentage = 100.0;
                        statusMsg = "🎉 Goal Achieved! Target weight reached.";
                    } else {
                        weightRemaining = currentWeight - targetWeight;
                        statusMsg = String.format("%.1f kg to lose", weightRemaining);
                    }

                } else { // Maintenance
                    double diff = Math.abs(currentWeight - targetWeight);
                    if (diff <= 1.5) {
                        progressPercentage = 100.0;
                        statusMsg = "✅ On Track! Weight maintained within range.";
                    } else {
                        progressPercentage = Math.max(0.0, 100.0 - (diff * 10.0));
                        statusMsg = String.format("%.1f kg off maintenance target", diff);
                    }
                }

                // Clamp percentage between 0% and 100%
                if (progressPercentage < 0) progressPercentage = 0.0;
                if (progressPercentage > 100) progressPercentage = 100.0;
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"success\",");
            json.append("\"currentWeight\":").append(currentWeight).append(",");
            json.append("\"startWeight\":").append(startWeight).append(",");
            json.append("\"progressPercentage\":").append(String.format("%.1f", progressPercentage)).append(",");
            json.append("\"weightRemaining\":").append(String.format("%.1f", weightRemaining)).append(",");
            json.append("\"statusMessage\":\"").append(escapeJson(statusMsg)).append("\",");

            json.append("\"activeGoal\":");
            if (activeGoal != null) {
                json.append(toJsonGoal(activeGoal));
            } else {
                json.append("null");
            }
            json.append(",");

            json.append("\"history\":[");
            for (int i = 0; i < history.size(); i++) {
                json.append(toJsonGoal(history.get(i)));
                if (i < history.size() - 1) json.append(",");
            }
            json.append("]}");

            out.print(json.toString());

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

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in.\"}");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        if ("add".equalsIgnoreCase(action)) {
            String type = request.getParameter("goalType");
            String weightStr = request.getParameter("targetWeight");
            String targetDateStr = request.getParameter("targetDate");
            String caloriesStr = request.getParameter("dailyCalories");
            String proteinStr = request.getParameter("dailyProtein");
            String notes = request.getParameter("notes");

            if (type == null || type.trim().isEmpty() || weightStr == null || weightStr.trim().isEmpty() || targetDateStr == null || targetDateStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Goal Type, Target Weight, and Target Date are required.\"}");
                return;
            }

            try {
                double targetWeight = Double.parseDouble(weightStr.trim());
                if (targetWeight < 20.0 || targetWeight > 300.0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"status\":\"error\",\"message\":\"Target weight must be between 20 kg and 300 kg.\"}");
                    return;
                }

                Date targetDate = Date.valueOf(targetDateStr.trim());

                double dailyCalories = 0.0;
                if (caloriesStr != null && !caloriesStr.trim().isEmpty()) {
                    dailyCalories = Double.parseDouble(caloriesStr.trim());
                }

                double dailyProtein = 0.0;
                if (proteinStr != null && !proteinStr.trim().isEmpty()) {
                    dailyProtein = Double.parseDouble(proteinStr.trim());
                }

                Goal goal = new Goal();
                goal.setUserId(userId);
                goal.setGoalType(type.trim());
                goal.setTargetWeight(targetWeight);
                goal.setStartDate(new Date(System.currentTimeMillis()));
                goal.setTargetDate(targetDate);
                goal.setDailyCalories(dailyCalories);
                goal.setDailyProtein(dailyProtein);
                goal.setNotes(notes != null ? notes.trim() : "");
                goal.setStatus("IN_PROGRESS");

                boolean added = goalDAO.addGoal(goal);
                if (added) {
                    out.print("{\"status\":\"success\",\"message\":\"Goal created and set as active target!\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"status\":\"error\",\"message\":\"Failed to save goal.\"}");
                }

            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid input format for numbers or date.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
            }

        } else if ("update".equalsIgnoreCase(action)) {
            String goalIdStr = request.getParameter("goalId");
            String type = request.getParameter("goalType");
            String weightStr = request.getParameter("targetWeight");
            String targetDateStr = request.getParameter("targetDate");
            String caloriesStr = request.getParameter("dailyCalories");
            String proteinStr = request.getParameter("dailyProtein");
            String notes = request.getParameter("notes");
            String status = request.getParameter("status");

            if (goalIdStr == null || goalIdStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Goal ID is required for update.\"}");
                return;
            }

            try {
                int goalId = Integer.parseInt(goalIdStr.trim());
                double targetWeight = Double.parseDouble(weightStr.trim());
                Date targetDate = Date.valueOf(targetDateStr.trim());

                double dailyCalories = (caloriesStr != null && !caloriesStr.trim().isEmpty()) ? Double.parseDouble(caloriesStr.trim()) : 0.0;
                double dailyProtein = (proteinStr != null && !proteinStr.trim().isEmpty()) ? Double.parseDouble(proteinStr.trim()) : 0.0;

                Goal goal = new Goal();
                goal.setGoalId(goalId);
                goal.setUserId(userId);
                goal.setGoalType(type.trim());
                goal.setTargetWeight(targetWeight);
                goal.setTargetDate(targetDate);
                goal.setDailyCalories(dailyCalories);
                goal.setDailyProtein(dailyProtein);
                goal.setNotes(notes != null ? notes.trim() : "");
                goal.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : "IN_PROGRESS");

                boolean updated = goalDAO.updateGoal(goal);
                if (updated) {
                    out.print("{\"status\":\"success\",\"message\":\"Goal updated successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"Goal record not found or permission denied.\"}");
                }

            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid input format for numbers or date.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
            }

        } else if ("delete".equalsIgnoreCase(action)) {
            String goalIdStr = request.getParameter("goalId");
            if (goalIdStr == null || goalIdStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Goal ID is required for deletion.\"}");
                return;
            }

            try {
                int goalId = Integer.parseInt(goalIdStr.trim());
                boolean deleted = goalDAO.deleteGoal(goalId, userId);
                if (deleted) {
                    out.print("{\"status\":\"success\",\"message\":\"Goal deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"Goal record not found or permission denied.\"}");
                }

            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid Goal ID format.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
            }

        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid action.\"}");
        }
    }

    private String toJsonGoal(Goal g) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"goalId\":").append(g.getGoalId()).append(",");
        sb.append("\"userId\":").append(g.getUserId()).append(",");
        sb.append("\"goalType\":\"").append(escapeJson(g.getGoalType())).append("\",");
        sb.append("\"targetValue\":\"").append(escapeJson(g.getTargetValue())).append("\",");
        sb.append("\"targetWeight\":").append(g.getTargetWeight()).append(",");
        sb.append("\"startDate\":\"").append(g.getStartDate() != null ? g.getStartDate().toString() : "").append("\",");
        sb.append("\"targetDate\":\"").append(g.getTargetDate() != null ? g.getTargetDate().toString() : "").append("\",");
        sb.append("\"dailyCalories\":").append(g.getDailyCalories()).append(",");
        sb.append("\"dailyProtein\":").append(g.getDailyProtein()).append(",");
        sb.append("\"notes\":\"").append(escapeJson(g.getNotes())).append("\",");
        sb.append("\"status\":\"").append(escapeJson(g.getStatus())).append("\"");
        sb.append("}");
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
