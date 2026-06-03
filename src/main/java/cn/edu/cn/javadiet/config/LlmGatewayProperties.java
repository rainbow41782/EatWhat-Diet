package cn.edu.cn.javadiet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "llm.gateway")
public class LlmGatewayProperties {

    private boolean enabled = false;
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private Double temperature = 0.1;
    private Integer timeoutSeconds = 60;
    private boolean jsonMode = true;
}
