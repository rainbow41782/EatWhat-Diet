package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.MeituanCaptureImportResponse;
import cn.edu.cn.javadiet.model.dto.MeituanCapturedShop;
import cn.edu.cn.javadiet.model.dto.MeituanCleanupResult;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlResult;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlRunResponse;
import cn.edu.cn.javadiet.model.dto.MeituanCrawlTaskInput;
import cn.edu.cn.javadiet.model.dto.MeituanMenuItem;
import cn.edu.cn.javadiet.model.entity.FoodItem;
import cn.edu.cn.javadiet.model.entity.MeituanCrawlTask;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import cn.edu.cn.javadiet.model.enums.MeituanCrawlStatus;
import cn.edu.cn.javadiet.model.enums.RestaurantStatus;
import cn.edu.cn.javadiet.repository.FoodItemRepository;
import cn.edu.cn.javadiet.repository.MeituanCrawlTaskRepository;
import cn.edu.cn.javadiet.repository.RestaurantFoodItemRepository;
import cn.edu.cn.javadiet.repository.RestaurantRepository;
import cn.edu.cn.javadiet.service.MeituanCrawlService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeituanCrawlServiceImpl implements MeituanCrawlService {

    private static final String EXTERNAL_SOURCE = "MEITUAN_WAIMAI";
    private static final String NUTRITION_COMPLETE = "COMPLETE";
    private static final String NUTRITION_PENDING = "PENDING";
    private static final String PLACEHOLDER_RESTAURANT_NAME = "美团手动捕获店铺";

    private final MeituanCrawlTaskRepository taskRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantFoodItemRepository restaurantFoodItemRepository;
    private final MeituanMenuCrawler crawler;
    private final MeituanSignedRequestCaptureService captureService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MeituanCrawlServiceImpl(
            MeituanCrawlTaskRepository taskRepository,
            RestaurantRepository restaurantRepository,
            FoodItemRepository foodItemRepository,
            RestaurantFoodItemRepository restaurantFoodItemRepository,
            MeituanMenuCrawler crawler,
            MeituanSignedRequestCaptureService captureService) {
        this.taskRepository = taskRepository;
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantFoodItemRepository = restaurantFoodItemRepository;
        this.crawler = crawler;
        this.captureService = captureService;
    }

    @Override
    @Transactional
    public List<MeituanCrawlTask> createTasks(List<MeituanCrawlTaskInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("tasks are required");
        }
        LocalDateTime now = LocalDateTime.now();
        return inputs.stream()
                .filter(Objects::nonNull)
                .map(input -> saveInput(input, now))
                .toList();
    }

    @Override
    public List<MeituanCrawlTask> listTasks(MeituanCrawlStatus status, String keyword) {
        List<MeituanCrawlTask> tasks;
        if (keyword != null && !keyword.isBlank()) {
            tasks = taskRepository.findByShopNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrderByIdAsc(
                    keyword,
                    keyword);
            if (status != null) {
                tasks = tasks.stream().filter(task -> task.getStatus() == status).toList();
            }
        } else if (status != null) {
            tasks = taskRepository.findByStatusOrderByIdAsc(status);
        } else {
            tasks = taskRepository.findAll().stream()
                    .sorted(Comparator.comparing(MeituanCrawlTask::getId))
                    .toList();
        }
        return tasks;
    }

    @Override
    public MeituanCrawlRunResponse runPendingTasks() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("crawl batch is already running");
        }
        recoverStaleRunningTasks();
        int queuedCount = taskRepository.findByStatusOrderByIdAsc(MeituanCrawlStatus.PENDING).size();
        CompletableFuture.runAsync(this::runBatch);
        return new MeituanCrawlRunResponse(true, queuedCount);
    }

    @Override
    @Transactional
    public MeituanCaptureImportResponse importLatestCapturedMenu() {
        return importCapturedResult(captureService.previewCapturedMenu());
    }

    @Override
    public List<MeituanCapturedShop> listCapturedShops() {
        return captureService.listCapturedShops().stream()
                .map(this::withImportedRestaurant)
                .toList();
    }

    @Override
    public MeituanCrawlResult previewCapturedMenu(String captureKey) {
        return captureService.previewCapturedMenu(captureKey);
    }

    @Override
    @Transactional
    public MeituanCaptureImportResponse importCapturedMenu(String captureKey) {
        return importCapturedResult(captureService.previewCapturedMenu(captureKey));
    }

    @Override
    public void deleteCapturedShop(String captureKey) {
        captureService.deleteCapturedShop(captureKey);
    }

    @Override
    public MeituanCleanupResult previewBadDataCleanup() {
        return cleanupResult(false, findBadDataSnapshot(), "已统计可清理的早期美团测试数据");
    }

    @Override
    @Transactional
    public MeituanCleanupResult cleanupBadData() {
        BadDataSnapshot snapshot = findBadDataSnapshot();
        restaurantFoodItemRepository.deleteAll(snapshot.menuItems());
        foodItemRepository.deleteAll(snapshot.foodItems());
        restaurantRepository.deleteAll(snapshot.restaurants());
        return cleanupResult(true, snapshot, "已清理早期不合格的美团测试数据");
    }

    @Override
    @Transactional
    public MeituanCrawlTask retryTask(Long taskId) {
        MeituanCrawlTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("crawl task not found"));
        if (task.getStatus() == MeituanCrawlStatus.RUNNING && running.get()) {
            throw new IllegalStateException("running task cannot be retried");
        }
        task.setStatus(MeituanCrawlStatus.PENDING);
        task.setFailureReason(null);
        task.setImportedItemCount(0);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        MeituanCrawlTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("crawl task not found"));
        if (task.getStatus() == MeituanCrawlStatus.RUNNING && running.get()) {
            throw new IllegalStateException("running task cannot be deleted");
        }
        taskRepository.delete(task);
    }

    private MeituanCaptureImportResponse importCapturedResult(MeituanCrawlResult result) {
        if (result.getItems().isEmpty()) {
            throw new IllegalStateException("没有捕获到可导入的菜单数据，请先在养号浏览器里打开目标店铺菜单");
        }
        MeituanCrawlTask task = MeituanCrawlTask.builder()
                .shopName(coalesce(result.getRestaurantName(), fallbackRestaurantName(result.getMeituanPoiId())))
                .address(result.getAddress())
                .latitude(result.getLatitude())
                .longitude(result.getLongitude())
                .meituanPoiId(result.getMeituanPoiId())
                .build();
        ImportSummary summary = importResult(task, result);
        return MeituanCaptureImportResponse.builder()
                .imported(true)
                .restaurantId(summary.restaurantId())
                .itemCount(summary.itemCount())
                .pendingNutritionCount(summary.pendingNutritionCount())
                .restaurantName(coalesce(result.getRestaurantName(), task.getShopName()))
                .meituanPoiId(result.getMeituanPoiId())
                .message("已导入 " + summary.itemCount() + " 个菜品，其中 "
                        + summary.pendingNutritionCount() + " 个待补全营养")
                .build();
    }

    private void recoverStaleRunningTasks() {
        List<MeituanCrawlTask> staleTasks = taskRepository.findByStatusOrderByIdAsc(MeituanCrawlStatus.RUNNING);
        if (staleTasks.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (MeituanCrawlTask task : staleTasks) {
            task.setStatus(MeituanCrawlStatus.PENDING);
            task.setFailureReason("上次批次异常中断，已自动恢复为待查询");
            task.setLastFinishedAt(now);
            task.setUpdatedAt(now);
        }
        taskRepository.saveAll(staleTasks);
    }

    private MeituanCrawlTask saveInput(MeituanCrawlTaskInput input, LocalDateTime now) {
        String shopName = normalizeRequired(input.getShopName(), "shop name is required");
        MeituanCrawlTask task = findExisting(input, shopName)
                .orElseGet(() -> MeituanCrawlTask.builder()
                        .shopName(shopName)
                        .status(MeituanCrawlStatus.PENDING)
                        .attemptCount(0)
                        .importedItemCount(0)
                        .createdAt(now)
                        .build());
        if (task.getStatus() != MeituanCrawlStatus.RUNNING) {
            task.setStatus(MeituanCrawlStatus.PENDING);
            task.setFailureReason(null);
        }
        task.setShopName(shopName);
        task.setAddress(trimToNull(input.getAddress()));
        task.setLatitude(input.getLatitude());
        task.setLongitude(input.getLongitude());
        task.setMeituanPoiId(trimToNull(input.getMeituanPoiId()));
        task.setMenuUrl(trimToNull(input.getMenuUrl()));
        task.setUpdatedAt(now);
        return taskRepository.save(task);
    }

    private Optional<MeituanCrawlTask> findExisting(MeituanCrawlTaskInput input, String shopName) {
        String poiId = trimToNull(input.getMeituanPoiId());
        if (poiId != null) {
            return taskRepository.findFirstByMeituanPoiId(poiId);
        }
        String address = trimToNull(input.getAddress());
        if (address != null) {
            return taskRepository.findFirstByShopNameIgnoreCaseAndAddressIgnoreCase(shopName, address);
        }
        return Optional.empty();
    }

    private void runBatch() {
        try {
            List<Long> taskIds = taskRepository.findByStatusOrderByIdAsc(MeituanCrawlStatus.PENDING).stream()
                    .map(MeituanCrawlTask::getId)
                    .toList();
            for (Long taskId : taskIds) {
                processTask(taskId);
            }
        } finally {
            running.set(false);
        }
    }

    private void processTask(Long taskId) {
        MeituanCrawlTask task = markRunning(taskId);
        try {
            MeituanCrawlResult result = crawler.crawl(task);
            ImportSummary summary = importResult(task, result);
            markSuccess(task.getId(), summary.restaurantId(), summary.itemCount());
        } catch (Exception exception) {
            markFailed(task.getId(), exception.getMessage());
        }
    }

    @Transactional
    public MeituanCrawlTask markRunning(Long taskId) {
        MeituanCrawlTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("crawl task not found"));
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(MeituanCrawlStatus.RUNNING);
        task.setFailureReason(null);
        task.setLastStartedAt(now);
        task.setLastFinishedAt(null);
        task.setAttemptCount((task.getAttemptCount() == null ? 0 : task.getAttemptCount()) + 1);
        task.setUpdatedAt(now);
        return taskRepository.save(task);
    }

    @Transactional
    public ImportSummary importResult(MeituanCrawlTask task, MeituanCrawlResult result) {
        String restaurantExternalId = trimToNull(coalesce(result.getMeituanPoiId(), task.getMeituanPoiId()));
        if (restaurantExternalId == null) {
            throw new IllegalStateException("无法识别美团店铺 ID，请重新打开店铺菜单后再导入");
        }
        Restaurant restaurant = restaurantRepository.findByExternalSourceAndExternalId(EXTERNAL_SOURCE, restaurantExternalId)
                .orElseGet(Restaurant::new);
        LocalDateTime now = LocalDateTime.now();
        if (restaurant.getId() == null) {
            restaurant.setCreatedAt(now);
        }
        restaurant.setName(coalesce(result.getRestaurantName(), task.getShopName(), fallbackRestaurantName(restaurantExternalId)));
        restaurant.setAddress(coalesce(result.getAddress(), task.getAddress()));
        restaurant.setLatitude(coalesce(result.getLatitude(), task.getLatitude()));
        restaurant.setLongitude(coalesce(result.getLongitude(), task.getLongitude()));
        if (result.getAvgPrice() != null) {
            restaurant.setAvgPrice(result.getAvgPrice());
        }
        if (result.getRating() != null) {
            restaurant.setRating(result.getRating());
        }
        if (trimToNull(result.getTags()) != null) {
            restaurant.setTags(result.getTags());
        }
        restaurant.setDeliverySupported(true);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setExternalSource(EXTERNAL_SOURCE);
        restaurant.setExternalId(restaurantExternalId);
        restaurant.setUpdatedAt(now);
        restaurant = restaurantRepository.save(restaurant);

        int itemCount = 0;
        int pendingNutritionCount = 0;
        for (MeituanMenuItem item : result.getItems()) {
            if (item.getName() == null || item.getName().isBlank()) {
                continue;
            }
            FoodItem foodItem = upsertFoodItem(item, now);
            upsertRestaurantFoodItem(restaurant.getId(), foodItem.getId(), item);
            itemCount++;
            if (!hasCompleteNutrition(foodItem)) {
                pendingNutritionCount++;
            }
        }
        return new ImportSummary(restaurant.getId(), itemCount, pendingNutritionCount);
    }

    private FoodItem upsertFoodItem(MeituanMenuItem item, LocalDateTime now) {
        String externalId = menuExternalId(item);
        FoodItem foodItem = foodItemRepository.findByExternalSourceAndExternalId(EXTERNAL_SOURCE, externalId)
                .orElseGet(FoodItem::new);
        if (foodItem.getId() == null) {
            foodItem.setCreatedAt(now);
        }
        foodItem.setName(item.getName());
        foodItem.setCategory(coalesce(trimToNull(item.getCategory()), foodItem.getCategory(), "未分类"));
        foodItem.setDescription(coalesce(trimToNull(item.getDescription()), foodItem.getDescription()));
        foodItem.setExternalSource(EXTERNAL_SOURCE);
        foodItem.setExternalId(externalId);
        boolean nutritionComplete = hasCompleteNutrition(foodItem);
        foodItem.setNutritionStatus(nutritionComplete ? NUTRITION_COMPLETE : NUTRITION_PENDING);
        if (!nutritionComplete) {
            foodItem.setIsRecommended(false);
        } else if (foodItem.getIsRecommended() == null) {
            foodItem.setIsRecommended(true);
        }
        foodItem.setUpdatedAt(now);
        return foodItemRepository.save(foodItem);
    }

    private void upsertRestaurantFoodItem(Long restaurantId, Long foodItemId, MeituanMenuItem item) {
        String externalId = menuExternalId(item);
        RestaurantFoodItem menuItem = restaurantFoodItemRepository.findByRestaurantIdAndExternalSourceAndExternalFoodId(
                        restaurantId,
                        EXTERNAL_SOURCE,
                        externalId)
                .orElseGet(RestaurantFoodItem::new);
        menuItem.setRestaurantId(restaurantId);
        menuItem.setFoodItemId(foodItemId);
        menuItem.setPrice(item.getPrice());
        menuItem.setAvailable(item.getAvailable() == null || item.getAvailable());
        menuItem.setPortionSize(item.getPortionSize());
        menuItem.setCategoryName(item.getCategory());
        menuItem.setSkuId(item.getSkuId());
        menuItem.setImageUrl(truncateNullable(item.getImageUrl(), 1000));
        menuItem.setRawSpec(truncateNullable(item.getRawSpec(), 1000));
        menuItem.setSourcePayloadJson(item.getRawJson());
        menuItem.setRemark(compactRemark(item));
        menuItem.setExternalSource(EXTERNAL_SOURCE);
        menuItem.setExternalFoodId(externalId);
        restaurantFoodItemRepository.save(menuItem);
    }

    @Transactional
    public void markSuccess(Long taskId, Long restaurantId, int itemCount) {
        MeituanCrawlTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("crawl task not found"));
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(MeituanCrawlStatus.SUCCESS);
        task.setFailureReason(null);
        task.setImportedItemCount(itemCount);
        task.setRestaurantId(restaurantId);
        task.setLastFinishedAt(now);
        task.setUpdatedAt(now);
        taskRepository.save(task);
    }

    @Transactional
    public void markFailed(Long taskId, String message) {
        MeituanCrawlTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("crawl task not found"));
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(MeituanCrawlStatus.FAILED);
        task.setFailureReason(message == null || message.isBlank() ? "crawl failed" : truncate(message, 1000));
        task.setLastFinishedAt(now);
        task.setUpdatedAt(now);
        taskRepository.save(task);
    }

    private MeituanCapturedShop withImportedRestaurant(MeituanCapturedShop shop) {
        String externalId = trimToNull(shop.getMeituanPoiId());
        if (externalId != null) {
            restaurantRepository.findByExternalSourceAndExternalId(EXTERNAL_SOURCE, externalId)
                    .ifPresent(restaurant -> shop.setImportedRestaurantId(restaurant.getId()));
        }
        return shop;
    }

    private BadDataSnapshot findBadDataSnapshot() {
        List<FoodItem> badFoods = foodItemRepository.findByExternalSource(EXTERNAL_SOURCE).stream()
                .filter(this::isLegacyBadFood)
                .toList();
        List<Restaurant> badRestaurants = restaurantRepository.findByExternalSource(EXTERNAL_SOURCE).stream()
                .filter(this::isLegacyBadRestaurant)
                .toList();
        Set<Long> badFoodIds = idsOfFoods(badFoods);
        Set<Long> badRestaurantIds = idsOfRestaurants(badRestaurants);
        List<RestaurantFoodItem> badMenuItems = restaurantFoodItemRepository.findAll().stream()
                .filter(item -> badFoodIds.contains(item.getFoodItemId())
                        || badRestaurantIds.contains(item.getRestaurantId())
                        || (EXTERNAL_SOURCE.equals(item.getExternalSource()) && isLegacyBadMenuItem(item)))
                .toList();
        return new BadDataSnapshot(badRestaurants, badFoods, badMenuItems);
    }

    private boolean isLegacyBadFood(FoodItem foodItem) {
        return trimToNull(foodItem.getNutritionStatus()) == null
                && (!hasCompleteNutrition(foodItem) || trimToNull(foodItem.getCategory()) == null);
    }

    private boolean isLegacyBadRestaurant(Restaurant restaurant) {
        return trimToNull(restaurant.getExternalId()) == null
                || PLACEHOLDER_RESTAURANT_NAME.equals(restaurant.getName());
    }

    private boolean isLegacyBadMenuItem(RestaurantFoodItem item) {
        return trimToNull(item.getCategoryName()) == null
                && trimToNull(item.getSkuId()) == null
                && trimToNull(item.getImageUrl()) == null
                && trimToNull(item.getSourcePayloadJson()) == null;
    }

    private static Set<Long> idsOfFoods(List<FoodItem> foodItems) {
        Set<Long> ids = new HashSet<>();
        for (FoodItem foodItem : foodItems) {
            if (foodItem.getId() != null) {
                ids.add(foodItem.getId());
            }
        }
        return ids;
    }

    private static Set<Long> idsOfRestaurants(List<Restaurant> restaurants) {
        Set<Long> ids = new HashSet<>();
        for (Restaurant restaurant : restaurants) {
            if (restaurant.getId() != null) {
                ids.add(restaurant.getId());
            }
        }
        return ids;
    }

    private static MeituanCleanupResult cleanupResult(boolean cleaned, BadDataSnapshot snapshot, String message) {
        return MeituanCleanupResult.builder()
                .cleaned(cleaned)
                .restaurantCount(snapshot.restaurants().size())
                .foodItemCount(snapshot.foodItems().size())
                .menuItemCount(snapshot.menuItems().size())
                .message(message)
                .build();
    }

    private static boolean hasCompleteNutrition(FoodItem foodItem) {
        return trimToNull(foodItem.getCategory()) != null
                && foodItem.getCaloriesPer100g() != null
                && foodItem.getProteinPer100g() != null
                && foodItem.getFatPer100g() != null
                && foodItem.getCarbPer100g() != null;
    }

    private static String menuExternalId(MeituanMenuItem item) {
        String externalId = trimToNull(item.getExternalId());
        if (externalId != null) {
            return externalId;
        }
        return "NAME:" + item.getName().trim().toLowerCase();
    }

    private static String fallbackRestaurantName(String poiId) {
        return "美团店铺-" + coalesce(trimToNull(poiId), "未识别");
    }

    private static String compactRemark(MeituanMenuItem item) {
        StringBuilder remark = new StringBuilder();
        if (item.getSkuId() != null) {
            remark.append("skuId=").append(item.getSkuId());
        }
        if (item.getImageUrl() != null) {
            if (!remark.isEmpty()) {
                remark.append("; ");
            }
            remark.append("image=").append(item.getImageUrl());
        }
        return remark.isEmpty() ? null : truncate(remark.toString(), 1000);
    }

    private static String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @SafeVarargs
    private static <T> T coalesce(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String truncateNullable(String value, int maxLength) {
        return value == null ? null : truncate(value, maxLength);
    }

    public record ImportSummary(Long restaurantId, int itemCount, int pendingNutritionCount) {
    }

    private record BadDataSnapshot(
            List<Restaurant> restaurants,
            List<FoodItem> foodItems,
            List<RestaurantFoodItem> menuItems) {
    }
}
