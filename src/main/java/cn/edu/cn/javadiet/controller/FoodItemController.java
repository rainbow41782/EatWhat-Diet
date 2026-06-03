package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.service.FoodItemService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foods")
public class FoodItemController {

    private final FoodItemService foodItemService;

    public FoodItemController(FoodItemService foodItemService) {
        this.foodItemService = foodItemService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FoodItem>> create(@RequestBody FoodItem foodItem) {
        return ResponseEntity.ok(ApiResponse.ok(foodItemService.save(foodItem)));
    }

    @PutMapping("/{foodItemId}")
    public ResponseEntity<ApiResponse<FoodItem>> update(
            @PathVariable Long foodItemId,
            @RequestBody FoodItem foodItem) {
        foodItem.setId(foodItemId);
        return ResponseEntity.ok(ApiResponse.ok(foodItemService.save(foodItem)));
    }

    @GetMapping("/{foodItemId}")
    public ResponseEntity<ApiResponse<FoodItem>> get(@PathVariable Long foodItemId) {
        FoodItem foodItem = foodItemService.findById(foodItemId)
                .orElseThrow(() -> new IllegalArgumentException("food item not found"));
        return ResponseEntity.ok(ApiResponse.ok(foodItem));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FoodItem>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String nutritionStatus) {
        return ResponseEntity.ok(ApiResponse.ok(foodItemService.findAll(keyword, category, nutritionStatus)));
    }

    @DeleteMapping("/{foodItemId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long foodItemId) {
        foodItemService.deleteById(foodItemId);
        return ResponseEntity.ok(ApiResponse.ok("deleted", null));
    }
}
