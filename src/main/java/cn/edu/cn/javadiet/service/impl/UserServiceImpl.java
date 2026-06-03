package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.LocationUpdateRequest;
import cn.edu.cn.javadiet.model.dto.LoginRequest;
import cn.edu.cn.javadiet.model.dto.LoginResponse;
import cn.edu.cn.javadiet.model.dto.NutritionPlanPreviewRequest;
import cn.edu.cn.javadiet.model.dto.NutritionPlanResult;
import cn.edu.cn.javadiet.model.dto.RegisterRequest;
import cn.edu.cn.javadiet.model.dto.UpdateUserBasicRequest;
import cn.edu.cn.javadiet.model.entity.User;
import cn.edu.cn.javadiet.model.entity.UserLoginSession;
import cn.edu.cn.javadiet.model.entity.UserProfile;
import cn.edu.cn.javadiet.model.enums.AccountStatus;
import cn.edu.cn.javadiet.model.enums.ActivityLevel;
import cn.edu.cn.javadiet.model.enums.Gender;
import cn.edu.cn.javadiet.model.enums.HealthGoal;
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
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private static final class GoalPlanConfig {
        final double calorieFactor;
        final boolean proteinByLeanBodyMass;
        final double proteinFactor;
        final double fatFactor;
        final int durationMinWeeks;
        final int durationMaxWeeks;

        private GoalPlanConfig(
                double calorieFactor,
                boolean proteinByLeanBodyMass,
                double proteinFactor,
                double fatFactor,
                int durationMinWeeks,
                int durationMaxWeeks) {
            this.calorieFactor = calorieFactor;
            this.proteinByLeanBodyMass = proteinByLeanBodyMass;
            this.proteinFactor = proteinFactor;
            this.fatFactor = fatFactor;
            this.durationMinWeeks = durationMinWeeks;
            this.durationMaxWeeks = durationMaxWeeks;
        }
    }

    private static final Map<HealthGoal, GoalPlanConfig> GOAL_PLAN_CONFIGS = new EnumMap<>(HealthGoal.class);
    private static final Map<ActivityLevel, Double> ACTIVITY_MULTIPLIERS = new EnumMap<>(ActivityLevel.class);

    static {
        GOAL_PLAN_CONFIGS.put(HealthGoal.RAPID_FAT_LOSS, new GoalPlanConfig(0.80, true, 2.6, 0.60, 1, 4));
        GOAL_PLAN_CONFIGS.put(HealthGoal.HIGH_INTENSITY_FAT_LOSS, new GoalPlanConfig(0.85, true, 2.4, 0.65, 8, 16));
        GOAL_PLAN_CONFIGS.put(HealthGoal.DAILY_FAT_LOSS, new GoalPlanConfig(0.90, true, 2.2, 0.70, 15, 20));
        GOAL_PLAN_CONFIGS.put(HealthGoal.LEAN_BULK, new GoalPlanConfig(1.05, false, 2.0, 0.80, 8, 12));
        GOAL_PLAN_CONFIGS.put(HealthGoal.BULK, new GoalPlanConfig(1.12, false, 1.9, 0.90, 8, 12));
        GOAL_PLAN_CONFIGS.put(HealthGoal.MAINTAIN, new GoalPlanConfig(1.00, false, 1.8, 0.80, 1, 52));
        GOAL_PLAN_CONFIGS.put(HealthGoal.INCREASE_STRENGTH, new GoalPlanConfig(1.03, false, 1.8, 0.70, 6, 8));
        GOAL_PLAN_CONFIGS.put(HealthGoal.IMPROVE_PERFORMANCE, new GoalPlanConfig(1.05, false, 1.7, 0.65, 8, 16));

        ACTIVITY_MULTIPLIERS.put(ActivityLevel.SEDENTARY, 1.2);
        ACTIVITY_MULTIPLIERS.put(ActivityLevel.LIGHTLY_ACTIVE, 1.375);
        ACTIVITY_MULTIPLIERS.put(ActivityLevel.MODERATELY_ACTIVE, 1.55);
        ACTIVITY_MULTIPLIERS.put(ActivityLevel.VERY_ACTIVE, 1.725);
        ACTIVITY_MULTIPLIERS.put(ActivityLevel.EXTRA_ACTIVE, 1.9);
        ACTIVITY_MULTIPLIERS.put(ActivityLevel.LOW, 1.2);
        ACTIVITY_MULTIPLIERS.put(ActivityLevel.MEDIUM, 1.55);
        ACTIVITY_MULTIPLIERS.put(ActivityLevel.HIGH, 1.725);
    }

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
        public NutritionPlanResult previewNutritionPlan(Long userId, NutritionPlanPreviewRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));

        User workingUser = User.builder()
            .id(user.getId())
            .gender(request.getGender() != null ? request.getGender() : user.getGender())
            .age(request.getAge() != null ? request.getAge() : user.getAge())
            .build();

        UserProfile profile = UserProfile.builder()
            .heightCm(request.getHeightCm())
            .weightKg(request.getWeightKg())
            .healthGoal(request.getHealthGoal())
            .activityLevel(request.getActivityLevel())
            .build();

        return calculateNutritionPlan(workingUser, profile, true);
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
        UserProfile merged = userProfileRepository.findByUserId(userId)
                .orElseGet(UserProfile::new);

        merged.setUserId(userId);
        if (profile.getHeightCm() != null) merged.setHeightCm(profile.getHeightCm());
        if (profile.getWeightKg() != null) merged.setWeightKg(profile.getWeightKg());
        if (profile.getHealthGoal() != null) merged.setHealthGoal(profile.getHealthGoal());
        if (profile.getDietPreference() != null) merged.setDietPreference(profile.getDietPreference());
        if (profile.getAllergies() != null) merged.setAllergies(profile.getAllergies());
        if (profile.getDislikedFoods() != null) merged.setDislikedFoods(profile.getDislikedFoods());
        if (profile.getActivityLevel() != null) merged.setActivityLevel(profile.getActivityLevel());
        if (profile.getRemark() != null) merged.setRemark(profile.getRemark());

        merged.setHealthGoal(normalizeHealthGoal(merged.getHealthGoal()));
        NutritionPlanResult calculated = calculateNutritionPlan(user, merged, false);
        if (calculated != null) {
            merged.setBmr(calculated.getBmr());
            merged.setDailyCalorieTarget(calculated.getTargetCalories());
            merged.setDailyProteinTarget(calculated.getTargetProtein());
            merged.setDailyFatTarget(calculated.getTargetFat());
            merged.setDailyCarbTarget(calculated.getTargetCarb());
            merged.setPlanStartDate(LocalDate.now());
            merged.setPlanEndDate(LocalDate.now().plusWeeks(calculated.getDurationMinWeeks()));
        } else {
            merged.setBmr(calculateBmr(user, merged.getHeightCm(), merged.getWeightKg()));
        }
        merged.setUpdatedAt(LocalDateTime.now());
        return userProfileRepository.save(merged);
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
        double bodyFat = estimateBodyFatPct(user, heightCm, weightKg);
        double leanBodyMass = weightKg * (1.0 - bodyFat / 100.0);
        return round1(370.0 + 21.6 * leanBodyMass);
    }

    private static double estimateBodyFatPct(User user, Double heightCm, Double weightKg) {
        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);
        double sex = user.getGender() == Gender.MALE ? 1.0 : 0.0;
        double bodyFat = 1.20 * bmi + 0.23 * user.getAge() - 10.8 * sex - 5.4 + 5.0;
        return Math.max(5.0, Math.min(70.0, bodyFat));
    }

    private static HealthGoal normalizeHealthGoal(HealthGoal goal) {
        if (goal == null) return null;
        return switch (goal) {
            case FAT_LOSS -> HealthGoal.DAILY_FAT_LOSS;
            case MUSCLE_GAIN -> HealthGoal.LEAN_BULK;
            case BLOOD_SUGAR_CONTROL -> HealthGoal.MAINTAIN;
            default -> goal;
        };
    }

    private NutritionPlanResult calculateNutritionPlan(User user, UserProfile profile, boolean strictValidation) {
        if (profile == null) {
            if (strictValidation) throw new IllegalArgumentException("profile is required");
            return null;
        }
        if (user.getAge() == null || user.getGender() == null || profile.getHeightCm() == null || profile.getWeightKg() == null
                || profile.getHealthGoal() == null || profile.getActivityLevel() == null) {
            if (strictValidation) {
                throw new IllegalArgumentException("gender, age, heightCm, weightKg, healthGoal, activityLevel are required");
            }
            return null;
        }

        HealthGoal normalizedGoal = normalizeHealthGoal(profile.getHealthGoal());
        GoalPlanConfig config = GOAL_PLAN_CONFIGS.get(normalizedGoal);
        Double activityMultiplier = ACTIVITY_MULTIPLIERS.get(profile.getActivityLevel());
        if (config == null || activityMultiplier == null) {
            if (strictValidation) {
                throw new IllegalArgumentException("unsupported healthGoal or activityLevel");
            }
            return null;
        }

        double bodyFatPct = estimateBodyFatPct(user, profile.getHeightCm(), profile.getWeightKg());
        double leanBodyMass = profile.getWeightKg() * (1.0 - bodyFatPct / 100.0);
        double bmr = 370.0 + 21.6 * leanBodyMass;
        double tdee = bmr * activityMultiplier;

        double targetCalories = Math.round(Math.max(Math.max(tdee * config.calorieFactor, bmr * 1.2), 1200.0));
        double proteinBase = config.proteinByLeanBodyMass ? leanBodyMass : profile.getWeightKg();
        double targetProtein = Math.round(config.proteinFactor * proteinBase);
        double targetFat = Math.round(Math.max(config.fatFactor * profile.getWeightKg(), profile.getWeightKg() * 0.5));
        double targetCarb = Math.max(0.0, Math.round((targetCalories - targetProtein * 4.0 - targetFat * 9.0) / 4.0));

        double macroCalories = targetProtein * 4.0 + targetFat * 9.0 + targetCarb * 4.0;
        double proteinPct = macroCalories > 0 ? Math.round((targetProtein * 4.0 / macroCalories) * 100.0) : 0.0;
        double fatPct = macroCalories > 0 ? Math.round((targetFat * 9.0 / macroCalories) * 100.0) : 0.0;
        double carbPct = macroCalories > 0 ? Math.round((targetCarb * 4.0 / macroCalories) * 100.0) : 0.0;

        return NutritionPlanResult.builder()
                .healthGoal(normalizedGoal)
                .estimatedBodyFatPct(round1(bodyFatPct))
                .leanBodyMassKg(round1(leanBodyMass))
                .bmr(round1(bmr))
                .tdee(round1(tdee))
                .targetCalories(targetCalories)
                .calorieDelta((double) Math.round(targetCalories - tdee))
                .targetProtein(targetProtein)
                .targetFat(targetFat)
                .targetCarb(targetCarb)
                .macroProteinPct(proteinPct)
                .macroFatPct(fatPct)
                .macroCarbPct(carbPct)
                .durationMinWeeks(config.durationMinWeeks)
                .durationMaxWeeks(config.durationMaxWeeks)
                .build();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
