package com.fitness.model;

/**
 * JavaBean POJO representing an individual set within a Workout session.
 */
public class WorkoutSet {
    private int setId;
    private int workoutId;
    private int setNumber;
    private int reps;
    private double weight; // weight lifted in kg

    public WorkoutSet() {}

    public WorkoutSet(int setNumber, int reps, double weight) {
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
    }

    public WorkoutSet(int setId, int workoutId, int setNumber, int reps, double weight) {
        this.setId = setId;
        this.workoutId = workoutId;
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
    }

    // Getters and Setters
    public int getSetId() { return setId; }
    public void setSetId(int setId) { this.setId = setId; }

    public int getWorkoutId() { return workoutId; }
    public void setWorkoutId(int workoutId) { this.workoutId = workoutId; }

    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
