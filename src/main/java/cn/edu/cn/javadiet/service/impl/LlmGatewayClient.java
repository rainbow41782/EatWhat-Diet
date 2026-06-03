package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.config.LlmGatewayProperties;
import cn.edu.cn.javadiet.model.dto.FoodNutritionMenuContext;
import cn.edu.cn.javadiet.model.dto.FoodNutritionReferenceCandidate;
import cn.edu.cn.javadiet.model.dto.FoodNutritionSuggestion;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LlmGatewayClient {

    private static final double DEFAULT_CONFIDENCE = 0.35d;

    private final LlmGatewayProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public LlmGatewayClient(LlmGatewayProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && notBlank(properties.getBaseUrl())
                && notBlank(properties.getApiKey())
                && notBlank(properties.getModel());
    }

    public FoodNutritionSuggestion suggest(
            FoodItem food,
            List<FoodNutritionReferenceCandidate> references,
            List<FoodNutritionMenuContext> menuContexts) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM Gateway 未启用或配置不完整");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(buildChatRequest(food, references, menuContexts));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(completionUrl()))
                    .timeout(Duration.ofSeconds(normalizedTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("LLM Gateway 返回异常状态：" + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            FoodNutritionSuggestion suggestion = parseSuggestionContent(food.getId(), food.getName(), content);
            suggestion.setReferences(references);
            return suggestion;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("LLM Gateway 请求失败：" + exception.getMessage(), exception);
        }
    }

    public List<String> suggestSearchQueries(
            FoodItem food,
            List<FoodNutritionMenuContext> menuContexts,
            List<String> dictionaryHints) {
        if (!isConfigured()) {
            return List.of();
        }
        try {
            String requestBody = objectMapper.writeValueAsString(
                    buildSearchQueryRequest(food, menuContexts, dictionaryHints));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(completionUrl()))
                    .timeout(Duration.ofSeconds(normalizedTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return parseSearchQueryContent(content);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public List<RecommendationLlmChoice> suggestRecommendations(Map<String, Object> recommendationContext) {
        if (!isConfigured()) {
            return List.of();
        }
        try {
            String requestBody = objectMapper.writeValueAsString(buildRecommendationRequest(recommendationContext));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(completionUrl()))
                    .timeout(Duration.ofSeconds(normalizedTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return parseRecommendationContent(content);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    FoodNutritionSuggestion parseSuggestionContent(Long foodItemId, String foodName, String content) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(content));
            List<String> warnings = new ArrayList<>();
            String category = requiredText(root, "category");
            Double calories = requiredNumber(root, "caloriesPer100g", 0, 1200);
            Double protein = requiredNumber(root, "proteinPer100g", 0, 100);
            Double fat = requiredNumber(root, "fatPer100g", 0, 100);
            Double carb = requiredNumber(root, "carbPer100g", 0, 100);
            Double confidence = readConfidence(root.path("confidence"), warnings);
            String basis = requiredText(root, "basis");
            return FoodNutritionSuggestion.builder()
                    .foodItemId(foodItemId)
                    .foodName(foodName)
                    .category(category)
                    .caloriesPer100g(roundOne(calories))
                    .proteinPer100g(roundOne(protein))
                    .fatPer100g(roundOne(fat))
                    .carbPer100g(roundOne(carb))
                    .confidence(roundTwo(confidence))
                    .basis(basis)
                    .matchedFdcIds(readMatchedFdcIds(root.path("matchedFdcIds")))
                    .rawModelResponse(truncate(content))
                    .parseWarning(warnings.isEmpty() ? null : String.join("; ", warnings))
                    .build();
        } catch (ModelResponseParseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ModelResponseParseException(
                    "模型返回不是可用的营养 JSON：" + exception.getMessage(),
                    truncate(content),
                    null,
                    exception);
        }
    }

    List<String> parseSearchQueryContent(String content) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(content));
            List<String> queries = new ArrayList<>();
            JsonNode queryArray = root.path("queries");
            if (queryArray.isArray()) {
                for (JsonNode item : queryArray) {
                    addSearchQuery(queries, item.asText(""));
                }
            } else {
                addSearchQuery(queries, root.path("query").asText(""));
            }
            return queries.stream().limit(3).toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static void addSearchQuery(List<String> queries, String query) {
        String value = normalizeSearchQuery(query);
        if (value != null && queries.stream().noneMatch(existing -> existing.equalsIgnoreCase(value))) {
            queries.add(value);
        }
    }

    private static String normalizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }
        String value = query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (value.isBlank() || value.length() > 48) {
            return null;
        }
        boolean hasLetter = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current > 127) {
                return null;
            }
            if (Character.isLetter(current)) {
                hasLetter = true;
            }
            if (!(Character.isLetterOrDigit(current) || current == ' ' || current == '-' || current == '\'')) {
                return null;
            }
        }
        return hasLetter ? value : null;
    }

    private ObjectNode buildChatRequest(
            FoodItem food,
            List<FoodNutritionReferenceCandidate> references,
            List<FoodNutritionMenuContext> menuContexts) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getModel());
        request.put("temperature", properties.getTemperature() == null ? 0.1 : properties.getTemperature());
        if (properties.isJsonMode()) {
            request.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        }
        ArrayNode messages = request.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", """
                        You estimate nutrition per 100g for foods from Chinese restaurant menus.
                        Return exactly one valid JSON object. Do not use Markdown, prose, comments, or code fences.
                        Required fields:
                        category, caloriesPer100g, proteinPer100g, fatPer100g, carbPer100g, confidence, matchedFdcIds, basis.
                        caloriesPer100g, proteinPer100g, fatPer100g, carbPer100g, and confidence MUST be JSON numbers, not strings.
                        All nutrient values MUST be normalized to 100g of edible food.
                        Never return nutrients per serving, per package, per piece, per roll, per bottle, per 500ml, or per menu unit.
                        If the menu item is described by count, serving size, package size, or volume, infer or approximate the conversion and still output per 100g.
                        For beverages and liquid foods, also normalize to 100g, not per bottle or per 100ml unless 100ml is effectively equivalent to 100g.
                        confidence MUST be a number from 0 to 1. Use lower confidence when USDA references are weak or absent.
                        matchedFdcIds MUST be an array of numeric FDC IDs used from the provided USDA references, or [] when none are used.
                        category and basis should be Chinese. basis should briefly say this is a per-100g AI estimate based on menu context and USDA references.
                        If USDA references are empty, still estimate from the food name, description, restaurant/menu context, and set confidence <= 0.45.
                        Never leave required nutrient fields null or empty.
                        """);
        ObjectNode userPayload = objectMapper.createObjectNode();
        userPayload.put("foodItemId", food.getId());
        userPayload.put("name", food.getName());
        userPayload.put("category", food.getCategory());
        userPayload.put("description", food.getDescription());
        userPayload.set("menuContexts", objectMapper.valueToTree(menuContexts));
        userPayload.set("usdaFoodDataCentralReferences", objectMapper.valueToTree(references));
        messages.addObject()
                .put("role", "user")
                .put("content", objectMapper.writeValueAsString(userPayload));
        return request;
    }

    private ObjectNode buildSearchQueryRequest(
            FoodItem food,
            List<FoodNutritionMenuContext> menuContexts,
            List<String> dictionaryHints) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getModel());
        request.put("temperature", 0);
        if (properties.isJsonMode()) {
            request.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        }
        ArrayNode messages = request.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", """
                        Convert a Chinese restaurant menu item into English USDA FoodData Central search queries.
                        Return exactly one valid JSON object with this shape: {"queries":["query one","query two"]}.
                        Rules:
                        - Return 1 to 3 generic English food/ingredient queries.
                        - Use ASCII English only.
                        - Prefer core edible substance over brand, restaurant name, promotion, portion count, or package size.
                        - Use dictionaryHints when they match, but improve them when the menu name/description is clearer.
                        - Examples: 虾滑 -> shrimp; 捞派肥牛3卷 -> beef; 巴沙鱼6片 -> catfish, fish; 德式小麦啤酒500ml -> wheat beer, beer.
                        """);
        ObjectNode userPayload = objectMapper.createObjectNode();
        userPayload.put("foodItemId", food.getId());
        userPayload.put("name", food.getName());
        userPayload.put("category", food.getCategory());
        userPayload.put("description", food.getDescription());
        userPayload.set("menuContexts", objectMapper.valueToTree(menuContexts));
        userPayload.set("dictionaryHints", objectMapper.valueToTree(dictionaryHints));
        messages.addObject()
                .put("role", "user")
                .put("content", objectMapper.writeValueAsString(userPayload));
        return request;
    }

    private ObjectNode buildRecommendationRequest(Map<String, Object> recommendationContext) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getModel());
        request.put("temperature", properties.getTemperature() == null ? 0.1 : properties.getTemperature());
        if (properties.isJsonMode()) {
            request.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));
        }
        ArrayNode messages = request.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", """
                        You are a diet recommendation reranker for a Chinese food tracking app.
                        Return exactly one valid JSON object. Do not use Markdown, prose, comments, or code fences.
                        Required shape:
                        {"items":[{"foodItemId":123,"reason":"中文推荐理由","displayDescription":"中文短描述","suggestedGrams":150}]}
                        Rules:
                        - Select 3 to 4 items only from the provided candidates.
                        - foodItemId MUST be one of the provided candidate foodItemId values.
                        - Never invent food items, restaurants, prices, nutrition, or IDs.
                        - suggestedGrams MUST be a JSON number between 80 and 350.
                        - Nutrition values are fixed by the database. Do not change or re-estimate them.
                        - reason should explain fit against the user's current meal nutrition target.
                        - displayDescription should be concise and appetizing, based only on the provided name/description/menu context.
                        """);
        messages.addObject()
                .put("role", "user")
                .put("content", objectMapper.writeValueAsString(recommendationContext));
        return request;
    }

    private List<RecommendationLlmChoice> parseRecommendationContent(String content) throws Exception {
        JsonNode root = objectMapper.readTree(extractJson(content));
        JsonNode items = root.path("items");
        if (!items.isArray()) {
            return List.of();
        }
        List<RecommendationLlmChoice> choices = new ArrayList<>();
        for (JsonNode item : items) {
            Long foodItemId = readLong(item.path("foodItemId"));
            if (foodItemId == null) {
                continue;
            }
            String reason = item.path("reason").asText("").trim();
            String description = item.path("displayDescription").asText("").trim();
            Double grams = readOptionalNumber(item.path("suggestedGrams"));
            choices.add(new RecommendationLlmChoice(
                    foodItemId,
                    reason.isBlank() ? null : reason,
                    description.isBlank() ? null : description,
                    grams));
        }
        return choices;
    }

    private String completionUrl() {
        String baseUrl = properties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/chat/completions";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private int normalizedTimeoutSeconds() {
        Integer configured = properties.getTimeoutSeconds();
        if (configured == null) {
            return 60;
        }
        return Math.max(5, Math.min(configured, 180));
    }

    private static String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("empty content");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewLine >= 0 && lastFence > firstNewLine) {
                trimmed = trimmed.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("missing json object");
        }
        return trimmed.substring(start, end + 1);
    }

    private static String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText("").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static Double requiredNumber(JsonNode root, String field, double min, double max) {
        JsonNode value = root.path(field);
        double number = parseFlexibleNumber(value, field);
        if (!Double.isFinite(number) || number < min || number > max) {
            throw new IllegalArgumentException(field + " is out of range");
        }
        return number;
    }

    private static double parseFlexibleNumber(JsonNode value, String field) {
        double number;
        if (value.isNumber()) {
            number = value.asDouble();
        } else if (value.isTextual()) {
            String text = value.asText("").trim();
            boolean percent = text.endsWith("%");
            if (percent) {
                text = text.substring(0, text.length() - 1).trim();
            }
            try {
                number = Double.parseDouble(text);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(field + " must be a number");
            }
            if (percent) {
                number = number / 100d;
            }
        } else {
            throw new IllegalArgumentException(field + " must be a number");
        }
        return number;
    }

    private static Long readLong(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        if (value.isTextual()) {
            try {
                return Long.valueOf(value.asText("").trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double readOptionalNumber(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            return parseFlexibleNumber(value, "suggestedGrams");
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static double readConfidence(JsonNode value, List<String> warnings) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            warnings.add("confidence 缺失，已使用默认低置信度 0.35");
            return DEFAULT_CONFIDENCE;
        }
        try {
            double confidence = parseFlexibleNumber(value, "confidence");
            if (confidence > 1d && confidence <= 100d) {
                warnings.add("confidence 像百分比，已自动换算为 0-1 小数");
                confidence = confidence / 100d;
            }
            if (!Double.isFinite(confidence) || confidence < 0d || confidence > 1d) {
                warnings.add("confidence 超出 0-1 范围，已使用默认低置信度 0.35");
                return DEFAULT_CONFIDENCE;
            }
            return confidence;
        } catch (IllegalArgumentException exception) {
            warnings.add("confidence 不是数字，已使用默认低置信度 0.35");
            return DEFAULT_CONFIDENCE;
        }
    }

    private static List<Long> readMatchedFdcIds(JsonNode node) {
        List<Long> ids = new ArrayList<>();
        if (!node.isArray()) {
            return ids;
        }
        for (JsonNode item : node) {
            if (item.isNumber()) {
                ids.add(item.asLong());
            } else if (item.isTextual()) {
                try {
                    ids.add(Long.valueOf(item.asText()));
                } catch (NumberFormatException ignored) {
                    // Ignore invalid model-provided IDs.
                }
            }
        }
        return ids;
    }

    private static double roundOne(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private static double roundTwo(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= 4000 ? trimmed : trimmed.substring(0, 4000) + "...";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static class ModelResponseParseException extends IllegalArgumentException {

        private final String rawModelResponse;
        private final String parseWarning;

        ModelResponseParseException(
                String message,
                String rawModelResponse,
                String parseWarning,
                Throwable cause) {
            super(message, cause);
            this.rawModelResponse = rawModelResponse;
            this.parseWarning = parseWarning;
        }

        public String getRawModelResponse() {
            return rawModelResponse;
        }

        public String getParseWarning() {
            return parseWarning;
        }
    }

    public record RecommendationLlmChoice(
            Long foodItemId,
            String reason,
            String displayDescription,
            Double suggestedGrams) {
    }
}
