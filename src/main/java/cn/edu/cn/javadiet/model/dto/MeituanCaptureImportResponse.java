package cn.edu.cn.javadiet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeituanCaptureImportResponse {

    private boolean imported;
    private Long restaurantId;
    private int itemCount;
    private int pendingNutritionCount;
    private String restaurantName;
    private String meituanPoiId;
    private String message;
}
