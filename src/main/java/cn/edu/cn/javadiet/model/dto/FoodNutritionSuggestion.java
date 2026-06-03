package cn.edu.cn.javadiet.model.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutritionSuggestion {

    private Long foodItemId;
    private String foodName;
    private String category;
    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbPer100g;
    private Double confidence;
    private String basis;
    private String failureReason;
    private String rawModelResponse;
    private String parseWarning;
    @Builder.Default
    private List<Long> matchedFdcIds = new ArrayList<>();
    @Builder.Default
    private List<FoodNutritionReferenceCandidate> references = new ArrayList<>();
    @Builder.Default
    private List<String> searchQueries = new ArrayList<>();
}
