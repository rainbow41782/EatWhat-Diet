package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.config.TencentMapProperties;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.enums.RestaurantStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TencentPlaceClient {

    private static final String SOURCE = "TENCENT_PLACE";
    private static final Logger log = LoggerFactory.getLogger(TencentPlaceClient.class);

    private final TencentMapProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public TencentPlaceClient(TencentMapProperties properties) {
        this.properties = properties;
        int timeout = normalizedTimeout(properties.getTimeoutSeconds());
        System.setProperty("java.net.useSystemProxies", "true");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .proxy(ProxySelector.getDefault())
                .build();
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getKey() != null
                && !properties.getKey().isBlank();
    }

    public List<Restaurant> searchNearby(double latitude, double longitude, Integer maxDistanceKm) {
        if (!isConfigured()) {
            return List.of();
        }
        try {
            int radiusMeters = normalizedRadiusMeters(maxDistanceKm);
            int pageSize = normalizedPageSize(properties.getPageSize());
            int maxPages = normalizedMaxPages(properties.getMaxPagesPerCategory());
            int maxResults = normalizedMaxResults(properties.getMaxResults());
            int timeout = normalizedTimeout(properties.getTimeoutSeconds());
            String keyword = properties.getKeyword() == null || properties.getKeyword().isBlank()
                    ? "餐厅"
                    : properties.getKeyword();
            String key = properties.getKey();

            Map<String, Restaurant> merged = new LinkedHashMap<>();
            for (String categoryCode : resolveCategoryCodes(properties.getCategoryCodes())) {
                for (int pageIndex = 1; pageIndex <= maxPages; pageIndex++) {
                    String url = buildUrl(
                            latitude,
                            longitude,
                            radiusMeters,
                            keyword,
                            categoryCode,
                            pageSize,
                            pageIndex,
                            properties.isAutoExtend(),
                            key);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(timeout))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        log.warn("Tencent place category query failed, status={}, categoryCode={}, pageIndex={}",
                                response.statusCode(), categoryCode, pageIndex);
                        break;
                    }

                    List<Restaurant> pageResults = parse(response.body());
                    if (pageResults.isEmpty()) {
                        break;
                    }

                    for (Restaurant item : pageResults) {
                        String externalId = item.getExternalId();
                        if (externalId == null || externalId.isBlank()) {
                            continue;
                        }
                        merged.putIfAbsent(externalId, item);
                        if (merged.size() >= maxResults) {
                            return withGeneratedIds(merged.values(), maxResults);
                        }
                    }

                    if (pageResults.size() < pageSize) {
                        break;
                    }
                }
            }
            List<Restaurant> aggregated = withGeneratedIds(merged.values(), maxResults);
            if (!aggregated.isEmpty()) {
                return aggregated;
            }

            // Fallback to a basic nearby search to avoid empty results when category aggregation fails.
            List<Restaurant> fallback = searchNearbyBasic(latitude, longitude, radiusMeters, pageSize, timeout, keyword, key);
            if (!fallback.isEmpty()) {
                return fallback;
            }
            return List.of();
        } catch (Exception ex) {
            log.warn("Tencent place search error: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<Restaurant> searchNearbyBasic(
            double latitude,
            double longitude,
            int radiusMeters,
            int pageSize,
            int timeout,
            String keyword,
            String key) {
        try {
            String boundary = "nearby(" + latitude + "," + longitude + "," + radiusMeters + "," + (properties.isAutoExtend() ? 1 : 0) + ")";
            String url = "https://apis.map.qq.com/ws/place/v1/search"
                    + "?keyword=" + encode(keyword)
                    + "&boundary=" + encode(boundary)
                    + "&orderby=_distance"
                    + "&page_size=" + pageSize
                    + "&page_index=1"
                    + "&key=" + encode(key);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Tencent place basic query failed, status={}", response.statusCode());
                return List.of();
            }
            List<Restaurant> parsed = parse(response.body());
            return withGeneratedIds(parsed, normalizedMaxResults(properties.getMaxResults()));
        } catch (Exception ex) {
            log.warn("Tencent place basic query error: {}", ex.getMessage());
            return List.of();
        }
    }

    private static List<String> resolveCategoryCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("100000", "101600", "102000", "102600", "102800", "161300", "161500");
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

        private static String buildUrl(
            double latitude,
            double longitude,
            int radiusMeters,
            String keyword,
            String categoryCode,
            int pageSize,
            int pageIndex,
            boolean autoExtend,
            String key) {
        String boundary = "nearby(" + latitude + "," + longitude + "," + radiusMeters + "," + (autoExtend ? 1 : 0) + ")";
        String filter = "category=" + categoryCode;
        return "https://apis.map.qq.com/ws/place/v1/search"
                + "?keyword=" + encode(keyword)
                + "&boundary=" + encode(boundary)
                + "&filter=" + encode(filter)
                + "&added_fields=category_code"
                + "&orderby=_distance"
                + "&page_size=" + pageSize
                + "&page_index=" + pageIndex
                + "&key=" + encode(key);
    }

    private List<Restaurant> withGeneratedIds(Iterable<Restaurant> source, int maxResults) {
        List<Restaurant> results = new ArrayList<>();
        int index = 1;
        for (Restaurant item : source) {
            if (results.size() >= maxResults) {
                break;
            }
            item.setId(-1L * index);
            results.add(item);
            index++;
        }
        return results;
    }

    private List<Restaurant> parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("status").asInt(-1) != 0) {
                return List.of();
            }
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return List.of();
            }

            List<Restaurant> restaurants = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode location = item.path("location");
                LocalDateTime now = LocalDateTime.now();

                Restaurant restaurant = Restaurant.builder()
                        .id(null)
                        .name(text(item, "title"))
                        .address(text(item, "address"))
                        .latitude(number(location, "lat"))
                        .longitude(number(location, "lng"))
                        .phone(text(item, "tel"))
                        .businessHours(null)
                        .avgPrice((BigDecimal) null)
                        .rating((Double) null)
                        .tags(text(item, "category"))
                        .externalSource(SOURCE)
                        .externalId(text(item, "id"))
                        .status(RestaurantStatus.OPEN)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                if (restaurant.getName() != null && !restaurant.getName().isBlank()) {
                    restaurants.add(restaurant);
                }
            }
            return restaurants;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }

    private static int normalizedRadiusMeters(Integer maxDistanceKm) {
        int km = maxDistanceKm == null ? 5 : maxDistanceKm;
        km = Math.max(1, Math.min(km, 25));
        return Math.min(km * 1000, 1000);
    }

    private static int normalizedMaxPages(Integer maxPagesPerCategory) {
        int pages = maxPagesPerCategory == null ? 5 : maxPagesPerCategory;
        return Math.max(1, Math.min(pages, 10));
    }

    private static int normalizedMaxResults(Integer maxResults) {
        int total = maxResults == null ? 120 : maxResults;
        return Math.max(20, Math.min(total, 200));
    }

    private static int normalizedPageSize(Integer pageSize) {
        int size = pageSize == null ? 20 : pageSize;
        return Math.max(1, Math.min(size, 20));
    }

    private static int normalizedTimeout(Integer timeoutSeconds) {
        int timeout = timeoutSeconds == null ? 8 : timeoutSeconds;
        return Math.max(3, Math.min(timeout, 30));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}