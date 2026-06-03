package cn.edu.cn.javadiet.service;

import cn.edu.cn.javadiet.model.entity.BodyMeasurement;
import java.util.List;

public interface BodyMeasurementService {
    BodyMeasurement addMeasurement(Long userId, BodyMeasurement measurement);
    List<BodyMeasurement> findByUserId(Long userId);
}
