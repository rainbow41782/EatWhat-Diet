package cn.edu.cn.javadiet.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.edu.cn.javadiet.config.LlmGatewayProperties;
import cn.edu.cn.javadiet.model.dto.FoodNutritionSuggestion;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmGatewayClientTests {

    @Test
    void parsesStrictNutritionJsonFromModelContent() {
        LlmGatewayClient client = new LlmGatewayClient(new LlmGatewayProperties());
        String content = """
                ```json
                {
                  "category": "快餐",
                  "caloriesPer100g": 312.2,
                  "proteinPer100g": 3.43,
                  "fatPer100g": 14.73,
                  "carbPer100g": 41.44,
                  "confidence": 0.82,
                  "matchedFdcIds": [170698, "234567"],
                  "basis": "AI 估算，参考 USDA 炸薯条条目。"
                }
                ```
                """;

        FoodNutritionSuggestion suggestion = client.parseSuggestionContent(1L, "薯薯成双", content);

        assertEquals("快餐", suggestion.getCategory());
        assertEquals(312.2, suggestion.getCaloriesPer100g());
        assertEquals(3.4, suggestion.getProteinPer100g());
        assertEquals(14.7, suggestion.getFatPer100g());
        assertEquals(41.4, suggestion.getCarbPer100g());
        assertEquals(0.82, suggestion.getConfidence());
        assertEquals(2, suggestion.getMatchedFdcIds().size());
    }

    @Test
    void rejectsModelJsonWithMissingRequiredNutrients() {
        LlmGatewayClient client = new LlmGatewayClient(new LlmGatewayProperties());
        String content = """
                {
                  "category": "快餐",
                  "caloriesPer100g": 312,
                  "proteinPer100g": 3.4,
                  "fatPer100g": 14.7,
                  "confidence": 0.8,
                  "matchedFdcIds": [],
                  "basis": "missing carb"
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> client.parseSuggestionContent(1L, "薯薯成双", content));
    }

    @Test
    void acceptsNumericStringsFromModelContent() {
        LlmGatewayClient client = new LlmGatewayClient(new LlmGatewayProperties());
        String content = """
                {
                  "category": "fast food",
                  "caloriesPer100g": "92.4",
                  "proteinPer100g": "17.1",
                  "fatPer100g": "1.5",
                  "carbPer100g": "2.2",
                  "confidence": "82%",
                  "matchedFdcIds": ["169190"],
                  "basis": "estimated from shrimp reference"
                }
                """;

        FoodNutritionSuggestion suggestion = client.parseSuggestionContent(1L, "shrimp paste", content);

        assertEquals(92.4, suggestion.getCaloriesPer100g());
        assertEquals(17.1, suggestion.getProteinPer100g());
        assertEquals(0.82, suggestion.getConfidence());
    }

    @Test
    void keepsSuggestionWhenConfidenceIsNotNumeric() {
        LlmGatewayClient client = new LlmGatewayClient(new LlmGatewayProperties());
        String content = """
                {
                  "category": "海鲜",
                  "caloriesPer100g": 92.4,
                  "proteinPer100g": 17.1,
                  "fatPer100g": 1.5,
                  "carbPer100g": 2.2,
                  "confidence": "高",
                  "matchedFdcIds": [175180],
                  "basis": "按虾类 USDA 参考估算"
                }
                """;

        FoodNutritionSuggestion suggestion = client.parseSuggestionContent(1L, "招牌大颗粒虾滑4颗", content);

        assertEquals(0.35, suggestion.getConfidence());
        assertTrue(suggestion.getParseWarning().contains("confidence"));
        assertTrue(suggestion.getRawModelResponse().contains("\"confidence\": \"高\""));
    }

    @Test
    void exposesRawModelContentWhenJsonIsMissing() {
        LlmGatewayClient client = new LlmGatewayClient(new LlmGatewayProperties());

        LlmGatewayClient.ModelResponseParseException exception = assertThrows(
                LlmGatewayClient.ModelResponseParseException.class,
                () -> client.parseSuggestionContent(1L, "捞派肥牛3卷", "我估计这道菜热量较高"));

        assertTrue(exception.getRawModelResponse().contains("热量较高"));
    }

    @Test
    void parsesEnglishSearchQueriesFromModelContent() {
        LlmGatewayClient client = new LlmGatewayClient(new LlmGatewayProperties());
        String content = """
                {
                  "queries": ["Beef slices", "牛肉", "shabu-shabu beef", "this query is far too long to be useful for usda fooddata central"]
                }
                """;

        assertEquals(List.of("beef slices", "shabu-shabu beef"), client.parseSearchQueryContent(content));
    }
}
