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
public class FoodNutritionPendingItem {

    private Long foodItemId;
    private String name;
    private String category;
    private String description;
    private String nutritionStatus;
    private String externalSource;
    private String externalId;
    @Builder.Default
    private List<FoodNutritionMenuContext> menuContexts = new ArrayList<>();
}
