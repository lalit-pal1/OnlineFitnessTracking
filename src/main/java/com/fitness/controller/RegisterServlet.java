package com.fitness.controller;

import com.fitness.dao.UserDAO;
import com.fitness.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Controller Servlet for handling User Registration requests.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String ageStr = request.getParameter("age");
        String gender = request.getParameter("gender");
        String heightStr = request.getParameter("height");
        String weightStr = request.getParameter("weight");
        String fitnessGoal = request.getParameter("fitnessGoal");

        // Backend Validation
        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.isEmpty() ||
            ageStr == null || heightStr == null || weightStr == null) {
            
            response.sendRedirect("register.html?error=missing_fields");
            return;
        }

        try {
            int age = Integer.parseInt(ageStr.trim());
            double height = Double.parseDouble(heightStr.trim());
            double weight = Double.parseDouble(weightStr.trim());

            if (age < 10 || age > 100 || height < 50 || height > 250 || weight < 20 || weight > 300) {
                response.sendRedirect("register.html?error=invalid_numbers");
                return;
            }

            if (!email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                response.sendRedirect("register.html?error=invalid_email");
                return;
            }

            if (password.length() < 8) {
                response.sendRedirect("register.html?error=weak_password");
                return;
            }

            if (name.trim().length() < 2 || name.trim().length() > 100) {
                response.sendRedirect("register.html?error=invalid_name");
                return;
            }

            // Check duplicate email
            if (userDAO.emailExists(email)) {
                response.sendRedirect("register.html?error=email_exists");
                return;
            }

            // Construct User object
            User user = new User(name.trim(), email.trim(), password, age, gender, height, weight, fitnessGoal);

            // Register via DAO
            boolean success = userDAO.registerUser(user);

            if (success) {
                response.sendRedirect("login.html?registered=true");
            } else {
                response.sendRedirect("register.html?error=registration_failed");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Database Error during User Registration");
            response.sendRedirect("register.html?error=registration_failed");
        } catch (NumberFormatException e) {
            response.sendRedirect("register.html?error=invalid_format");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("register.html");
    }
}
