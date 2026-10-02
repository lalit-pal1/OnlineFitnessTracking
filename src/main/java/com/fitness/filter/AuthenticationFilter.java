package com.fitness.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter for role-based session protection.
 * Protects user dashboard, workout tracking, nutrition, progress, goals, and admin pages/servlets from unauthenticated access.
 */
@WebFilter(urlPatterns = {
    "/user-dashboard.html", "/user-dashboard", "/user-home.html", "/profile.html",
    "/workout.html", "/workout",
    "/nutrition.html", "/nutrition",
    "/progress.html", "/progress",
    "/goals.html", "/goals", "/goal",
    "/admin-home.html", "/admin-dashboard.html", "/admin-dashboard",
    "/admin-users.html", "/admin-users",
    "/admin-exercises.html", "/admin-exercises",
    "/admin-nutrition.html", "/admin-nutrition",
    "/admin-workouts.html", "/admin-workouts",
    "/admin-progress.html", "/admin-progress"
})
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        String uri = req.getRequestURI();

        // Prevent browser caching of protected pages
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);

        // Protect Admin pages and servlets
        if (uri.contains("admin-")) {
            // Allow admin-login.html and /admin-login servlet to pass through
            if (!uri.contains("admin-login")) {
                if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
                    if (isApiRequest(uri)) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized admin access.\"}");
                    } else {
                        res.sendRedirect("admin-login.html?error=unauthorized");
                    }
                    return;
                }
            }
        }

        // Protect User pages and user servlets
        if (uri.contains("user-dashboard") || uri.contains("user-home.html") || 
            uri.contains("workout") || uri.contains("nutrition") || uri.contains("progress") ||
            uri.contains("goals") || uri.endsWith("/goal")) {
            
            // Exclude admin URIs from user check (already handled above)
            if (!uri.contains("admin-")) {
                if (session == null || !"USER".equals(session.getAttribute("role"))) {
                    if (isApiRequest(uri)) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized access.\"}");
                    } else {
                        res.sendRedirect("login.html?error=unauthorized");
                    }
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isApiRequest(String uri) {
        return uri.endsWith("/user-dashboard") || uri.endsWith("/workout") ||
               uri.endsWith("/nutrition") || uri.endsWith("/progress") ||
               uri.endsWith("/goals") || uri.endsWith("/goal") ||
               uri.endsWith("/admin-dashboard") || uri.endsWith("/admin-users") ||
               uri.endsWith("/admin-exercises") || uri.endsWith("/admin-nutrition") ||
               uri.endsWith("/admin-workouts") || uri.endsWith("/admin-progress");
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
