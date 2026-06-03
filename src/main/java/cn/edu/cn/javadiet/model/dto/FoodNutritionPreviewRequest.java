package cn.edu.cn.javadiet.model.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FoodNutritionPreviewRequest {

    private List<Long> foodItemIds = new ArrayList<>();
}
