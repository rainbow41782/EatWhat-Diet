package cn.edu.cn.javadiet.model.dto;

import java.math.BigDecimal;
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
public class MeituanCrawlResult {

    private String restaurantName;
    private String address;
    private Double latitude;
    private Double longitude;
    private String meituanPoiId;
    private BigDecimal avgPrice;
    private Double rating;
    private String tags;

    @Builder.Default
    private List<MeituanMenuItem> items = new ArrayList<>();
}
