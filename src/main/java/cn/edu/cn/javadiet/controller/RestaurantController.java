package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.IpLocationResult;
import cn.edu.cn.javadiet.model.dto.NearbyRestaurantQuery;
import cn.edu.cn.javadiet.model.entity.Restaurant;
import cn.edu.cn.javadiet.model.entity.RestaurantFoodItem;
import cn.edu.cn.javadiet.service.RestaurantService;
import cn.edu.cn.javadiet.service.impl.TencentIpLocationClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final TencentIpLocationClient tencentIpLocationClient;

    public RestaurantController(RestaurantService restaurantService, TencentIpLocationClient tencentIpLocationClient) {
        this.restaurantService = restaurantService;
        this.tencentIpLocationClient = tencentIpLocationClient;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Restaurant>> save(@RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.save(restaurant)));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<Restaurant>> get(@PathVariable Long restaurantId) {
        Restaurant restaurant = restaurantService.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("restaurant not found"));
        return ResponseEntity.ok(ApiResponse.ok(restaurant));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<Restaurant>>> nearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) Integer maxDistanceKm,
            @RequestParam(defaultValue = "true") Boolean onlyOpen) {
        NearbyRestaurantQuery query = new NearbyRestaurantQuery(latitude, longitude, maxDistanceKm, onlyOpen);
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.findNearby(query)));
    }

    @GetMapping("/locate/tencent-ip")
    public ResponseEntity<ApiResponse<IpLocationResult>> locateByTencentIp(
            HttpServletRequest request,
            @RequestParam(required = false) String ip) {
        String resolvedIp = (ip != null && !ip.isBlank()) ? ip.trim() : resolveClientIp(request);
        if (resolvedIp == null || resolvedIp.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("无法识别客户端公网IP，请改用浏览器定位"));
        }
        Optional<IpLocationResult> result = tencentIpLocationClient.locateByIp(resolvedIp);
        return result
                .map(item -> ResponseEntity.ok(ApiResponse.ok(item)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("腾讯IP定位失败，请检查key是否开通定位服务或改用浏览器定位")));
    }

    @PostMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<RestaurantFoodItem>> addMenuItem(
            @PathVariable Long restaurantId,
            @RequestBody RestaurantFoodItem menuItem) {
        menuItem.setRestaurantId(restaurantId);
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.addMenuItem(menuItem)));
    }

    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<List<RestaurantFoodItem>>> listMenuItems(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.findMenuItems(restaurantId)));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank() || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return null;
        }
        return remoteAddr;
    }
}
