package cn.edu.cn.javadiet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "map.tencent")
public class TencentMapProperties {

    private boolean enabled = false;
    private String key = "";
    private String keyword = "餐厅";
    private String categoryCodes = "100000,101600,102000,102600,102800,161300,161500";
    private Integer pageSize = 20;
    private Integer maxPagesPerCategory = 5;
    private Integer maxResults = 120;
    private boolean autoExtend = true;
    private Integer timeoutSeconds = 8;
}