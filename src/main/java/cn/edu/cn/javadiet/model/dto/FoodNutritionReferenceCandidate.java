package cn.edu.cn.javadiet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutritionReferenceCandidate {

    private Long fdcId;
    private String description;
    private String dataType;
    private String foodCategory;
    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbPer100g;
}
