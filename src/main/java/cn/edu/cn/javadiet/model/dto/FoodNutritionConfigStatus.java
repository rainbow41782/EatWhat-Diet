package cn.edu.cn.javadiet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutritionConfigStatus {

    private Boolean llmEnabled;
    private Boolean llmConfigured;
    private String llmModel;
    private Boolean fdcConfigured;
    private Integer fdcMaxCandidates;
}
