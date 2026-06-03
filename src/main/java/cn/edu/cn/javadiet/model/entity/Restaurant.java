package cn.edu.cn.javadiet.model.entity;

import cn.edu.cn.javadiet.model.enums.RestaurantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String businessHours;
    private BigDecimal avgPrice;
    private Double rating;
    private String tags;
    private Boolean deliverySupported;
    private String externalSource;
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RestaurantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
