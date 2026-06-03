package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.enums.HealthGoal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NutritionPlanResult {
    private HealthGoal healthGoal;
    private Double estimatedBodyFatPct;
    private Double leanBodyMassKg;
    private Double bmr;
    private Double tdee;
    private Double targetCalories;
    private Double calorieDelta;
    private Double targetProtein;
    private Double targetFat;
    private Double targetCarb;
    private Double macroProteinPct;
    private Double macroFatPct;
    private Double macroCarbPct;
    private Integer durationMinWeeks;
    private Integer durationMaxWeeks;
}
