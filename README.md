# Online Fitness Tracking Application

A Java-based web application for managing personal fitness activities through workout tracking, nutrition records, progress monitoring, fitness goals, and an administrative management portal.

## 1. Project Overview

The **Online Fitness Tracking Application** provides a centralized web interface where users can:

- Create an account and log in securely through the application flow.
- Access a protected user dashboard.
- Record and review workout sessions.
- Browse the exercise catalogue.
- Maintain nutrition records.
- Track fitness progress.
- Create and manage fitness goals.
- Log out and have protected routes controlled through authentication.

The application also provides an **Admin module** for managing application data and monitoring user-related fitness information.

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Frontend | HTML5, CSS3, JavaScript |
| Backend | Core Java, Jakarta Servlets |
| Database access | JDBC |
| Database | MySQL |
| Build tool | Apache Maven |
| Web server | Apache Tomcat 10.1+ |
| Java version | Java 21 |

## 3. Application Architecture

The project follows a layered web-application structure:

```text
Browser
   ↓
HTML / CSS / JavaScript
   ↓ HTTP Request
Servlet / Controller
   ↓
DAO Layer
   ↓ JDBC
DatabaseConnection
   ↓
MySQL Database
   ↓
Response
   ↓
Browser
```

### Main packages

```text
src/main/java/com/fitness/
├── controller/   # HTTP request handling through Servlets
├── dao/          # Database operations and CRUD logic
├── filter/       # Authentication and route protection
├── model/        # Java model/entity classes
└── util/         # Shared database connection utility
```

This separation keeps request handling, business/data-access logic, model objects, and database connectivity organized into separate responsibilities.

## 4. Main Functional Modules

### User module

- Registration
- Login/logout
- Session-based access
- User dashboard
- Profile-related functionality

### Workout module

- Exercise catalogue
- Workout entry creation
- Workout history
- Workout deletion
- User-specific workout data
- Workout-set handling

### Nutrition module

- Nutrition record management
- Nutrition information displayed through the user application

### Progress module

- Progress records
- Progress history/management

### Goals module

- Fitness goal creation and management

### Admin module

- Admin authentication
- Admin dashboard
- User management
- Exercise management
- Workout management
- Nutrition management
- Progress management

## 5. Database Design

The application uses an 8-table MySQL schema:

```text
users
admin
exercises
workouts
workout_sets
nutrition
progress
goals
```

Important relationships include:

```text
users
 ├── workouts
 │     └── workout_sets
 ├── nutrition
 ├── progress
 └── goals

exercises
 └── workouts
```

The authoritative database initialization script is:

```text
database/fitness_tracking.sql
```

The `sql/` directory contains supporting SQL/documentation files.

## 6. Java Components

### Controllers / Servlets

The application contains Servlets for major user and administrator operations, including:

- `RegisterServlet`
- `LoginServlet`
- `LogoutServlet`
- `UserDashboardServlet`
- `WorkoutServlet`
- `NutritionServlet`
- `ProgressServlet`
- `GoalServlet`
- `AdminLoginServlet`
- `AdminDashboardServlet`
- `AdminUserServlet`
- `AdminExerciseServlet`
- `AdminWorkoutServlet`
- `AdminNutritionServlet`
- `AdminProgressServlet`

### DAO layer

Database operations are separated into DAO classes such as:

- `UserDAO`
- `AdminDAO`
- `AdminUserDAO`
- `ExerciseDAO`
- `WorkoutDAO`
- `NutritionDAO`
- `ProgressDAO`
- `GoalDAO`

### Models

The application uses Java model/entity classes including:

- `User`
- `Admin`
- `Exercise`
- `Workout`
- `WorkoutSet`
- `Nutrition`
- `Progress`
- `Goal`

### Authentication

`AuthenticationFilter` is used to protect application routes and control access based on the authenticated session/role.

## 7. JDBC Implementation

Database access is centralized through:

```text
com.fitness.util.DatabaseConnection
```

DAO classes use JDBC operations such as:

- `Connection`
- `PreparedStatement`
- `ResultSet`
- SQL CRUD operations
- Exception handling for database operations

The workout workflow also uses transaction handling where multiple related database operations need to succeed together.

## 8. Project Structure

```text
OnlineFitnessTracking/
├── database/
│   └── fitness_tracking.sql
├── sql/
├── src/
│   ├── main/
│   │   ├── java/com/fitness/
│   │   │   ├── controller/
│   │   │   ├── dao/
│   │   │   ├── filter/
│   │   │   ├── model/
│   │   │   └── util/
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       ├── css/
│   │       ├── js/
│   │       └── *.html
│   └── test/
├── pom.xml
├── README.md
├── .gitignore
└── sample_meal.jpg
```

