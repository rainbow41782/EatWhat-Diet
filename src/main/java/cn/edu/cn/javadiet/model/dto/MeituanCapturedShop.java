package cn.edu.cn.javadiet.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeituanCapturedShop {

    private String captureKey;
    private String meituanPoiId;
    private String restaurantName;
    private String address;
    private int endpointCount;
    private int itemCount;
    private int missingNutritionCount;
    private String latestEndpoint;
    private LocalDateTime latestCapturedAt;
    private Long importedRestaurantId;
}
