package cn.edu.cn.javadiet.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodIntakeCreateRequest {

    private Long mealRecordId;
    private Long foodItemId;
    private Double quantity;
    private String unit;
    private LocalDateTime intakeTime;
    private String remark;

    // 手动录入时使用（foodItemId 为 null）
    private String name;
    private Double calories;
    private Double protein;
    private Double fat;
    private Double carb;
}
