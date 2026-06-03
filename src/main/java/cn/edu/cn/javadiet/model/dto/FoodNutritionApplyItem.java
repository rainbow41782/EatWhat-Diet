package cn.edu.cn.javadiet.model.dto;

import lombok.Data;

@Data
public class FoodNutritionApplyItem {

    private Long foodItemId;
    private String category;
    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbPer100g;
}
