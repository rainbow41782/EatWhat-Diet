package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.Recommendation;
import cn.edu.cn.javadiet.model.enums.RecommendationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Recommendation> findByUserIdAndCreatedAtBetweenAndStatusInOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end,
            Collection<RecommendationStatus> statuses);
}
