package cn.edu.cn.javadiet.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "food_items")
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String category;
    @Column(length = 1000)
    private String description;
    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbPer100g;
    private Double fiberPer100g;
    private Double sugarPer100g;
    private Double sodiumPer100g;
    private String allergenInfo;
    private String nutritionStatus;
    private Boolean isRecommended;
    private String externalSource;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
