package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.FoodIntakeCreateRequest;
import cn.edu.cn.javadiet.model.dto.MealRecordCreateRequest;
import cn.edu.cn.javadiet.model.entity.DailyCheckIn;
import cn.edu.cn.javadiet.model.entity.FoodIntake;
import cn.edu.cn.javadiet.model.entity.MealRecord;
import cn.edu.cn.javadiet.model.entity.NutritionSummary;
import java.time.LocalDate;
import java.util.List;

public interface DietRecordService {

    DailyCheckIn getOrCreateDailyCheckIn(Long userId, LocalDate checkDate);

    List<DailyCheckIn> findDailyCheckIns(Long userId);

    MealRecord addMealRecord(Long userId, MealRecordCreateRequest request);

    FoodIntake addFoodIntake(Long userId, FoodIntakeCreateRequest request);

    NutritionSummary getDailyNutrition(Long userId, LocalDate checkDate);

    DailyCheckIn updateWaterIntake(Long userId, Double waterMl);

    List<MealRecord> findRecentMealRecords(Long userId, int limit);

    List<FoodIntake> getIntakesByMeal(Long userId, Long mealId);

    void deleteFoodIntake(Long userId, Long intakeId);

    FoodIntake updateFoodIntake(Long userId, Long intakeId, FoodIntakeCreateRequest request);

    void deleteMealRecord(Long userId, Long mealId);
}
