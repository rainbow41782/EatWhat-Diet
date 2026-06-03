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
@Table(name = "food_intakes")
public class FoodIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mealRecordId;

    private Long foodItemId;
    private Double quantity;
    private String unit;
    private Double calories;
    private Double protein;
    private Double fat;
    private Double carb;
    private Double fiber;
    private LocalDateTime intakeTime;
    private String remark;
}
