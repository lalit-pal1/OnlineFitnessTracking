package com.fitness.dao;

import com.fitness.model.Workout;
import com.fitness.model.WorkoutSet;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Workout & WorkoutSet entities.
 * Handles transactional logging, profile retrieval, and workout deletion.
 */
public class WorkoutDAO {

    /**
     * Inserts a workout session header and all set details in a single database transaction.
     * Rollbacks automatically if any step fails.
     */
    public boolean addWorkoutWithSets(Workout workout, List<WorkoutSet> setsList) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Insert Workout Session Header
            String sqlWorkout = "INSERT INTO workouts (user_id, exercise_id, custom_exercise_name, workout_date) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psW = conn.prepareStatement(sqlWorkout, Statement.RETURN_GENERATED_KEYS)) {
                psW.setInt(1, workout.getUserId());

                if (workout.getExerciseId() != null && workout.getExerciseId() > 0) {
                    psW.setInt(2, workout.getExerciseId());
                } else {
                    psW.setNull(2, java.sql.Types.INTEGER);
                }

                if (workout.getCustomExerciseName() != null && !workout.getCustomExerciseName().trim().isEmpty()) {
                    psW.setString(3, workout.getCustomExerciseName().trim());
                } else {
                    psW.setNull(3, java.sql.Types.VARCHAR);
                }

                psW.setDate(4, workout.getWorkoutDate());
                psW.executeUpdate();

                ResultSet rsKeys = psW.getGeneratedKeys();
                int workoutId = 0;
                if (rsKeys.next()) {
                    workoutId = rsKeys.getInt(1);
                }

                if (workoutId <= 0) {
                    conn.rollback();
                    return false;
                }

                workout.setWorkoutId(workoutId);
            }

            // 2. Insert Sets Records Batch
            String sqlSet = "INSERT INTO workout_sets (workout_id, set_number, reps, weight) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psS = conn.prepareStatement(sqlSet)) {
                for (WorkoutSet set : setsList) {
                    psS.setInt(1, workout.getWorkoutId());
                    psS.setInt(2, set.getSetNumber());
                    psS.setInt(3, set.getReps());
                    psS.setDouble(4, set.getWeight());
                    psS.addBatch();
                }
                psS.executeBatch();
            }

            conn.commit(); // Commit Transaction
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    /**
     * Retrieves all workout sessions logged by user_id with set breakdowns.
     * Resolves exercise display name (master exercise OR custom exercise name).
     */
    public List<Workout> getWorkoutsByUserId(int userId) throws SQLException {
        List<Workout> workouts = new ArrayList<>();
        String sql = "SELECT w.workout_id, w.user_id, w.exercise_id, w.custom_exercise_name, " +
                     "COALESCE(e.exercise_name, w.custom_exercise_name) AS display_exercise_name, " +
                     "COALESCE(e.muscle_group, 'Custom') AS display_muscle_group, " +
                     "w.workout_date " +
                     "FROM workouts w " +
                     "LEFT JOIN exercises e ON w.exercise_id = e.exercise_id " +
                     "WHERE w.user_id = ? " +
                     "ORDER BY w.workout_date DESC, w.workout_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Workout workout = new Workout();
                    workout.setWorkoutId(rs.getInt("workout_id"));
                    workout.setUserId(rs.getInt("user_id"));
                    
                    int exId = rs.getInt("exercise_id");
                    if (!rs.wasNull()) {
                        workout.setExerciseId(exId);
                    }
                    
                    workout.setCustomExerciseName(rs.getString("custom_exercise_name"));
                    workout.setExerciseName(rs.getString("display_exercise_name"));
                    workout.setMuscleGroup(rs.getString("display_muscle_group"));
                    workout.setWorkoutDate(rs.getDate("workout_date"));

                    // Fetch Sets for this Workout Session
                    workout.setSetsList(getSetsForWorkout(conn, workout.getWorkoutId()));
                    workouts.add(workout);
                }
            }
        }
        return workouts;
    }

    /**
     * Helper method to fetch sets for a given workout_id.
     */
    private List<WorkoutSet> getSetsForWorkout(Connection conn, int workoutId) throws SQLException {
        List<WorkoutSet> sets = new ArrayList<>();
        String sql = "SELECT set_id, workout_id, set_number, reps, weight FROM workout_sets WHERE workout_id = ? ORDER BY set_number ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workoutId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WorkoutSet set = new WorkoutSet();
                    set.setSetId(rs.getInt("set_id"));
                    set.setWorkoutId(rs.getInt("workout_id"));
                    set.setSetNumber(rs.getInt("set_number"));
                    set.setReps(rs.getInt("reps"));
                    set.setWeight(rs.getDouble("weight"));
                    sets.add(set);
                }
            }
        }
        return sets;
    }

    /**
     * Deletes a workout record with user_id ownership check (workout_id = ? AND user_id = ?).
     * Cascades automatically to workout_sets child table.
     */
    public boolean deleteWorkout(int workoutId, int userId) throws SQLException {
        String sql = "DELETE FROM workouts WHERE workout_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workoutId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }
}
