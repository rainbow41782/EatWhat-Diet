package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.enums.MealType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {

    private Long userId;
    private MealType targetMealType;
    private BigDecimal maxBudget;
    private Integer maxDistanceKm;
}
