package com.fitness.dao;

import com.fitness.model.Exercise;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Exercise entity.
 * Handles database operations for exercise catalog retrieval and Admin CRUD management.
 */
public class ExerciseDAO {

    /**
     * Fetches all available exercises from the exercises table using PreparedStatement.
     */
    public List<Exercise> getAllExercises() throws SQLException {
        return searchExercises(null, null);
    }

    /**
     * Fetches exercises with optional name search and muscle group filter.
     */
    public List<Exercise> searchExercises(String search, String muscleGroup) throws SQLException {
        List<Exercise> exercises = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM exercises WHERE 1=1 ");

        boolean hasSearch = (search != null && !search.trim().isEmpty());
        boolean hasMuscle = (muscleGroup != null && !muscleGroup.trim().isEmpty() && !"ALL".equalsIgnoreCase(muscleGroup.trim()));

        if (hasSearch) {
            sql.append("AND (LOWER(exercise_name) LIKE ? OR LOWER(description) LIKE ?) ");
        }
        if (hasMuscle) {
            sql.append("AND LOWER(muscle_group) = ? ");
        }
        sql.append("ORDER BY exercise_name ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (hasSearch) {
                String term = "%" + search.trim().toLowerCase() + "%";
                ps.setString(paramIndex++, term);
                ps.setString(paramIndex++, term);
            }
            if (hasMuscle) {
                ps.setString(paramIndex++, muscleGroup.trim().toLowerCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Exercise exercise = new Exercise();
                    exercise.setExerciseId(rs.getInt("exercise_id"));
                    exercise.setExerciseName(rs.getString("exercise_name"));
                    exercise.setMuscleGroup(rs.getString("muscle_group"));
                    exercise.setDescription(rs.getString("description"));
                    exercise.setCreatedAt(rs.getTimestamp("created_at"));
                    exercises.add(exercise);
                }
            }
        }
        return exercises;
    }

    /**
     * Retrieves an Exercise by exercise_id.
     */
    public Exercise getExerciseById(int exerciseId) throws SQLException {
        String sql = "SELECT * FROM exercises WHERE exercise_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, exerciseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Exercise exercise = new Exercise();
                    exercise.setExerciseId(rs.getInt("exercise_id"));
                    exercise.setExerciseName(rs.getString("exercise_name"));
                    exercise.setMuscleGroup(rs.getString("muscle_group"));
                    exercise.setDescription(rs.getString("description"));
                    exercise.setCreatedAt(rs.getTimestamp("created_at"));
                    return exercise;
                }
            }
        }
        return null;
    }

    /**
     * Adds a new exercise into the catalog.
     */
    public boolean addExercise(Exercise exercise) throws SQLException {
        String sql = "INSERT INTO exercises (exercise_name, muscle_group, description) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, exercise.getExerciseName().trim());
            ps.setString(2, exercise.getMuscleGroup().trim());
            ps.setString(3, exercise.getDescription() != null ? exercise.getDescription().trim() : null);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        exercise.setExerciseId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Updates an existing exercise in the catalog.
     */
    public boolean updateExercise(Exercise exercise) throws SQLException {
        String sql = "UPDATE exercises SET exercise_name = ?, muscle_group = ?, description = ? WHERE exercise_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, exercise.getExerciseName().trim());
            ps.setString(2, exercise.getMuscleGroup().trim());
            ps.setString(3, exercise.getDescription() != null ? exercise.getDescription().trim() : null);
            ps.setInt(4, exercise.getExerciseId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes an exercise from the catalog by exercise_id.
     */
    public boolean deleteExercise(int exerciseId) throws SQLException {
        String sql = "DELETE FROM exercises WHERE exercise_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, exerciseId);
            return ps.executeUpdate() > 0;
        }
    }
}
