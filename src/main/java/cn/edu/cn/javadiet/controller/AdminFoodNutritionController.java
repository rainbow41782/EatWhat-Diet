package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyResponse;
import cn.edu.cn.javadiet.model.dto.FoodNutritionConfigStatus;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPendingItem;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewJobStatus;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewResponse;
import cn.edu.cn.javadiet.service.FoodNutritionEnrichmentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/foods/nutrition")
public class AdminFoodNutritionController {

    private final FoodNutritionEnrichmentService enrichmentService;

    public AdminFoodNutritionController(FoodNutritionEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<FoodNutritionPendingItem>>> pending(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(enrichmentService.findPending(keyword, limit)));
    }

    @PostMapping("/enrich-preview")
    public ResponseEntity<ApiResponse<FoodNutritionPreviewResponse>> enrichPreview(
            @RequestBody FoodNutritionPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(enrichmentService.preview(request)));
    }

    @PostMapping("/enrich-preview-jobs")
    public ResponseEntity<ApiResponse<FoodNutritionPreviewJobStatus>> startEnrichPreviewJob(
            @RequestBody FoodNutritionPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(enrichmentService.startPreviewJob(request)));
    }

    @GetMapping("/enrich-preview-jobs/{jobId}")
    public ResponseEntity<ApiResponse<FoodNutritionPreviewJobStatus>> enrichPreviewJobStatus(
            @PathVariable String jobId) {
        return ResponseEntity.ok(ApiResponse.ok(enrichmentService.getPreviewJob(jobId)));
    }

    @PostMapping("/enrich-preview-jobs/{jobId}/cancel")
    public ResponseEntity<ApiResponse<FoodNutritionPreviewJobStatus>> cancelEnrichPreviewJob(
            @PathVariable String jobId) {
        return ResponseEntity.ok(ApiResponse.ok(enrichmentService.cancelPreviewJob(jobId)));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<FoodNutritionApplyResponse>> apply(
            @RequestBody FoodNutritionApplyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(enrichmentService.apply(request)));
    }

    @GetMapping("/config/status")
    public ResponseEntity<ApiResponse<FoodNutritionConfigStatus>> configStatus() {
        return ResponseEntity.ok(ApiResponse.ok(enrichmentService.configStatus()));
    }
}
