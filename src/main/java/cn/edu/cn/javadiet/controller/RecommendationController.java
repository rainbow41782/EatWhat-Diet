package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.RecommendationRequest;
import cn.edu.cn.javadiet.model.dto.RecommendationResponse;
import cn.edu.cn.javadiet.service.RecommendationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> generate(@RequestBody RecommendationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.generate(request)));
    }

    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> generateDaily(@RequestBody RecommendationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.generateDaily(request)));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> current(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.findCurrent(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> history(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.findHistory(userId)));
    }

    @PatchMapping("/{recommendationId}/accept")
    public ResponseEntity<ApiResponse<RecommendationResponse>> accept(@PathVariable Long recommendationId) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.accept(recommendationId)));
    }

    @PatchMapping("/{recommendationId}/ignore")
    public ResponseEntity<ApiResponse<RecommendationResponse>> ignore(@PathVariable Long recommendationId) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.ignore(recommendationId)));
    }
}
