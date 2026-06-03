package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.FoodIntakeCreateRequest;
import cn.edu.cn.javadiet.model.dto.MealRecordCreateRequest;
import cn.edu.cn.javadiet.model.entity.DailyCheckIn;
import cn.edu.cn.javadiet.model.entity.FoodIntake;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.model.entity.MealRecord;
import cn.edu.cn.javadiet.model.entity.NutritionSummary;
import cn.edu.cn.javadiet.repository.DailyCheckInRepository;
import cn.edu.cn.javadiet.repository.FoodIntakeRepository;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.MealRecordRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import cn.edu.cn.javadiet.service.DietRecordService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DietRecordServiceImpl implements DietRecordService {

    private final UserRepository userRepository;
    private final DailyCheckInRepository dailyCheckInRepository;
    private final MealRecordRepository mealRecordRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodIntakeRepository foodIntakeRepository;

    public DietRecordServiceImpl(
            UserRepository userRepository,
            DailyCheckInRepository dailyCheckInRepository,
            MealRecordRepository mealRecordRepository,
            FoodItemRepository foodItemRepository,
            FoodIntakeRepository foodIntakeRepository) {
        this.userRepository = userRepository;
        this.dailyCheckInRepository = dailyCheckInRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.foodItemRepository = foodItemRepository;
        this.foodIntakeRepository = foodIntakeRepository;
    }

    @Override
    @Transactional
    public DailyCheckIn getOrCreateDailyCheckIn(Long userId, LocalDate checkDate) {
        requireUser(userId);
        LocalDate date = checkDate == null ? LocalDate.now() : checkDate;
        return dailyCheckInRepository.findByUserIdAndCheckDate(userId, date)
                .orElseGet(() -> createDailyCheckIn(userId, date));
    }

    @Override
    public List<DailyCheckIn> findDailyCheckIns(Long userId) {
        requireUser(userId);
        return dailyCheckInRepository.findByUserIdOrderByCheckDateDesc(userId);
    }

    @Override
    @Transactional
    public MealRecord addMealRecord(Long userId, MealRecordCreateRequest request) {
        requireUser(userId);
        DailyCheckIn checkIn = request.getDailyCheckInId() == null
                ? getOrCreateDailyCheckIn(userId, LocalDate.now())
                : dailyCheckInRepository.findById(request.getDailyCheckInId()).orElse(null);
        if (checkIn == null || !userId.equals(checkIn.getUserId())) {
            throw new IllegalArgumentException("daily check-in not found");
        }

        MealRecord mealRecord = MealRecord.builder()
                .dailyCheckInId(checkIn.getId())
                .userId(userId)
                .mealType(request.getMealType())
                .mealTime(request.getMealTime() == null ? LocalDateTime.now() : request.getMealTime())
                .totalCalories(0.0)
                .totalProtein(0.0)
                .totalFat(0.0)
                .totalCarb(0.0)
                .remark(request.getRemark())
                .createdAt(LocalDateTime.now())
                .build();
        return mealRecordRepository.save(mealRecord);
    }

    @Override
    @Transactional
    public FoodIntake addFoodIntake(Long userId, FoodIntakeCreateRequest request) {
        requireUser(userId);
        MealRecord mealRecord = mealRecordRepository.findById(request.getMealRecordId()).orElse(null);
        if (mealRecord == null || !userId.equals(mealRecord.getUserId())) {
            throw new IllegalArgumentException("meal record not found");
        }

        FoodIntake foodIntake;
        if (request.getFoodItemId() != null) {
            // 从食物数据库查找并计算营养
            FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                    .orElseThrow(() -> new IllegalArgumentException("food item not found"));
            double quantity = valueOrZero(request.getQuantity());
            double ratio = quantity / 100.0;
            foodIntake = FoodIntake.builder()
                    .mealRecordId(mealRecord.getId())
                    .foodItemId(foodItem.getId())
                    .quantity(quantity)
                    .unit(request.getUnit())
                    .calories(valueOrZero(foodItem.getCaloriesPer100g()) * ratio)
                    .protein(valueOrZero(foodItem.getProteinPer100g()) * ratio)
                    .fat(valueOrZero(foodItem.getFatPer100g()) * ratio)
                    .carb(valueOrZero(foodItem.getCarbPer100g()) * ratio)
                    .fiber(valueOrZero(foodItem.getFiberPer100g()) * ratio)
                    .intakeTime(request.getIntakeTime() == null ? LocalDateTime.now() : request.getIntakeTime())
                    .remark(request.getRemark())
                    .build();
        } else {
            // 手动录入：直接使用前端传来的营养值
            foodIntake = FoodIntake.builder()
                    .mealRecordId(mealRecord.getId())
                    .foodItemId(null)
                    .quantity(request.getQuantity())
                    .unit(request.getUnit() != null ? request.getUnit() : "克")
                    .calories(valueOrZero(request.getCalories()))
                    .protein(valueOrZero(request.getProtein()))
                    .fat(valueOrZero(request.getFat()))
                    .carb(valueOrZero(request.getCarb()))
                    .fiber(0.0)
                    .remark(request.getName() != null ? request.getName() : request.getRemark())
                    .intakeTime(request.getIntakeTime() == null ? LocalDateTime.now() : request.getIntakeTime())
                    .build();
        }

        foodIntake = foodIntakeRepository.save(foodIntake);
        refreshMealTotal(mealRecord.getId());
        refreshDailyTotal(mealRecord.getDailyCheckInId());
        return foodIntake;
    }

    @Override
    @Transactional
    public NutritionSummary getDailyNutrition(Long userId, LocalDate checkDate) {
        DailyCheckIn checkIn = getOrCreateDailyCheckIn(userId, checkDate);
        refreshDailyTotal(checkIn.getId());
        return NutritionSummary.builder()
                .calories(checkIn.getTotalCalories())
                .protein(checkIn.getTotalProtein())
                .fat(checkIn.getTotalFat())
                .carb(checkIn.getTotalCarb())
                .fiber(checkIn.getTotalFiber())
                .waterIntakeMl(checkIn.getWaterIntake())
                .build();
    }

    @Override
    public DailyCheckIn updateWaterIntake(Long userId, Double waterMl) {
        requireUser(userId);
        DailyCheckIn checkIn = getOrCreateDailyCheckIn(userId, LocalDate.now());
        double current = checkIn.getWaterIntake() == null ? 0.0 : checkIn.getWaterIntake();
        checkIn.setWaterIntake(current + (waterMl == null ? 0.0 : waterMl));
        checkIn.setUpdatedAt(LocalDateTime.now());
        return dailyCheckInRepository.save(checkIn);
    }

    @Override
    public List<MealRecord> findRecentMealRecords(Long userId, int limit) {
        requireUser(userId);
        List<MealRecord> all = mealRecordRepository.findByUserIdOrderByMealTimeDesc(userId);
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    @Override
    public List<FoodIntake> getIntakesByMeal(Long userId, Long mealId) {
        MealRecord meal = mealRecordRepository.findById(mealId).orElse(null);
        if (meal == null || !userId.equals(meal.getUserId())) {
            throw new IllegalArgumentException("meal record not found");
        }
        return foodIntakeRepository.findByMealRecordId(mealId);
    }

    @Override
    @Transactional
    public void deleteFoodIntake(Long userId, Long intakeId) {
        FoodIntake intake = foodIntakeRepository.findById(intakeId).orElse(null);
        if (intake == null) return;
        MealRecord meal = mealRecordRepository.findById(intake.getMealRecordId()).orElse(null);
        if (meal == null || !userId.equals(meal.getUserId())) {
            throw new IllegalArgumentException("intake not found");
        }
        foodIntakeRepository.deleteById(intakeId);
        refreshMealTotal(meal.getId());
        refreshDailyTotal(meal.getDailyCheckInId());
    }

    @Override
    @Transactional
    public FoodIntake updateFoodIntake(Long userId, Long intakeId, FoodIntakeCreateRequest request) {
        FoodIntake intake = foodIntakeRepository.findById(intakeId).orElse(null);
        if (intake == null) throw new IllegalArgumentException("intake not found");
        MealRecord meal = mealRecordRepository.findById(intake.getMealRecordId()).orElse(null);
        if (meal == null || !userId.equals(meal.getUserId())) {
            throw new IllegalArgumentException("intake not found");
        }
        if (request.getRemark() != null) intake.setRemark(request.getRemark());
        if (request.getCalories() != null) intake.setCalories(request.getCalories());
        if (request.getProtein() != null) intake.setProtein(request.getProtein());
        if (request.getFat() != null) intake.setFat(request.getFat());
        if (request.getCarb() != null) intake.setCarb(request.getCarb());
        FoodIntake saved = foodIntakeRepository.save(intake);
        refreshMealTotal(meal.getId());
        refreshDailyTotal(meal.getDailyCheckInId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteMealRecord(Long userId, Long mealId) {
        MealRecord meal = mealRecordRepository.findById(mealId).orElse(null);
        if (meal == null || !userId.equals(meal.getUserId())) {
            throw new IllegalArgumentException("meal record not found");
        }
        Long dailyCheckInId = meal.getDailyCheckInId();
        List<FoodIntake> intakes = foodIntakeRepository.findByMealRecordId(mealId);
        foodIntakeRepository.deleteAll(intakes);
        mealRecordRepository.deleteById(mealId);
        refreshDailyTotal(dailyCheckInId);
    }

    private DailyCheckIn createDailyCheckIn(Long userId, LocalDate checkDate) {
        LocalDateTime now = LocalDateTime.now();
        DailyCheckIn checkIn = DailyCheckIn.builder()
                .userId(userId)
                .checkDate(checkDate)
                .totalCalories(0.0)
                .totalProtein(0.0)
                .totalFat(0.0)
                .totalCarb(0.0)
                .totalFiber(0.0)
                .waterIntake(0.0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return dailyCheckInRepository.save(checkIn);
    }

    private void refreshMealTotal(Long mealRecordId) {
        MealRecord mealRecord = mealRecordRepository.findById(mealRecordId).orElse(null);
        if (mealRecord == null) {
            return;
        }
        List<FoodIntake> intakes = foodIntakeRepository.findByMealRecordId(mealRecordId);
        mealRecord.setTotalCalories(sumCalories(intakes));
        mealRecord.setTotalProtein(sumProtein(intakes));
        mealRecord.setTotalFat(sumFat(intakes));
        mealRecord.setTotalCarb(sumCarb(intakes));
        mealRecordRepository.save(mealRecord);
    }

    private void refreshDailyTotal(Long dailyCheckInId) {
        DailyCheckIn checkIn = dailyCheckInRepository.findById(dailyCheckInId).orElse(null);
        if (checkIn == null) {
            return;
        }
        List<Long> mealIds = mealRecordRepository.findByDailyCheckInId(dailyCheckInId).stream()
                .map(MealRecord::getId)
                .toList();
        List<FoodIntake> intakes = mealIds.isEmpty()
                ? List.of()
                : foodIntakeRepository.findByMealRecordIdIn(mealIds);
        checkIn.setTotalCalories(sumCalories(intakes));
        checkIn.setTotalProtein(sumProtein(intakes));
        checkIn.setTotalFat(sumFat(intakes));
        checkIn.setTotalCarb(sumCarb(intakes));
        checkIn.setTotalFiber(intakes.stream().mapToDouble(item -> valueOrZero(item.getFiber())).sum());
        checkIn.setUpdatedAt(LocalDateTime.now());
        dailyCheckInRepository.save(checkIn);
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("user not found");
        }
    }

    private static double sumCalories(List<FoodIntake> intakes) {
        return intakes.stream().mapToDouble(item -> valueOrZero(item.getCalories())).sum();
    }

    private static double sumProtein(List<FoodIntake> intakes) {
        return intakes.stream().mapToDouble(item -> valueOrZero(item.getProtein())).sum();
    }

    private static double sumFat(List<FoodIntake> intakes) {
        return intakes.stream().mapToDouble(item -> valueOrZero(item.getFat())).sum();
    }

    private static double sumCarb(List<FoodIntake> intakes) {
        return intakes.stream().mapToDouble(item -> valueOrZero(item.getCarb())).sum();
    }

    private static double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }
}
