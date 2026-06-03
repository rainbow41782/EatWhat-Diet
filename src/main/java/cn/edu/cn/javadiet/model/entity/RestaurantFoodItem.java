package cn.edu.cn.javadiet.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restaurant_food_items")
public class RestaurantFoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private Long foodItemId;
    private BigDecimal price;
    private Boolean available;
    private String portionSize;
    private String categoryName;
    private String skuId;
    @Column(length = 1000)
    private String imageUrl;
    @Column(length = 1000)
    private String rawSpec;
    @Column(columnDefinition = "TEXT")
    private String sourcePayloadJson;
    private String remark;
    private String externalSource;
    private String externalFoodId;
}
