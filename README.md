# Online Fitness Tracking Application (Part 4: Workout Tracking Module)

> **College Project**: Part 4 - Workout Tracking Module  
> **Tech Stack**: HTML5, CSS3, JavaScript, Core Java, Java Servlets (Jakarta EE 10), JDBC, MySQL  
> **Server**: Apache Tomcat 10.1+  
> **Localhost Base URL**: `http://localhost:8080/OnlineFitnessTracking/`  

---

## 1. Project Overview

The **Online Fitness Tracking Application** is a web-based fitness management platform designed to track workout sessions, daily nutrition, and physical progress.

**Part 4** introduces the **Workout Tracking Module**:
- **Live Exercise Catalog**: Exercise dropdown populated directly from the master `exercises` database table.
- **Log Workout Form**: Log workout entries (`exercise`, `sets`, `reps`, `weight`, `date`) with strict client-side & server-side validation.
- **Workout History**: Displays logged workouts (newest first) with Exercise Name, Muscle Group, Sets, Reps, Weight (kg), Date, and Delete button.
- **Strict Data Ownership**: Ownership-checked deletion query `DELETE FROM workouts WHERE workout_id = ? AND user_id = ?` ensures User A cannot view or delete User B's workout data.
- **Dashboard Integration**: The **Track Workout** button on `user-dashboard.html` links directly to `workout.html`.

---

## 2. Directory Structure

```text
OnlineFitnessTracking/
├── pom.xml                                  # Maven project configuration (Servlet 6.0 & MySQL Connector)
├── database/
│   └── fitness_tracking.sql                 # MySQL schema (users, admin, exercises, workouts, nutrition, etc.)
├── src/
│   └── main/
│       ├── java/com/fitness/
│       │   ├── util/
│       │   │   └── DatabaseConnection.java  # Centralized JDBC Connection Manager
│       │   ├── model/
│       │   │   ├── User.java            # User entity JavaBean
│       │   │   ├── Admin.java           # Admin entity JavaBean
│       │   │   ├── Exercise.java        # Exercise entity JavaBean
│       │   │   └── Workout.java         # Workout entity JavaBean
│       │   ├── dao/
│       │   │   ├── UserDAO.java         # User CRUD & authentication
│       │   │   ├── AdminDAO.java        # Admin authentication
│       │   │   ├── ExerciseDAO.java     # Master exercises list query
│       │   │   └── WorkoutDAO.java      # Workout add, list by user, delete operations
│       │   ├── controller/
│       │   │   ├── RegisterServlet.java # @WebServlet("/register")
│       │   │   ├── LoginServlet.java    # @WebServlet("/login")
│       │   │   ├── AdminLoginServlet.java # @WebServlet("/admin-login")
│       │   │   ├── UserDashboardServlet.java # @WebServlet("/user-dashboard")
│       │   │   ├── WorkoutServlet.java  # @WebServlet("/workout") - Exercises, add, list, delete
│       │   │   └── LogoutServlet.java   # @WebServlet("/logout")
│       │   └── filter/
│       │       └── AuthenticationFilter.java # Protects dashboard, workout, and admin routes
│       └── webapp/
│           ├── WEB-INF/
│           │   └── web.xml              # Deployment Descriptor (Servlet 6.0)
│           ├── index.html               # Main Landing Page
│           ├── register.html            # User Registration Form
│           ├── login.html               # User Login Form
│           ├── admin-login.html         # Admin Portal Login Form
│           ├── user-dashboard.html      # Protected User Dashboard UI
│           ├── workout.html             # Protected Workout Tracking UI
│           ├── admin-home.html          # Protected Admin Welcome Page
│           ├── css/
│           │   └── style.css            # Responsive Dark Fitness Theme Stylesheet
│           └── js/
│               └── script.js            # Client-side Dashboard Loader, Workout AJAX & Modal Handlers
└── README.md
```

---

## 3. Database Schema Used (Part 4)

### `exercises` Table (Master Catalog)
* `exercise_id` (INT, PK, Auto Increment)
* `exercise_name` (VARCHAR)
* `muscle_group` (VARCHAR)
* `description` (TEXT)

### `workouts` Table (Logged Sessions)
* `workout_id` (INT, PK, Auto Increment)
* `user_id` (INT, FK -> `users.user_id`)
* `exercise_id` (INT, FK -> `exercises.exercise_id`)
* `sets` (INT)
* `reps` (INT)
* `weight` (DECIMAL)
* `workout_date` (DATE)

---

## 4. Manual Testing Steps for Part 4

### **TEST 1: Unauthenticated Workout Page Access**
1. Open a new browser window without logging in.
2. Navigate to `http://localhost:8080/OnlineFitnessTracking/workout.html`.
3. **Expected Result**: `AuthenticationFilter` blocks access and redirects to `login.html?error=unauthorized`.

### **TEST 2: Dynamic Exercise Dropdown Population**
1. Log in as a user (`john@example.com` / `user123`).
2. Click **Track Workout** on the dashboard (or open `workout.html`).
3. **Expected Result**: The Exercise dropdown is populated with live exercises from the database (*Push-Up*, *Barbell Squat*, *Dumbbell Bicep Curl*).

### **TEST 3: Logging a Workout Session**
1. Select an exercise (e.g. *Push-Up*), enter Sets (`4`), Reps (`15`), Weight (`0.0`), and select a Workout Date.
2. Click **Log Workout Entry**.
3. **Expected Result**: Success alert displays: *"✅ Workout logged successfully!"*, the form resets, and the Workout History table updates instantly showing the new entry.

### **TEST 4: Deleting a Workout Entry**
1. Click **Delete** next to any logged workout entry in the history table.
2. Confirm the prompt popup.
3. **Expected Result**: Success alert displays: *"✅ Workout entry deleted successfully!"* and the row is removed from the table.

### **TEST 5: Data Ownership Isolation**
1. Log in as User A (`john@example.com`) and log a workout.
2. Log out and register/log in as User B (`userB@example.com`).
3. Open `workout.html`.
4. **Expected Result**: User B sees "No Workouts Logged Yet" and cannot see or delete User A's workout data.

---

## 5. Deployment Instructions

1. Copy [`target/OnlineFitnessTracking.war`](file:///C:/Users/Lalit/.gemini/antigravity/scratch/OnlineFitnessTracking/target/OnlineFitnessTracking.war) into Tomcat's `webapps/` folder.
2. Open `http://localhost:8080/OnlineFitnessTracking/` in your browser.
