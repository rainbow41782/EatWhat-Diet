package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.DailyCheckIn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {

    Optional<DailyCheckIn> findByUserIdAndCheckDate(Long userId, LocalDate checkDate);

    List<DailyCheckIn> findByUserIdOrderByCheckDateDesc(Long userId);
}
