package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.config.TencentMapProperties;
import cn.edu.cn.javadiet.model.dto.IpLocationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TencentIpLocationClient {

    private static final Logger log = LoggerFactory.getLogger(TencentIpLocationClient.class);

    private final TencentMapProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public TencentIpLocationClient(TencentMapProperties properties) {
        this.properties = properties;
        int timeout = normalizedTimeout(properties.getTimeoutSeconds());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getKey() != null
                && !properties.getKey().isBlank();
    }

    public Optional<IpLocationResult> locateByIp(String ip) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            StringBuilder url = new StringBuilder("https://apis.map.qq.com/ws/location/v1/ip")
                    .append("?key=").append(encode(properties.getKey()));
            if (ip != null && !ip.isBlank()) {
                url.append("&ip=").append(encode(ip));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(normalizedTimeout(properties.getTimeoutSeconds())))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Tencent IP location failed, status={}", response.statusCode());
                return Optional.empty();
            }
            return parse(response.body());
        } catch (Exception ex) {
            log.warn("Tencent IP location error: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<IpLocationResult> parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("status").asInt(-1) != 0) {
                log.warn("Tencent IP location status not ok: {}", root.path("message").asText("unknown"));
                return Optional.empty();
            }
            JsonNode result = root.path("result");
            JsonNode location = result.path("location");
            JsonNode adInfo = result.path("ad_info");

            if (!location.isObject() || !location.path("lat").isNumber() || !location.path("lng").isNumber()) {
                return Optional.empty();
            }

            return Optional.of(IpLocationResult.builder()
                    .latitude(location.path("lat").asDouble())
                    .longitude(location.path("lng").asDouble())
                    .ip(result.path("ip").asText(null))
                    .province(adInfo.path("province").asText(null))
                    .city(adInfo.path("city").asText(null))
                    .district(adInfo.path("district").asText(null))
                    .source("TENCENT_IP")
                    .accuracyNote("IP定位仅精确到城市/区县级，建议仅用于粗定位")
                    .build());
        } catch (Exception ex) {
            log.warn("Tencent IP location parse error: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static int normalizedTimeout(Integer timeoutSeconds) {
        int timeout = timeoutSeconds == null ? 8 : timeoutSeconds;
        return Math.max(3, Math.min(timeout, 30));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
