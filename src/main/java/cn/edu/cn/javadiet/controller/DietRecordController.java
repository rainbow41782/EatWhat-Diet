package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.FoodIntakeCreateRequest;
import cn.edu.cn.javadiet.model.dto.MealRecordCreateRequest;
import cn.edu.cn.javadiet.model.entity.DailyCheckIn;
import cn.edu.cn.javadiet.model.entity.FoodIntake;
import cn.edu.cn.javadiet.model.entity.MealRecord;
import cn.edu.cn.javadiet.model.entity.NutritionSummary;
import cn.edu.cn.javadiet.service.DietRecordService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diet")
public class DietRecordController {

    private final DietRecordService dietRecordService;

    public DietRecordController(DietRecordService dietRecordService) {
        this.dietRecordService = dietRecordService;
    }

    @PostMapping("/users/{userId}/check-ins")
    public ResponseEntity<ApiResponse<DailyCheckIn>> createCheckIn(
            @PathVariable Long userId,
            @RequestParam LocalDate checkDate) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.getOrCreateDailyCheckIn(userId, checkDate)));
    }

    @GetMapping("/users/{userId}/check-ins")
    public ResponseEntity<ApiResponse<List<DailyCheckIn>>> listCheckIns(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.findDailyCheckIns(userId)));
    }

    @PostMapping("/users/{userId}/meals")
    public ResponseEntity<ApiResponse<MealRecord>> addMealRecord(
            @PathVariable Long userId,
            @RequestBody MealRecordCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.addMealRecord(userId, request)));
    }

    @PostMapping("/users/{userId}/intakes")
    public ResponseEntity<ApiResponse<FoodIntake>> addFoodIntake(
            @PathVariable Long userId,
            @RequestBody FoodIntakeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.addFoodIntake(userId, request)));
    }

    @GetMapping("/users/{userId}/nutrition")
    public ResponseEntity<ApiResponse<NutritionSummary>> getDailyNutrition(
            @PathVariable Long userId,
            @RequestParam LocalDate checkDate) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.getDailyNutrition(userId, checkDate)));
    }

    @PatchMapping("/users/{userId}/water")
    public ResponseEntity<ApiResponse<DailyCheckIn>> addWaterIntake(
            @PathVariable Long userId,
            @RequestBody Map<String, Double> body) {
        Double waterMl = body.get("waterMl");
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.updateWaterIntake(userId, waterMl)));
    }

    @GetMapping("/users/{userId}/recent-meals")
    public ResponseEntity<ApiResponse<List<MealRecord>>> getRecentMeals(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.findRecentMealRecords(userId, limit)));
    }

    @GetMapping("/users/{userId}/meals/{mealId}/intakes")
    public ResponseEntity<ApiResponse<List<FoodIntake>>> getMealIntakes(
            @PathVariable Long userId, @PathVariable Long mealId) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.getIntakesByMeal(userId, mealId)));
    }

    @PutMapping("/users/{userId}/intakes/{intakeId}")
    public ResponseEntity<ApiResponse<FoodIntake>> updateFoodIntake(
            @PathVariable Long userId, @PathVariable Long intakeId,
            @RequestBody FoodIntakeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dietRecordService.updateFoodIntake(userId, intakeId, request)));
    }

    @DeleteMapping("/users/{userId}/intakes/{intakeId}")
    public ResponseEntity<ApiResponse<Void>> deleteFoodIntake(
            @PathVariable Long userId, @PathVariable Long intakeId) {
        dietRecordService.deleteFoodIntake(userId, intakeId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/users/{userId}/meals/{mealId}")
    public ResponseEntity<ApiResponse<Void>> deleteMealRecord(
            @PathVariable Long userId, @PathVariable Long mealId) {
        dietRecordService.deleteMealRecord(userId, mealId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
