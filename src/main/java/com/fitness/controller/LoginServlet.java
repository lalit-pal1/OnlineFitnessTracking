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
import java.sql.SQLException;

/**
 * Controller Servlet for handling User Login & Session Creation.
 * Stores authenticated user details and role in HttpSession.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
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

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.sendRedirect("login.html?error=missing_fields");
            return;
        }

        if (!email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            response.sendRedirect("login.html?error=invalid_email");
            return;
        }

        try {
            User user = userDAO.loginUser(email.trim(), password);

            if (user != null) {
                // Authentication Success: Create HTTP Session
                HttpSession session = request.getSession(true);
                session.setAttribute("user", user);
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("userName", user.getName());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userAge", user.getAge());
                session.setAttribute("userGender", user.getGender());
                session.setAttribute("userHeight", user.getHeight());
                session.setAttribute("userWeight", user.getWeight());
                session.setAttribute("userGoal", user.getFitnessGoal());
                session.setAttribute("role", "USER");

                response.sendRedirect("user-dashboard.html");
            } else {
                // Authentication Failure
                response.sendRedirect("login.html?error=invalid_credentials");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Database Error during User Login");
            response.sendRedirect("login.html?error=invalid_credentials");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("login.html");
    }
}
