package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.NearbyRestaurantQuery;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import cn.edu.cn.javadiet.service.RestaurantService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Restaurant>> save(@RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.save(restaurant)));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<Restaurant>> get(@PathVariable Long restaurantId) {
        Restaurant restaurant = restaurantService.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("restaurant not found"));
        return ResponseEntity.ok(ApiResponse.ok(restaurant));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<Restaurant>>> nearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) Integer maxDistanceKm,
            @RequestParam(defaultValue = "true") Boolean onlyOpen) {
        NearbyRestaurantQuery query = new NearbyRestaurantQuery(latitude, longitude, maxDistanceKm, onlyOpen);
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.findNearby(query)));
    }

    @PostMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<RestaurantFoodItem>> addMenuItem(
            @PathVariable Long restaurantId,
            @RequestBody RestaurantFoodItem menuItem) {
        menuItem.setRestaurantId(restaurantId);
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.addMenuItem(menuItem)));
    }

    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<List<RestaurantFoodItem>>> listMenuItems(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.findMenuItems(restaurantId)));
    }
}
