package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionApplyResponse;
import cn.edu.cn.javadiet.model.dto.FoodNutritionConfigStatus;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPendingItem;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewJobStatus;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewRequest;
import cn.edu.cn.javadiet.model.dto.FoodNutritionPreviewResponse;
import java.util.List;

public interface FoodNutritionEnrichmentService {

    List<FoodNutritionPendingItem> findPending(String keyword, Integer limit);

    FoodNutritionPreviewResponse preview(FoodNutritionPreviewRequest request);

    FoodNutritionPreviewJobStatus startPreviewJob(FoodNutritionPreviewRequest request);

    FoodNutritionPreviewJobStatus getPreviewJob(String jobId);

    FoodNutritionPreviewJobStatus cancelPreviewJob(String jobId);

    FoodNutritionApplyResponse apply(FoodNutritionApplyRequest request);

    FoodNutritionConfigStatus configStatus();
}
