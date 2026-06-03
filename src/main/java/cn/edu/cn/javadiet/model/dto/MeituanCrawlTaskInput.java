package cn.edu.cn.javadiet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeituanCrawlTaskInput {

    private String shopName;
    private String address;
    private Double latitude;
    private Double longitude;
    private String meituanPoiId;
    private String menuUrl;
}
