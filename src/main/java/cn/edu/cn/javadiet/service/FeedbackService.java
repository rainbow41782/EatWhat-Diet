package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.FeedbackCreateRequest;
import cn.edu.cn.javadiet.model.entity.UserFeedback;
import java.util.List;

public interface FeedbackService {

    UserFeedback submit(FeedbackCreateRequest request);

    List<UserFeedback> findByUserId(Long userId);
}
