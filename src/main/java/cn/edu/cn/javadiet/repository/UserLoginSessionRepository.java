package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.UserLoginSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserLoginSessionRepository extends JpaRepository<UserLoginSession, Long> {

    Optional<UserLoginSession> findByTokenAndActiveTrue(String token);

    List<UserLoginSession> findByUserIdOrderByLoginAtDesc(Long userId);

    @Modifying
    @Query("update UserLoginSession session " +
            "set session.active = false, " +
            "session.logoutAt = :logoutAt " +
            "where session.userId = :userId " +
            "and session.active = true")
    int logoutActiveSessions(@Param("userId") Long userId, @Param("logoutAt") LocalDateTime logoutAt);
}
