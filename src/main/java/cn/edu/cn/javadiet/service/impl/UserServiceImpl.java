package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.LocationUpdateRequest;
import cn.edu.cn.javadiet.model.dto.LoginRequest;
import cn.edu.cn.javadiet.model.dto.LoginResponse;
import cn.edu.cn.javadiet.model.dto.RegisterRequest;
import cn.edu.cn.javadiet.model.dto.UpdateUserBasicRequest;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserLoginSession;
import cn.edu.cn.javadiet.model.entity.UserProfile;
import cn.edu.cn.javadiet.model.enums.AccountStatus;
import cn.edu.cn.javadiet.model.enums.Gender;
import cn.edu.cn.javadiet.model.enums.UserRole;
import cn.edu.cn.javadiet.repository.UserLoginSessionRepository;
import cn.edu.cn.javadiet.repository.UserProfileRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import cn.edu.cn.javadiet.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLoginSessionRepository userLoginSessionRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserLoginSessionRepository userLoginSessionRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userLoginSessionRepository = userLoginSessionRepository;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        requireText(request.getUsername(), "username is required");
        requireText(request.getPassword(), "password is required");
        boolean usernameExists = userRepository.existsByUsernameIgnoreCase(request.getUsername());
        if (usernameExists) {
            throw new IllegalStateException("username already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(hashPassword(request.getPassword()))
                .role(UserRole.USER)
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .email(request.getEmail())
                .gender(request.getGender())
                .age(request.getAge())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(AccountStatus.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .userId(user.getId())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .bmr(calculateBmr(user, request.getHeightCm(), request.getWeightKg()))
                .updatedAt(now)
                .build();
        userProfileRepository.save(profile);

        return user;
    }

    @Override
    @Transactional
    public User login(LoginRequest request) {
        return loginWithSession(request).getUser();
    }

    @Override
    @Transactional
    public LoginResponse loginWithSession(LoginRequest request) {
        requireText(request.getUsername(), "username is required");
        requireText(request.getPassword(), "password is required");
        User user = userRepository.findByUsernameIgnoreCase(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));
        if (!hashPassword(request.getPassword()).equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }
        if (user.getStatus() == AccountStatus.DISABLED) {
            throw new IllegalStateException("account is disabled");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        UserLoginSession session = UserLoginSession.builder()
                .userId(user.getId())
                .token(token)
                .active(true)
                .loginAt(LocalDateTime.now())
                .build();
        userLoginSessionRepository.save(session);
        return new LoginResponse(user, token);
    }

    @Override
    @Transactional
    public void logout(String token) {
        requireText(token, "token is required");
        UserLoginSession session = userLoginSessionRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new IllegalArgumentException("active login session not found"));
        session.setActive(false);
        session.setLogoutAt(LocalDateTime.now());
        userLoginSessionRepository.save(session);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<UserProfile> findProfile(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }

    @Override
    public List<User> findUsers(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword;
        List<User> users = normalizedKeyword.isBlank()
                ? userRepository.findAll()
                : userRepository
                        .findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCaseOrPhoneContainingOrEmailContainingIgnoreCase(
                                normalizedKeyword,
                                normalizedKeyword,
                                normalizedKeyword,
                                normalizedKeyword);
        return users.stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    @Override
    @Transactional
    public UserProfile updateProfile(Long userId, UserProfile profile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        userProfileRepository.findByUserId(userId)
                .ifPresent(oldProfile -> profile.setId(oldProfile.getId()));
        profile.setUserId(userId);
        profile.setBmr(calculateBmr(user, profile.getHeightCm(), profile.getWeightKg()));
        profile.setUpdatedAt(LocalDateTime.now());
        // 自动设置计划周期
        profile.setPlanStartDate(LocalDate.now());
        profile.setPlanEndDate(LocalDate.now().plusWeeks(planDurationWeeks(profile)));
        return userProfileRepository.save(profile);
    }

    /** 根据健康目标返回计划的保守最短周数 */
    private int planDurationWeeks(UserProfile profile) {
        if (profile.getHealthGoal() == null) return 8;
        return switch (profile.getHealthGoal()) {
            case RAPID_FAT_LOSS -> 4;
            case HIGH_INTENSITY_FAT_LOSS -> 6;
            case DAILY_FAT_LOSS -> 8;
            case LEAN_BULK, BULK, INCREASE_STRENGTH -> 12;
            default -> 8; // MAINTAIN, IMPROVE_PERFORMANCE, legacy values
        };
    }

    @Override
    @Transactional
    public User updateLocation(Long userId, LocationUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        user.setAddress(request.getAddress());
        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateBasicInfo(Long userId, UpdateUserBasicRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        requireText(oldPassword, "old password is required");
        requireText(newPassword, "new password is required");
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("new password must be at least 6 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!hashPassword(oldPassword).equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("old password is incorrect");
        }

        user.setPasswordHash(hashPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
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

    private static Double calculateBmr(User user, Double heightCm, Double weightKg) {
        if (user.getAge() == null || user.getGender() == null || heightCm == null || weightKg == null) {
            return null;
        }
        // Katch-McArdle via Deurenberg body-fat estimate (Asian correction +5%)
        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);
        double sex = user.getGender() == Gender.MALE ? 1.0 : 0.0;
        double bfFraction = (1.20 * bmi + 0.23 * user.getAge() - 10.8 * sex - 5.4 + 5.0) / 100.0;
        bfFraction = Math.max(0.05, Math.min(0.50, bfFraction));
        double lbm = weightKg * (1.0 - bfFraction);
        return Math.round((370.0 + 21.6 * lbm) * 10.0) / 10.0;
    }
}
