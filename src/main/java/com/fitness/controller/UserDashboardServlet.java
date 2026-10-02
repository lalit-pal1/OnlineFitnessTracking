package com.fitness.controller;

import com.fitness.dao.UserDAO;
import com.fitness.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Controller Servlet for User Dashboard metrics and profile info.
 * Accessible only via valid user session.
 */
@WebServlet("/user-dashboard")
public class UserDashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        // Session & Role Verification
        HttpSession session = request.getSession(false);
        if (session == null || !"USER".equals(session.getAttribute("role"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in.\"}");
            return;
        }

        User user = null;
        Object userIdObj = session.getAttribute("userId");

        if (userIdObj instanceof Integer) {
            int userId = (Integer) userIdObj;
            try {
                user = userDAO.getUserById(userId);
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("⚠️ Database warning in UserDashboardServlet: " + e.getMessage());
            }
        }

        // Fallback to session user object if DB fetch was null
        if (user == null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof User) {
                user = (User) userObj;
            }
        }

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"User session not found.\"}");
            return;
        }

        // Calculate BMI using height (cm) and weight (kg)
        double heightCm = user.getHeight();
        double weightKg = user.getWeight();
        double bmi = 0.0;
        String bmiCategory = "N/A";

        if (heightCm > 0 && weightKg > 0) {
            double heightMeters = heightCm / 100.0;
            bmi = weightKg / (heightMeters * heightMeters);

            if (bmi < 18.5) {
                bmiCategory = "Underweight";
            } else if (bmi <= 24.9) {
                bmiCategory = "Normal";
            } else if (bmi <= 29.9) {
                bmiCategory = "Overweight";
            } else {
                bmiCategory = "Obese";
            }
        }

        String formattedBmi = (bmi > 0) ? String.format(Locale.US, "%.1f", bmi) : "N/A";

        // Construct JSON Response
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write("{");
        out.write("\"status\":\"success\",");
        out.write("\"userId\":" + user.getUserId() + ",");
        out.write("\"name\":\"" + escapeJson(user.getName()) + "\",");
        out.write("\"email\":\"" + escapeJson(user.getEmail()) + "\",");
        out.write("\"age\":" + user.getAge() + ",");
        out.write("\"gender\":\"" + escapeJson(user.getGender()) + "\",");
        out.write("\"height\":" + user.getHeight() + ",");
        out.write("\"weight\":" + user.getWeight() + ",");
        out.write("\"fitnessGoal\":\"" + escapeJson(user.getFitnessGoal()) + "\",");
        out.write("\"bmi\":" + String.format(Locale.US, "%.2f", bmi) + ",");
        out.write("\"formattedBmi\":\"" + formattedBmi + "\",");
        out.write("\"bmiCategory\":\"" + bmiCategory + "\"");
        out.write("}");
    }

    /**
     * Simple utility to escape quotes and special characters for JSON output.
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
