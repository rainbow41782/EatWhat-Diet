package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.RecommendationFoodItemResponse;
import cn.edu.cn.javadiet.model.dto.RecommendationRequest;
import cn.edu.cn.javadiet.model.dto.RecommendationResponse;
import cn.edu.cn.javadiet.model.dto.RecommendationRestaurantResponse;
import cn.edu.cn.javadiet.model.entity.DailyCheckIn;
import cn.edu.cn.javadiet.model.entity.FoodIntake;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.model.entity.MealRecord;
import cn.edu.cn.javadiet.model.entity.Recommendation;
import cn.edu.cn.javadiet.model.entity.RecommendationFoodItem;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserProfile;
import cn.edu.cn.javadiet.model.enums.HealthGoal;
import cn.edu.cn.javadiet.model.enums.MealType;
import cn.edu.cn.javadiet.model.enums.RecommendationStatus;
import cn.edu.cn.javadiet.model.enums.RestaurantStatus;
import cn.edu.cn.javadiet.repository.DailyCheckInRepository;
import cn.edu.cn.javadiet.repository.FoodIntakeRepository;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.MealRecordRepository;
import cn.edu.cn.javadiet.repository.RecommendationFoodItemRepository;
import cn.edu.cn.javadiet.repository.RecommendationRepository;
import cn.edu.cn.javadiet.repository.RestaurantFoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantRepository;
import cn.edu.cn.javadiet.repository.UserProfileRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import cn.edu.cn.javadiet.service.RecommendationService;
import cn.edu.cn.javadiet.service.impl.LlmGatewayClient.RecommendationLlmChoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final double DEFAULT_DAILY_CALORIES = 2000.0;
    private static final double DEFAULT_DAILY_PROTEIN = 75.0;
    private static final double DEFAULT_DAILY_FAT = 60.0;
    private static final double DEFAULT_DAILY_CARB = 250.0;
    private static final double MIN_GRAMS = 80.0;
    private static final double MAX_GRAMS = 350.0;
    private static final int LLM_CANDIDATE_LIMIT = 12;
    private static final int LOCAL_CANDIDATE_LIMIT = 20;
    private static final int RESULT_LIMIT = 4;
    private static final Pattern GRAMS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(g|克|ml|毫升)", Pattern.CASE_INSENSITIVE);
    private static final EnumMap<MealType, Double> MEAL_WEIGHTS = new EnumMap<>(MealType.class);
    private static final List<MealType> MEAL_ORDER = List.of(
            MealType.BREAKFAST,
            MealType.LUNCH,
            MealType.DINNER,
            MealType.SNACK);

    static {
        MEAL_WEIGHTS.put(MealType.BREAKFAST, 0.25);
        MEAL_WEIGHTS.put(MealType.LUNCH, 0.35);
        MEAL_WEIGHTS.put(MealType.DINNER, 0.30);
        MEAL_WEIGHTS.put(MealType.SNACK, 0.10);
    }

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final DailyCheckInRepository dailyCheckInRepository;
    private final MealRecordRepository mealRecordRepository;
    private final FoodIntakeRepository foodIntakeRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantFoodItemRepository restaurantFoodItemRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationFoodItemRepository recommendationFoodItemRepository;
    private final LlmGatewayClient llmGatewayClient;

    public RecommendationServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            DailyCheckInRepository dailyCheckInRepository,
            MealRecordRepository mealRecordRepository,
            FoodIntakeRepository foodIntakeRepository,
            FoodItemRepository foodItemRepository,
            RestaurantRepository restaurantRepository,
            RestaurantFoodItemRepository restaurantFoodItemRepository,
            RecommendationRepository recommendationRepository,
            RecommendationFoodItemRepository recommendationFoodItemRepository,
            LlmGatewayClient llmGatewayClient) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.dailyCheckInRepository = dailyCheckInRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.foodIntakeRepository = foodIntakeRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantFoodItemRepository = restaurantFoodItemRepository;
        this.recommendationRepository = recommendationRepository;
        this.recommendationFoodItemRepository = recommendationFoodItemRepository;
        this.llmGatewayClient = llmGatewayClient;
    }

    @Override
    @Transactional
    public List<RecommendationResponse> generate(RecommendationRequest request) {
        User user = requireUser(request);
        MealType mealType = request.getTargetMealType() == null ? inferCurrentMealType() : request.getTargetMealType();
        return generateForMeal(request, user, mealType, UUID.randomUUID().toString(), Set.of());
    }

    @Override
    @Transactional
    public List<RecommendationResponse> generateDaily(RecommendationRequest request) {
        User user = requireUser(request);
        String batchId = UUID.randomUUID().toString();
        Set<Long> excludedFoodIds = new HashSet<>(currentFoodIds(user.getId()));
        List<RecommendationResponse> responses = new ArrayList<>();
        for (MealType mealType : MEAL_ORDER) {
            List<RecommendationResponse> mealResponses = generateForMeal(request, user, mealType, batchId, excludedFoodIds);
            mealResponses.stream()
                    .flatMap(response -> response.getItems().stream())
                    .map(RecommendationFoodItemResponse::getFoodItemId)
                    .filter(Objects::nonNull)
                    .forEach(excludedFoodIds::add);
            responses.addAll(mealResponses);
        }
        return sortResponsesForDisplay(responses);
    }

    @Override
    public List<RecommendationResponse> findCurrent(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("user not found");
        }
        return toResponses(currentRecommendations(userId));
    }

    @Override
    public List<RecommendationResponse> findHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("user not found");
        }
        return toResponses(recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional
    public RecommendationResponse accept(Long recommendationId) {
        Recommendation recommendation = requireRecommendation(recommendationId);
        recommendation.setStatus(RecommendationStatus.ACCEPTED);
        return firstResponse(recommendationRepository.save(recommendation));
    }

    @Override
    @Transactional
    public RecommendationResponse ignore(Long recommendationId) {
        Recommendation recommendation = requireRecommendation(recommendationId);
        recommendation.setStatus(RecommendationStatus.IGNORED);
        return firstResponse(recommendationRepository.save(recommendation));
    }

    private User requireUser(RecommendationRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private List<RecommendationResponse> generateForMeal(
            RecommendationRequest request,
            User user,
            MealType mealType,
            String batchId,
            Set<Long> excludedFoodIds) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        LocalDate today = LocalDate.now();
        DailyCheckIn todayCheckIn = dailyCheckInRepository.findByUserIdAndCheckDate(user.getId(), today).orElse(null);
        HistoryContext history = historyContext(user.getId(), today);
        MealTargets targets = mealTargets(profile, todayCheckIn, history.recordedMeals(), mealType);

        List<Candidate> localCandidates = prioritizeFreshCandidates(
                rankLocalCandidates(profile, request, history, targets, mealType),
                excludedFoodIds);
        if (localCandidates.isEmpty()) {
            return List.of();
        }

        List<Candidate> selected = llmEnhancedSelection(user, profile, history, targets, mealType, localCandidates);
        if (selected.isEmpty()) {
            selected = selectWithDiversity(localCandidates, RESULT_LIMIT);
        }

        LocalDateTime now = LocalDateTime.now();
        List<RecommendationResponse> responses = new ArrayList<>();
        for (Candidate candidate : selected) {
            candidate.finalScore = scoreWithDiversity(candidate, selected);
            Recommendation recommendation = recommendationRepository.save(Recommendation.builder()
                    .userId(user.getId())
                    .batchId(batchId)
                    .restaurantId(candidate.restaurant == null ? null : candidate.restaurant.getId())
                    .recommendationTime(now)
                    .mealType(mealType)
                    .recommendedReason(candidate.reason == null ? buildFallbackReason(candidate, targets) : candidate.reason)
                    .targetCalories(roundOne(targets.calories()))
                    .targetProtein(roundOne(targets.protein()))
                    .targetFat(roundOne(targets.fat()))
                    .targetCarb(roundOne(targets.carb()))
                    .score(roundTwo(candidate.finalScore))
                    .status(RecommendationStatus.GENERATED)
                    .createdAt(now)
                    .build());
            recommendationFoodItemRepository.save(RecommendationFoodItem.builder()
                    .recommendationId(recommendation.getId())
                    .foodItemId(candidate.food.getId())
                    .quantity(roundOne(candidate.suggestedGrams))
                    .unit("g")
                    .build());
            responses.add(toResponse(recommendation, List.of(candidate)));
        }
        return responses;
    }

    private List<Candidate> prioritizeFreshCandidates(List<Candidate> candidates, Set<Long> excludedFoodIds) {
        if (excludedFoodIds == null || excludedFoodIds.isEmpty() || candidates.isEmpty()) {
            return candidates;
        }
        List<Candidate> fresh = candidates.stream()
                .filter(candidate -> !excludedFoodIds.contains(candidate.food.getId()))
                .toList();
        List<Candidate> repeated = candidates.stream()
                .filter(candidate -> excludedFoodIds.contains(candidate.food.getId()))
                .map(Candidate::copy)
                .peek(candidate -> {
                    candidate.baseScore *= 0.55;
                    candidate.finalScore = candidate.baseScore;
                })
                .toList();
        if (fresh.size() >= RESULT_LIMIT || repeated.isEmpty()) {
            return fresh;
        }
        List<Candidate> merged = new ArrayList<>(fresh);
        merged.addAll(repeated);
        return merged.stream()
                .sorted(Comparator.comparingDouble((Candidate candidate) -> candidate.baseScore).reversed())
                .toList();
    }

    private List<Recommendation> currentRecommendations(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Recommendation> today = recommendationRepository
                .findByUserIdAndCreatedAtBetweenAndStatusInOrderByCreatedAtDesc(userId, start, end, activeStatuses());
        if (today.isEmpty()) {
            return List.of();
        }
        Optional<String> latestBatchId = today.stream()
                .map(Recommendation::getBatchId)
                .filter(RecommendationServiceImpl::notBlank)
                .findFirst();
        List<Recommendation> current = latestBatchId
                .map(batchId -> today.stream()
                        .filter(recommendation -> batchId.equals(recommendation.getBatchId()))
                        .toList())
                .orElseGet(() -> fallbackCurrentRecommendations(today));
        return sortRecommendationsForDisplay(current);
    }

    private Set<Long> currentFoodIds(Long userId) {
        List<Recommendation> current = currentRecommendations(userId);
        if (current.isEmpty()) {
            return Set.of();
        }
        List<Long> recommendationIds = current.stream().map(Recommendation::getId).toList();
        return recommendationFoodItemRepository.findByRecommendationIdIn(recommendationIds).stream()
                .map(RecommendationFoodItem::getFoodItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<Recommendation> fallbackCurrentRecommendations(List<Recommendation> today) {
        Map<MealType, List<Recommendation>> grouped = new EnumMap<>(MealType.class);
        for (Recommendation recommendation : today) {
            MealType mealType = recommendation.getMealType();
            if (mealType == null) {
                continue;
            }
            List<Recommendation> mealRecommendations = grouped.computeIfAbsent(mealType, key -> new ArrayList<>());
            if (mealRecommendations.size() < RESULT_LIMIT) {
                mealRecommendations.add(recommendation);
            }
        }
        return MEAL_ORDER.stream()
                .flatMap(mealType -> grouped.getOrDefault(mealType, List.of()).stream())
                .toList();
    }

    private List<Recommendation> sortRecommendationsForDisplay(List<Recommendation> recommendations) {
        return recommendations.stream()
                .sorted(Comparator
                        .comparingInt((Recommendation recommendation) -> mealOrderIndex(recommendation.getMealType()))
                        .thenComparing(Recommendation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Recommendation::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<RecommendationResponse> sortResponsesForDisplay(List<RecommendationResponse> responses) {
        return responses.stream()
                .sorted(Comparator
                        .comparingInt((RecommendationResponse response) -> mealOrderIndex(response.getMealType()))
                        .thenComparing(RecommendationResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RecommendationResponse::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<RecommendationStatus> activeStatuses() {
        return List.of(RecommendationStatus.GENERATED, RecommendationStatus.ACCEPTED);
    }

    private int mealOrderIndex(MealType mealType) {
        int index = mealType == null ? -1 : MEAL_ORDER.indexOf(mealType);
        return index < 0 ? MEAL_ORDER.size() : index;
    }

    private int mealOrderIndex(String mealType) {
        if (!notBlank(mealType)) {
            return MEAL_ORDER.size();
        }
        try {
            return mealOrderIndex(MealType.valueOf(mealType));
        } catch (IllegalArgumentException ex) {
            return MEAL_ORDER.size();
        }
    }

    private List<Candidate> rankLocalCandidates(
            UserProfile profile,
            RecommendationRequest request,
            HistoryContext history,
            MealTargets targets,
            MealType mealType) {
        Map<Long, List<RestaurantFoodItem>> menuByFood = restaurantFoodItemRepository.findAll().stream()
                .collect(Collectors.groupingBy(RestaurantFoodItem::getFoodItemId));
        Map<Long, Restaurant> restaurants = restaurantRepository.findAll().stream()
                .collect(Collectors.toMap(Restaurant::getId, restaurant -> restaurant, (a, b) -> a));

        List<Candidate> allCandidates = new ArrayList<>();
        for (FoodItem food : foodItemRepository.findAll()) {
            if (!isEligibleFood(food, profile)) {
                continue;
            }
            List<RestaurantFoodItem> menuItems = menuByFood.getOrDefault(food.getId(), List.of()).stream()
                    .filter(this::isAvailableMenuItem)
                    .filter(item -> matchesBudget(item, restaurants.get(item.getRestaurantId()), request.getMaxBudget()))
                    .toList();
            if (menuItems.isEmpty()) {
                if (!isMeituanFood(food)) {
                    allCandidates.add(scoreCandidate(new Candidate(food, null, null, defaultGrams(mealType)), profile, history, targets));
                }
                continue;
            }
            for (RestaurantFoodItem menuItem : menuItems) {
                Restaurant restaurant = restaurants.get(menuItem.getRestaurantId());
                if (!isOpenRestaurant(restaurant)) {
                    continue;
                }
                double grams = suggestedGrams(menuItem, mealType);
                allCandidates.add(scoreCandidate(new Candidate(food, menuItem, restaurant, grams), profile, history, targets));
            }
        }

        List<Candidate> sorted = allCandidates.stream()
                .sorted(Comparator.comparingDouble((Candidate candidate) -> candidate.baseScore).reversed())
                .toList();
        Map<Long, Candidate> bestByFood = new LinkedHashMap<>();
        for (Candidate candidate : sorted) {
            bestByFood.merge(candidate.food.getId(), candidate, this::betterCandidate);
        }
        return bestByFood.values().stream()
                .sorted(Comparator.comparingDouble((Candidate candidate) -> candidate.baseScore).reversed())
                .limit(LOCAL_CANDIDATE_LIMIT)
                .toList();
    }

    private List<Candidate> llmEnhancedSelection(
            User user,
            UserProfile profile,
            HistoryContext history,
            MealTargets targets,
            MealType mealType,
            List<Candidate> localCandidates) {
        List<Candidate> llmCandidates = localCandidates.stream().limit(LLM_CANDIDATE_LIMIT).toList();
        List<RecommendationLlmChoice> choices = llmGatewayClient.suggestRecommendations(
                buildLlmContext(user, profile, history, targets, mealType, llmCandidates));
        if (choices.isEmpty()) {
            return List.of();
        }

        Map<Long, Candidate> byFoodId = llmCandidates.stream()
                .collect(Collectors.toMap(candidate -> candidate.food.getId(), Candidate::copy, (a, b) -> a, LinkedHashMap::new));
        List<Candidate> selected = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (RecommendationLlmChoice choice : choices) {
            Candidate candidate = byFoodId.get(choice.foodItemId());
            if (candidate == null || !used.add(candidate.food.getId())) {
                continue;
            }
            if (choice.suggestedGrams() != null) {
                candidate.suggestedGrams = clamp(choice.suggestedGrams(), MIN_GRAMS, MAX_GRAMS);
                candidate.recalculateTotals();
            }
            candidate.reason = blankToNull(choice.reason());
            candidate.displayDescription = blankToNull(choice.displayDescription());
            selected.add(candidate);
            if (selected.size() >= RESULT_LIMIT) {
                break;
            }
        }
        if (selected.isEmpty()) {
            return List.of();
        }
        List<Candidate> fillers = selectWithDiversity(
                localCandidates.stream().filter(candidate -> !used.contains(candidate.food.getId())).toList(),
                RESULT_LIMIT - selected.size());
        selected.addAll(fillers);
        return selected;
    }

    private Map<String, Object> buildLlmContext(
            User user,
            UserProfile profile,
            HistoryContext history,
            MealTargets targets,
            MealType mealType,
            List<Candidate> candidates) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getId());
        payload.put("mealType", mealType.name());
        payload.put("healthGoal", profile == null || profile.getHealthGoal() == null ? null : profile.getHealthGoal().name());
        payload.put("dietPreference", profile == null ? null : profile.getDietPreference());
        payload.put("allergies", profile == null ? null : profile.getAllergies());
        payload.put("dislikedFoods", profile == null ? null : profile.getDislikedFoods());
        payload.put("mealTarget", Map.of(
                "calories", roundOne(targets.calories()),
                "protein", roundOne(targets.protein()),
                "fat", roundOne(targets.fat()),
                "carb", roundOne(targets.carb())));
        payload.put("todayConsumed", Map.of(
                "calories", roundOne(targets.consumedCalories()),
                "protein", roundOne(targets.consumedProtein()),
                "fat", roundOne(targets.consumedFat()),
                "carb", roundOne(targets.consumedCarb())));
        payload.put("recentMeals", history.recentMealSummaries());
        payload.put("candidates", candidates.stream().map(this::candidatePayload).toList());
        return payload;
    }

    private Map<String, Object> candidatePayload(Candidate candidate) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("foodItemId", candidate.food.getId());
        item.put("name", candidate.food.getName());
        item.put("category", candidate.category());
        item.put("description", candidate.food.getDescription());
        item.put("restaurantName", candidate.restaurant == null ? null : candidate.restaurant.getName());
        item.put("price", candidate.menuItem == null || candidate.menuItem.getPrice() == null
                ? null : candidate.menuItem.getPrice());
        item.put("portionSize", candidate.menuItem == null ? null : candidate.menuItem.getPortionSize());
        item.put("suggestedGrams", roundOne(candidate.suggestedGrams));
        item.put("localScore", roundTwo(candidate.baseScore));
        item.put("nutritionPer100g", Map.of(
                "calories", valueOrZero(candidate.food.getCaloriesPer100g()),
                "protein", valueOrZero(candidate.food.getProteinPer100g()),
                "fat", valueOrZero(candidate.food.getFatPer100g()),
                "carb", valueOrZero(candidate.food.getCarbPer100g())));
        item.put("estimatedNutrition", Map.of(
                "calories", roundOne(candidate.totalCalories),
                "protein", roundOne(candidate.totalProtein),
                "fat", roundOne(candidate.totalFat),
                "carb", roundOne(candidate.totalCarb)));
        return item;
    }

    private List<Candidate> selectWithDiversity(List<Candidate> candidates, int limit) {
        List<Candidate> remaining = candidates.stream().map(Candidate::copy).collect(Collectors.toCollection(ArrayList::new));
        List<Candidate> selected = new ArrayList<>();
        while (!remaining.isEmpty() && selected.size() < limit) {
            Candidate best = remaining.stream()
                    .max(Comparator.comparingDouble(candidate -> scoreWithDiversity(candidate, selected)))
                    .orElse(null);
            if (best == null) {
                break;
            }
            best.finalScore = scoreWithDiversity(best, selected);
            selected.add(best);
            remaining.remove(best);
        }
        return selected;
    }

    private Candidate scoreCandidate(Candidate candidate, UserProfile profile, HistoryContext history, MealTargets targets) {
        candidate.recalculateTotals();
        candidate.macroScore = macroFitScore(candidate, targets);
        candidate.goalScore = goalScore(candidate.food, profile == null ? null : profile.getHealthGoal());
        candidate.restaurantScore = restaurantScore(candidate.restaurant, candidate.menuItem);
        candidate.historyScore = historyScore(candidate.food, history.today(), history.lastFoodDate(), history.todayCategoryCounts());
        candidate.baseScore = candidate.macroScore * 0.55
                + candidate.goalScore * 0.20
                + candidate.restaurantScore * 0.10
                + candidate.historyScore * 0.05
                + 10.0;
        candidate.finalScore = candidate.baseScore;
        return candidate;
    }

    private double macroFitScore(Candidate candidate, MealTargets targets) {
        double calorieFit = componentFit(candidate.totalCalories, targets.calories());
        double proteinFit = componentFit(candidate.totalProtein, targets.protein());
        double fatFit = componentFit(candidate.totalFat, targets.fat());
        double carbFit = componentFit(candidate.totalCarb, targets.carb());
        return calorieFit * 0.35 + proteinFit * 0.30 + fatFit * 0.15 + carbFit * 0.20;
    }

    private double goalScore(FoodItem food, HealthGoal goal) {
        double calories = valueOrZero(food.getCaloriesPer100g());
        double protein = valueOrZero(food.getProteinPer100g());
        double carb = valueOrZero(food.getCarbPer100g());
        double fat = valueOrZero(food.getFatPer100g());
        double proteinScore = clamp(protein / 35.0 * 100.0, 0, 100);
        double lowCalScore = clamp((320.0 - calories) / 240.0 * 100.0, 0, 100);
        double carbModeration = 100.0 - clamp(Math.abs(carb - 35.0) / 35.0 * 100.0, 0, 100);
        double lowCarbScore = clamp((35.0 - carb) / 35.0 * 100.0, 0, 100);
        double fatModeration = 100.0 - clamp(Math.abs(fat - 12.0) / 20.0 * 100.0, 0, 100);
        if (goal == null) {
            return proteinScore * 0.35 + lowCalScore * 0.25 + carbModeration * 0.20 + fatModeration * 0.20;
        }
        return switch (goal) {
            case RAPID_FAT_LOSS, HIGH_INTENSITY_FAT_LOSS, DAILY_FAT_LOSS, FAT_LOSS ->
                    proteinScore * 0.55 + lowCalScore * 0.35 + fatModeration * 0.10;
            case LEAN_BULK, BULK, MUSCLE_GAIN, INCREASE_STRENGTH, IMPROVE_PERFORMANCE ->
                    proteinScore * 0.50 + carbModeration * 0.30 + fatModeration * 0.20;
            case BLOOD_SUGAR_CONTROL -> lowCarbScore * 0.55 + proteinScore * 0.25 + fatModeration * 0.20;
            case MAINTAIN -> proteinScore * 0.35 + lowCalScore * 0.25 + carbModeration * 0.20 + fatModeration * 0.20;
        };
    }

    private double restaurantScore(Restaurant restaurant, RestaurantFoodItem menuItem) {
        if (restaurant == null || menuItem == null) {
            return 45.0;
        }
        double ratingScore = restaurant.getRating() == null ? 55.0 : clamp(restaurant.getRating() / 5.0 * 100.0, 0, 100);
        double menuCompleteness = 45.0;
        if (menuItem.getPrice() != null) menuCompleteness += 20.0;
        if (notBlank(menuItem.getPortionSize())) menuCompleteness += 10.0;
        if (notBlank(menuItem.getImageUrl())) menuCompleteness += 10.0;
        if (notBlank(menuItem.getCategoryName())) menuCompleteness += 10.0;
        if (Boolean.FALSE.equals(menuItem.getAvailable())) menuCompleteness -= 35.0;
        return clamp(ratingScore * 0.55 + menuCompleteness * 0.45, 0, 100);
    }

    private double historyScore(
            FoodItem food,
            LocalDate today,
            Map<Long, LocalDate> lastFoodDate,
            Map<String, Integer> todayCategoryCounts) {
        double score = 100.0;
        LocalDate lastDate = lastFoodDate.get(food.getId());
        if (lastDate != null) {
            long days = ChronoUnit.DAYS.between(lastDate, today);
            if (days <= 0) {
                score = 8.0;
            } else if (days <= 3) {
                score = 18.0;
            } else if (days <= 7) {
                score = 45.0;
            } else if (days <= 14) {
                score = 72.0;
            }
        }
        String category = normalizeCategory(food.getCategory());
        int todayCategoryCount = todayCategoryCounts.getOrDefault(category, 0);
        if (todayCategoryCount > 0) {
            score -= Math.min(35.0, todayCategoryCount * 15.0);
        }
        return clamp(score, 0, 100);
    }

    private double scoreWithDiversity(Candidate candidate, Collection<Candidate> selected) {
        double diversity = 100.0;
        for (Candidate existing : selected) {
            if (candidate.restaurant != null && existing.restaurant != null
                    && Objects.equals(candidate.restaurant.getId(), existing.restaurant.getId())) {
                diversity -= 35.0;
            }
            if (Objects.equals(normalizeCategory(candidate.category()), normalizeCategory(existing.category()))) {
                diversity -= 25.0;
            }
        }
        diversity = clamp(diversity, 0, 100);
        return candidate.baseScore + diversity * 0.10;
    }

    private MealTargets mealTargets(
            UserProfile profile,
            DailyCheckIn today,
            Set<MealType> recordedMeals,
            MealType mealType) {
        double dailyCalories = dailyTarget(
                profile == null ? null : profile.getDailyCalorieTarget(),
                macroCalories(profile),
                DEFAULT_DAILY_CALORIES);
        double dailyProtein = dailyTarget(profile == null ? null : profile.getDailyProteinTarget(), null, DEFAULT_DAILY_PROTEIN);
        double dailyFat = dailyTarget(profile == null ? null : profile.getDailyFatTarget(), null, DEFAULT_DAILY_FAT);
        double dailyCarb = dailyTarget(profile == null ? null : profile.getDailyCarbTarget(), null, DEFAULT_DAILY_CARB);

        double consumedCalories = valueOrZero(today == null ? null : today.getTotalCalories());
        double consumedProtein = valueOrZero(today == null ? null : today.getTotalProtein());
        double consumedFat = valueOrZero(today == null ? null : today.getTotalFat());
        double consumedCarb = valueOrZero(today == null ? null : today.getTotalCarb());

        return new MealTargets(
                mealTarget(dailyCalories, consumedCalories, recordedMeals, mealType),
                mealTarget(dailyProtein, consumedProtein, recordedMeals, mealType),
                mealTarget(dailyFat, consumedFat, recordedMeals, mealType),
                mealTarget(dailyCarb, consumedCarb, recordedMeals, mealType),
                consumedCalories,
                consumedProtein,
                consumedFat,
                consumedCarb);
    }

    private double mealTarget(double dailyTarget, double consumed, Set<MealType> recordedMeals, MealType mealType) {
        double currentWeight = MEAL_WEIGHTS.getOrDefault(mealType, 0.30);
        double standard = dailyTarget * currentWeight;
        double minimum = standard * 0.40;
        double remaining = Math.max(0.0, dailyTarget - consumed);
        double remainingWeight = remainingWeight(recordedMeals, mealType);
        double share = remainingWeight <= 0 ? 1.0 : currentWeight / remainingWeight;
        return Math.max(minimum, remaining * share);
    }

    private double remainingWeight(Set<MealType> recordedMeals, MealType mealType) {
        int currentIndex = MEAL_ORDER.indexOf(mealType);
        double total = 0.0;
        for (int i = Math.max(currentIndex, 0); i < MEAL_ORDER.size(); i++) {
            MealType candidate = MEAL_ORDER.get(i);
            if (!recordedMeals.contains(candidate)) {
                total += MEAL_WEIGHTS.getOrDefault(candidate, 0.0);
            }
        }
        return total <= 0 ? MEAL_WEIGHTS.getOrDefault(mealType, 0.30) : total;
    }

    private HistoryContext historyContext(Long userId, LocalDate today) {
        List<DailyCheckIn> recentCheckIns = dailyCheckInRepository.findByUserIdOrderByCheckDateDesc(userId).stream()
                .filter(checkIn -> checkIn.getCheckDate() != null && !checkIn.getCheckDate().isBefore(today.minusDays(14)))
                .toList();
        if (recentCheckIns.isEmpty()) {
            return new HistoryContext(today, Map.of(), Map.of(), Set.of(), List.of());
        }
        Map<Long, LocalDate> dateByCheckInId = recentCheckIns.stream()
                .collect(Collectors.toMap(DailyCheckIn::getId, DailyCheckIn::getCheckDate));
        List<MealRecord> meals = mealRecordRepository.findByDailyCheckInIdIn(dateByCheckInId.keySet());
        Set<MealType> recordedToday = meals.stream()
                .filter(meal -> today.equals(dateByCheckInId.get(meal.getDailyCheckInId())))
                .map(MealRecord::getMealType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (meals.isEmpty()) {
            return new HistoryContext(today, Map.of(), Map.of(), recordedToday, List.of());
        }

        Map<Long, MealRecord> mealsById = meals.stream().collect(Collectors.toMap(MealRecord::getId, meal -> meal));
        List<FoodIntake> intakes = foodIntakeRepository.findByMealRecordIdIn(mealsById.keySet());
        Set<Long> foodIds = intakes.stream()
                .map(FoodIntake::getFoodItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, FoodItem> foods = foodIds.isEmpty() ? Map.of() : foodItemRepository.findAllById(foodIds).stream()
                .collect(Collectors.toMap(FoodItem::getId, food -> food));
        Map<Long, LocalDate> lastFoodDate = new HashMap<>();
        Map<String, Integer> todayCategoryCounts = new HashMap<>();
        for (FoodIntake intake : intakes) {
            if (intake.getFoodItemId() == null) {
                continue;
            }
            MealRecord meal = mealsById.get(intake.getMealRecordId());
            if (meal == null) {
                continue;
            }
            LocalDate date = dateByCheckInId.get(meal.getDailyCheckInId());
            if (date == null) {
                date = meal.getMealTime() == null ? today : meal.getMealTime().toLocalDate();
            }
            lastFoodDate.merge(intake.getFoodItemId(), date, (a, b) -> a.isAfter(b) ? a : b);
            if (today.equals(date)) {
                FoodItem food = foods.get(intake.getFoodItemId());
                if (food != null) {
                    String category = normalizeCategory(food.getCategory());
                    todayCategoryCounts.put(category, todayCategoryCounts.getOrDefault(category, 0) + 1);
                }
            }
        }
        List<Map<String, Object>> summaries = meals.stream()
                .sorted(Comparator.comparing(MealRecord::getMealTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .map(meal -> recentMealSummary(meal, dateByCheckInId.get(meal.getDailyCheckInId())))
                .toList();
        return new HistoryContext(today, lastFoodDate, todayCategoryCounts, recordedToday, summaries);
    }

    private Map<String, Object> recentMealSummary(MealRecord meal, LocalDate date) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("date", date == null ? null : date.toString());
        summary.put("mealType", meal.getMealType() == null ? null : meal.getMealType().name());
        summary.put("calories", roundOne(valueOrZero(meal.getTotalCalories())));
        summary.put("protein", roundOne(valueOrZero(meal.getTotalProtein())));
        summary.put("fat", roundOne(valueOrZero(meal.getTotalFat())));
        summary.put("carb", roundOne(valueOrZero(meal.getTotalCarb())));
        return summary;
    }

    private RecommendationResponse firstResponse(Recommendation recommendation) {
        return toResponses(List.of(recommendation)).stream()
                .findFirst()
                .orElseGet(() -> toResponse(recommendation, List.of()));
    }

    private List<RecommendationResponse> toResponses(List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of();
        }

        Map<Long, Recommendation> recommendationsById = recommendations.stream()
                .collect(Collectors.toMap(Recommendation::getId, recommendation -> recommendation, (a, b) -> a, LinkedHashMap::new));
        List<Long> recommendationIds = recommendationsById.keySet().stream().toList();
        List<RecommendationFoodItem> links = recommendationFoodItemRepository.findByRecommendationIdIn(recommendationIds);

        Set<Long> foodIds = links.stream()
                .map(RecommendationFoodItem::getFoodItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, FoodItem> foods = foodIds.isEmpty()
                ? Map.of()
                : foodItemRepository.findAllById(foodIds).stream()
                        .collect(Collectors.toMap(FoodItem::getId, food -> food, (a, b) -> a));

        List<RestaurantFoodItem> menuItems = foodIds.isEmpty()
                ? List.of()
                : restaurantFoodItemRepository.findByFoodItemIdIn(foodIds);
        Map<Long, List<RestaurantFoodItem>> menuByFood = menuItems.stream()
                .collect(Collectors.groupingBy(RestaurantFoodItem::getFoodItemId));
        Map<String, RestaurantFoodItem> menuByRestaurantAndFood = menuItems.stream()
                .collect(Collectors.toMap(
                        item -> menuKey(item.getRestaurantId(), item.getFoodItemId()),
                        item -> item,
                        this::betterMenuItem,
                        LinkedHashMap::new));

        Set<Long> restaurantIds = new HashSet<>();
        recommendations.stream()
                .map(Recommendation::getRestaurantId)
                .filter(Objects::nonNull)
                .forEach(restaurantIds::add);
        menuItems.stream()
                .map(RestaurantFoodItem::getRestaurantId)
                .filter(Objects::nonNull)
                .forEach(restaurantIds::add);
        Map<Long, Restaurant> restaurants = restaurantIds.isEmpty()
                ? Map.of()
                : restaurantRepository.findAllById(restaurantIds).stream()
                        .collect(Collectors.toMap(Restaurant::getId, restaurant -> restaurant, (a, b) -> a));

        Map<Long, List<Candidate>> candidatesByRecommendation = new HashMap<>();
        for (RecommendationFoodItem link : links) {
            Recommendation recommendation = recommendationsById.get(link.getRecommendationId());
            FoodItem food = foods.get(link.getFoodItemId());
            if (recommendation == null || food == null) {
                continue;
            }
            Restaurant restaurant = recommendation.getRestaurantId() == null
                    ? null
                    : restaurants.get(recommendation.getRestaurantId());
            RestaurantFoodItem menuItem = restaurant == null
                    ? null
                    : menuByRestaurantAndFood.get(menuKey(restaurant.getId(), food.getId()));
            if (menuItem == null) {
                menuItem = menuByFood.getOrDefault(food.getId(), List.of()).stream()
                        .reduce(this::betterMenuItem)
                        .orElse(null);
            }
            if (restaurant == null && menuItem != null) {
                restaurant = restaurants.get(menuItem.getRestaurantId());
            }
            double grams = link.getQuantity() == null ? 100.0 : link.getQuantity();
            Candidate candidate = new Candidate(food, menuItem, restaurant, grams);
            candidate.reason = recommendation.getRecommendedReason();
            candidate.recalculateTotals();
            candidatesByRecommendation
                    .computeIfAbsent(recommendation.getId(), key -> new ArrayList<>())
                    .add(candidate);
        }

        return recommendations.stream()
                .map(recommendation -> toResponse(
                        recommendation,
                        candidatesByRecommendation.getOrDefault(recommendation.getId(), List.of())))
                .toList();
    }

    private RecommendationResponse toResponse(Recommendation recommendation, List<Candidate> candidates) {
        RecommendationRestaurantResponse restaurantResponse = candidates.stream()
                .map(candidate -> candidate.restaurant)
                .filter(Objects::nonNull)
                .findFirst()
                .map(this::toRestaurantResponse)
                .orElseGet(() -> recommendation.getRestaurantId() == null
                        ? null
                        : restaurantRepository.findById(recommendation.getRestaurantId()).map(this::toRestaurantResponse).orElse(null));
        List<RecommendationFoodItemResponse> itemResponses = candidates.stream()
                .map(this::toFoodItemResponse)
                .toList();
        RecommendationFoodItemResponse firstItem = itemResponses.isEmpty() ? null : itemResponses.get(0);
        return RecommendationResponse.builder()
                .id(recommendation.getId())
                .userId(recommendation.getUserId())
                .batchId(recommendation.getBatchId())
                .restaurantId(recommendation.getRestaurantId())
                .recommendationTime(recommendation.getRecommendationTime())
                .mealType(recommendation.getMealType() == null ? null : recommendation.getMealType().name())
                .recommendedReason(recommendation.getRecommendedReason())
                .targetCalories(recommendation.getTargetCalories())
                .targetProtein(recommendation.getTargetProtein())
                .targetFat(recommendation.getTargetFat())
                .targetCarb(recommendation.getTargetCarb())
                .score(recommendation.getScore())
                .status(recommendation.getStatus() == null ? null : recommendation.getStatus().name())
                .createdAt(recommendation.getCreatedAt())
                .foodItemName(firstItem == null ? null : firstItem.getName())
                .totalCalories(firstItem == null ? null : firstItem.getTotalCalories())
                .restaurant(restaurantResponse)
                .items(itemResponses)
                .build();
    }

    private RecommendationFoodItemResponse toFoodItemResponse(Candidate candidate) {
        return RecommendationFoodItemResponse.builder()
                .foodItemId(candidate.food.getId())
                .name(candidate.food.getName())
                .category(candidate.category())
                .description(candidate.displayDescription == null ? candidate.food.getDescription() : candidate.displayDescription)
                .caloriesPer100g(candidate.food.getCaloriesPer100g())
                .proteinPer100g(candidate.food.getProteinPer100g())
                .fatPer100g(candidate.food.getFatPer100g())
                .carbPer100g(candidate.food.getCarbPer100g())
                .suggestedGrams(roundOne(candidate.suggestedGrams))
                .unit("g")
                .totalCalories(roundOne(candidate.totalCalories))
                .totalProtein(roundOne(candidate.totalProtein))
                .totalFat(roundOne(candidate.totalFat))
                .totalCarb(roundOne(candidate.totalCarb))
                .price(candidate.menuItem == null ? null : candidate.menuItem.getPrice())
                .portionSize(candidate.menuItem == null ? null : candidate.menuItem.getPortionSize())
                .menuCategory(candidate.menuItem == null ? null : candidate.menuItem.getCategoryName())
                .imageUrl(candidate.menuItem == null ? null : candidate.menuItem.getImageUrl())
                .restaurantId(candidate.restaurant == null ? null : candidate.restaurant.getId())
                .restaurantName(candidate.restaurant == null ? null : candidate.restaurant.getName())
                .restaurantAddress(candidate.restaurant == null ? null : candidate.restaurant.getAddress())
                .build();
    }

    private RecommendationRestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        return RecommendationRestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .rating(restaurant.getRating())
                .avgPrice(restaurant.getAvgPrice())
                .tags(restaurant.getTags())
                .build();
    }

    private String buildFallbackReason(Candidate candidate, MealTargets targets) {
        return "推荐 " + candidate.food.getName()
                + "：当前餐目标约 "
                + Math.round(targets.calories())
                + " kcal，"
                + "这道菜按建议份量约 "
                + Math.round(candidate.totalCalories)
                + " kcal，蛋白质约 "
                + roundOne(candidate.totalProtein)
                + "g，适合补齐本餐营养缺口。";
    }

    private Candidate betterCandidate(Candidate first, Candidate second) {
        double firstCompleteness = menuCompleteness(first);
        double secondCompleteness = menuCompleteness(second);
        if (Math.abs(firstCompleteness - secondCompleteness) > 0.01) {
            return firstCompleteness > secondCompleteness ? first : second;
        }
        return first.baseScore >= second.baseScore ? first : second;
    }

    private double menuCompleteness(Candidate candidate) {
        if (candidate.menuItem == null) {
            return 0.0;
        }
        double score = 30.0;
        if (candidate.menuItem.getPrice() != null) score += 20.0;
        if (notBlank(candidate.menuItem.getPortionSize())) score += 15.0;
        if (notBlank(candidate.menuItem.getImageUrl())) score += 15.0;
        if (notBlank(candidate.menuItem.getCategoryName())) score += 10.0;
        if (candidate.restaurant != null && notBlank(candidate.restaurant.getName())) score += 10.0;
        return score;
    }

    private RestaurantFoodItem betterMenuItem(RestaurantFoodItem first, RestaurantFoodItem second) {
        return menuItemCompleteness(first) >= menuItemCompleteness(second) ? first : second;
    }

    private double menuItemCompleteness(RestaurantFoodItem menuItem) {
        if (menuItem == null) {
            return 0.0;
        }
        double score = Boolean.FALSE.equals(menuItem.getAvailable()) ? 0.0 : 20.0;
        if (menuItem.getPrice() != null) score += 20.0;
        if (notBlank(menuItem.getPortionSize())) score += 15.0;
        if (notBlank(menuItem.getImageUrl())) score += 15.0;
        if (notBlank(menuItem.getCategoryName())) score += 10.0;
        if (notBlank(menuItem.getExternalFoodId())) score += 10.0;
        if (notBlank(menuItem.getSkuId())) score += 5.0;
        return score;
    }

    private String menuKey(Long restaurantId, Long foodItemId) {
        return String.valueOf(restaurantId) + ":" + String.valueOf(foodItemId);
    }

    private boolean isEligibleFood(FoodItem food, UserProfile profile) {
        if (food == null || !hasCompleteNutrition(food)) {
            return false;
        }
        if (Boolean.FALSE.equals(food.getIsRecommended())) {
            return false;
        }
        if ("PENDING".equalsIgnoreCase(blankToNull(food.getNutritionStatus()))) {
            return false;
        }
        if (!matchesProfile(food, profile)) {
            return false;
        }
        return true;
    }

    private boolean matchesProfile(FoodItem food, UserProfile profile) {
        if (profile == null) {
            return true;
        }
        if (containsAny(food.getName(), profile.getDislikedFoods())
                || containsAny(food.getCategory(), profile.getDislikedFoods())
                || containsAny(food.getDescription(), profile.getDislikedFoods())
                || containsAny(food.getName(), profile.getAllergies())
                || containsAny(food.getAllergenInfo(), profile.getAllergies())) {
            return false;
        }
        String dietPreference = profile.getDietPreference();
        if (dietPreference != null && dietPreference.toLowerCase(Locale.ROOT).contains("vegetarian")) {
            return !containsAny(food.getName(), "beef,pork,chicken,fish,shrimp,meat");
        }
        return true;
    }

    private boolean isAvailableMenuItem(RestaurantFoodItem menuItem) {
        return menuItem != null && !Boolean.FALSE.equals(menuItem.getAvailable());
    }

    private boolean matchesBudget(RestaurantFoodItem menuItem, Restaurant restaurant, BigDecimal maxBudget) {
        if (maxBudget == null) {
            return true;
        }
        if (menuItem != null && menuItem.getPrice() != null && menuItem.getPrice().compareTo(maxBudget) > 0) {
            return false;
        }
        return restaurant == null || restaurant.getAvgPrice() == null || restaurant.getAvgPrice().compareTo(maxBudget) <= 0;
    }

    private boolean isOpenRestaurant(Restaurant restaurant) {
        return restaurant == null || restaurant.getStatus() == null || restaurant.getStatus() == RestaurantStatus.OPEN;
    }

    private boolean isMeituanFood(FoodItem food) {
        return "MEITUAN_WAIMAI".equalsIgnoreCase(blankToNull(food.getExternalSource()));
    }

    private boolean hasCompleteNutrition(FoodItem food) {
        return notBlank(food.getCategory())
                && food.getCaloriesPer100g() != null
                && food.getProteinPer100g() != null
                && food.getFatPer100g() != null
                && food.getCarbPer100g() != null;
    }

    private double suggestedGrams(RestaurantFoodItem menuItem, MealType mealType) {
        String text = ((menuItem.getPortionSize() == null ? "" : menuItem.getPortionSize())
                + " "
                + (menuItem.getRawSpec() == null ? "" : menuItem.getRawSpec())).trim();
        Matcher matcher = GRAMS_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return clamp(Double.parseDouble(matcher.group(1)), MIN_GRAMS, MAX_GRAMS);
            } catch (NumberFormatException ignored) {
                return defaultGrams(mealType);
            }
        }
        return defaultGrams(mealType);
    }

    private double defaultGrams(MealType mealType) {
        return mealType == MealType.LUNCH || mealType == MealType.DINNER ? 150.0 : 100.0;
    }

    private double dailyTarget(Double configured, Double fallback, double defaultValue) {
        if (configured != null && configured > 0) {
            return configured;
        }
        if (fallback != null && fallback > 0) {
            return fallback;
        }
        return defaultValue;
    }

    private Double macroCalories(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        double protein = valueOrZero(profile.getDailyProteinTarget());
        double fat = valueOrZero(profile.getDailyFatTarget());
        double carb = valueOrZero(profile.getDailyCarbTarget());
        double calories = protein * 4.0 + fat * 9.0 + carb * 4.0;
        return calories > 0 ? calories : null;
    }

    private MealType inferCurrentMealType() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 10) return MealType.BREAKFAST;
        if (hour < 15) return MealType.LUNCH;
        if (hour < 20) return MealType.DINNER;
        return MealType.SNACK;
    }

    private Recommendation requireRecommendation(Long recommendationId) {
        return recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("recommendation not found"));
    }

    private double componentFit(double actual, double target) {
        if (target <= 0) {
            return 0.0;
        }
        return clamp((1.0 - Math.min(1.0, Math.abs(actual - target) / target)) * 100.0, 0, 100);
    }

    private static boolean containsAny(String source, String keywords) {
        if (source == null || keywords == null || keywords.isBlank()) {
            return false;
        }
        String normalizedSource = source.toLowerCase(Locale.ROOT);
        for (String keyword : keywords.split("[,;，；\\s]+")) {
            if (!keyword.isBlank() && normalizedSource.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeCategory(String category) {
        String value = blankToNull(category);
        return value == null ? "uncategorized" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record MealTargets(
            double calories,
            double protein,
            double fat,
            double carb,
            double consumedCalories,
            double consumedProtein,
            double consumedFat,
            double consumedCarb) {
    }

    private record HistoryContext(
            LocalDate today,
            Map<Long, LocalDate> lastFoodDate,
            Map<String, Integer> todayCategoryCounts,
            Set<MealType> recordedMeals,
            List<Map<String, Object>> recentMealSummaries) {
    }

    private static class Candidate {

        private final FoodItem food;
        private final RestaurantFoodItem menuItem;
        private final Restaurant restaurant;
        private double suggestedGrams;
        private double totalCalories;
        private double totalProtein;
        private double totalFat;
        private double totalCarb;
        private double macroScore;
        private double goalScore;
        private double restaurantScore;
        private double historyScore;
        private double baseScore;
        private double finalScore;
        private String reason;
        private String displayDescription;

        Candidate(FoodItem food, RestaurantFoodItem menuItem, Restaurant restaurant, double suggestedGrams) {
            this.food = food;
            this.menuItem = menuItem;
            this.restaurant = restaurant;
            this.suggestedGrams = suggestedGrams;
        }

        static Candidate copy(Candidate source) {
            Candidate copy = new Candidate(source.food, source.menuItem, source.restaurant, source.suggestedGrams);
            copy.totalCalories = source.totalCalories;
            copy.totalProtein = source.totalProtein;
            copy.totalFat = source.totalFat;
            copy.totalCarb = source.totalCarb;
            copy.macroScore = source.macroScore;
            copy.goalScore = source.goalScore;
            copy.restaurantScore = source.restaurantScore;
            copy.historyScore = source.historyScore;
            copy.baseScore = source.baseScore;
            copy.finalScore = source.finalScore;
            copy.reason = source.reason;
            copy.displayDescription = source.displayDescription;
            return copy;
        }

        void recalculateTotals() {
            double ratio = suggestedGrams / 100.0;
            totalCalories = valueOrZero(food.getCaloriesPer100g()) * ratio;
            totalProtein = valueOrZero(food.getProteinPer100g()) * ratio;
            totalFat = valueOrZero(food.getFatPer100g()) * ratio;
            totalCarb = valueOrZero(food.getCarbPer100g()) * ratio;
        }

        String category() {
            if (menuItem != null && notBlank(menuItem.getCategoryName())) {
                return menuItem.getCategoryName();
            }
            return food.getCategory();
        }
    }
}
