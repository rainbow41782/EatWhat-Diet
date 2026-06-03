package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.UserFeedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    List<UserFeedback> findByUserIdOrderByCreatedAtDesc(Long userId);
}
