package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.LocationUpdateRequest;
import cn.edu.cn.javadiet.model.dto.LoginRequest;
import cn.edu.cn.javadiet.model.dto.LoginResponse;
import cn.edu.cn.javadiet.model.dto.NutritionPlanPreviewRequest;
import cn.edu.cn.javadiet.model.dto.NutritionPlanResult;
import cn.edu.cn.javadiet.model.dto.RegisterRequest;
import cn.edu.cn.javadiet.model.dto.UpdateUserBasicRequest;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserProfile;
import java.util.List;
import java.util.Optional;

public interface UserService {

    User register(RegisterRequest request);

    User login(LoginRequest request);

    LoginResponse loginWithSession(LoginRequest request);

    void logout(String token);

    Optional<User> findById(Long userId);

    Optional<UserProfile> findProfile(Long userId);

    NutritionPlanResult previewNutritionPlan(Long userId, NutritionPlanPreviewRequest request);

    List<User> findUsers(String keyword);

    UserProfile updateProfile(Long userId, UserProfile profile);

    User updateLocation(Long userId, LocationUpdateRequest request);

    User updateBasicInfo(Long userId, UpdateUserBasicRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
