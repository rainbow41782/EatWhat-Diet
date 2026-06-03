package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.RecommendationRequest;
import cn.edu.cn.javadiet.model.entity.DailyCheckIn;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.model.entity.Recommendation;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserProfile;
import cn.edu.cn.javadiet.model.enums.RecommendationStatus;
import cn.edu.cn.javadiet.model.enums.RestaurantStatus;
import cn.edu.cn.javadiet.repository.DailyCheckInRepository;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.RecommendationRepository;
import cn.edu.cn.javadiet.repository.RestaurantRepository;
import cn.edu.cn.javadiet.repository.UserProfileRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import cn.edu.cn.javadiet.service.RecommendationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final double DEFAULT_MEAL_CALORIES = 600.0;
    private static final double DEFAULT_MEAL_PROTEIN = 25.0;
    private static final double DEFAULT_MEAL_FAT = 18.0;
    private static final double DEFAULT_MEAL_CARB = 75.0;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final DailyCheckInRepository dailyCheckInRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RecommendationRepository recommendationRepository;

    public RecommendationServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            DailyCheckInRepository dailyCheckInRepository,
            FoodItemRepository foodItemRepository,
            RestaurantRepository restaurantRepository,
            RecommendationRepository recommendationRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.dailyCheckInRepository = dailyCheckInRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Override
    @Transactional
    public List<Recommendation> generate(RecommendationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        DailyCheckIn today = findTodayCheckIn(user.getId());
        double targetCalories = remainingTarget(profile == null ? null : profile.getDailyCalorieTarget(),
                today == null ? null : today.getTotalCalories(),
                DEFAULT_MEAL_CALORIES);
        double targetProtein = remainingTarget(profile == null ? null : profile.getDailyProteinTarget(),
                today == null ? null : today.getTotalProtein(),
                DEFAULT_MEAL_PROTEIN);
        double targetFat = remainingTarget(profile == null ? null : profile.getDailyFatTarget(),
                today == null ? null : today.getTotalFat(),
                DEFAULT_MEAL_FAT);
        double targetCarb = remainingTarget(profile == null ? null : profile.getDailyCarbTarget(),
                today == null ? null : today.getTotalCarb(),
                DEFAULT_MEAL_CARB);

        List<FoodItem> foods = foodItemRepository.findAll().stream()
                .filter(food -> Boolean.TRUE.equals(food.getIsRecommended()) || food.getIsRecommended() == null)
                .filter(RecommendationServiceImpl::hasCompleteNutrition)
                .filter(food -> matchesProfile(food, profile))
                .sorted(Comparator.<FoodItem>comparingDouble(
                        food -> foodScore(food, targetCalories, targetProtein)).reversed())
                .limit(3)
                .toList();
        if (foods.isEmpty()) {
            return List.of();
        }

        List<Restaurant> restaurants = restaurantRepository.findAll().stream()
                .filter(restaurant -> restaurant.getStatus() == RestaurantStatus.OPEN)
                .filter(restaurant -> matchesBudget(restaurant, request.getMaxBudget()))
                .sorted(Comparator.comparing(
                        Restaurant::getRating,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        return foods.stream()
                .map(food -> {
                    Restaurant restaurant = restaurants.isEmpty() ? null : restaurants.get(0);
                    Recommendation recommendation = Recommendation.builder()
                            .userId(user.getId())
                            .restaurantId(restaurant == null ? null : restaurant.getId())
                            .recommendationTime(now)
                            .mealType(request.getTargetMealType())
                            .recommendedReason(buildReason(food, targetCalories, targetProtein))
                            .targetCalories(targetCalories)
                            .targetProtein(targetProtein)
                            .targetFat(targetFat)
                            .targetCarb(targetCarb)
                            .score(foodScore(food, targetCalories, targetProtein))
                            .status(RecommendationStatus.GENERATED)
                            .createdAt(now)
                            .build();
                    return recommendationRepository.save(recommendation);
                })
                .toList();
    }

    @Override
    public List<Recommendation> findHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("user not found");
        }
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Recommendation accept(Long recommendationId) {
        Recommendation recommendation = requireRecommendation(recommendationId);
        recommendation.setStatus(RecommendationStatus.ACCEPTED);
        return recommendationRepository.save(recommendation);
    }

    @Override
    @Transactional
    public Recommendation ignore(Long recommendationId) {
        Recommendation recommendation = requireRecommendation(recommendationId);
        recommendation.setStatus(RecommendationStatus.IGNORED);
        return recommendationRepository.save(recommendation);
    }

    private DailyCheckIn findTodayCheckIn(Long userId) {
        LocalDate today = LocalDate.now();
        return dailyCheckInRepository.findByUserIdAndCheckDate(userId, today).orElse(null);
    }

    private static double remainingTarget(Double target, Double current, double defaultValue) {
        if (target == null || target <= 0) {
            return defaultValue;
        }
        return Math.max(defaultValue * 0.4, target - valueOrZero(current));
    }

    private static boolean matchesProfile(FoodItem food, UserProfile profile) {
        if (profile == null) {
            return true;
        }
        return !containsAny(food.getName(), profile.getDislikedFoods())
                && !containsAny(food.getCategory(), profile.getDislikedFoods())
                && !containsAny(food.getAllergenInfo(), profile.getAllergies());
    }

    private static boolean matchesBudget(Restaurant restaurant, BigDecimal maxBudget) {
        return maxBudget == null || restaurant.getAvgPrice() == null
                || restaurant.getAvgPrice().compareTo(maxBudget) <= 0;
    }

    private static double foodScore(FoodItem food, double targetCalories, double targetProtein) {
        double calories = valueOrZero(food.getCaloriesPer100g());
        double protein = valueOrZero(food.getProteinPer100g());
        double calorieFit = 1.0 / (1.0 + Math.abs(targetCalories - calories));
        return protein * 2 + calorieFit * 100 + targetProtein * 0.05;
    }

    private static String buildReason(FoodItem food, double targetCalories, double targetProtein) {
        return "Recommended " + food.getName()
                + " to match the current calorie target "
                + Math.round(targetCalories)
                + " kcal and protein target "
                + Math.round(targetProtein)
                + " g.";
    }

    private Recommendation requireRecommendation(Long recommendationId) {
        return recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("recommendation not found"));
    }

    private static boolean containsAny(String source, String keywords) {
        if (source == null || keywords == null || keywords.isBlank()) {
            return false;
        }
        String normalizedSource = source.toLowerCase();
        for (String keyword : keywords.split("[,;，；\\s]+")) {
            if (!keyword.isBlank() && normalizedSource.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static boolean hasCompleteNutrition(FoodItem food) {
        return food.getCategory() != null && !food.getCategory().isBlank()
                && food.getCaloriesPer100g() != null
                && food.getProteinPer100g() != null
                && food.getFatPer100g() != null
                && food.getCarbPer100g() != null;
    }
}
