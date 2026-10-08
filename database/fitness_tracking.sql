-- ============================================================================
-- ONLINE FITNESS TRACKING APPLICATION - AUTHORITATIVE DATABASE SETUP SCRIPT
-- Database Engine: MySQL 5.7+ / 8.0+ / 9.0+
-- Database Name: fitness_tracking
-- Description: Complete production schema matching all Java DAOs (Parts 1-8).
-- ============================================================================

-- 1. CREATE DATABASE
CREATE DATABASE IF NOT EXISTS fitness_tracking;
USE fitness_tracking;

-- 2. DROP TABLES IN SAFE FOREIGN-KEY ORDER
DROP TABLE IF EXISTS workout_sets;
DROP TABLE IF EXISTS goals;
DROP TABLE IF EXISTS progress;
DROP TABLE IF EXISTS nutrition;
DROP TABLE IF EXISTS workouts;
DROP TABLE IF EXISTS exercises;
DROP TABLE IF EXISTS admin;
DROP TABLE IF EXISTS users;

-- ----------------------------------------------------------------------------
-- TABLE: users
-- Stores registered user profile information
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    age INT,
    gender VARCHAR(20),
    height DECIMAL(5, 2), -- Height in cm (e.g., 175.50)
    weight DECIMAL(5, 2), -- Weight in kg (e.g., 75.50)
    fitness_goal VARCHAR(100), -- e.g., 'Muscle Gain (78.0 kg)'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- TABLE: admin
