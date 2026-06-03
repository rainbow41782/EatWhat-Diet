package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.FeedbackCreateRequest;
import cn.edu.cn.javadiet.model.entity.UserFeedback;
import cn.edu.cn.javadiet.service.FeedbackService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserFeedback>> submit(@RequestBody FeedbackCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.submit(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserFeedback>>> list(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.findByUserId(userId)));
    }
}
