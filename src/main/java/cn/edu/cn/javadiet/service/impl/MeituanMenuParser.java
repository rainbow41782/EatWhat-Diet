package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.MeituanCrawlResult;
import cn.edu.cn.javadiet.model.dto.MeituanMenuItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MeituanMenuParser {

    private static final String DOM_SNAPSHOT_SOURCE = "MEITUAN_DOM_SNAPSHOT";

    private final ObjectMapper objectMapper;

    public MeituanMenuParser() {
        this.objectMapper = new ObjectMapper();
    }

    public MeituanCrawlResult parse(List<String> responseBodies) {
        Map<String, MeituanMenuItem> itemMap = new LinkedHashMap<>();
        MeituanCrawlResult result = MeituanCrawlResult.builder().build();
        for (String body : responseBodies) {
            if (body == null || body.isBlank() || !body.stripLeading().startsWith("{")) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(body);
                if (DOM_SNAPSHOT_SOURCE.equals(root.path("source").asText())) {
                    applyDomSnapshot(root, result, itemMap);
                    continue;
                }
                String poiId = firstValue(root, "poi_id_str");
                if (result.getMeituanPoiId() == null) {
                    result.setMeituanPoiId(poiId);
                }
                String restaurantName = firstValue(root, "poi_name", "shop_name", "wm_poi_name", "restaurant_name");
                if (shouldReplaceRestaurantName(result.getRestaurantName(), restaurantName, result.getMeituanPoiId())) {
                    result.setRestaurantName(restaurantName);
                }
                if (result.getAddress() == null) {
                    result.setAddress(firstValue(root, "address", "addr"));
                }
                if (result.getRating() == null) {
                    result.setRating(firstDouble(root, "wm_poi_score", "poi_score", "rating", "score"));
                }
                if (result.getAvgPrice() == null) {
                    result.setAvgPrice(firstBigDecimal(root, "avg_price", "average_price", "avgPrice"));
                }
                if (result.getTags() == null) {
                    result.setTags(firstValue(root, "tag", "tags", "poi_tags", "shipping_time"));
                }
                collectItems(root, null, itemMap);
            } catch (Exception ignored) {
                // Ignore non-menu payloads captured during page bootstrap.
            }
        }
        result.setItems(itemMap.values().stream().toList());
        return result;
    }

    private void collectItems(JsonNode node, String category, Map<String, MeituanMenuItem> itemMap) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            String nextCategory = category;
            if (hasAnyArray(node, "spus", "product_spu_list", "foodlist", "foods", "items", "products", "spu_list")
                    && firstDirectValue(node, "name", "tag", "tag_name", "tagName", "category_name",
                            "categoryName", "spu_tag_name", "title") != null) {
                nextCategory = firstDirectValue(node, "name", "tag", "tag_name", "tagName", "category_name",
                        "categoryName", "spu_tag_name", "title");
            }
            if (isMenuItem(node)) {
                MeituanMenuItem item = toMenuItem(node, nextCategory);
                if (item.getName() != null && !item.getName().isBlank()) {
                    mergeItem(itemMap, item);
                }
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                collectItems(field.getValue(), nextCategory, itemMap);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectItems(child, category, itemMap);
            }
        }
    }

    private static boolean isMenuItem(JsonNode node) {
        String name = firstDirectValue(node, "name", "spu_name", "spuName", "product_name", "food_name");
        String id = firstDirectValue(node, "id", "spu_id", "spuId", "spu_id_str", "product_spu_id", "food_id");
        return name != null
                && id != null
                && (firstDirectValue(node, "activity_price", "activityPrice", "discount_price", "discountPrice",
                                "member_price", "memberPrice", "current_price", "currentPrice", "min_price",
                                "minPrice", "price", "origin_price", "originPrice", "original_price",
                                "originalPrice") != null
                        || node.has("skus")
                        || node.has("picture")
                        || node.has("picture_url")
                        || node.has("pic_url")
                        || node.has("image_url"));
    }

    private MeituanMenuItem toMenuItem(JsonNode node, String category) {
        JsonNode firstSku = node.path("skus").isArray() && node.path("skus").size() > 0
                ? node.path("skus").get(0)
                : null;
        BigDecimal price = effectivePrice(node, firstSku);
        String directCategory = firstDirectValue(node, "category", "category_name", "categoryName", "tag_name",
                "tagName", "spu_tag_name");
        String imageUrl = firstDirectValue(node, "picture", "picture_url", "pic_url", "image_url");
        if (imageUrl == null && firstSku != null) {
            imageUrl = firstDirectValue(firstSku, "picture", "picture_url", "pic_url", "image_url");
        }
        return MeituanMenuItem.builder()
                .externalId(firstDirectValue(node, "id", "spu_id", "spuId", "spu_id_str", "product_spu_id",
                        "food_id"))
                .skuId(firstSku == null ? null : firstDirectValue(firstSku, "id", "sku_id", "skuId"))
                .name(firstDirectValue(node, "name", "spu_name", "spuName", "product_name", "food_name"))
                .category(directCategory == null ? category : directCategory)
                .description(firstDirectValue(node, "description", "desc", "spu_desc", "spuDesc", "unit"))
                .price(price)
                .available(!isSoldOut(node))
                .portionSize(firstDirectValue(node, "unit", "spec", "standard", "spec_desc"))
                .imageUrl(imageUrl)
                .rawSpec(buildRawSpec(node, firstSku))
                .rawJson(toJson(node))
                .build();
    }

    private static boolean isSoldOut(JsonNode node) {
        for (String key : List.of("sold_out", "soldout", "is_sold_out", "isSoldOut")) {
            JsonNode value = node.get(key);
            if (value != null && value.asBoolean(false)) {
                return true;
            }
        }
        String status = firstDirectValue(node, "status", "sell_status", "sale_status");
        return status != null && ("0".equals(status) || "soldout".equalsIgnoreCase(status));
    }

    private static BigDecimal effectivePrice(JsonNode node, JsonNode firstSku) {
        BigDecimal price = firstPriceByKeys(node, preferredPriceKeys());
        if (price == null) {
            price = firstPriceByKeys(firstSku, preferredPriceKeys());
        }
        if (price == null) {
            price = firstPriceByKeys(node, fallbackPriceKeys());
        }
        if (price == null) {
            price = firstPriceByKeys(firstSku, fallbackPriceKeys());
        }
        return price;
    }

    private static BigDecimal firstPrice(JsonNode node) {
        BigDecimal price = firstPriceByKeys(node, preferredPriceKeys());
        if (price == null) {
            price = firstPriceByKeys(node, fallbackPriceKeys());
        }
        return price;
    }

    private static BigDecimal firstPriceByKeys(JsonNode node, List<String> keys) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                continue;
            }
            try {
                return parsePrice(value.asText());
            } catch (NumberFormatException ignored) {
                // Try lower-priority price fields before giving up.
            }
        }
        return null;
    }

    private static List<String> preferredPriceKeys() {
        return List.of(
                "activity_price",
                "activityPrice",
                "discount_price",
                "discountPrice",
                "member_price",
                "memberPrice",
                "real_price",
                "realPrice",
                "current_price",
                "currentPrice",
                "min_price",
                "minPrice");
    }

    private static List<String> fallbackPriceKeys() {
        return List.of(
                "price",
                "origin_price",
                "originPrice",
                "original_price",
                "originalPrice");
    }

    private void applyDomSnapshot(
            JsonNode root,
            MeituanCrawlResult result,
            Map<String, MeituanMenuItem> itemMap) {
        String poiId = firstDirectValue(root, "poiId", "poi_id_str", "meituanPoiId");
        if (result.getMeituanPoiId() == null) {
            result.setMeituanPoiId(poiId);
        }
        String restaurantName = firstDirectValue(root, "restaurantName", "shopName", "poiName");
        if (shouldReplaceRestaurantName(result.getRestaurantName(), restaurantName, result.getMeituanPoiId())) {
            result.setRestaurantName(restaurantName);
        }
        JsonNode items = root.path("items");
        if (!items.isArray()) {
            return;
        }
        for (JsonNode itemNode : items) {
            MeituanMenuItem item = MeituanMenuItem.builder()
                    .name(firstDirectValue(itemNode, "name"))
                    .category(firstDirectValue(itemNode, "category"))
                    .description(firstDirectValue(itemNode, "description", "desc"))
                    .price(firstPrice(itemNode))
                    .portionSize(firstDirectValue(itemNode, "portionSize", "spec"))
                    .rawSpec(buildDomRawSpec(itemNode))
                    .rawJson(toJson(itemNode))
                    .build();
            if (item.getName() != null && !item.getName().isBlank()) {
                mergeItem(itemMap, item);
            }
        }
    }

    private static String buildDomRawSpec(JsonNode node) {
        List<String> parts = new ArrayList<>();
        appendPart(parts, "domOriginalPrice", firstDirectValue(node, "originalPrice"));
        appendPart(parts, "domDiscountText", firstDirectValue(node, "discountText"));
        appendPart(parts, "domSource", firstDirectValue(node, "source"));
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static void mergeItem(Map<String, MeituanMenuItem> itemMap, MeituanMenuItem incoming) {
        String key = findMergeKey(itemMap, incoming);
        MeituanMenuItem existing = itemMap.get(key);
        if (existing == null) {
            itemMap.put(key, incoming);
            return;
        }
        existing.setExternalId(coalesce(existing.getExternalId(), incoming.getExternalId()));
        existing.setSkuId(coalesce(existing.getSkuId(), incoming.getSkuId()));
        existing.setName(coalesce(existing.getName(), incoming.getName()));
        existing.setCategory(coalesce(incoming.getCategory(), existing.getCategory()));
        existing.setDescription(coalesce(incoming.getDescription(), existing.getDescription()));
        existing.setPrice(coalesce(incoming.getPrice(), existing.getPrice()));
        existing.setAvailable(coalesce(existing.getAvailable(), incoming.getAvailable()));
        existing.setPortionSize(coalesce(existing.getPortionSize(), incoming.getPortionSize()));
        existing.setImageUrl(coalesce(existing.getImageUrl(), incoming.getImageUrl()));
        existing.setRawSpec(mergeText(existing.getRawSpec(), incoming.getRawSpec()));
        existing.setRawJson(mergeRawJson(existing.getRawJson(), incoming.getRawJson()));
    }

    private static String findMergeKey(Map<String, MeituanMenuItem> itemMap, MeituanMenuItem incoming) {
        String incomingName = normalizeName(incoming.getName());
        String incomingCategory = normalizeName(incoming.getCategory());
        for (Map.Entry<String, MeituanMenuItem> entry : itemMap.entrySet()) {
            MeituanMenuItem existing = entry.getValue();
            if (!normalizeName(existing.getName()).equals(incomingName)) {
                continue;
            }
            String existingCategory = normalizeName(existing.getCategory());
            if (incomingCategory.isBlank() || existingCategory.isBlank() || existingCategory.equals(incomingCategory)) {
                return entry.getKey();
            }
        }
        return dedupeKey(incoming);
    }

    private static boolean shouldReplaceRestaurantName(String current, String candidate, String poiId) {
        String normalizedCandidate = trimToNull(candidate);
        if (normalizedCandidate == null || "店铺".equals(normalizedCandidate)) {
            return false;
        }
        String normalizedCurrent = trimToNull(current);
        if (normalizedCurrent == null) {
            return true;
        }
        if (poiId != null && normalizedCurrent.equals(poiId)) {
            return true;
        }
        if (normalizedCurrent.startsWith("美团店铺-")) {
            return true;
        }
        return looksLikeHash(normalizedCurrent) && !looksLikeHash(normalizedCandidate);
    }

    private static boolean looksLikeHash(String value) {
        return value != null
                && value.length() >= 16
                && value.matches("[A-Za-z0-9_-]+")
                && !value.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private static BigDecimal parsePrice(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.replace("¥", "")
                .replace("￥", "")
                .replace(",", "")
                .trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)?").matcher(normalized);
        if (!matcher.find()) {
            throw new NumberFormatException(rawValue);
        }
        return new BigDecimal(matcher.group()).stripTrailingZeros();
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String buildRawSpec(JsonNode node, JsonNode firstSku) {
        List<String> parts = new ArrayList<>();
        appendPart(parts, "unit", firstDirectValue(node, "unit"));
        appendPart(parts, "spec", firstDirectValue(node, "spec", "standard", "spec_desc"));
        appendPart(parts, "minOrder", firstDirectValue(node, "min_order_count", "minOrderCount"));
        if (firstSku != null) {
            appendPart(parts, "skuId", firstDirectValue(firstSku, "id", "sku_id", "skuId"));
            appendPart(parts, "skuName", firstDirectValue(firstSku, "name", "sku_name", "spec"));
            appendPart(parts, "skuPrice", firstPrice(firstSku) == null ? null : firstPrice(firstSku).toPlainString());
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static void appendPart(List<String> parts, String key, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(key + "=" + value);
        }
    }

    private static String mergeText(String existing, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return incoming;
        }
        if (existing.contains(incoming)) {
            return existing;
        }
        return existing + "; " + incoming;
    }

    private static String mergeRawJson(String existing, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return incoming;
        }
        if (existing.equals(incoming) || existing.contains(incoming)) {
            return existing;
        }
        return "{\"api\":" + existing + ",\"dom\":" + incoming + "}";
    }

    private static String normalizeName(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized.replaceAll("\\s+", "").toLowerCase();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }

    private static Double firstDouble(JsonNode node, String... keys) {
        String value = firstValue(node, keys);
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal firstBigDecimal(JsonNode node, String... keys) {
        String value = firstValue(node, keys);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).stripTrailingZeros();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean hasAnyArray(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.path(key).isArray()) {
                return true;
            }
        }
        return false;
    }

    private static String firstValue(JsonNode node, String... keys) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            String direct = firstDirectValue(node, keys);
            if (direct != null) {
                return direct;
            }
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                String nested = firstValue(values.next(), keys);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = firstValue(child, keys);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static String firstDirectValue(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static String dedupeKey(MeituanMenuItem item) {
        if (item.getExternalId() != null && !item.getExternalId().isBlank()) {
            return item.getExternalId();
        }
        return item.getName() + "|" + item.getPrice();
    }
}