-- Stores system administrator accounts
-- ----------------------------------------------------------------------------
CREATE TABLE admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- TABLE: exercises
-- Master catalog of available physical exercises
-- ----------------------------------------------------------------------------
CREATE TABLE exercises (
    exercise_id INT AUTO_INCREMENT PRIMARY KEY,
    exercise_name VARCHAR(100) NOT NULL,
    muscle_group VARCHAR(50) NOT NULL, -- e.g., 'Chest', 'Legs', 'Arms', 'Back', 'Core'
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- TABLE: workouts
-- Logs workout session headers per user (supports master or custom exercise)
-- ----------------------------------------------------------------------------
CREATE TABLE workouts (
    workout_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    exercise_id INT NULL, -- NULL if custom exercise name is provided
    custom_exercise_name VARCHAR(100) NULL, -- Custom exercise name entered by user
    workout_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workouts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_workouts_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- TABLE: workout_sets
-- Child table storing set-by-set reps and weight lifted per workout session
-- ----------------------------------------------------------------------------
CREATE TABLE workout_sets (
    set_id INT AUTO_INCREMENT PRIMARY KEY,
    workout_id INT NOT NULL,
    set_number INT NOT NULL,
    reps INT NOT NULL,
    weight DECIMAL(5, 2) DEFAULT 0.00, -- Weight lifted in kg
    CONSTRAINT fk_workout_sets_workout FOREIGN KEY (workout_id) REFERENCES workouts(workout_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- TABLE: nutrition
-- Tracks food, macros (protein, carbs, fats), meal timing, and AI scanner images
-- ----------------------------------------------------------------------------
CREATE TABLE nutrition (
    nutrition_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    calories DECIMAL(8, 2) NOT NULL,
    protein DECIMAL(8, 2) NOT NULL DEFAULT 0.00, -- in grams
    carbs DECIMAL(8, 2) NOT NULL DEFAULT 0.00, -- in grams (carbs, NOT carbohydrates)
    fats DECIMAL(8, 2) NOT NULL DEFAULT 0.00, -- in grams
    quantity VARCHAR(50) DEFAULT NULL, -- e.g., '1 bowl', '200g', '2 slices'
    nutrition_date DATE NOT NULL,
    meal_type VARCHAR(50) DEFAULT 'Other', -- e.g., 'Breakfast', 'Lunch', 'Dinner', 'Post Workout'
    meal_time TIME DEFAULT NULL, -- e.g., '08:30:00'
    image_name VARCHAR(255) DEFAULT NULL, -- reference to uploaded meal photo
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nutrition_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- TABLE: progress
-- Logs periodic body weight, calculated BMI, and progress notes
-- ----------------------------------------------------------------------------
CREATE TABLE progress (
    progress_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    weight DECIMAL(5, 2) NOT NULL, -- Weight in kg
    bmi DECIMAL(4, 2), -- Body Mass Index (e.g., 23.83)
    progress_date DATE NOT NULL,
    notes VARCHAR(255) DEFAULT NULL, -- Observation notes
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- TABLE: goals
-- Tracks user fitness goals, target weights, dates, daily calories/protein, and status
-- ----------------------------------------------------------------------------
CREATE TABLE goals (
    goal_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    goal_type VARCHAR(50) NOT NULL, -- e.g., 'Muscle Gain', 'Weight Loss', 'Maintenance'
    target_value VARCHAR(100) DEFAULT NULL, -- Formatted string e.g. '78.00 kg'
    target_weight DECIMAL(5, 2) DEFAULT NULL, -- Target weight in kg
    start_date DATE NOT NULL,
    target_date DATE NOT NULL,
    daily_calories DECIMAL(8, 2) DEFAULT 0.00, -- Daily calorie target (kcal)
    daily_protein DECIMAL(8, 2) DEFAULT 0.00, -- Daily protein target (g)
    notes TEXT DEFAULT NULL, -- Goal notes / description
    status VARCHAR(20) DEFAULT 'IN_PROGRESS', -- 'IN_PROGRESS', 'COMPLETED', 'SUPERSEDED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- SAMPLE / SEED DATA FOR TESTING & DEMO
-- ============================================================================

-- 1. Sample Admin Account
INSERT INTO admin (name, email, password) VALUES
('System Admin', 'admin@fitnesstracker.com', 'pbkdf2_sha256$600000$3UQ8zvxCk4FxvZB1u402YA==$4OMmIhKRolZhFkcuBG5jqst2pSuc+FmJLZvEDhp0T1k=');

-- 2. Sample User Account
INSERT INTO users (name, email, password, age, gender, height, weight, fitness_goal) VALUES
('John Doe', 'john@example.com', 'pbkdf2_sha256$600000$s/wWsbUjRjes8HHNapcZkQ==$XNGGzlN3vIT7+f1qKVqLSvjcJbNgpfrIVBGA5EGEAWk=', 25, 'Male', 178.00, 75.50, 'Muscle Gain (78.0 kg)');

-- 3. Master Exercises List
INSERT INTO exercises (exercise_name, muscle_group, description) VALUES
('Push-Up', 'Chest', 'Bodyweight exercise targeting chest, shoulders, and triceps.'),
('Barbell Squat', 'Legs', 'Compound leg exercise focusing on quadriceps, glutes, and hamstrings.'),
('Dumbbell Bicep Curl', 'Arms', 'Isolation exercise targeting the biceps brachii muscle.'),
('Lat Pulldown', 'Back', 'Upper body exercise strengthening latissimus dorsi muscles.'),
('Plank', 'Core', 'Isometric core strength exercise.');

-- 4. Sample Workout Sessions
INSERT INTO workouts (workout_id, user_id, exercise_id, custom_exercise_name, workout_date) VALUES
(1, 1, 1, NULL, CURDATE()),
(2, 1, 2, NULL, CURDATE());

-- 5. Sample Set Details
INSERT INTO workout_sets (workout_id, set_number, reps, weight) VALUES
(1, 1, 15, 0.00),
(1, 2, 15, 0.00),
(1, 3, 12, 0.00),
(1, 4, 10, 0.00),
(2, 1, 10, 60.00),
(2, 2, 8, 65.00),
(2, 3, 6, 70.00);

-- 6. Sample Nutrition Logs (using 'carbs' column, meal_type, meal_time)
INSERT INTO nutrition (user_id, food_name, calories, protein, carbs, fats, quantity, nutrition_date, meal_type, meal_time, image_name) VALUES
(1, 'Oatmeal with Milk & Honey', 350.00, 12.50, 55.00, 6.00, '1 large bowl', CURDATE(), 'Breakfast', '08:30:00', NULL),
(1, 'Grilled Chicken Breast & Rice', 520.00, 48.00, 45.00, 9.50, '200g chicken + 150g rice', CURDATE(), 'Lunch', '13:00:00', NULL);

-- 7. Sample Progress Records
INSERT INTO progress (user_id, weight, bmi, progress_date, notes) VALUES
(1, 75.50, 23.83, CURDATE(), 'Initial baseline measurement logged.');

-- 8. Sample Goal Record
INSERT INTO goals (user_id, goal_type, target_value, target_weight, start_date, target_date, daily_calories, daily_protein, notes, status) VALUES
(1, 'Muscle Gain', '78.00 kg', 78.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 2800.00, 160.00, 'Aiming for lean bulk with consistent strength training.', 'IN_PROGRESS');

-- Verification Output
SELECT 'Database fitness_tracking initialized successfully!' AS Status;
