package com.fitness.model;

import java.sql.Date;

/**
 * JavaBean Model representing a daily food/nutrition log entry with meal timing and AI scanner support.
 */
public class Nutrition {

    private int nutritionId;
    private int userId;
    private String foodName;
    private double calories;
    private double protein;
    private double carbs;
    private double fats;
    private Date nutritionDate;
    private String mealType;  // e.g. 'Breakfast', 'Lunch', 'Dinner', 'Post Workout', 'Other'
    private String mealTime;  // e.g. '08:30:00' or '08:30'
    private String imageName; // Optional reference to uploaded image file

    public Nutrition() {
        this.mealType = "Other";
    }

    public Nutrition(int nutritionId, int userId, String foodName, double calories, double protein, double carbs, double fats, Date nutritionDate) {
        this(nutritionId, userId, foodName, calories, protein, carbs, fats, nutritionDate, "Other", null, null);
    }

    public Nutrition(int nutritionId, int userId, String foodName, double calories, double protein, double carbs, double fats, Date nutritionDate, String mealType, String mealTime, String imageName) {
        this.nutritionId = nutritionId;
        this.userId = userId;
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.nutritionDate = nutritionDate;
        this.mealType = (mealType != null && !mealType.trim().isEmpty()) ? mealType.trim() : "Other";
        this.mealTime = mealTime;
        this.imageName = imageName;
    }

    public int getNutritionId() {
        return nutritionId;
    }

    public void setNutritionId(int nutritionId) {
        this.nutritionId = nutritionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }

    public double getFats() {
        return fats;
    }

    public void setFats(double fats) {
        this.fats = fats;
    }

    public Date getNutritionDate() {
        return nutritionDate;
    }

    public void setNutritionDate(Date nutritionDate) {
        this.nutritionDate = nutritionDate;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = (mealType != null && !mealType.trim().isEmpty()) ? mealType.trim() : "Other";
    }

    public String getMealTime() {
        return mealTime;
    }

    public void setMealTime(String mealTime) {
        this.mealTime = mealTime;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public String toString() {
        return "Nutrition{" +
                "nutritionId=" + nutritionId +
                ", userId=" + userId +
                ", foodName='" + foodName + '\'' +
                ", calories=" + calories +
                ", protein=" + protein +
                ", carbs=" + carbs +
                ", fats=" + fats +
                ", nutritionDate=" + nutritionDate +
                ", mealType='" + mealType + '\'' +
                ", mealTime='" + mealTime + '\'' +
                ", imageName='" + imageName + '\'' +
                '}';
    }
}
