package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.NearbyRestaurantQuery;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import cn.edu.cn.javadiet.model.enums.RestaurantStatus;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantFoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantRepository;
import cn.edu.cn.javadiet.service.LocationService;
import cn.edu.cn.javadiet.service.RestaurantService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantFoodItemRepository restaurantFoodItemRepository;
    private final LocationService locationService;

    public RestaurantServiceImpl(
            RestaurantRepository restaurantRepository,
            FoodItemRepository foodItemRepository,
            RestaurantFoodItemRepository restaurantFoodItemRepository,
            LocationService locationService) {
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantFoodItemRepository = restaurantFoodItemRepository;
        this.locationService = locationService;
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        LocalDateTime now = LocalDateTime.now();
        if (restaurant.getId() == null) {
            restaurant.setCreatedAt(now);
        }
        if (restaurant.getStatus() == null) {
            restaurant.setStatus(RestaurantStatus.OPEN);
        }
        restaurant.setUpdatedAt(now);
        return restaurantRepository.save(restaurant);
    }

    @Override
    public Optional<Restaurant> findById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId);
    }

    @Override
    public List<Restaurant> findNearby(NearbyRestaurantQuery query) {
        return restaurantRepository.findAll().stream()
                .filter(item -> !Boolean.TRUE.equals(query.getOnlyOpen()) || item.getStatus() == RestaurantStatus.OPEN)
                .filter(item -> item.getLatitude() != null && item.getLongitude() != null)
                .filter(item -> query.getMaxDistanceKm() == null
                        || distance(query, item) <= query.getMaxDistanceKm())
                .sorted(Comparator.comparingDouble(item -> distance(query, item)))
                .toList();
    }

    @Override
    public RestaurantFoodItem addMenuItem(RestaurantFoodItem menuItem) {
        if (!restaurantRepository.existsById(menuItem.getRestaurantId())) {
            throw new IllegalArgumentException("restaurant not found");
        }
        if (!foodItemRepository.existsById(menuItem.getFoodItemId())) {
            throw new IllegalArgumentException("food item not found");
        }
        if (menuItem.getAvailable() == null) {
            menuItem.setAvailable(true);
        }
        return restaurantFoodItemRepository.save(menuItem);
    }

    @Override
    public List<RestaurantFoodItem> findMenuItems(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException("restaurant not found");
        }
        return restaurantFoodItemRepository.findByRestaurantIdOrderByIdAsc(restaurantId);
    }

    private double distance(NearbyRestaurantQuery query, Restaurant restaurant) {
        return locationService.calculateDistanceKm(
                query.getLatitude(),
                query.getLongitude(),
                restaurant.getLatitude(),
                restaurant.getLongitude());
    }
}
