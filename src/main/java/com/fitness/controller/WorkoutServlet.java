package com.fitness.controller;

import com.fitness.dao.ExerciseDAO;
import com.fitness.dao.WorkoutDAO;
import com.fitness.model.Exercise;
import com.fitness.model.Workout;
import com.fitness.model.WorkoutSet;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller Servlet handling workout logging with custom exercise support,
 * set-by-set reps & weights, workout history, and quick statistics.
 */
@WebServlet("/workout")
public class WorkoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private WorkoutDAO workoutDAO;
    private ExerciseDAO exerciseDAO;

    @Override
    public void init() throws ServletException {
        workoutDAO = new WorkoutDAO();
        exerciseDAO = new ExerciseDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        // Session & Role Security
        HttpSession session = request.getSession(false);
        if (session == null || !"USER".equals(session.getAttribute("role"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            if ("exercises".equalsIgnoreCase(action)) {
                // Return Master Exercises List
                List<Exercise> exercises = exerciseDAO.getAllExercises();
                out.write(buildExercisesJson(exercises));
            } else {
                // Return Logged-in User's Workout History & Quick Stats
                List<Workout> workouts = workoutDAO.getWorkoutsByUserId(userId);
                out.write(buildWorkoutsJson(workouts));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Session & Role Security
        HttpSession session = request.getSession(false);
        if (session == null || !"USER".equals(session.getAttribute("role"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            if ("delete".equalsIgnoreCase(action)) {
                // Delete Workout Entry with Ownership Check
                String workoutIdStr = request.getParameter("workoutId");
                if (workoutIdStr == null || workoutIdStr.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Workout ID is required for deletion.\"}");
                    return;
                }

                int workoutId = Integer.parseInt(workoutIdStr.trim());
                boolean deleted = workoutDAO.deleteWorkout(workoutId, userId);

                if (deleted) {
                    out.write("{\"status\":\"success\",\"message\":\"Workout entry deleted successfully!\"}");
                } else {
                    out.write("{\"status\":\"error\",\"message\":\"Failed to delete workout or unauthorized access.\"}");
                }

            } else {
                // Add New Workout Entry (Supports Custom Exercise & Dynamic Sets)
                String exerciseType = request.getParameter("exerciseType");
                String exerciseIdStr = request.getParameter("exerciseId");
                String customExerciseName = request.getParameter("customExerciseName");
                String workoutDateStr = request.getParameter("workoutDate");
                String numSetsStr = request.getParameter("numSets");

                if (workoutDateStr == null || workoutDateStr.trim().isEmpty() ||
                    numSetsStr == null || numSetsStr.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Workout date and number of sets are required.\"}");
                    return;
                }

                int numSets = Integer.parseInt(numSetsStr.trim());
                if (numSets <= 0 || numSets > 50) {
                    out.write("{\"status\":\"error\",\"message\":\"Number of sets must be between 1 and 50.\"}");
                    return;
                }

                Integer exerciseId = null;
                if ("custom".equalsIgnoreCase(exerciseType) || (exerciseIdStr == null || exerciseIdStr.trim().isEmpty())) {
                    if (customExerciseName == null || customExerciseName.trim().isEmpty()) {
                        out.write("{\"status\":\"error\",\"message\":\"Please enter a custom exercise name.\"}");
                        return;
                    }
                } else {
                    exerciseId = Integer.parseInt(exerciseIdStr.trim());
                    if (exerciseId <= 0) {
                        out.write("{\"status\":\"error\",\"message\":\"Please select a valid exercise.\"}");
                        return;
                    }
                }

                Date workoutDate = Date.valueOf(workoutDateStr.trim());

                // Parse and validate individual Set details
                List<WorkoutSet> setsList = new ArrayList<>();
                for (int i = 1; i <= numSets; i++) {
                    String repsStr = request.getParameter("reps_" + i);
                    String weightStr = request.getParameter("weight_" + i);

                    if (repsStr == null || weightStr == null || repsStr.trim().isEmpty() || weightStr.trim().isEmpty()) {
                        out.write("{\"status\":\"error\",\"message\":\"Please enter Reps and Weight for Set #" + i + ".\"}");
                        return;
                    }

                    int reps = Integer.parseInt(repsStr.trim());
                    double weight = Double.parseDouble(weightStr.trim());

                    if (reps <= 0) {
                        out.write("{\"status\":\"error\",\"message\":\"Reps for Set #" + i + " must be a positive integer.\"}");
                        return;
                    }
                    if (weight < 0) {
                        out.write("{\"status\":\"error\",\"message\":\"Weight for Set #" + i + " cannot be negative.\"}");
                        return;
                    }

                    setsList.add(new WorkoutSet(i, reps, weight));
                }

                Workout workout = new Workout(userId, exerciseId, customExerciseName, workoutDate);
                boolean added = workoutDAO.addWorkoutWithSets(workout, setsList);

                if (added) {
                    out.write("{\"status\":\"success\",\"message\":\"Workout logged successfully!\"}");
                } else {
                    out.write("{\"status\":\"error\",\"message\":\"Failed to log workout session.\"}");
                }
            }

        } catch (NumberFormatException e) {
            out.write("{\"status\":\"error\",\"message\":\"Invalid numeric input. Please check set reps and weights.\"}");
        } catch (IllegalArgumentException e) {
            out.write("{\"status\":\"error\",\"message\":\"Invalid date format.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
        }
    }

    private String buildExercisesJson(List<Exercise> exercises) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"status\":\"success\",\"exercises\":[");
        for (int i = 0; i < exercises.size(); i++) {
            Exercise e = exercises.get(i);
            sb.append("{");
            sb.append("\"exerciseId\":").append(e.getExerciseId()).append(",");
            sb.append("\"exerciseName\":\"").append(escapeJson(e.getExerciseName())).append("\",");
            sb.append("\"muscleGroup\":\"").append(escapeJson(e.getMuscleGroup())).append("\"");
            sb.append("}");
            if (i < exercises.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildWorkoutsJson(List<Workout> workouts) {
        StringBuilder sb = new StringBuilder();
        int totalWorkouts = workouts.size();
        int totalSets = 0;
        double totalVolume = 0.0;

        for (Workout w : workouts) {
            totalSets += w.getTotalSets();
            totalVolume += w.getTotalVolume();
        }

        sb.append("{");
        sb.append("\"status\":\"success\",");
        sb.append("\"totalWorkouts\":").append(totalWorkouts).append(",");
        sb.append("\"totalSets\":").append(totalSets).append(",");
        sb.append("\"totalVolume\":").append(String.format(Locale.US, "%.1f", totalVolume)).append(",");
        sb.append("\"workouts\":[");

        for (int i = 0; i < workouts.size(); i++) {
            Workout w = workouts.get(i);
            sb.append("{");
            sb.append("\"workoutId\":").append(w.getWorkoutId()).append(",");
            sb.append("\"userId\":").append(w.getUserId()).append(",");
            sb.append("\"exerciseId\":").append(w.getExerciseId() != null ? w.getExerciseId() : "null").append(",");
            sb.append("\"customExerciseName\":\"").append(escapeJson(w.getCustomExerciseName())).append("\",");
            sb.append("\"exerciseName\":\"").append(escapeJson(w.getExerciseName())).append("\",");
            sb.append("\"muscleGroup\":\"").append(escapeJson(w.getMuscleGroup())).append("\",");
            sb.append("\"workoutDate\":\"").append(w.getWorkoutDate() != null ? w.getWorkoutDate().toString() : "").append("\",");
            sb.append("\"totalSets\":").append(w.getTotalSets()).append(",");
            sb.append("\"repsSummary\":\"").append(escapeJson(w.getRepsSummary())).append("\",");
            sb.append("\"weightsSummary\":\"").append(escapeJson(w.getWeightsSummary())).append("\",");
            sb.append("\"totalVolume\":").append(String.format(Locale.US, "%.1f", w.getTotalVolume())).append(",");

            // Set Details Array
            sb.append("\"setsList\":[");
            List<WorkoutSet> sets = w.getSetsList();
            for (int j = 0; j < sets.size(); j++) {
                WorkoutSet s = sets.get(j);
                sb.append("{");
                sb.append("\"setNumber\":").append(s.getSetNumber()).append(",");
                sb.append("\"reps\":").append(s.getReps()).append(",");
                sb.append("\"weight\":").append(s.getWeight());
                sb.append("}");
                if (j < sets.size() - 1) sb.append(",");
            }
            sb.append("]");

            sb.append("}");
            if (i < workouts.size() - 1) sb.append(",");
        }

        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
