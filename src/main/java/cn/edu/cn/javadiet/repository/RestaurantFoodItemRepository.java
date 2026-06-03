package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantFoodItemRepository extends JpaRepository<RestaurantFoodItem, Long> {

    List<RestaurantFoodItem> findByRestaurantIdOrderByIdAsc(Long restaurantId);

    List<RestaurantFoodItem> findByExternalSource(String externalSource);

    List<RestaurantFoodItem> findByFoodItemId(Long foodItemId);

    List<RestaurantFoodItem> findByFoodItemIdIn(Collection<Long> foodItemIds);

    Optional<RestaurantFoodItem> findByRestaurantIdAndExternalSourceAndExternalFoodId(
            Long restaurantId,
            String externalSource,
            String externalFoodId);

    Optional<RestaurantFoodItem> findByRestaurantIdAndFoodItemId(Long restaurantId, Long foodItemId);
}
