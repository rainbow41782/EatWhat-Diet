package cn.edu.cn.javadiet.model.entity;

import cn.edu.cn.javadiet.model.enums.MeituanCrawlStatus;
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
@Table(name = "meituan_crawl_tasks")
public class MeituanCrawlTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String shopName;

    private String address;
    private Double latitude;
    private Double longitude;
    private String meituanPoiId;

    @Column(length = 1000)
    private String menuUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeituanCrawlStatus status;

    @Column(length = 1000)
    private String failureReason;

    private Integer attemptCount;
    private Integer importedItemCount;
    private Long restaurantId;
    private LocalDateTime lastStartedAt;
    private LocalDateTime lastFinishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
