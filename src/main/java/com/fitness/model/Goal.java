package com.fitness.model;

import java.io.Serializable;
import java.sql.Date;

/**
 * JavaBean Model representing a User Fitness Goal.
 */
public class Goal implements Serializable {
    private static final long serialVersionUID = 1L;

    private int goalId;
    private int userId;
    private String goalType;       // 'Muscle Gain', 'Weight Loss', 'Maintenance'
    private String targetValue;     // Legacy column formatted display e.g. "75.00 kg"
    private double targetWeight;    // Target Weight in kg
    private Date startDate;
    private Date targetDate;
    private double dailyCalories;   // Target daily calories (kcal)
    private double dailyProtein;    // Target daily protein (g)
    private String notes;
    private String status;          // 'IN_PROGRESS', 'COMPLETED', 'SUPERSEDED'

    public Goal() {}

    public Goal(int userId, String goalType, double targetWeight, Date startDate, Date targetDate, double dailyCalories, double dailyProtein, String notes) {
        this.userId = userId;
        this.goalType = goalType;
        this.targetWeight = targetWeight;
        this.targetValue = String.format("%.2f kg", targetWeight);
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.dailyCalories = dailyCalories;
        this.dailyProtein = dailyProtein;
        this.notes = notes;
        this.status = "IN_PROGRESS";
    }

    // Getters and Setters
    public int getGoalId() { return goalId; }
    public void setGoalId(int goalId) { this.goalId = goalId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }

    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }

    public double getTargetWeight() { return targetWeight; }
    public void setTargetWeight(double targetWeight) { 
        this.targetWeight = targetWeight;
        this.targetValue = String.format("%.2f kg", targetWeight);
    }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getTargetDate() { return targetDate; }
    public void setTargetDate(Date targetDate) { this.targetDate = targetDate; }

    public double getDailyCalories() { return dailyCalories; }
    public void setDailyCalories(double dailyCalories) { this.dailyCalories = dailyCalories; }

    public double getDailyProtein() { return dailyProtein; }
    public void setDailyProtein(double dailyProtein) { this.dailyProtein = dailyProtein; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
