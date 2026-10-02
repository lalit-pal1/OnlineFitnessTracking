package com.fitness.controller;

import com.fitness.dao.AdminDAO;
import com.fitness.model.Admin;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Controller Servlet for handling Admin Login & Session Creation.
 */
@WebServlet("/admin-login")
public class AdminLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AdminDAO adminDAO;

    @Override
    public void init() throws ServletException {
        adminDAO = new AdminDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.sendRedirect("admin-login.html?error=missing_fields");
            return;
        }

        try {
            Admin admin = adminDAO.loginAdmin(email.trim(), password);

            if (admin != null) {
                // Admin Authentication Success
                HttpSession session = request.getSession(true);
                session.setAttribute("admin", admin);
                session.setAttribute("adminId", admin.getAdminId());
                session.setAttribute("adminName", admin.getName());
                session.setAttribute("adminEmail", admin.getEmail());
                session.setAttribute("role", "ADMIN");

                response.sendRedirect("admin-dashboard.html");
            } else {
                // Admin Authentication Failure
                response.sendRedirect("admin-login.html?error=invalid_credentials");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Database Error during Admin Login: " + e.getMessage());
            String encodedMsg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect("admin-login.html?error=invalid_credentials&msg=" + encodedMsg);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("admin-login.html");
    }
}
