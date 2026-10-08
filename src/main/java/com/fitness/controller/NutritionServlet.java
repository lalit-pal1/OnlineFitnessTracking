package com.fitness.controller;

import com.fitness.dao.NutritionDAO;
import com.fitness.model.Nutrition;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller Servlet managing Nutrition log entries, daily macro totals, and AI Food Scanner image analysis.
 * Endpoint: /nutrition
 */
@WebServlet("/nutrition")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 10,      // 10MB
    maxRequestSize = 1024 * 1024 * 15    // 15MB
)
public class NutritionServlet extends HttpServlet {

    private NutritionDAO nutritionDAO;

    @Override
    public void init() throws ServletException {
        nutritionDAO = new NutritionDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || !"USER".equals(session.getAttribute("role")) || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in first.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");
        String dateStr = request.getParameter("date");

        Date filterDate = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                filterDate = Date.valueOf(dateStr.trim());
            } catch (IllegalArgumentException e) {
                // Fallback to null if invalid
            }
        }

        try {
            if ("dailyTotals".equalsIgnoreCase(action)) {
                Date targetDate = (filterDate != null) ? filterDate : new Date(System.currentTimeMillis());
                Map<String, Double> totals = nutritionDAO.getDailyNutritionTotals(userId, targetDate);

                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"status\":\"success\",");
                json.append("\"date\":\"").append(targetDate.toString()).append("\",");
                json.append("\"totalCalories\":").append(formatDouble(totals.get("totalCalories"))).append(",");
                json.append("\"totalProtein\":").append(formatDouble(totals.get("totalProtein"))).append(",");
                json.append("\"totalCarbs\":").append(formatDouble(totals.get("totalCarbs"))).append(",");
                json.append("\"totalFats\":").append(formatDouble(totals.get("totalFats")));
                json.append("}");

                out.write(json.toString());
                return;
            }

            // Default: action=list (returns entries & daily totals)
            List<Nutrition> logs;
            Map<String, Double> totals;

            if (filterDate != null) {
                logs = nutritionDAO.getNutritionByUserIdAndDate(userId, filterDate);
                totals = nutritionDAO.getDailyNutritionTotals(userId, filterDate);
            } else {
                logs = nutritionDAO.getNutritionByUserId(userId);
                totals = new java.util.HashMap<>();
                double sumCal = 0, sumPro = 0, sumCarb = 0, sumFat = 0;
                for (Nutrition n : logs) {
                    sumCal += n.getCalories();
                    sumPro += n.getProtein();
                    sumCarb += n.getCarbs();
                    sumFat += n.getFats();
                }
                totals.put("totalCalories", sumCal);
                totals.put("totalProtein", sumPro);
                totals.put("totalCarbs", sumCarb);
                totals.put("totalFats", sumFat);
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"success\",");
            json.append("\"filterDate\":\"").append(filterDate != null ? filterDate.toString() : "").append("\",");
            json.append("\"totalCalories\":").append(formatDouble(totals.get("totalCalories"))).append(",");
            json.append("\"totalProtein\":").append(formatDouble(totals.get("totalProtein"))).append(",");
            json.append("\"totalCarbs\":").append(formatDouble(totals.get("totalCarbs"))).append(",");
            json.append("\"totalFats\":").append(formatDouble(totals.get("totalFats"))).append(",");
            json.append("\"logs\":[");

            for (int i = 0; i < logs.size(); i++) {
                Nutrition n = logs.get(i);
                json.append("{");
                json.append("\"nutritionId\":").append(n.getNutritionId()).append(",");
                json.append("\"foodName\":\"").append(escapeJson(n.getFoodName())).append("\",");
                json.append("\"calories\":").append(formatDouble(n.getCalories())).append(",");
                json.append("\"protein\":").append(formatDouble(n.getProtein())).append(",");
                json.append("\"carbs\":").append(formatDouble(n.getCarbs())).append(",");
                json.append("\"fats\":").append(formatDouble(n.getFats())).append(",");
                json.append("\"nutritionDate\":\"").append(n.getNutritionDate().toString()).append("\",");
                json.append("\"mealType\":\"").append(escapeJson(n.getMealType())).append("\",");
                json.append("\"mealTime\":\"").append(n.getMealTime() != null ? escapeJson(n.getMealTime()) : "").append("\",");
                json.append("\"imageName\":\"").append(n.getImageName() != null ? escapeJson(n.getImageName()) : "").append("\"");
                json.append("}");
                if (i < logs.size() - 1) json.append(",");
            }

            json.append("]}");
            out.write(json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || !"USER".equals(session.getAttribute("role")) || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"status\":\"error\",\"message\":\"Unauthorized access. Please log in first.\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            try {
                Part actionPart = request.getPart("action");
                if (actionPart != null) {
                    action = new String(readAllBytes(actionPart.getInputStream()), "UTF-8").trim();
                }
            } catch (Exception ignored) {}
        }

        try {
            // ACTION: ANALYZE IMAGE WITH AI VISION
            if ("analyzeImage".equalsIgnoreCase(action)) {
                Part filePart = null;
                try {
                    filePart = request.getPart("mealImage");
                } catch (Exception e) {
                    // Try fallback parameter name
                    try { filePart = request.getPart("image"); } catch (Exception ex) {}
                }

                if (filePart == null || filePart.getSize() <= 0) {
                    out.write("{\"status\":\"error\",\"message\":\"No image file uploaded. Please select a valid meal photo.\"}");
                    return;
                }

                String contentType = filePart.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    out.write("{\"status\":\"error\",\"message\":\"Invalid file format. Please upload a JPG, PNG, or WEBP image.\"}");
                    return;
                }

                if (filePart.getSize() > 5 * 1024 * 1024) { // 5MB limit
                    out.write("{\"status\":\"error\",\"message\":\"Image file is too large. Maximum allowed size is 5 MB.\"}");
                    return;
                }

                // Check GEMINI_API_KEY environment variable (never hardcoded)
                String apiKey = System.getenv("GEMINI_API_KEY");
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    apiKey = System.getProperty("GEMINI_API_KEY");
                }

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"AI Food Scanner requires GEMINI_API_KEY environment variable. Please set GEMINI_API_KEY in Windows environment and restart application.\"}");
                    return;
                }

                byte[] imageBytes = readAllBytes(filePart.getInputStream());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                GeminiResult aiResult = callGeminiVisionApi(apiKey.trim(), base64Image, contentType);
                if (aiResult.success && aiResult.jsonPayload != null && !aiResult.jsonPayload.trim().isEmpty()) {
                    out.write("{\"status\":\"success\",\"result\":" + aiResult.jsonPayload + ",\"modelUsed\":\"" + escapeJson(aiResult.modelUsed) + "\"}");
                } else {
                    out.write("{\"status\":\"error\",\"message\":\"" + escapeJson(aiResult.errorMessage != null ? aiResult.errorMessage : "Unable to analyze meal image.") + "\"}");
                }
                return;
            }

            // ACTION: ADD NUTRITION ENTRY
            if ("add".equalsIgnoreCase(action)) {
                String foodName = request.getParameter("foodName");
                String caloriesStr = request.getParameter("calories");
                String proteinStr = request.getParameter("protein");
                String carbsStr = request.getParameter("carbs");
                String fatsStr = request.getParameter("fats");
                String dateStr = request.getParameter("nutritionDate");
                String mealType = request.getParameter("mealType");
                String customMealName = request.getParameter("customMealName");
                String mealTime = request.getParameter("mealTime");
                String imageName = request.getParameter("imageName");

                if ("Other".equalsIgnoreCase(mealType) && customMealName != null && !customMealName.trim().isEmpty()) {
                    mealType = customMealName.trim();
                }

                if (foodName == null || foodName.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Food name is required.\"}");
                    return;
                }

                if (dateStr == null || dateStr.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Nutrition date is required.\"}");
                    return;
                }

                double calories, protein, carbs, fats;
                Date nutritionDate;

                try {
                    calories = Double.parseDouble(caloriesStr);
                    protein = Double.parseDouble(proteinStr);
                    carbs = Double.parseDouble(carbsStr);
                    fats = Double.parseDouble(fatsStr);
                } catch (Exception e) {
                    out.write("{\"status\":\"error\",\"message\":\"Calories, protein, carbs, and fats must be valid numbers.\"}");
                    return;
                }

                if (calories < 0 || protein < 0 || carbs < 0 || fats < 0) {
                    out.write("{\"status\":\"error\",\"message\":\"Nutrition values cannot be negative.\"}");
                    return;
                }

                try {
                    nutritionDate = Date.valueOf(dateStr.trim());
                } catch (IllegalArgumentException e) {
                    out.write("{\"status\":\"error\",\"message\":\"Invalid date format. Use YYYY-MM-DD.\"}");
                    return;
                }

                Nutrition nutrition = new Nutrition(0, userId, foodName.trim(), calories, protein, carbs, fats, nutritionDate, mealType, mealTime, imageName);
                boolean success = nutritionDAO.addNutrition(nutrition);

                if (success) {
                    out.write("{\"status\":\"success\",\"message\":\"Food entry logged successfully!\",\"nutritionId\":" + nutrition.getNutritionId() + "}");
                } else {
                    out.write("{\"status\":\"error\",\"message\":\"Failed to save nutrition entry.\"}");
                }
                return;
            }

            // ACTION: DELETE ENTRY
            if ("delete".equalsIgnoreCase(action)) {
                String idStr = request.getParameter("nutritionId");
                if (idStr == null || idStr.trim().isEmpty()) {
                    out.write("{\"status\":\"error\",\"message\":\"Nutrition ID is required for deletion.\"}");
                    return;
                }

                int nutritionId = Integer.parseInt(idStr);
                boolean deleted = nutritionDAO.deleteNutrition(nutritionId, userId);

                if (deleted) {
                    out.write("{\"status\":\"success\",\"message\":\"Food entry deleted successfully.\"}");
                } else {
                    out.write("{\"status\":\"error\",\"message\":\"Unable to delete entry. Record not found or unauthorized.\"}");
                }
                return;
            }

            out.write("{\"status\":\"error\",\"message\":\"Invalid action parameter.\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"status\":\"error\",\"message\":\"Database error. Please try again later." + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"status\":\"error\",\"message\":\"Server error. Please try again later." + "\"}");
        }
    }

    /**
     * Inner helper class holding Gemini vision response result.
     */
    private static class GeminiResult {
        final boolean success;
        final String jsonPayload;
        final String errorMessage;
        final String modelUsed;

        GeminiResult(boolean success, String jsonPayload, String errorMessage, String modelUsed) {
            this.success = success;
            this.jsonPayload = jsonPayload;
            this.errorMessage = errorMessage;
            this.modelUsed = modelUsed;
        }
    }

    /**
     * Helper to invoke Gemini Vision API using current supported Flash models with fallback.
     */
    private GeminiResult callGeminiVisionApi(String apiKey, String base64Image, String mimeType) {
        String[] candidateModels = {"gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.6-flash", "gemini-flash-latest"};
        
        String promptText = "Analyze this meal photo. Identify likely food items and estimate total nutrition. " +
                            "Respond strictly with a single JSON object containing keys: " +
                            "mealTitle (String title of meal), " +
                            "foodName (String title of meal), " +
                            "items (array of objects with 'name' and 'portion' strings), " +
                            "calories (number kcal), " +
                            "protein (number grams), " +
                            "carbs (number grams), " +
                            "fats (number grams). Do not wrap inside markdown codeblock or any extra text.";

        String jsonRequestBody = "{"
            + "\"contents\": [{"
            + "  \"parts\": ["
            + "    {\"text\": \"" + escapeJson(promptText) + "\"},"
            + "    {\"inline_data\": {\"mime_type\": \"" + mimeType + "\", \"data\": \"" + base64Image + "\"}}"
            + "  ]"
            + "}]"
            + "}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        String lastErrorMsg = "AI analysis is temporarily unavailable. Please try again.";

        for (String modelName : candidateModels) {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();

                if (status == 200) {
                    String body = resp.body();
                    String textContent = extractGeminiTextContent(body);
                    if (textContent != null && !textContent.trim().isEmpty()) {
                        String cleanJson = textContent.trim();
                        if (cleanJson.startsWith("```json")) {
                            cleanJson = cleanJson.substring(7);
                        } else if (cleanJson.startsWith("```")) {
                            cleanJson = cleanJson.substring(3);
                        }
                        if (cleanJson.endsWith("```")) {
                            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                        }
                        cleanJson = cleanJson.trim();

                        return new GeminiResult(true, cleanJson, null, modelName);
                    } else {
                        lastErrorMsg = "Gemini API returned empty text content. Please review values manually.";
                    }
                } else if (status == 404) {
                    lastErrorMsg = "AI model configuration needs to be updated.";
                    continue;
                } else if (status == 401 || status == 403) {
                    return new GeminiResult(false, null, "Gemini API authentication failed. Please check your GEMINI_API_KEY environment variable.", modelName);
                } else if (status == 429) {
                    return new GeminiResult(false, null, "AI service limit reached. Please try again later.", modelName);
                } else if (status == 400) {
                    return new GeminiResult(false, null, "Gemini API request error. Please check meal image format and size.", modelName);
                } else {
                    return new GeminiResult(false, null, "AI analysis is temporarily unavailable. Please try again.", modelName);
                }

            } catch (Exception e) {
                e.printStackTrace();
                lastErrorMsg = "AI analysis is temporarily unavailable. Please try again.";
            }
        }

        return new GeminiResult(false, null, lastErrorMsg, "none");
    }

    private String extractGeminiTextContent(String jsonBody) {
        if (jsonBody == null) return null;
        int textIdx = jsonBody.indexOf("\"text\"");
        if (textIdx == -1) return null;

        int colonIdx = jsonBody.indexOf(":", textIdx);
        if (colonIdx == -1) return null;

        int startQuote = jsonBody.indexOf("\"", colonIdx);
        if (startQuote == -1) return null;

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = startQuote + 1; i < jsonBody.length(); i++) {
            char c = jsonBody.charAt(i);
            if (escaped) {
                if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else if (c == '"') sb.append('"');
                else if (c == '\\') sb.append('\\');
                else sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    private String formatDouble(Double val) {
        if (val == null) return "0.0";
        return String.format(java.util.Locale.US, "%.1f", val);
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
