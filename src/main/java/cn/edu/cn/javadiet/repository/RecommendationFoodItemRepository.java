package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.RecommendationFoodItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationFoodItemRepository extends JpaRepository<RecommendationFoodItem, Long> {

    List<RecommendationFoodItem> findByRecommendationId(Long recommendationId);
}
