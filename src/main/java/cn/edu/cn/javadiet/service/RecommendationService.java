package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.RecommendationRequest;
import cn.edu.cn.javadiet.model.entity.Recommendation;
import java.util.List;

public interface RecommendationService {

    List<Recommendation> generate(RecommendationRequest request);

    List<Recommendation> findHistory(Long userId);

    Recommendation accept(Long recommendationId);

    Recommendation ignore(Long recommendationId);
}
