package cn.edu.cn.javadiet.model.dto;

import java.time.LocalDateTime;
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
public class RecommendationResponse {

    private Long id;
    private Long userId;
    private String batchId;
    private Long restaurantId;
    private LocalDateTime recommendationTime;
    private String mealType;
    private String recommendedReason;
    private Double targetCalories;
    private Double targetProtein;
    private Double targetFat;
    private Double targetCarb;
    private Double score;
    private String status;
    private LocalDateTime createdAt;
    private String foodItemName;
    private Double totalCalories;
    private RecommendationRestaurantResponse restaurant;
    @Builder.Default
    private List<RecommendationFoodItemResponse> items = new ArrayList<>();
}
