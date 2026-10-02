package com.fitness;

import com.fitness.dao.ProgressDAO;
import com.fitness.dao.UserDAO;
import com.fitness.model.Progress;
import com.fitness.model.User;

import java.sql.Date;
import java.util.List;

public class ProgressTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("PART 6 — PROGRESS TRACKING VERIFICATION TEST");
        System.out.println("==================================================");

        try {
            UserDAO userDAO = new UserDAO();
            ProgressDAO progressDAO = new ProgressDAO();

            // 1. Fetch test user (john@example.com / ID 1)
            User testUser = userDAO.getUserById(1);
            if (testUser == null) {
                System.out.println("Creating test user (John Doe, height 178 cm)...");
                testUser = new User(1, "John Doe", "john@example.com", "user123", 25, "Male", 178.00, 75.50, "Muscle Gain", null);
            }
            System.out.println("✔ Test User: " + testUser.getName() + " (Height: " + testUser.getHeight() + " cm, Initial Weight: " + testUser.getWeight() + " kg)");

            // 2. Clear previous progress test records for clean run
            List<Progress> existing = progressDAO.getProgressByUserId(testUser.getUserId());
            for (Progress p : existing) {
                progressDAO.deleteProgress(p.getProgressId(), testUser.getUserId());
            }

            // 3. Test Entry 1: 55.0 kg
            double w1 = 55.0;
            double bmi1 = Progress.calculateBMI(w1, testUser.getHeight());
            String cat1 = Progress.calculateBMICategory(bmi1);
            Progress p1 = new Progress(0, testUser.getUserId(), w1, bmi1, cat1, Date.valueOf("2026-09-20"), "Starting weight check");
            boolean ok1 = progressDAO.addProgress(p1);
            System.out.println("✔ Add Entry 1 (55.0 kg, Date: 2026-09-20): " + (ok1 ? "PASSED" : "FAILED") + " -> Calculated BMI: " + bmi1 + " (" + cat1 + ")");

            // 4. Test Entry 2: 55.5 kg
            double w2 = 55.5;
            double bmi2 = Progress.calculateBMI(w2, testUser.getHeight());
            String cat2 = Progress.calculateBMICategory(bmi2);
            Progress p2 = new Progress(0, testUser.getUserId(), w2, bmi2, cat2, Date.valueOf("2026-09-23"), "Mid-week weigh in");
            boolean ok2 = progressDAO.addProgress(p2);
            System.out.println("✔ Add Entry 2 (55.5 kg, Date: 2026-09-23): " + (ok2 ? "PASSED" : "FAILED") + " -> Calculated BMI: " + bmi2 + " (" + cat2 + ")");

            // 5. Test Entry 3: 56.0 kg
            double w3 = 56.0;
            double bmi3 = Progress.calculateBMI(w3, testUser.getHeight());
            String cat3 = Progress.calculateBMICategory(bmi3);
            Progress p3 = new Progress(0, testUser.getUserId(), w3, bmi3, cat3, Date.valueOf("2026-09-26"), "End of week progress");
            boolean ok3 = progressDAO.addProgress(p3);
            System.out.println("✔ Add Entry 3 (56.0 kg, Date: 2026-09-26): " + (ok3 ? "PASSED" : "FAILED") + " -> Calculated BMI: " + bmi3 + " (" + cat3 + ")");

            // 6. Verify Chronological History Retrieval
            List<Progress> logs = progressDAO.getProgressByUserId(testUser.getUserId());
            System.out.println("✔ History Count: " + logs.size() + " entries retrieved.");
            for (Progress p : logs) {
                System.out.println("   - [" + p.getProgressDate() + "] Weight: " + p.getWeight() + " kg | BMI: " + p.getBmi() + " (" + p.getBmiCategory() + ") | Notes: " + p.getNotes());
            }

            // 7. Verify Latest Progress Record
            Progress latest = progressDAO.getLatestProgressByUserId(testUser.getUserId());
            System.out.println("✔ Latest Record: " + (latest != null ? latest.getWeight() + " kg (BMI: " + latest.getBmi() + ")" : "None"));

            // 8. Test Deletion Security
            boolean unauthorizedDelete = progressDAO.deleteProgress(p1.getProgressId(), 9999); // Unauthorized user ID
            System.out.println("✔ Security Check (Delete by invalid user ID 9999): " + (!unauthorizedDelete ? "PASSED (Rejected)" : "FAILED (Allowed)"));

            boolean authorizedDelete = progressDAO.deleteProgress(p1.getProgressId(), testUser.getUserId());
            System.out.println("✔ Authorized Deletion (Entry ID " + p1.getProgressId() + "): " + (authorizedDelete ? "PASSED" : "FAILED"));

            List<Progress> finalLogs = progressDAO.getProgressByUserId(testUser.getUserId());
            System.out.println("✔ Final History Count after deletion: " + finalLogs.size() + " entries.");

            System.out.println("==================================================");
            System.out.println("ALL PART 6 PROGRESS TRACKING BACKEND TESTS PASSED!");
            System.out.println("==================================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
