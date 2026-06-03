package cn.edu.cn.javadiet.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRestaurantResponse {

    private Long id;
    private String name;
    private String address;
    private Double rating;
    private BigDecimal avgPrice;
    private String tags;
}
