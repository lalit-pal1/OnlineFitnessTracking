package com.fitness.model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaBean POJO representing a Workout session with custom exercise support and set details list.
 */
public class Workout {
    private int workoutId;
    private int userId;
    private Integer exerciseId; // Can be null if custom exercise name is used
    private String customExerciseName; // Used if exerciseId is null
    private String exerciseName; // Resolved display name (master catalog or custom)
    private String muscleGroup; // Display muscle group or "Custom"
    private Date workoutDate;
    private List<WorkoutSet> setsList = new ArrayList<>();

    public Workout() {}

    public Workout(int userId, Integer exerciseId, String customExerciseName, Date workoutDate) {
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.customExerciseName = customExerciseName;
        this.workoutDate = workoutDate;
    }

    public Workout(int workoutId, int userId, Integer exerciseId, String customExerciseName, String exerciseName, String muscleGroup, Date workoutDate) {
        this.workoutId = workoutId;
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.customExerciseName = customExerciseName;
        this.exerciseName = exerciseName;
        this.muscleGroup = muscleGroup;
        this.workoutDate = workoutDate;
    }

    // Helper methods for calculations
    public int getTotalSets() {
        return setsList != null ? setsList.size() : 0;
    }

    public String getRepsSummary() {
        if (setsList == null || setsList.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < setsList.size(); i++) {
            sb.append(setsList.get(i).getReps());
            if (i < setsList.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public String getWeightsSummary() {
        if (setsList == null || setsList.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < setsList.size(); i++) {
            double w = setsList.get(i).getWeight();
            if (w == (long) w) {
                sb.append((long) w);
            } else {
                sb.append(w);
            }
            if (i < setsList.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public double getTotalVolume() {
        if (setsList == null || setsList.isEmpty()) return 0.0;
        double total = 0.0;
        for (WorkoutSet set : setsList) {
            total += (set.getReps() * set.getWeight());
        }
        return total;
    }

    // Getters and Setters
    public int getWorkoutId() { return workoutId; }
    public void setWorkoutId(int workoutId) { this.workoutId = workoutId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getExerciseId() { return exerciseId; }
    public void setExerciseId(Integer exerciseId) { this.exerciseId = exerciseId; }

    public String getCustomExerciseName() { return customExerciseName; }
    public void setCustomExerciseName(String customExerciseName) { this.customExerciseName = customExerciseName; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public Date getWorkoutDate() { return workoutDate; }
    public void setWorkoutDate(Date workoutDate) { this.workoutDate = workoutDate; }

    public List<WorkoutSet> getSetsList() { return setsList; }
    public void setSetsList(List<WorkoutSet> setsList) { this.setsList = setsList; }
    public void addSet(WorkoutSet set) { this.setsList.add(set); }
}
