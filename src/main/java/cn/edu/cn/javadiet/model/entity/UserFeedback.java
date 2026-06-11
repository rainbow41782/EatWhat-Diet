package cn.edu.cn.javadiet.model.entity;

import cn.edu.cn.javadiet.model.enums.FeedbackType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_feedback")
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long recommendationId;

    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeedbackType feedbackType;

    @Column(length = 1000)
    private String content;
    private Boolean useful;
    private LocalDateTime createdAt;
}
