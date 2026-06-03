package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.ChangePasswordRequest;
import cn.edu.cn.javadiet.model.dto.LocationUpdateRequest;
import cn.edu.cn.javadiet.model.dto.NutritionPlanPreviewRequest;
import cn.edu.cn.javadiet.model.dto.NutritionPlanResult;
import cn.edu.cn.javadiet.model.dto.UpdateUserBasicRequest;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserProfile;
import cn.edu.cn.javadiet.service.UserService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfile>> getProfile(@PathVariable Long userId) {
        UserProfile profile = userService.findProfile(userId).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> listUsers(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findUsers(keyword)));
    }

    @PatchMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfile(
            @PathVariable Long userId,
            @RequestBody UserProfile profile) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(userId, profile)));
    }

    @PostMapping("/{userId}/nutrition-plan-preview")
    public ResponseEntity<ApiResponse<NutritionPlanResult>> previewNutritionPlan(
            @PathVariable Long userId,
            @RequestBody NutritionPlanPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.previewNutritionPlan(userId, request)));
    }

    @PatchMapping("/{userId}/location")
    public ResponseEntity<ApiResponse<User>> updateLocation(
            @PathVariable Long userId,
            @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateLocation(userId, request)));
    }

    @PatchMapping("/{userId}/basic")
    public ResponseEntity<ApiResponse<User>> updateBasic(
            @PathVariable Long userId,
            @RequestBody UpdateUserBasicRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateBasicInfo(userId, request)));
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long userId,
            @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("password updated", null));
    }
}
