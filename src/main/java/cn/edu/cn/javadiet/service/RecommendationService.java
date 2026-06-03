package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.RecommendationRequest;
import cn.edu.cn.javadiet.model.dto.RecommendationResponse;
import java.util.List;

public interface RecommendationService {

    List<RecommendationResponse> generate(RecommendationRequest request);

    List<RecommendationResponse> generateDaily(RecommendationRequest request);

    List<RecommendationResponse> findCurrent(Long userId);

    List<RecommendationResponse> findHistory(Long userId);

    RecommendationResponse accept(Long recommendationId);

    RecommendationResponse ignore(Long recommendationId);
}
