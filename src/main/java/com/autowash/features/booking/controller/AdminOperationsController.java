package com.autowash.features.booking.controller;

import com.autowash.features.booking.dto.request.UpdateDailyConfigRequest;
import com.autowash.features.booking.entity.DailyOperationsConfig;
import com.autowash.features.booking.service.OperationsConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operations")
@RequiredArgsConstructor
public class AdminOperationsController {

    private final OperationsConfigService configService;

    @PostMapping("/config-tomorrow")
    public ResponseEntity<DailyOperationsConfig> updateConfigForTomorrow(
            @RequestBody UpdateDailyConfigRequest request
    ) {
        DailyOperationsConfig savedConfig = configService.updateConfigForTomorrow(request);
        return ResponseEntity.ok(savedConfig);
    }
}
