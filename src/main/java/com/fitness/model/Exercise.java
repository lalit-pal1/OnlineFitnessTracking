package com.fitness.model;

import java.sql.Timestamp;

/**
 * JavaBean POJO representing an Exercise from the master catalog.
 */
public class Exercise {
    private int exerciseId;
    private String exerciseName;
    private String muscleGroup;
    private String description;
    private Timestamp createdAt;

    public Exercise() {}

    public Exercise(int exerciseId, String exerciseName, String muscleGroup, String description) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.muscleGroup = muscleGroup;
        this.description = description;
    }

    public Exercise(int exerciseId, String exerciseName, String muscleGroup, String description, Timestamp createdAt) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.muscleGroup = muscleGroup;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
