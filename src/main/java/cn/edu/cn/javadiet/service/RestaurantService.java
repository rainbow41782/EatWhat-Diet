package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.NearbyRestaurantQuery;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import java.util.List;
import java.util.Optional;

public interface RestaurantService {

    Restaurant save(Restaurant restaurant);

    Optional<Restaurant> findById(Long restaurantId);

    List<Restaurant> findNearby(NearbyRestaurantQuery query);

    RestaurantFoodItem addMenuItem(RestaurantFoodItem menuItem);

    List<RestaurantFoodItem> findMenuItems(Long restaurantId);
}
