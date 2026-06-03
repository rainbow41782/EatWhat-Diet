package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.config.FoodDataCentralProperties;
import cn.edu.cn.javadiet.config.LlmGatewayProperties;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyItem;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyResponse;
import cn.edu.cn.javadiet.model.dto.FoodNutritionConfigStatus;
import cn.edu.cn.javadiet.model.dto.FoodNutritionMenuContext;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPendingItem;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewJobStatus;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewResponse;
import cn.edu.cn.javadiet.model.dto.FoodNutritionReferenceCandidate;
import cn.edu.cn.javadiet.model.dto.FoodNutritionSuggestion;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantFoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantRepository;
import cn.edu.cn.javadiet.service.FoodItemService;
import cn.edu.cn.javadiet.service.FoodNutritionEnrichmentService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class FoodNutritionEnrichmentServiceImpl implements FoodNutritionEnrichmentService {

    private static final String NUTRITION_PENDING = "PENDING";

    private final FoodItemRepository foodItemRepository;
    private final RestaurantFoodItemRepository restaurantFoodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemService foodItemService;
    private final FoodDataCentralClient foodDataCentralClient;
    private final LlmGatewayClient llmGatewayClient;
    private final LlmGatewayProperties llmGatewayProperties;
    private final FoodDataCentralProperties fdcProperties;
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, PreviewJob> previewJobs = new ConcurrentHashMap<>();

    public FoodNutritionEnrichmentServiceImpl(
            FoodItemRepository foodItemRepository,
            RestaurantFoodItemRepository restaurantFoodItemRepository,
            RestaurantRepository restaurantRepository,
            FoodItemService foodItemService,
            FoodDataCentralClient foodDataCentralClient,
            LlmGatewayClient llmGatewayClient,
            LlmGatewayProperties llmGatewayProperties,
            FoodDataCentralProperties fdcProperties) {
        this.foodItemRepository = foodItemRepository;
        this.restaurantFoodItemRepository = restaurantFoodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.foodItemService = foodItemService;
        this.foodDataCentralClient = foodDataCentralClient;
        this.llmGatewayClient = llmGatewayClient;
        this.llmGatewayProperties = llmGatewayProperties;
        this.fdcProperties = fdcProperties;
    }

    @Override
    public List<FoodNutritionPendingItem> findPending(String keyword, Integer limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return foodItemRepository.findAll().stream()
                .filter(item -> NUTRITION_PENDING.equalsIgnoreCase(item.getNutritionStatus()))
                .filter(item -> normalizedKeyword.isBlank()
                        || contains(item.getName(), normalizedKeyword)
                        || contains(item.getDescription(), normalizedKeyword))
                .sorted(Comparator.comparing(FoodItem::getId))
                .limit(normalizedLimit(limit))
                .map(this::toPendingItem)
                .toList();
    }

    @Override
    public FoodNutritionPreviewResponse preview(FoodNutritionPreviewRequest request) {
        List<FoodNutritionSuggestion> suggestions = new ArrayList<>();
        for (Long foodItemId : safeIds(request)) {
            suggestions.add(previewOne(foodItemId));
        }
        return FoodNutritionPreviewResponse.builder().items(suggestions).build();
    }

    @Override
    public FoodNutritionPreviewJobStatus startPreviewJob(FoodNutritionPreviewRequest request) {
        List<Long> ids = safeIds(request);
        PreviewJob job = new PreviewJob(UUID.randomUUID().toString(), ids);
        previewJobs.put(job.jobId(), job);
        previewExecutor.submit(() -> runPreviewJob(job));
        return toJobStatus(job);
    }

    @Override
    public FoodNutritionPreviewJobStatus getPreviewJob(String jobId) {
        return toJobStatus(findJob(jobId));
    }

    @Override
    public FoodNutritionPreviewJobStatus cancelPreviewJob(String jobId) {
        PreviewJob job = findJob(jobId);
        job.cancelRequested().set(true);
        if ("PENDING".equals(job.status()) || "RUNNING".equals(job.status())) {
            job.status("CANCELLING");
        }
        return toJobStatus(job);
    }

    @Override
    @Transactional
    public FoodNutritionApplyResponse apply(FoodNutritionApplyRequest request) {
        List<Long> updatedIds = new ArrayList<>();
        for (FoodNutritionApplyItem item : request == null ? List.<FoodNutritionApplyItem>of() : request.getItems()) {
            if (item == null || item.getFoodItemId() == null) {
                continue;
            }
            FoodItem foodItem = foodItemRepository.findById(item.getFoodItemId())
                    .orElseThrow(() -> new IllegalArgumentException("food item not found: " + item.getFoodItemId()));
            if (hasCompleteNutrition(foodItem)) {
                throw new IllegalArgumentException("食品营养已完整，默认不覆盖：" + foodItem.getName());
            }
            validateApplyItem(item);
            foodItem.setCategory(item.getCategory().trim());
            foodItem.setCaloriesPer100g(roundOne(item.getCaloriesPer100g()));
            foodItem.setProteinPer100g(roundOne(item.getProteinPer100g()));
            foodItem.setFatPer100g(roundOne(item.getFatPer100g()));
            foodItem.setCarbPer100g(roundOne(item.getCarbPer100g()));
            foodItem.setIsRecommended(true);
            foodItemService.save(foodItem);
            updatedIds.add(foodItem.getId());
        }
        return FoodNutritionApplyResponse.builder()
                .updatedCount(updatedIds.size())
                .updatedFoodItemIds(updatedIds)
                .build();
    }

    @Override
    public FoodNutritionConfigStatus configStatus() {
        return FoodNutritionConfigStatus.builder()
                .llmEnabled(llmGatewayClient.isEnabled())
                .llmConfigured(llmGatewayClient.isConfigured())
                .llmModel(blankToNull(llmGatewayProperties.getModel()))
                .fdcConfigured(foodDataCentralClient.isConfigured())
                .fdcMaxCandidates(fdcProperties.getMaxCandidates())
                .build();
    }

    private void runPreviewJob(PreviewJob job) {
        if (job.cancelRequested().get()) {
            job.status("CANCELLED");
            return;
        }
        job.status("RUNNING");
        try {
            for (Long foodItemId : job.foodItemIds()) {
                if (job.cancelRequested().get()) {
                    break;
                }
                FoodItem current = foodItemRepository.findById(foodItemId).orElse(null);
                job.currentFoodItemId(foodItemId);
                job.currentFoodName(current == null ? null : current.getName());
                job.items().add(previewOne(foodItemId));
                job.completedCount(job.completedCount() + 1);
                job.currentFoodItemId(null);
                job.currentFoodName(null);
                if (job.cancelRequested().get()) {
                    break;
                }
            }
            job.status(job.cancelRequested().get()
                    && job.completedCount() < job.totalCount() ? "CANCELLED" : "COMPLETED");
        } catch (Exception exception) {
            job.status("FAILED");
            job.items().add(FoodNutritionSuggestion.builder()
                    .failureReason("补全任务异常：" + exception.getMessage())
                    .build());
        } finally {
            job.currentFoodItemId(null);
            job.currentFoodName(null);
            trimPreviewJobs();
        }
    }

    private PreviewJob findJob(String jobId) {
        PreviewJob job = previewJobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("补全预览任务不存在：" + jobId);
        }
        return job;
    }

    private FoodNutritionPreviewJobStatus toJobStatus(PreviewJob job) {
        return FoodNutritionPreviewJobStatus.builder()
                .jobId(job.jobId())
                .status(job.status())
                .totalCount(job.totalCount())
                .completedCount(job.completedCount())
                .cancelRequested(job.cancelRequested().get())
                .currentFoodItemId(job.currentFoodItemId())
                .currentFoodName(job.currentFoodName())
                .items(new ArrayList<>(job.items()))
                .build();
    }

    private void trimPreviewJobs() {
        if (previewJobs.size() <= 8) {
            return;
        }
        previewJobs.values().stream()
                .filter(job -> !"RUNNING".equals(job.status()) && !"CANCELLING".equals(job.status()))
                .sorted(Comparator.comparing(PreviewJob::createdOrder))
                .limit(Math.max(0, previewJobs.size() - 8))
                .forEach(job -> previewJobs.remove(job.jobId()));
    }

    private FoodNutritionSuggestion previewOne(Long foodItemId) {
        FoodItem foodItem = foodItemRepository.findById(foodItemId).orElse(null);
        if (foodItem == null) {
            return failed(foodItemId, null, "食品不存在");
        }
        if (hasCompleteNutrition(foodItem)) {
            return failed(foodItem.getId(), foodItem.getName(), "食品营养已完整，默认不覆盖");
        }
        List<FoodNutritionMenuContext> menuContexts = menuContexts(foodItem.getId());
        List<String> searchQueries = searchQueries(foodItem, menuContexts);
        List<FoodNutritionReferenceCandidate> references = referenceCandidates(searchQueries);
        try {
            return withDebugFields(
                    llmGatewayClient.suggest(foodItem, references, menuContexts),
                    references,
                    searchQueries);
        } catch (LlmGatewayClient.ModelResponseParseException exception) {
            return FoodNutritionSuggestion.builder()
                    .foodItemId(foodItem.getId())
                    .foodName(foodItem.getName())
                    .references(references)
                    .searchQueries(searchQueries)
                    .rawModelResponse(exception.getRawModelResponse())
                    .parseWarning(exception.getParseWarning())
                    .failureReason(exception.getMessage())
                    .build();
        } catch (Exception exception) {
            return FoodNutritionSuggestion.builder()
                    .foodItemId(foodItem.getId())
                    .foodName(foodItem.getName())
                    .references(references)
                    .searchQueries(searchQueries)
                    .failureReason(exception.getMessage())
                    .build();
        }
    }

    private FoodNutritionSuggestion withDebugFields(
            FoodNutritionSuggestion suggestion,
            List<FoodNutritionReferenceCandidate> references,
            List<String> searchQueries) {
        suggestion.setReferences(references);
        suggestion.setSearchQueries(searchQueries);
        return suggestion;
    }

    private List<FoodNutritionReferenceCandidate> referenceCandidates(List<String> searchQueries) {
        Map<Long, FoodNutritionReferenceCandidate> candidates = new LinkedHashMap<>();
        int max = Math.max(1, fdcProperties.getMaxCandidates() == null ? 6 : fdcProperties.getMaxCandidates());
        for (String query : searchQueries) {
            for (FoodNutritionReferenceCandidate candidate : foodDataCentralClient.search(query)) {
                if (candidate.getFdcId() != null) {
                    candidates.putIfAbsent(candidate.getFdcId(), candidate);
                }
                if (candidates.size() >= max) {
                    return new ArrayList<>(candidates.values());
                }
            }
        }
        return new ArrayList<>(candidates.values());
    }

    private FoodNutritionPendingItem toPendingItem(FoodItem foodItem) {
        return FoodNutritionPendingItem.builder()
                .foodItemId(foodItem.getId())
                .name(foodItem.getName())
                .category(foodItem.getCategory())
                .description(foodItem.getDescription())
                .nutritionStatus(foodItem.getNutritionStatus())
                .externalSource(foodItem.getExternalSource())
                .externalId(foodItem.getExternalId())
                .menuContexts(menuContexts(foodItem.getId()))
                .build();
    }

    private List<FoodNutritionMenuContext> menuContexts(Long foodItemId) {
        return restaurantFoodItemRepository.findByFoodItemId(foodItemId).stream()
                .sorted(Comparator.comparing(RestaurantFoodItem::getId))
                .limit(5)
                .map(this::toMenuContext)
                .toList();
    }

    private FoodNutritionMenuContext toMenuContext(RestaurantFoodItem menuItem) {
        Restaurant restaurant = restaurantRepository.findById(menuItem.getRestaurantId()).orElse(null);
        return FoodNutritionMenuContext.builder()
                .restaurantId(menuItem.getRestaurantId())
                .restaurantName(restaurant == null ? null : restaurant.getName())
                .price(menuItem.getPrice())
                .portionSize(menuItem.getPortionSize())
                .categoryName(menuItem.getCategoryName())
                .rawSpec(menuItem.getRawSpec())
                .remark(menuItem.getRemark())
                .build();
    }

    private List<String> searchQueries(FoodItem foodItem, List<FoodNutritionMenuContext> menuContexts) {
        List<String> dictionaryQueries = dictionarySearchQueries(foodItem, menuContexts);
        List<String> queries = new ArrayList<>();
        addQueries(queries, llmSearchQueries(foodItem, menuContexts, dictionaryQueries));
        addQueries(queries, dictionaryQueries);
        return queries.stream().limit(3).toList();
    }

    private List<String> llmSearchQueries(
            FoodItem foodItem,
            List<FoodNutritionMenuContext> menuContexts,
            List<String> dictionaryQueries) {
        if (!llmGatewayClient.isConfigured()) {
            return List.of();
        }
        return llmGatewayClient.suggestSearchQueries(foodItem, menuContexts, dictionaryQueries);
    }

    private List<String> dictionarySearchQueries(FoodItem foodItem, List<FoodNutritionMenuContext> menuContexts) {
        List<String> queries = new ArrayList<>();
        addQueries(queries, englishHints(foodItem.getName() + " " + foodItem.getDescription()));
        for (FoodNutritionMenuContext context : menuContexts) {
            addQueries(queries, englishHints(context.getCategoryName() + " " + context.getPortionSize() + " "
                    + context.getRawSpec()));
        }
        addEnglishTextQuery(queries, foodItem.getName());
        addEnglishTextQuery(queries, foodItem.getDescription());
        addEnglishTextQuery(queries, foodItem.getCategory());
        return queries;
    }

    private static List<String> englishHints(String text) {
        String value = text == null ? "" : text;
        if (containsAny(value, "小麦啤酒")) {
            return List.of("wheat beer", "beer");
        }
        if (containsAny(value, "啤酒")) {
            return List.of("beer");
        }
        if (containsAny(value, "虾滑", "虾仁", "鲜虾", "大虾", "虾")) {
            return List.of("shrimp");
        }
        if (containsAny(value, "巴沙鱼")) {
            return List.of("catfish", "fish");
        }
        if (containsAny(value, "鱼片", "鱼")) {
            return List.of("fish");
        }
        if (containsAny(value, "薯条", "脆脆薯", "薯薯", "马铃薯", "土豆")) {
            return List.of("french fries potato");
        }
        if (containsAny(value, "鸡腿堡", "板烧", "麦辣鸡", "鸡肉堡")) {
            return List.of("chicken sandwich");
        }
        if (containsAny(value, "汉堡", "牛肉堡", "巨无霸")) {
            return List.of("hamburger sandwich");
        }
        if (containsAny(value, "鸡翅", "鸡块", "炸鸡")) {
            return List.of("fried chicken");
        }
        if (containsAny(value, "米饭", "盖饭", "炒饭", "拌饭")) {
            return List.of("rice dish");
        }
        if (containsAny(value, "面条", "拉面", "意面", "炒面")) {
            return List.of("noodles");
        }
        if (containsAny(value, "咖啡", "拿铁", "美式")) {
            return List.of("coffee beverage");
        }
        if (containsAny(value, "奶茶", "牛奶", "酸奶")) {
            return List.of("milk beverage");
        }
        if (containsAny(value, "可乐", "汽水")) {
            return List.of("cola soft drink");
        }
        if (containsAny(value, "豆腐", "豆浆")) {
            return List.of("tofu soy");
        }
        if (containsAny(value, "肥牛", "牛肉卷", "牛肉")) {
            return List.of("beef", "beef slices");
        }
        if (containsAny(value, "猪肉")) {
            return List.of("pork");
        }
        if (containsAny(value, "鸡肉", "鸡")) {
            return List.of("chicken");
        }
        return List.of();
    }

    private static void addQuery(List<String> queries, String query) {
        String value = blankToNull(query);
        if (value != null && queries.stream().noneMatch(existing -> existing.equalsIgnoreCase(value))) {
            queries.add(value);
        }
    }

    private static void addQueries(List<String> queries, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addQuery(queries, value);
        }
    }

    private static void addEnglishTextQuery(List<String> queries, String query) {
        String value = blankToNull(query);
        if (value != null && isEnglishSearchText(value)) {
            addQuery(queries, value);
        }
    }

    private static boolean isEnglishSearchText(String value) {
        boolean hasLetter = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current > 127) {
                return false;
            }
            if (Character.isLetter(current)) {
                hasLetter = true;
            }
        }
        return hasLetter;
    }

    private static List<Long> safeIds(FoodNutritionPreviewRequest request) {
        if (request == null || request.getFoodItemIds() == null) {
            return List.of();
        }
        return request.getFoodItemIds().stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private static long normalizedLimit(Integer limit) {
        if (limit == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, limit.longValue());
    }

    private static void validateApplyItem(FoodNutritionApplyItem item) {
        if (blankToNull(item.getCategory()) == null) {
            throw new IllegalArgumentException("分类不能为空");
        }
        validateNumber(item.getCaloriesPer100g(), "热量", 0, 1200);
        validateNumber(item.getProteinPer100g(), "蛋白质", 0, 100);
        validateNumber(item.getFatPer100g(), "脂肪", 0, 100);
        validateNumber(item.getCarbPer100g(), "碳水", 0, 100);
    }

    private static void validateNumber(Double value, String field, double min, double max) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(field + "数值不合法");
        }
    }

    private static FoodNutritionSuggestion failed(Long foodItemId, String foodName, String reason) {
        return FoodNutritionSuggestion.builder()
                .foodItemId(foodItemId)
                .foodName(foodName)
                .failureReason(reason)
                .build();
    }

    private static boolean hasCompleteNutrition(FoodItem foodItem) {
        return foodItem.getCategory() != null && !foodItem.getCategory().isBlank()
                && foodItem.getCaloriesPer100g() != null
                && foodItem.getProteinPer100g() != null
                && foodItem.getFatPer100g() != null
                && foodItem.getCarbPer100g() != null;
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null) {
            return false;
        }
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static double roundOne(Double value) {
        return Math.round(value * 10d) / 10d;
    }

    private static class PreviewJob {

        private final String jobId;
        private final List<Long> foodItemIds;
        private final List<FoodNutritionSuggestion> items = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
        private final long createdOrder = System.nanoTime();
        private volatile String status = "PENDING";
        private volatile int completedCount = 0;
        private volatile Long currentFoodItemId;
        private volatile String currentFoodName;

        PreviewJob(String jobId, List<Long> foodItemIds) {
            this.jobId = jobId;
            this.foodItemIds = foodItemIds;
        }

        String jobId() {
            return jobId;
        }

        List<Long> foodItemIds() {
            return foodItemIds;
        }

        List<FoodNutritionSuggestion> items() {
            return items;
        }

        AtomicBoolean cancelRequested() {
            return cancelRequested;
        }

        long createdOrder() {
            return createdOrder;
        }

        String status() {
            return status;
        }

        void status(String status) {
            this.status = status;
        }

        int totalCount() {
            return foodItemIds.size();
        }

        int completedCount() {
            return completedCount;
        }

        void completedCount(int completedCount) {
            this.completedCount = completedCount;
        }

        Long currentFoodItemId() {
            return currentFoodItemId;
        }

        void currentFoodItemId(Long currentFoodItemId) {
            this.currentFoodItemId = currentFoodItemId;
        }

        String currentFoodName() {
            return currentFoodName;
        }

        void currentFoodName(String currentFoodName) {
            this.currentFoodName = currentFoodName;
        }
    }
}
