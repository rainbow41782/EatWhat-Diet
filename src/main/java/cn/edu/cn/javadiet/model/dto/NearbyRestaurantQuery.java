package cn.edu.cn.javadiet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRestaurantQuery {

    private Double latitude;
    private Double longitude;
    private Integer maxDistanceKm;
    private Boolean onlyOpen;
}
