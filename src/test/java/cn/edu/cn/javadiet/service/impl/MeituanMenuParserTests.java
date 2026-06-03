package cn.edu.cn.javadiet.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cn.edu.cn.javadiet.model.dto.MeituanCrawlResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeituanMenuParserTests {

    @Test
    void parsesMenuItemsFromCapturedFoodResponse() {
        String body = """
                {
                  "code": 0,
                  "data": {
                    "poi_id_str": "B2X9-JMSWo_VPH81kcYmiQI",
                    "poi_name": "海底捞测试店",
                    "food_spu_tags": [
                      {
                        "name": "热销",
                        "spus": [
                          {
                            "id": 123,
                            "name": "番茄牛肉饭",
                            "description": "一人份",
                            "price": 29.9,
                            "skus": [{ "id": 456, "price": 29.9 }],
                            "picture": "https://p0.meituan.net/test.png"
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        MeituanCrawlResult result = new MeituanMenuParser().parse(List.of(body));

        assertEquals("B2X9-JMSWo_VPH81kcYmiQI", result.getMeituanPoiId());
        assertEquals("海底捞测试店", result.getRestaurantName());
        assertFalse(result.getItems().isEmpty());
        assertEquals("番茄牛肉饭", result.getItems().get(0).getName());
        assertEquals("热销", result.getItems().get(0).getCategory());
        assertEquals("29.9", result.getItems().get(0).getPrice().toPlainString());
        assertEquals("456", result.getItems().get(0).getSkuId());
        assertEquals("https://p0.meituan.net/test.png", result.getItems().get(0).getImageUrl());
    }

    @Test
    void carriesAlternativeCategoryContainersToItems() {
        String body = """
                {
                  "data": {
                    "poi_id_str": "EWwMRw9dDZB6OvrK7ozaMgI",
                    "wm_poi_name": "麦当劳测试店",
                    "menus": [
                      {
                        "tag_name": "汉堡",
                        "product_spu_list": [
                          {
                            "spu_id": "spu-1",
                            "spu_name": "板烧鸡腿堡",
                            "min_price": "18.5",
                            "spec_desc": "1个",
                            "skus": [{ "sku_id": "sku-1", "price": "18.5", "spec": "单品" }]
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        MeituanCrawlResult result = new MeituanMenuParser().parse(List.of(body));

        assertEquals("EWwMRw9dDZB6OvrK7ozaMgI", result.getMeituanPoiId());
        assertEquals("麦当劳测试店", result.getRestaurantName());
        assertEquals("汉堡", result.getItems().get(0).getCategory());
        assertEquals("spu-1", result.getItems().get(0).getExternalId());
        assertEquals("sku-1", result.getItems().get(0).getSkuId());
        assertEquals("18.5", result.getItems().get(0).getPrice().toPlainString());
    }

    @Test
    void prefersDiscountPriceOverOriginalPrice() {
        String body = """
                {
                  "data": {
                    "poi_id_str": "qfw8n5FXwaB7fkCLSxNmIAI",
                    "spus": [
                      {
                        "spu_id": "spu-fries",
                        "spu_name": "薯薯成双",
                        "price": "35",
                        "origin_price": "35",
                        "activity_price": "19.9",
                        "skus": [{ "sku_id": "sku-fries", "price": "35", "activity_price": "19.9" }]
                      }
                    ]
                  }
                }
                """;

        MeituanCrawlResult result = new MeituanMenuParser().parse(List.of(body));

        assertEquals("19.9", result.getItems().get(0).getPrice().toPlainString());
    }

    @Test
    void prefersSkuDiscountPriceWhenProductOnlyHasOriginalPrice() {
        String body = """
                {
                  "data": {
                    "poi_id_str": "qfw8n5FXwaB7fkCLSxNmIAI",
                    "spus": [
                      {
                        "spu_id": "spu-fries",
                        "spu_name": "薯薯成双",
                        "price": "35",
                        "skus": [{ "sku_id": "sku-fries", "price": "35", "discount_price": "19.9" }]
                      }
                    ]
                  }
                }
                """;

        MeituanCrawlResult result = new MeituanMenuParser().parse(List.of(body));

        assertEquals("19.9", result.getItems().get(0).getPrice().toPlainString());
    }

    @Test
    void mergesDomRestaurantNameDiscountPriceAndDescription() {
        String apiBody = """
                {
                  "data": {
                    "poi_id_str": "qfw8n5FXwaB7fkCLSxNmIAI",
                    "poi_name": "qfw8n5FXwaB7fkCLSxNmIAI",
                    "food_spu_tags": [
                      {
                        "name": "折扣",
                        "spus": [
                          {
                            "spu_id": "spu-fries",
                            "spu_name": "薯薯成双",
                            "price": "35",
                            "skus": [{ "sku_id": "sku-fries", "price": "35" }]
                          }
                        ]
                      }
                    ]
                  }
                }
                """;
        String domSnapshot = """
                {
                  "source": "MEITUAN_DOM_SNAPSHOT",
                  "poiId": "qfw8n5FXwaB7fkCLSxNmIAI",
                  "restaurantName": "麦当劳＆麦咖啡（北京天通苑2店）",
                  "items": [
                    {
                      "source": "dom",
                      "name": "薯薯成双",
                      "category": "折扣",
                      "price": "19.9",
                      "originalPrice": "35",
                      "description": "精选优质马铃薯，口感细腻；波浪纹深度切割，炸至黄金酥脆。套餐包含：2份脆脆薯条。"
                    }
                  ]
                }
                """;

        MeituanCrawlResult result = new MeituanMenuParser().parse(List.of(apiBody, domSnapshot));

        assertEquals("麦当劳＆麦咖啡（北京天通苑2店）", result.getRestaurantName());
        assertEquals(1, result.getItems().size());
        assertEquals("spu-fries", result.getItems().get(0).getExternalId());
        assertEquals("19.9", result.getItems().get(0).getPrice().toPlainString());
        assertEquals("精选优质马铃薯，口感细腻；波浪纹深度切割，炸至黄金酥脆。套餐包含：2份脆脆薯条。",
                result.getItems().get(0).getDescription());
    }
}
