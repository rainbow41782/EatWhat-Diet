package cn.edu.cn.javadiet.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.cn.javadiet.config.FoodDataCentralProperties;
import cn.edu.cn.javadiet.config.LlmGatewayProperties;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyItem;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewJobStatus;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewResponse;
import cn.edu.cn.javadiet.model.dto.FoodNutritionReferenceCandidate;
import cn.edu.cn.javadiet.model.dto.FoodNutritionSuggestion;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantFoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FoodNutritionEnrichmentServiceImplTests {

    @Test
    void applyCompletesPendingFoodAndEnablesRecommendation() {
        FoodItemRepository foodItemRepository = mock(FoodItemRepository.class);
        FoodItem foodItem = FoodItem.builder()
                .id(1L)
                .name("薯薯成双")
                .nutritionStatus("PENDING")
                .isRecommended(false)
                .build();
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(foodItem));
        when(foodItemRepository.save(any(FoodItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FoodNutritionEnrichmentServiceImpl service = service(foodItemRepository);

        FoodNutritionApplyRequest request = new FoodNutritionApplyRequest();
        FoodNutritionApplyItem item = new FoodNutritionApplyItem();
        item.setFoodItemId(1L);
        item.setCategory("快餐");
        item.setCaloriesPer100g(312.2);
        item.setProteinPer100g(3.4);
        item.setFatPer100g(14.7);
        item.setCarbPer100g(41.4);
        request.setItems(List.of(item));

        service.apply(request);

        assertEquals("COMPLETE", foodItem.getNutritionStatus());
        assertEquals(true, foodItem.getIsRecommended());
        assertEquals(312.2, foodItem.getCaloriesPer100g());
        assertEquals(3.4, foodItem.getProteinPer100g());
    }

    @Test
    void applyDoesNotOverwriteCompleteFood() {
        FoodItemRepository foodItemRepository = mock(FoodItemRepository.class);
        FoodItem foodItem = FoodItem.builder()
                .id(1L)
                .name("完整食品")
                .category("快餐")
                .caloriesPer100g(300.0)
                .proteinPer100g(10.0)
                .fatPer100g(8.0)
                .carbPer100g(40.0)
                .nutritionStatus("COMPLETE")
                .build();
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(foodItem));
        FoodNutritionEnrichmentServiceImpl service = service(foodItemRepository);

        FoodNutritionApplyRequest request = new FoodNutritionApplyRequest();
        FoodNutritionApplyItem item = new FoodNutritionApplyItem();
        item.setFoodItemId(1L);
        item.setCategory("快餐");
        item.setCaloriesPer100g(312.0);
        item.setProteinPer100g(3.0);
        item.setFatPer100g(14.0);
        item.setCarbPer100g(41.0);
        request.setItems(List.of(item));

        assertThrows(IllegalArgumentException.class, () -> service.apply(request));
        verify(foodItemRepository, never()).save(any(FoodItem.class));
    }

    @Test
    void previewJobCanBeCancelledAfterCurrentItemFinishes() throws Exception {
        FoodItemRepository foodItemRepository = mock(FoodItemRepository.class);
        Map<Long, FoodItem> foods = new HashMap<>();
        foods.put(1L, pendingFood(1L, "食品1"));
        foods.put(2L, pendingFood(2L, "食品2"));
        foods.put(3L, pendingFood(3L, "食品3"));
        when(foodItemRepository.findById(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(foods.get(invocation.getArgument(0))));
        RestaurantFoodItemRepository menuRepository = mock(RestaurantFoodItemRepository.class);
        when(menuRepository.findByFoodItemId(anyLong())).thenReturn(List.of());
        FoodDataCentralClient fdcClient = mock(FoodDataCentralClient.class);
        when(fdcClient.search(anyString())).thenReturn(List.of());
        LlmGatewayClient llmClient = mock(LlmGatewayClient.class);
        when(llmClient.suggest(any(), any(), any())).thenAnswer(invocation -> {
            Thread.sleep(120);
            FoodItem food = invocation.getArgument(0);
            return FoodNutritionSuggestion.builder()
                    .foodItemId(food.getId())
                    .foodName(food.getName())
                    .category("快餐")
                    .caloriesPer100g(100.0)
                    .proteinPer100g(1.0)
                    .fatPer100g(2.0)
                    .carbPer100g(3.0)
                    .confidence(0.8)
                    .basis("test")
                    .build();
        });
        FoodNutritionEnrichmentServiceImpl service = new FoodNutritionEnrichmentServiceImpl(
                foodItemRepository,
                menuRepository,
                mock(RestaurantRepository.class),
                new FoodItemServiceImpl(foodItemRepository),
                fdcClient,
                llmClient,
                new LlmGatewayProperties(),
                new FoodDataCentralProperties());
        FoodNutritionPreviewRequest request = new FoodNutritionPreviewRequest();
        request.setFoodItemIds(List.of(1L, 2L, 3L));

        FoodNutritionPreviewJobStatus started = service.startPreviewJob(request);
        while (service.getPreviewJob(started.getJobId()).getCompletedCount() < 1) {
            Thread.sleep(20);
        }
        service.cancelPreviewJob(started.getJobId());
        FoodNutritionPreviewJobStatus status = service.getPreviewJob(started.getJobId());
        while ("RUNNING".equals(status.getStatus()) || "CANCELLING".equals(status.getStatus())) {
            Thread.sleep(20);
            status = service.getPreviewJob(started.getJobId());
        }

        assertEquals("CANCELLED", status.getStatus());
        assertTrue(status.getCompletedCount() < 3);
        assertEquals(status.getCompletedCount(), status.getItems().size());
    }

    @Test
    void previewUsesEnglishHintInsteadOfChineseRawSearchForShrimp() {
        FoodItemRepository foodItemRepository = mock(FoodItemRepository.class);
        FoodItem shrimpPaste = pendingFood(1L, "招牌大颗粒虾滑4颗");
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(shrimpPaste));
        RestaurantFoodItemRepository menuRepository = mock(RestaurantFoodItemRepository.class);
        when(menuRepository.findByFoodItemId(1L)).thenReturn(List.of());
        FoodDataCentralClient fdcClient = mock(FoodDataCentralClient.class);
        when(fdcClient.search(anyString())).thenReturn(List.of(FoodNutritionReferenceCandidate.builder()
                .fdcId(169190L)
                .description("Shrimp")
                .build()));
        LlmGatewayClient llmClient = mock(LlmGatewayClient.class);
        when(llmClient.suggest(any(), any(), any())).thenReturn(FoodNutritionSuggestion.builder()
                .foodItemId(1L)
                .foodName("招牌大颗粒虾滑4颗")
                .category("海鲜")
                .caloriesPer100g(92.0)
                .proteinPer100g(17.0)
                .fatPer100g(1.5)
                .carbPer100g(2.0)
                .confidence(0.8)
                .basis("test")
                .build());
        FoodNutritionEnrichmentServiceImpl service = new FoodNutritionEnrichmentServiceImpl(
                foodItemRepository,
                menuRepository,
                mock(RestaurantRepository.class),
                new FoodItemServiceImpl(foodItemRepository),
                fdcClient,
                llmClient,
                new LlmGatewayProperties(),
                new FoodDataCentralProperties());
        FoodNutritionPreviewRequest request = new FoodNutritionPreviewRequest();
        request.setFoodItemIds(List.of(1L));

        FoodNutritionPreviewResponse response = service.preview(request);

        assertEquals(1, response.getItems().size());
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(fdcClient).search(queryCaptor.capture());
        assertEquals(List.of("shrimp"), queryCaptor.getAllValues());
    }

    @Test
    void previewBuildsEnglishQueriesForCommonHotpotFoods() {
        assertSearchQueries("捞派肥牛3卷", List.of("beef", "beef slices"));
        assertSearchQueries("海底捞德式小麦啤酒500ml", List.of("wheat beer", "beer"));
        assertSearchQueries("【新客专享】捞派巴沙鱼6片", List.of("catfish", "fish"));
    }

    @Test
    void previewUsesLlmSearchQueriesBeforeDictionaryFallback() {
        FoodItemRepository foodItemRepository = mock(FoodItemRepository.class);
        FoodItem foodItem = pendingFood(1L, "捞派肥牛3卷");
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(foodItem));
        RestaurantFoodItemRepository menuRepository = mock(RestaurantFoodItemRepository.class);
        when(menuRepository.findByFoodItemId(1L)).thenReturn(List.of());
        FoodDataCentralClient fdcClient = mock(FoodDataCentralClient.class);
        when(fdcClient.search(anyString())).thenReturn(List.of());
        LlmGatewayClient llmClient = mock(LlmGatewayClient.class);
        when(llmClient.isConfigured()).thenReturn(true);
        when(llmClient.suggestSearchQueries(any(), any(), any())).thenReturn(List.of("sliced beef"));
        when(llmClient.suggest(any(), any(), any())).thenReturn(FoodNutritionSuggestion.builder()
                .foodItemId(1L)
                .foodName("捞派肥牛3卷")
                .category("肉类")
                .caloriesPer100g(250.0)
                .proteinPer100g(18.0)
                .fatPer100g(20.0)
                .carbPer100g(0.0)
                .confidence(0.6)
                .basis("test")
                .build());
        FoodNutritionEnrichmentServiceImpl service = new FoodNutritionEnrichmentServiceImpl(
                foodItemRepository,
                menuRepository,
                mock(RestaurantRepository.class),
                new FoodItemServiceImpl(foodItemRepository),
                fdcClient,
                llmClient,
                new LlmGatewayProperties(),
                new FoodDataCentralProperties());
        FoodNutritionPreviewRequest request = new FoodNutritionPreviewRequest();
        request.setFoodItemIds(List.of(1L));

        FoodNutritionPreviewResponse response = service.preview(request);

        assertEquals(List.of("sliced beef", "beef", "beef slices"), response.getItems().get(0).getSearchQueries());
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(fdcClient, org.mockito.Mockito.times(3)).search(queryCaptor.capture());
        assertEquals(List.of("sliced beef", "beef", "beef slices"), queryCaptor.getAllValues());
    }

    private static void assertSearchQueries(String foodName, List<String> expectedQueries) {
        FoodItemRepository foodItemRepository = mock(FoodItemRepository.class);
        FoodItem foodItem = pendingFood(1L, foodName);
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(foodItem));
        RestaurantFoodItemRepository menuRepository = mock(RestaurantFoodItemRepository.class);
        when(menuRepository.findByFoodItemId(1L)).thenReturn(List.of());
        FoodDataCentralClient fdcClient = mock(FoodDataCentralClient.class);
        when(fdcClient.search(anyString())).thenReturn(List.of());
        LlmGatewayClient llmClient = mock(LlmGatewayClient.class);
        when(llmClient.suggest(any(), any(), any())).thenReturn(FoodNutritionSuggestion.builder()
                .foodItemId(1L)
                .foodName(foodName)
                .category("待确认")
                .caloriesPer100g(100.0)
                .proteinPer100g(1.0)
                .fatPer100g(2.0)
                .carbPer100g(3.0)
                .confidence(0.35)
                .basis("test")
                .build());
        FoodNutritionEnrichmentServiceImpl service = new FoodNutritionEnrichmentServiceImpl(
                foodItemRepository,
                menuRepository,
                mock(RestaurantRepository.class),
                new FoodItemServiceImpl(foodItemRepository),
                fdcClient,
                llmClient,
                new LlmGatewayProperties(),
                new FoodDataCentralProperties());
        FoodNutritionPreviewRequest request = new FoodNutritionPreviewRequest();
        request.setFoodItemIds(List.of(1L));

        FoodNutritionPreviewResponse response = service.preview(request);

        assertEquals(expectedQueries, response.getItems().get(0).getSearchQueries());
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(fdcClient, org.mockito.Mockito.times(expectedQueries.size())).search(queryCaptor.capture());
        assertEquals(expectedQueries, queryCaptor.getAllValues());
    }

    private static FoodNutritionEnrichmentServiceImpl service(FoodItemRepository foodItemRepository) {
        return new FoodNutritionEnrichmentServiceImpl(
                foodItemRepository,
                mock(RestaurantFoodItemRepository.class),
                mock(RestaurantRepository.class),
                new FoodItemServiceImpl(foodItemRepository),
                mock(FoodDataCentralClient.class),
                mock(LlmGatewayClient.class),
                new LlmGatewayProperties(),
                new FoodDataCentralProperties());
    }

    private static FoodItem pendingFood(Long id, String name) {
        return FoodItem.builder()
                .id(id)
                .name(name)
                .nutritionStatus("PENDING")
                .build();
    }
}
