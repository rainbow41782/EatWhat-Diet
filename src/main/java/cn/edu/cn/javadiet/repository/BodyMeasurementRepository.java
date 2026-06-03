package cn.edu.cn.javadiet.repository;

import cn.edu.cn.javadiet.model.entity.BodyMeasurement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyMeasurementRepository extends JpaRepository<BodyMeasurement, Long> {
    List<BodyMeasurement> findByUserIdOrderByMeasureDateDesc(Long userId);
}
