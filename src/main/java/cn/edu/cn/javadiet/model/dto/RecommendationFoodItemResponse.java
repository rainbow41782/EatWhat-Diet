package cn.edu.cn.javadiet.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationFoodItemResponse {

    private Long foodItemId;
    private String name;
    private String category;
    private String description;
    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbPer100g;
    private Double suggestedGrams;
    private String unit;
    private Double totalCalories;
    private Double totalProtein;
    private Double totalFat;
    private Double totalCarb;
    private BigDecimal price;
    private String portionSize;
    private String menuCategory;
    private String imageUrl;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantAddress;
}
