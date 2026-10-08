package com.fitness.controller;

import com.fitness.dao.ExerciseDAO;
import com.fitness.model.Exercise;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller Servlet for Admin Exercise Management.
 * Handles Exercise search, catalog viewing, adding new exercises, editing exercises, and deleting exercises.
 */
@WebServlet("/admin-exercises")
public class AdminExerciseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ExerciseDAO exerciseDAO;

    @Override
    public void init() throws ServletException {
        exerciseDAO = new ExerciseDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String search = request.getParameter("search");
        String muscleGroup = request.getParameter("muscleGroup");

        try {
            List<Exercise> list = exerciseDAO.searchExercises(search, muscleGroup);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"success\",");
            json.append("\"exercises\":[");

            for (int i = 0; i < list.size(); i++) {
                Exercise e = list.get(i);
                json.append("{");
                json.append("\"exerciseId\":").append(e.getExerciseId()).append(",");
                json.append("\"exerciseName\":\"").append(escapeJson(e.getExerciseName())).append("\",");
                json.append("\"muscleGroup\":\"").append(escapeJson(e.getMuscleGroup())).append("\",");
                json.append("\"description\":\"").append(escapeJson(e.getDescription())).append("\",");
                json.append("\"createdAt\":\"").append(e.getCreatedAt() != null ? e.getCreatedAt().toString() : "").append("\"");
                json.append("}");
                if (i < list.size() - 1) json.append(",");
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

        String action = request.getParameter("action");

        if ("add".equalsIgnoreCase(action)) {
            String name = request.getParameter("exerciseName");
            String muscle = request.getParameter("muscleGroup");
            String desc = request.getParameter("description");

            if (name == null || name.trim().isEmpty() || muscle == null || muscle.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Exercise name and muscle group are required.\"}");
                return;
            }

            Exercise ex = new Exercise();
            ex.setExerciseName(name.trim());
            ex.setMuscleGroup(muscle.trim());
            ex.setDescription(desc != null ? desc.trim() : "");

            try {
                boolean added = exerciseDAO.addExercise(ex);
                if (added) {
                    out.print("{\"status\":\"success\",\"message\":\"Exercise added to catalog successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"status\":\"error\",\"message\":\"Failed to add exercise.\"}");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
            }

        } else if ("update".equalsIgnoreCase(action)) {
            String idStr = request.getParameter("exerciseId");
            String name = request.getParameter("exerciseName");
            String muscle = request.getParameter("muscleGroup");
            String desc = request.getParameter("description");

            if (idStr == null || idStr.trim().isEmpty() || name == null || name.trim().isEmpty() || muscle == null || muscle.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Exercise ID, name, and muscle group are required.\"}");
                return;
            }

            try {
                int id = Integer.parseInt(idStr.trim());
                Exercise ex = new Exercise();
                ex.setExerciseId(id);
                ex.setExerciseName(name.trim());
                ex.setMuscleGroup(muscle.trim());
                ex.setDescription(desc != null ? desc.trim() : "");

                boolean updated = exerciseDAO.updateExercise(ex);
                if (updated) {
                    out.print("{\"status\":\"success\",\"message\":\"Exercise updated successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"Exercise not found.\"}");
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid exercise ID format.\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
            }

        } else if ("delete".equalsIgnoreCase(action)) {
            String idStr = request.getParameter("exerciseId");
            if (idStr == null || idStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Exercise ID is required for deletion.\"}");
                return;
            }

            try {
                int id = Integer.parseInt(idStr.trim());
                boolean deleted = exerciseDAO.deleteExercise(id);
                if (deleted) {
                    out.print("{\"status\":\"success\",\"message\":\"Exercise deleted from catalog successfully.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"error\",\"message\":\"Exercise not found or already deleted.\"}");
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid exercise ID format.\"}");
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

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
