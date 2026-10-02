package com.fitness.model;

import java.sql.Date;

/**
 * JavaBean model representing a user body weight and BMI progress entry.
 */
public class Progress {

    private int progressId;
    private int userId;
    private double weight;
    private double bmi;
    private String bmiCategory;
    private Date progressDate;
    private String notes;

    public Progress() {}

    public Progress(int progressId, int userId, double weight, double bmi, String bmiCategory, Date progressDate, String notes) {
        this.progressId = progressId;
        this.userId = userId;
        this.weight = weight;
        this.bmi = bmi;
        this.bmiCategory = bmiCategory;
        this.progressDate = progressDate;
        this.notes = notes;
    }

    public int getProgressId() {
        return progressId;
    }

    public void setProgressId(int progressId) {
        this.progressId = progressId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getBmi() {
        return bmi;
    }

    public void setBmi(double bmi) {
        this.bmi = bmi;
    }

    public String getBmiCategory() {
        return bmiCategory;
    }

    public void setBmiCategory(String bmiCategory) {
        this.bmiCategory = bmiCategory;
    }

    public Date getProgressDate() {
        return progressDate;
    }

    public void setProgressDate(Date progressDate) {
        this.progressDate = progressDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Calculates BMI from weight (kg) and height (cm).
     */
    public static double calculateBMI(double weightKg, double heightCm) {
        if (heightCm <= 0 || weightKg <= 0) return 0.0;
        double heightMeters = heightCm / 100.0;
        double bmiVal = weightKg / (heightMeters * heightMeters);
        return Math.round(bmiVal * 100.0) / 100.0;
    }

    /**
     * Determines BMI Category based on BMI value.
     */
    public static String calculateBMICategory(double bmi) {
        if (bmi <= 0) return "N/A";
        if (bmi < 18.5) return "Underweight";
        if (bmi <= 24.9) return "Normal";
        if (bmi <= 29.9) return "Overweight";
        return "Obese";
    }
}