## 9. Requirements

Install the following before running the project:

- JDK 21
- Apache Maven 3.9+
- MySQL Server
- Apache Tomcat 10.1+
- A modern web browser

## 10. Database Setup

1. Start MySQL Server.
2. Open MySQL Workbench or the MySQL command-line client.
3. Execute:

```text
database/fitness_tracking.sql
```

4. Verify that the application database and required tables were created.
5. Configure the database connection using environment variables before starting Tomcat.

### Database environment variables

The application does **not** store the MySQL username or password in Java source code.
Set these variables in the terminal/environment used to start Tomcat:

```text
FITNESS_DB_HOST=localhost
FITNESS_DB_PORT=3306
FITNESS_DB_NAME=fitness_tracking
FITNESS_DB_USER=<your-mysql-username>
FITNESS_DB_PASSWORD=<your-mysql-password>
```

On Windows PowerShell, for example:

```powershell
$env:FITNESS_DB_HOST = "localhost"
$env:FITNESS_DB_PORT = "3306"
$env:FITNESS_DB_NAME = "fitness_tracking"
$env:FITNESS_DB_USER = "root"
$env:FITNESS_DB_PASSWORD = "your-password"
```

Open a new terminal after setting persistent Windows environment variables so Tomcat inherits the updated values.

> Never commit real database credentials to a public repository.

### Password security

User and administrator passwords are stored as salted **PBKDF2-HMAC-SHA-256** hashes. The application never compares or stores plain-text passwords in the database. Each password receives a unique random salt, and the stored value contains the hashing format, iteration count, salt, and derived key.

The demo accounts inserted by `database/fitness_tracking.sql` use these original passwords for local testing:

| Account | Email | Password |
|---|---|---|
| Admin | `admin@fitnesstracker.com` | `admin123` |
| User | `john@example.com` | `user123` |

These are **demo-only credentials** from the seed data and should not be reused for real accounts.

## 11. Build

From the project root:

```bash
mvn clean package
```

The Maven build produces a WAR file under:

```text
target/
```

## 12. Run with Apache Tomcat

1. Build the project using Maven.
2. Copy the generated WAR file to the Tomcat `webapps` directory.
3. Start Tomcat.
4. Open the application using the deployed context path, for example:

```text
http://localhost:8080/OnlineFitnessTracking/
```

The exact URL can vary depending on the WAR filename/context path used during deployment.

## 13. Typical User Flow

```text
Home
  ↓
Register / Login
  ↓
User Dashboard
  ├── Workout
  ├── Nutrition
  ├── Progress
  └── Goals
  ↓
Logout
```

## 14. Typical Admin Flow

```text
Admin Login
     ↓
Admin Dashboard
     ├── Users
     ├── Exercises
     ├── Workouts
     ├── Nutrition
     └── Progress
```

## 15. Testing

The project includes test/support code under `src/test` and manual testing can be performed for:

- Registration and login
- Protected route access
- Workout creation and history
- Nutrition, progress, and goal workflows
- Admin authentication and management pages
- Database CRUD operations
- User-specific data access
- Logout/session behaviour

## 16. Java Concepts Demonstrated

The implementation demonstrates relevant Core Java and web-development concepts, including:

- Classes and objects
- Encapsulation through model classes
- Inheritance through Servlet classes
- Method overriding such as `doGet()` and `doPost()`
- Interface implementation through servlet filters
- Collections and generics
- Exception handling
- Modular/layered application design
- JDBC and SQL integration
- HTTP request/response handling
- Session-based authentication

## 17. Team Contribution Structure

The project is developed as a four-member college team. Responsibilities can be organized around the following modules:

| Role | Main responsibility |
|---|---|
| Member 1 | Backend, authentication and Servlet flow |
| Member 2 | Database design, JDBC and DAO layer |
| Member 3 | User fitness modules: workout, nutrition, progress and goals |
| Member 4 | Frontend/UI, admin module, testing and documentation |

Each team member should also understand the overall application architecture and database flow for project review and viva.

## 18. Repository

The project source code is maintained in a public Git repository. The repository should contain the current source code, database scripts, Maven configuration, README and other required project assets.

---

**Project:** Online Fitness Tracking Application  
**Type:** Java Web Application  
**Backend:** Java Servlets + JDBC  
**Database:** MySQL  
**Frontend:** HTML + CSS + JavaScript
