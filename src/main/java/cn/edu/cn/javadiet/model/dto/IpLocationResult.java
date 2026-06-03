package cn.edu.cn.javadiet.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IpLocationResult {
    private Double latitude;
    private Double longitude;
    private String ip;
    private String province;
    private String city;
    private String district;
    private String source;
    private String accuracyNote;
}
