package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.service.FoodItemService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FoodItemServiceImpl implements FoodItemService {

    private static final String NUTRITION_COMPLETE = "COMPLETE";
    private static final String NUTRITION_PENDING = "PENDING";

    private final FoodItemRepository foodItemRepository;

    public FoodItemServiceImpl(FoodItemRepository foodItemRepository) {
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    public FoodItem save(FoodItem foodItem) {
        LocalDateTime now = LocalDateTime.now();
        if (foodItem.getId() == null) {
            foodItem.setCreatedAt(now);
        }
        boolean nutritionComplete = hasCompleteNutrition(foodItem);
        foodItem.setNutritionStatus(nutritionComplete ? NUTRITION_COMPLETE : NUTRITION_PENDING);
        if (!nutritionComplete) {
            foodItem.setIsRecommended(false);
        } else if (foodItem.getIsRecommended() == null) {
            foodItem.setIsRecommended(true);
        }
        foodItem.setUpdatedAt(now);
        return foodItemRepository.save(foodItem);
    }

    @Override
    public Optional<FoodItem> findById(Long foodItemId) {
        return foodItemRepository.findById(foodItemId);
    }

    @Override
    public List<FoodItem> findAll(String keyword, String category, String nutritionStatus) {
        String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase();
        String normalizedCategory = category == null ? "" : category.toLowerCase();
        String normalizedNutritionStatus = nutritionStatus == null ? "" : nutritionStatus.toUpperCase();
        return foodItemRepository.findAll().stream()
                .filter(item -> normalizedKeyword.isBlank()
                        || contains(item.getName(), normalizedKeyword)
                        || contains(item.getDescription(), normalizedKeyword))
                .filter(item -> normalizedCategory.isBlank() || contains(item.getCategory(), normalizedCategory))
                .filter(item -> normalizedNutritionStatus.isBlank()
                        || normalizedNutritionStatus.equalsIgnoreCase(item.getNutritionStatus()))
                .sorted(Comparator.comparing(FoodItem::getId))
                .toList();
    }

    @Override
    public void deleteById(Long foodItemId) {
        if (!foodItemRepository.existsById(foodItemId)) {
            throw new IllegalArgumentException("food item not found");
        }
        foodItemRepository.deleteById(foodItemId);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private static boolean hasCompleteNutrition(FoodItem foodItem) {
        return foodItem.getCategory() != null && !foodItem.getCategory().isBlank()
                && foodItem.getCaloriesPer100g() != null
                && foodItem.getProteinPer100g() != null
                && foodItem.getFatPer100g() != null
                && foodItem.getCarbPer100g() != null;
    }
}
