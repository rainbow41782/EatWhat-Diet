package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.entity.FoodItem;
import java.util.List;
import java.util.Optional;

public interface FoodItemService {

    FoodItem save(FoodItem foodItem);

    Optional<FoodItem> findById(Long foodItemId);

    List<FoodItem> findAll(String keyword, String category, String nutritionStatus);

    void deleteById(Long foodItemId);
}
