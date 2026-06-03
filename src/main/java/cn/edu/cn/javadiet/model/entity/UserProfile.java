package cn.edu.cn.javadiet.model.entity;

import cn.edu.cn.javadiet.model.enums.ActivityLevel;
import cn.edu.cn.javadiet.model.enums.HealthGoal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;
    private Double heightCm;
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private HealthGoal healthGoal;

    private String dietPreference;
    private String allergies;
    private String dislikedFoods;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ActivityLevel activityLevel;

    private Double dailyCalorieTarget;
    private Double dailyProteinTarget;
    private Double dailyFatTarget;
    private Double dailyCarbTarget;
    private Double bmr;
    private String remark;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private LocalDateTime updatedAt;
}
