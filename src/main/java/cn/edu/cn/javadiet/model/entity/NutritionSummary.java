package cn.edu.cn.javadiet.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionSummary {

    private Double calories;
    private Double protein;
    private Double fat;
    private Double carb;
    private Double fiber;
    private Double waterIntakeMl;
}
