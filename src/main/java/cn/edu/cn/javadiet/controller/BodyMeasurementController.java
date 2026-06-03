package cn.edu.cn.javadiet.controller;

import cn.edu.cn.javadiet.common.ApiResponse;
import cn.edu.cn.javadiet.model.entity.BodyMeasurement;
import cn.edu.cn.javadiet.service.BodyMeasurementService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/body-measurements")
public class BodyMeasurementController {

    private final BodyMeasurementService service;

    public BodyMeasurementController(BodyMeasurementService service) {
        this.service = service;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<BodyMeasurement>> add(
            @PathVariable Long userId,
            @RequestBody BodyMeasurement body) {
        return ResponseEntity.ok(ApiResponse.ok(service.addMeasurement(userId, body)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BodyMeasurement>>> list(Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByUserId(userId)));
    }
}
