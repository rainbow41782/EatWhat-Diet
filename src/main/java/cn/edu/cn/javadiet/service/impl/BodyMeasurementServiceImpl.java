package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.model.entity.BodyMeasurement;
import cn.edu.cn.javadiet.repository.BodyMeasurementRepository;
import cn.edu.cn.javadiet.service.BodyMeasurementService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BodyMeasurementServiceImpl implements BodyMeasurementService {

    private final BodyMeasurementRepository repo;

    public BodyMeasurementServiceImpl(BodyMeasurementRepository repo) {
        this.repo = repo;
    }

    @Override
    public BodyMeasurement addMeasurement(Long userId, BodyMeasurement measurement) {
        measurement.setUserId(userId);
        if (measurement.getMeasureDate() == null) {
            measurement.setMeasureDate(LocalDate.now());
        }
        measurement.setCreatedAt(LocalDateTime.now());
        return repo.save(measurement);
    }

    @Override
    public List<BodyMeasurement> findByUserId(Long userId) {
        return repo.findByUserIdOrderByMeasureDateDesc(userId);
    }
}
