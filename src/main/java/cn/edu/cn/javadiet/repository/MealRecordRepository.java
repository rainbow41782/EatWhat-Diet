package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.MealRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    List<MealRecord> findByDailyCheckInId(Long dailyCheckInId);

    List<MealRecord> findByDailyCheckInIdIn(Collection<Long> dailyCheckInIds);

    List<MealRecord> findByUserIdOrderByMealTimeDesc(Long userId);
}
