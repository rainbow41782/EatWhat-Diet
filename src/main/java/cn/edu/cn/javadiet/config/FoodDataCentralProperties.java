package cn.edu.cn.javadiet.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nutrition.fdc")
public class FoodDataCentralProperties {

    private String apiKey = "";
    private List<String> dataTypes = new ArrayList<>(List.of("Foundation", "SR Legacy", "FNDDS"));
    private Integer maxCandidates = 6;
}
