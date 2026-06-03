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
public class FoodNutritionMenuContext {

    private Long restaurantId;
    private String restaurantName;
    private BigDecimal price;
    private String portionSize;
    private String categoryName;
    private String rawSpec;
    private String remark;
}
