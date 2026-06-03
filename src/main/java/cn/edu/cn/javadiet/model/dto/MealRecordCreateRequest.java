package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.enums.MealType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealRecordCreateRequest {

    private Long dailyCheckInId;
    private MealType mealType;
    private LocalDateTime mealTime;
    private String remark;
}
