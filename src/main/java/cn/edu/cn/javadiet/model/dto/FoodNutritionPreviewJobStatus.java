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
public class FoodNutritionPreviewJobStatus {

    private String jobId;
    private String status;
    private Integer totalCount;
    private Integer completedCount;
    private Boolean cancelRequested;
    private Long currentFoodItemId;
    private String currentFoodName;
    @Builder.Default
    private List<FoodNutritionSuggestion> items = new ArrayList<>();
}
