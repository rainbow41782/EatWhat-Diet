package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.AdminDashboardStats;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantRepository;
import cn.edu.cn.javadiet.repository.UserFeedbackRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserFeedbackRepository userFeedbackRepository;

    public AdminDashboardController(
            UserRepository userRepository,
            FoodItemRepository foodItemRepository,
            RestaurantRepository restaurantRepository,
            UserFeedbackRepository userFeedbackRepository) {
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.userFeedbackRepository = userFeedbackRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> stats() {
        AdminDashboardStats stats = new AdminDashboardStats(
                userRepository.count(),
                foodItemRepository.count(),
                restaurantRepository.count(),
                userFeedbackRepository.count());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
