package com.fitness.dao;

import com.fitness.model.Nutrition;
import com.fitness.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) for Nutrition entities.
 * Supports meal timing and AI scanner image references while enforcing user_id isolation.
 */
public class NutritionDAO {

    /**
     * Inserts a new nutrition food log entry for a user (including meal_type, meal_time, image_name).
     */
    public boolean addNutrition(Nutrition nutrition) throws SQLException {
        String sql = "INSERT INTO nutrition (user_id, food_name, calories, protein, carbs, fats, nutrition_date, meal_type, meal_time, image_name) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, nutrition.getUserId());
            ps.setString(2, nutrition.getFoodName());
            ps.setDouble(3, nutrition.getCalories());
            ps.setDouble(4, nutrition.getProtein());
            ps.setDouble(5, nutrition.getCarbs());
            ps.setDouble(6, nutrition.getFats());
            ps.setDate(7, nutrition.getNutritionDate());
            ps.setString(8, nutrition.getMealType() != null ? nutrition.getMealType() : "Other");

            if (nutrition.getMealTime() != null && !nutrition.getMealTime().trim().isEmpty()) {
                String tStr = nutrition.getMealTime().trim();
                if (tStr.length() == 5) tStr += ":00"; // Format HH:mm -> HH:mm:ss
                try {
                    ps.setTime(9, Time.valueOf(tStr));
                } catch (IllegalArgumentException e) {
                    ps.setNull(9, java.sql.Types.TIME);
                }
            } else {
                ps.setNull(9, java.sql.Types.TIME);
            }

            if (nutrition.getImageName() != null && !nutrition.getImageName().trim().isEmpty()) {
                ps.setString(10, nutrition.getImageName().trim());
            } else {
                ps.setNull(10, java.sql.Types.VARCHAR);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        nutrition.setNutritionId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Retrieves all nutrition entries logged by a specific user.
     */
    public List<Nutrition> getNutritionByUserId(int userId) throws SQLException {
        List<Nutrition> list = new ArrayList<>();
        String sql = "SELECT nutrition_id, user_id, food_name, calories, protein, carbs, fats, nutrition_date, meal_type, meal_time, image_name " +
                     "FROM nutrition WHERE user_id = ? ORDER BY nutrition_date DESC, meal_time DESC, nutrition_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNutrition(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves nutrition entries logged by a user for a specific date.
     */
    public List<Nutrition> getNutritionByUserIdAndDate(int userId, Date date) throws SQLException {
        List<Nutrition> list = new ArrayList<>();
        String sql = "SELECT nutrition_id, user_id, food_name, calories, protein, carbs, fats, nutrition_date, meal_type, meal_time, image_name " +
                     "FROM nutrition WHERE user_id = ? AND nutrition_date = ? ORDER BY meal_time ASC, nutrition_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setDate(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNutrition(rs));
                }
            }
        }
        return list;
    }

    /**
     * Computes total calories, protein, carbs, and fats for a user on a given date.
     */
    public Map<String, Double> getDailyNutritionTotals(int userId, Date date) throws SQLException {
        Map<String, Double> totals = new HashMap<>();
        totals.put("totalCalories", 0.0);
        totals.put("totalProtein", 0.0);
        totals.put("totalCarbs", 0.0);
        totals.put("totalFats", 0.0);

        String sql = "SELECT COALESCE(SUM(calories), 0) AS total_calories, " +
                     "COALESCE(SUM(protein), 0) AS total_protein, " +
                     "COALESCE(SUM(carbs), 0) AS total_carbs, " +
                     "COALESCE(SUM(fats), 0) AS total_fats " +
                     "FROM nutrition WHERE user_id = ? AND nutrition_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setDate(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totals.put("totalCalories", rs.getDouble("total_calories"));
                    totals.put("totalProtein", rs.getDouble("total_protein"));
                    totals.put("totalCarbs", rs.getDouble("total_carbs"));
                    totals.put("totalFats", rs.getDouble("total_fats"));
                }
            }
        }
        return totals;
    }

    /**
     * Deletes a nutrition record with strict user_id ownership check.
     */
    public boolean deleteNutrition(int nutritionId, int userId) throws SQLException {
        String sql = "DELETE FROM nutrition WHERE nutrition_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, nutritionId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Helper to map ResultSet to Nutrition JavaBean.
     */
    private Nutrition mapResultSetToNutrition(ResultSet rs) throws SQLException {
        Nutrition n = new Nutrition();
        n.setNutritionId(rs.getInt("nutrition_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setFoodName(rs.getString("food_name"));
        n.setCalories(rs.getDouble("calories"));
        n.setProtein(rs.getDouble("protein"));
        n.setCarbs(rs.getDouble("carbs"));
        n.setFats(rs.getDouble("fats"));
        n.setNutritionDate(rs.getDate("nutrition_date"));
        n.setMealType(rs.getString("meal_type"));
        
        Time t = rs.getTime("meal_time");
        if (t != null) {
            n.setMealTime(t.toString()); // Format HH:mm:ss
        } else {
            n.setMealTime(null);
        }
        
        n.setImageName(rs.getString("image_name"));
        return n;
    }
}
