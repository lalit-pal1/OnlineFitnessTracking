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
 * Controller Servlet for Admin Workout Activity Management.
 * Displays all user workout logs with set breakdowns and volume calculation, supports filtering and deletion.
 */
@WebServlet("/admin-workouts")
public class AdminWorkoutServlet extends HttpServlet {
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

            // 2. Fetch Workouts with Filters
            StringBuilder sql = new StringBuilder(
                "SELECT w.workout_id, w.user_id, u.name AS user_name, u.email AS user_email, " +
                "w.exercise_id, w.custom_exercise_name, " +
                "COALESCE(e.exercise_name, w.custom_exercise_name) AS display_exercise_name, " +
                "COALESCE(e.muscle_group, 'Custom') AS display_muscle_group, " +
                "w.workout_date " +
                "FROM workouts w " +
                "JOIN users u ON w.user_id = u.user_id " +
                "LEFT JOIN exercises e ON w.exercise_id = e.exercise_id " +
                "WHERE 1=1 "
            );

            Integer filterUserId = null;
            if (userIdStr != null && !userIdStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(userIdStr.trim())) {
                try {
                    filterUserId = Integer.parseInt(userIdStr.trim());
                    sql.append("AND w.user_id = ? ");
                } catch (NumberFormatException ignored) {}
            }

            boolean hasDate = (dateStr != null && !dateStr.trim().isEmpty());
            if (hasDate) {
                sql.append("AND w.workout_date = ? ");
            }

            boolean hasSearch = (search != null && !search.trim().isEmpty());
            if (hasSearch) {
                sql.append("AND (LOWER(COALESCE(e.exercise_name, w.custom_exercise_name)) LIKE ? OR LOWER(u.name) LIKE ? OR LOWER(u.email) LIKE ?) ");
            }

            sql.append("ORDER BY w.workout_date DESC, w.workout_id DESC");

            List<Map<String, Object>> workouts = new ArrayList<>();
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
                        int workoutId = rs.getInt("workout_id");
                        map.put("workoutId", workoutId);
                        map.put("userId", rs.getInt("user_id"));
                        map.put("userName", rs.getString("user_name"));
                        map.put("userEmail", rs.getString("user_email"));
                        map.put("exerciseName", rs.getString("display_exercise_name"));
                        map.put("muscleGroup", rs.getString("display_muscle_group"));
                        map.put("workoutDate", rs.getDate("workout_date") != null ? rs.getDate("workout_date").toString() : "");

                        // Fetch sets and compute volume
                        List<Map<String, Object>> setsList = getSetsForWorkout(conn, workoutId);
                        double totalVolume = 0.0;
                        for (Map<String, Object> setMap : setsList) {
                            int reps = (int) setMap.get("reps");
                            double weight = (double) setMap.get("weight");
                            totalVolume += (reps * weight);
                        }
                        map.put("sets", setsList);
                        map.put("totalSets", setsList.size());
                        map.put("totalVolume", totalVolume);

                        workouts.add(map);
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

            json.append("\"workouts\":[");
            for (int i = 0; i < workouts.size(); i++) {
                Map<String, Object> w = workouts.get(i);
                json.append("{");
                json.append("\"workoutId\":").append(w.get("workoutId")).append(",");
                json.append("\"userId\":").append(w.get("userId")).append(",");
                json.append("\"userName\":\"").append(escapeJson((String) w.get("userName"))).append("\",");
                json.append("\"userEmail\":\"").append(escapeJson((String) w.get("userEmail"))).append("\",");
                json.append("\"exerciseName\":\"").append(escapeJson((String) w.get("exerciseName"))).append("\",");
                json.append("\"muscleGroup\":\"").append(escapeJson((String) w.get("muscleGroup"))).append("\",");
                json.append("\"workoutDate\":\"").append(w.get("workoutDate")).append("\",");
                json.append("\"totalSets\":").append(w.get("totalSets")).append(",");
                json.append("\"totalVolume\":").append(w.get("totalVolume")).append(",");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sets = (List<Map<String, Object>>) w.get("sets");
                json.append("\"sets\":[");
                for (int s = 0; s < sets.size(); s++) {
                    Map<String, Object> setMap = sets.get(s);
                    json.append("{");
                    json.append("\"setNumber\":").append(setMap.get("setNumber")).append(",");
                    json.append("\"reps\":").append(setMap.get("reps")).append(",");
                    json.append("\"weight\":").append(setMap.get("weight"));
                    json.append("}");
                    if (s < sets.size() - 1) json.append(",");
                }
                json.append("]");

                json.append("}");
                if (i < workouts.size() - 1) json.append(",");
            }
            json.append("]}");

            out.print(json.toString());

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid date format. Expected YYYY-MM-DD.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private List<Map<String, Object>> getSetsForWorkout(Connection conn, int workoutId) throws SQLException {
        List<Map<String, Object>> sets = new ArrayList<>();
        String sql = "SELECT set_number, reps, weight FROM workout_sets WHERE workout_id = ? ORDER BY set_number ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("setNumber", rs.getInt("set_number"));
                    map.put("reps", rs.getInt("reps"));
                    map.put("weight", rs.getDouble("weight"));
                    sets.add(map);
                }
            }
        }
        return sets;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        String workoutIdStr = request.getParameter("workoutId");

        if ("delete".equalsIgnoreCase(action) && workoutIdStr != null && !workoutIdStr.trim().isEmpty()) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM workouts WHERE workout_id = ?")) {

                int workoutId = Integer.parseInt(workoutIdStr.trim());
                ps.setInt(1, workoutId);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    out.print("{\"status\":\"success\",\"message\":\"Workout session deleted successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"Workout record not found or already deleted.\"}");
                }

            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid workout ID format.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"message\":\"Invalid request or missing action/workoutId parameter.\"}");
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
