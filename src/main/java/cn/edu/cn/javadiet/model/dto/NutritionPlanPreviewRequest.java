package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.enums.ActivityLevel;
import cn.edu.cn.javadiet.model.enums.Gender;
import cn.edu.cn.javadiet.model.enums.HealthGoal;
import lombok.Data;

@Data
public class NutritionPlanPreviewRequest {
    private Gender gender;
    private Integer age;
    private Double heightCm;
    private Double weightKg;
    private HealthGoal healthGoal;
    private ActivityLevel activityLevel;
}
