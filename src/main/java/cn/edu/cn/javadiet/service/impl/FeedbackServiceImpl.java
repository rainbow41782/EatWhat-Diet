package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.dto.FeedbackCreateRequest;
import cn.edu.cn.javadiet.model.entity.UserFeedback;
import cn.edu.cn.javadiet.repository.RecommendationRepository;
import cn.edu.cn.javadiet.repository.UserFeedbackRepository;
import cn.edu.cn.javadiet.repository.UserRepository;
import cn.edu.cn.javadiet.service.FeedbackService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final UserRepository userRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserFeedbackRepository userFeedbackRepository;

    public FeedbackServiceImpl(
            UserRepository userRepository,
            RecommendationRepository recommendationRepository,
            UserFeedbackRepository userFeedbackRepository) {
        this.userRepository = userRepository;
        this.recommendationRepository = recommendationRepository;
        this.userFeedbackRepository = userFeedbackRepository;
    }

    @Override
    public UserFeedback submit(FeedbackCreateRequest request) {
        if (!userRepository.existsById(request.getUserId())) {
            throw new IllegalArgumentException("user not found");
        }
        if (!recommendationRepository.existsById(request.getRecommendationId())) {
            throw new IllegalArgumentException("recommendation not found");
        }
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }

        UserFeedback feedback = UserFeedback.builder()
                .userId(request.getUserId())
                .recommendationId(request.getRecommendationId())
                .rating(request.getRating())
                .feedbackType(request.getFeedbackType())
                .content(request.getContent())
                .useful(request.getUseful())
                .createdAt(LocalDateTime.now())
                .build();
        return userFeedbackRepository.save(feedback);
    }

    @Override
    public List<UserFeedback> findByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("user not found");
        }
        return userFeedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
