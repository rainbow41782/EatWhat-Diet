package cn.edu.cn.javadiet.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "food_nutrition_references",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source", "fdc_id"}))
public class FoodNutritionReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "fdc_id", nullable = false)
    private Long fdcId;

    @Column(length = 500)
    private String description;

    @Column(length = 80)
    private String dataType;

    @Column(length = 200)
    private String foodCategory;

    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbPer100g;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
