package cn.edu.cn.javadiet.config;

import cn.edu.cn.javadiet.model.dto.RegisterRequest;
import cn.edu.cn.javadiet.model.entity.BodyMeasurement;
import cn.edu.cn.javadiet.model.entity.DailyCheckIn;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.model.entity.MealRecord;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserProfile;
import cn.edu.cn.javadiet.model.enums.AccountStatus;
import cn.edu.cn.javadiet.model.enums.ActivityLevel;
import cn.edu.cn.javadiet.model.enums.Gender;
import cn.edu.cn.javadiet.model.enums.HealthGoal;
import cn.edu.cn.javadiet.model.enums.MealType;
import cn.edu.cn.javadiet.model.enums.UserRole;
import cn.edu.cn.javadiet.repository.BodyMeasurementRepository;
import cn.edu.cn.javadiet.repository.DailyCheckInRepository;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.MealRecordRepository;
import cn.edu.cn.javadiet.repository.UserProfileRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import cn.edu.cn.javadiet.service.FoodItemService;
import cn.edu.cn.javadiet.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final int USER_COUNT = 50;

    private final UserRepository userRepository;
    private final UserService userService;
    private final FoodItemRepository foodItemRepository;
    private final FoodItemService foodItemService;
    private final JdbcTemplate jdbcTemplate;
    private final UserProfileRepository userProfileRepository;
    private final DailyCheckInRepository dailyCheckInRepository;
    private final MealRecordRepository mealRecordRepository;
    private final BodyMeasurementRepository bodyMeasurementRepository;

    public DataInitializer(UserRepository userRepository, UserService userService,
            FoodItemRepository foodItemRepository, FoodItemService foodItemService,
            JdbcTemplate jdbcTemplate,
            UserProfileRepository userProfileRepository,
            DailyCheckInRepository dailyCheckInRepository,
            MealRecordRepository mealRecordRepository,
            BodyMeasurementRepository bodyMeasurementRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.foodItemRepository = foodItemRepository;
        this.foodItemService = foodItemService;
        this.jdbcTemplate = jdbcTemplate;
        this.userProfileRepository = userProfileRepository;
        this.dailyCheckInRepository = dailyCheckInRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.bodyMeasurementRepository = bodyMeasurementRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 修复 food_item_id 列：允许为 null（手动录入时不需要关联食物库）
        jdbcTemplate.execute("ALTER TABLE food_intakes MODIFY COLUMN food_item_id BIGINT NULL");

        // 确保管理员账户存在
        if (!userRepository.existsByUsernameIgnoreCase("admin")) {
            LocalDateTime now = LocalDateTime.now();
            User admin = User.builder()
                    .username("admin")
                    .passwordHash(hashPassword("admin123"))
                    .role(UserRole.ADMIN)
                    .nickname("管理员")
                    .status(AccountStatus.ENABLED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            userRepository.save(admin);
        }

        // 初始化测试用户
        if (userRepository.existsByUsernameIgnoreCase("user001")) {
            initFoodItems(); // 食物数据也在这里初始化
            initPlanDemoUser();
            return;
        }
        for (int index = 1; index <= USER_COUNT; index++) {
            Gender gender = index % 2 == 0 ? Gender.FEMALE : Gender.MALE;
            userService.register(new RegisterRequest(
                    "user" + String.format("%03d", index),
                    "123456",
                    "Test User " + index,
                    "1380000" + String.format("%04d", index),
                    "user" + String.format("%03d", index) + "@example.com",
                    gender,
                    18 + index % 30,
                    gender == Gender.MALE ? 170.0 + index % 12 : 158.0 + index % 10,
                    gender == Gender.MALE ? 62.0 + index % 18 : 48.0 + index % 14,
                    "Test Address " + index,
                    22.20 + index * 0.01,
                    113.90 + index * 0.01));
        }
        initFoodItems();
        initPlanDemoUser();
    }

    /**
     * 生成一个已完成完整计划周期、有规律记录的演示用户
     * 用于测试健康报告页和计划到期提示
     */
    private void initPlanDemoUser() {
        if (userRepository.existsByUsernameIgnoreCase("plantest")) {
            return;
        }

        // 计划周期：8周（HIGH_INTENSITY_FAT_LOSS 对应6周，这里选8周的 DAILY_FAT_LOSS 更有代表性）
        // planStartDate = 2026-03-31, planEndDate = 2026-05-25 (8周=56天，刚好已过期)
        LocalDate planStart = LocalDate.of(2026, 3, 31);
        LocalDate planEnd = LocalDate.of(2026, 5, 25);
        LocalDateTime now = LocalDateTime.now();

        // 创建用户
        User user = User.builder()
                .username("plantest")
                .passwordHash(hashPassword("test123"))
                .role(UserRole.USER)
                .nickname("晓雯")
                .gender(Gender.FEMALE)
                .age(26)
                .status(AccountStatus.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        user = userRepository.save(user);
        Long uid = user.getId();

        // 创建档案（目标：日常减脂，适中活跃）
        // TDEE ≈ 1700, 缺口 ≈ -160, 目标热量 1540
        UserProfile profile = UserProfile.builder()
                .userId(uid)
                .heightCm(163.0)
                .weightKg(60.0)
                .healthGoal(HealthGoal.DAILY_FAT_LOSS)
                .activityLevel(ActivityLevel.MODERATELY_ACTIVE)
                .dailyCalorieTarget(1540.0)
                .dailyProteinTarget(125.0)
                .dailyFatTarget(55.0)
                .dailyCarbTarget(150.0)
                .bmr(1380.0)
                .planStartDate(planStart)
                .planEndDate(planEnd)
                .updatedAt(now)
                .build();
        userProfileRepository.save(profile);

        // 为计划的每一天创建打卡记录（跳过几天模拟偶尔漏打）
        int totalDays = (int) planStart.datesUntil(planEnd).count();
        for (int i = 0; i < totalDays; i++) {
            // 每周日漏打一次（模拟真实情况）
            if (i % 7 == 6) continue;

            LocalDate day = planStart.plusDays(i);
            // 前期热量略高，后期随着适应逐渐接近目标
            double weekFactor = Math.max(0, 1.0 - (i / (double) totalDays) * 0.15);
            double cal = Math.round((1480 + (i % 5) * 20) * weekFactor);
            double pro = Math.round(118 + (i % 4) * 3);
            double fat = Math.round(52 + (i % 3) * 2);
            double carb = Math.round((cal - pro * 4 - fat * 9) / 4);
            if (carb < 0) carb = 0;

            DailyCheckIn ci = DailyCheckIn.builder()
                    .userId(uid)
                    .checkDate(day)
                    .totalCalories(cal)
                    .totalProtein(pro)
                    .totalFat(fat)
                    .totalCarb(carb)
                    .waterIntake(2000.0)
                    .build();
            ci = dailyCheckInRepository.save(ci);
            Long ciId = ci.getId();

            // 早/中/晚三餐，按 25:40:35 分配
            double[] ratios = {0.25, 0.40, 0.35};
            MealType[] types = {MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER};
            int[] hours = {7, 12, 18};
            for (int m = 0; m < 3; m++) {
                LocalDateTime mealTime = day.atTime(hours[m], 30);
                MealRecord mr = MealRecord.builder()
                        .dailyCheckInId(ciId)
                        .userId(uid)
                        .mealType(types[m])
                        .mealTime(mealTime)
                        .totalCalories(Math.round(cal * ratios[m] * 10.0) / 10.0)
                        .totalProtein(Math.round(pro * ratios[m] * 10.0) / 10.0)
                        .totalFat(Math.round(fat * ratios[m] * 10.0) / 10.0)
                        .totalCarb(Math.round(carb * ratios[m] * 10.0) / 10.0)
                        .createdAt(mealTime)
                        .build();
                mealRecordRepository.save(mr);
            }

            // 每7天记录一次身体数据（体重从60kg减到55.5kg，腰围从75cm减到68cm）
            if (i % 7 == 0) {
                double weekIdx = i / 7.0;
                double totalWeeks = totalDays / 7.0;
                double weightKg = 60.0 - (weekIdx / totalWeeks) * 4.5;
                double waistCm  = 75.0 - (weekIdx / totalWeeks) * 7.0;
                double hipCm    = 92.0 - (weekIdx / totalWeeks) * 4.0;
                double armCm    = 27.0 - (weekIdx / totalWeeks) * 1.5;
                BodyMeasurement bm = BodyMeasurement.builder()
                        .userId(uid)
                        .measureDate(day)
                        .weightKg(Math.round(weightKg * 10.0) / 10.0)
                        .waistCm(Math.round(waistCm * 10.0) / 10.0)
                        .hipCm(Math.round(hipCm * 10.0) / 10.0)
                        .armCm(Math.round(armCm * 10.0) / 10.0)
                        .createdAt(now)
                        .build();
                bodyMeasurementRepository.save(bm);
            }
        }
    }

    /** 初始化常见中国食物数据，每次启动时补充缺少的食物 */
    private void initFoodItems() {
        if (foodItemRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<FoodItem> foods = List.of(
            food("白米饭", "主食", 116.0, 2.6, 0.3, 25.6, 0.3, now),
            food("全麦面包", "主食", 246.0, 8.6, 3.4, 45.5, 4.2, now),
            food("馒头", "主食", 223.0, 7.0, 1.1, 47.0, 0.9, now),
            food("燕麦片", "主食", 379.0, 13.2, 6.9, 67.7, 10.6, now),
            food("面条（煮熟）", "主食", 137.0, 4.5, 0.6, 28.6, 0.9, now),
            food("鸡胸肉", "肉类", 133.0, 24.6, 3.4, 0.0, 0.0, now),
            food("猪里脊", "肉类", 155.0, 20.2, 7.9, 0.0, 0.0, now),
            food("牛肉（瘦）", "肉类", 121.0, 21.3, 3.7, 0.0, 0.0, now),
            food("鸡蛋", "蛋类", 144.0, 13.1, 8.8, 2.8, 0.0, now),
            food("三文鱼", "水产", 208.0, 20.0, 13.0, 0.0, 0.0, now),
            food("虾仁", "水产", 87.0, 18.6, 0.8, 1.7, 0.0, now),
            food("带鱼", "水产", 127.0, 17.7, 4.9, 3.1, 0.0, now),
            food("豆腐（北）", "豆制品", 98.0, 12.2, 4.8, 1.5, 0.4, now),
            food("豆浆", "豆制品", 33.0, 3.0, 1.8, 1.8, 0.1, now),
            food("花生", "坚果", 589.0, 23.9, 44.3, 21.7, 6.3, now),
            food("核桃", "坚果", 627.0, 14.9, 58.8, 19.1, 9.5, now),
            food("苹果", "水果", 53.0, 0.2, 0.2, 13.7, 1.7, now),
            food("香蕉", "水果", 91.0, 1.1, 0.2, 23.1, 1.7, now),
            food("橙子", "水果", 48.0, 0.9, 0.2, 11.9, 2.4, now),
            food("西兰花", "蔬菜", 34.0, 2.8, 0.4, 6.6, 2.6, now),
            food("菠菜", "蔬菜", 28.0, 2.6, 0.3, 4.5, 2.2, now),
            food("番茄", "蔬菜", 18.0, 0.9, 0.2, 3.9, 1.2, now),
            food("黄瓜", "蔬菜", 16.0, 0.8, 0.2, 3.0, 0.5, now),
            food("牛奶（全脂）", "奶类", 66.0, 3.3, 3.7, 4.9, 0.0, now),
            food("酸奶（低脂）", "奶类", 72.0, 3.5, 1.9, 10.0, 0.0, now),
            food("可乐（355ml）", "饮料", 140.0, 0.0, 0.0, 38.0, 0.0, now),
            food("绿茶（无糖）", "饮料", 2.0, 0.2, 0.0, 0.5, 0.0, now),
            food("炸鸡腿", "快餐", 277.0, 18.0, 17.0, 14.0, 0.5, now),
            food("汉堡（普通）", "快餐", 257.0, 13.0, 10.0, 28.0, 1.5, now),
            food("方便面（一包）", "快餐", 449.0, 10.0, 18.0, 63.0, 2.0, now)
        );
        foods.forEach(foodItemService::save);
    }

    private static FoodItem food(String name, String category,
            double cal, double protein, double fat, double carb, double fiber,
            LocalDateTime now) {
        return FoodItem.builder()
                .name(name)
                .category(category)
                .caloriesPer100g(cal)
                .proteinPer100g(protein)
                .fatPer100g(fat)
                .carbPer100g(carb)
                .fiberPer100g(fiber)
                .isRecommended(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("password hashing is unavailable");
        }
    }
}
