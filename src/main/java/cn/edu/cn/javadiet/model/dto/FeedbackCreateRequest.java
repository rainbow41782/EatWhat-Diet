package cn.edu.cn.javadiet.model.dto;

import cn.edu.cn.javadiet.model.enums.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackCreateRequest {

    private Long userId;
    private Long recommendationId;
    private Integer rating;
    private FeedbackType feedbackType;
    private String content;
    private Boolean useful;
}
