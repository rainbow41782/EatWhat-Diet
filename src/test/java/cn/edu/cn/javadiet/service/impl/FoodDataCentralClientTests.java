package cn.edu.cn.javadiet.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import cn.edu.cn.javadiet.config.FoodDataCentralProperties;
import cn.edu.cn.javadiet.model.dto.FoodNutritionReferenceCandidate;
import cn.edu.cn.javadiet.repository.FoodNutritionReferenceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class FoodDataCentralClientTests {

    @Test
    void parsesCoreNutrientsFromSearchResponse() {
        FoodDataCentralClient client = new FoodDataCentralClient(
                new FoodDataCentralProperties(),
                mock(FoodNutritionReferenceRepository.class));
        String body = """
                {
                  "foods": [
                    {
                      "fdcId": 170698,
                      "description": "Potatoes, french fried",
                      "dataType": "SR Legacy",
                      "foodCategory": "Fast Foods",
                      "foodNutrients": [
                        { "nutrientId": 1008, "nutrientNumber": "208", "nutrientName": "Energy", "unitName": "kcal", "value": 312 },
                        { "nutrientId": 1003, "nutrientNumber": "203", "nutrientName": "Protein", "unitName": "g", "value": 3.43 },
                        { "nutrientId": 1004, "nutrientNumber": "204", "nutrientName": "Total lipid (fat)", "unitName": "g", "value": 14.73 },
                        { "nutrientId": 1005, "nutrientNumber": "205", "nutrientName": "Carbohydrate, by difference", "unitName": "g", "value": 41.44 }
                      ]
                    }
                  ]
                }
                """;

        List<FoodNutritionReferenceCandidate> candidates = client.parseSearchResponse(body);

        FoodNutritionReferenceCandidate candidate = candidates.get(0);
        assertEquals(170698L, candidate.getFdcId());
        assertEquals("Potatoes, french fried", candidate.getDescription());
        assertEquals(312.0, candidate.getCaloriesPer100g());
        assertEquals(3.4, candidate.getProteinPer100g());
        assertEquals(14.7, candidate.getFatPer100g());
        assertEquals(41.4, candidate.getCarbPer100g());
    }
}
