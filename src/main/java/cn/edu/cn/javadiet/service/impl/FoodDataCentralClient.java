package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.config.FoodDataCentralProperties;
import cn.edu.cn.javadiet.model.dto.FoodNutritionReferenceCandidate;
import cn.edu.cn.javadiet.model.entity.FoodNutritionReference;
import cn.edu.cn.javadiet.repository.FoodNutritionReferenceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FoodDataCentralClient {

    static final String SOURCE = "USDA_FDC";

    private final FoodDataCentralProperties properties;
    private final FoodNutritionReferenceRepository referenceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public FoodDataCentralClient(
            FoodDataCentralProperties properties,
            FoodNutritionReferenceRepository referenceRepository) {
        this.properties = properties;
        this.referenceRepository = referenceRepository;
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public List<FoodNutritionReferenceCandidate> search(String query) {
        if (!isConfigured() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String body = objectMapper.writeValueAsString(buildSearchRequest(query));
            String url = "https://api.nal.usda.gov/fdc/v1/foods/search?api_key="
                    + URLEncoder.encode(properties.getApiKey(), StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return parseAndCacheSearchResponse(response.body());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    List<FoodNutritionReferenceCandidate> parseSearchResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode foods = root.path("foods");
            if (!foods.isArray()) {
                return List.of();
            }
            List<FoodNutritionReferenceCandidate> candidates = new ArrayList<>();
            for (JsonNode food : foods) {
                FoodNutritionReferenceCandidate candidate = toCandidate(food);
                if (candidate.getFdcId() != null) {
                    candidates.add(candidate);
                }
            }
            return candidates;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<FoodNutritionReferenceCandidate> parseAndCacheSearchResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode foods = root.path("foods");
        if (!foods.isArray()) {
            return List.of();
        }
        Map<Long, FoodNutritionReferenceCandidate> candidates = new LinkedHashMap<>();
        for (JsonNode food : foods) {
            FoodNutritionReferenceCandidate candidate = toCandidate(food);
            if (candidate.getFdcId() == null) {
                continue;
            }
            candidates.putIfAbsent(candidate.getFdcId(), candidate);
            cacheCandidate(candidate, objectMapper.writeValueAsString(food));
        }
        return new ArrayList<>(candidates.values());
    }

    private ObjectNode buildSearchRequest(String query) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("query", query);
        request.put("pageSize", normalizedMaxCandidates());
        ArrayNode dataTypes = request.putArray("dataType");
        List<String> configuredTypes = properties.getDataTypes() == null ? List.of() : properties.getDataTypes();
        for (String dataType : configuredTypes) {
            if (dataType != null && !dataType.isBlank()) {
                dataTypes.add(dataType);
            }
        }
        return request;
    }

    private FoodNutritionReferenceCandidate toCandidate(JsonNode food) {
        return FoodNutritionReferenceCandidate.builder()
                .fdcId(food.path("fdcId").isNumber() ? food.path("fdcId").asLong() : null)
                .description(text(food, "description"))
                .dataType(text(food, "dataType"))
                .foodCategory(text(food, "foodCategory"))
                .caloriesPer100g(readNutrient(food, "1008", "208", "Energy"))
                .proteinPer100g(readNutrient(food, "1003", "203", "Protein"))
                .fatPer100g(readNutrient(food, "1004", "204", "Total lipid"))
                .carbPer100g(readNutrient(food, "1005", "205", "Carbohydrate"))
                .build();
    }

    private Double readNutrient(JsonNode food, String nutrientId, String nutrientNumber, String nutrientName) {
        JsonNode nutrients = food.path("foodNutrients");
        if (!nutrients.isArray()) {
            return null;
        }
        for (JsonNode nutrient : nutrients) {
            if (!matchesNutrient(nutrient, nutrientId, nutrientNumber, nutrientName)) {
                continue;
            }
            Double value = numericValue(nutrient);
            if (value == null) {
                return null;
            }
            String unit = text(nutrient, "unitName");
            if ("Energy".equals(nutrientName) && unit != null && unit.equalsIgnoreCase("kJ")) {
                return roundOne(value / 4.184d);
            }
            return roundOne(value);
        }
        return null;
    }

    private static boolean matchesNutrient(
            JsonNode nutrient,
            String nutrientId,
            String nutrientNumber,
            String nutrientName) {
        String idValue = text(nutrient, "nutrientId");
        String numberValue = text(nutrient, "nutrientNumber");
        String nameValue = text(nutrient, "nutrientName");
        return nutrientId.equals(idValue)
                || nutrientNumber.equals(numberValue)
                || (nameValue != null && nameValue.toLowerCase().contains(nutrientName.toLowerCase()));
    }

    private void cacheCandidate(FoodNutritionReferenceCandidate candidate, String rawJson) {
        LocalDateTime now = LocalDateTime.now();
        FoodNutritionReference reference = referenceRepository.findBySourceAndFdcId(SOURCE, candidate.getFdcId())
                .orElseGet(FoodNutritionReference::new);
        if (reference.getId() == null) {
            reference.setCreatedAt(now);
        }
        reference.setSource(SOURCE);
        reference.setFdcId(candidate.getFdcId());
        reference.setDescription(candidate.getDescription());
        reference.setDataType(candidate.getDataType());
        reference.setFoodCategory(candidate.getFoodCategory());
        reference.setCaloriesPer100g(candidate.getCaloriesPer100g());
        reference.setProteinPer100g(candidate.getProteinPer100g());
        reference.setFatPer100g(candidate.getFatPer100g());
        reference.setCarbPer100g(candidate.getCarbPer100g());
        reference.setRawJson(rawJson);
        reference.setUpdatedAt(now);
        referenceRepository.save(reference);
    }

    private int normalizedMaxCandidates() {
        Integer configured = properties.getMaxCandidates();
        if (configured == null) {
            return 6;
        }
        return Math.max(1, Math.min(configured, 20));
    }

    private static Double numericValue(JsonNode node) {
        JsonNode value = node.path("value");
        return value.isNumber() ? value.asDouble() : null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static double roundOne(double value) {
        return Math.round(value * 10d) / 10d;
    }
}
