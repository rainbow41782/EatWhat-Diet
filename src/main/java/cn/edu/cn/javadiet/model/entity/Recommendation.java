package cn.edu.cn.javadiet.model.entity;

import cn.edu.cn.javadiet.model.enums.MealType;
import cn.edu.cn.javadiet.model.enums.RecommendationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long restaurantId;
    private LocalDateTime recommendationTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MealType mealType;

    @Column(length = 1000)
    private String recommendedReason;
    private Double targetCalories;
    private Double targetProtein;
    private Double targetFat;
    private Double targetCarb;
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecommendationStatus status;
    private LocalDateTime createdAt;
}
