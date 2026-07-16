package com.autowash.features.booking.controller;

import com.autowash.features.booking.dto.request.UpdateDailyConfigRequest;
import com.autowash.features.booking.entity.DailyOperationsConfig;
import com.autowash.features.booking.service.OperationsConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

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

    @GetMapping("/config-tomorrow")
    public ResponseEntity<DailyOperationsConfig> getTomorrowConfig() {
        DailyOperationsConfig config = configService.getTomorrowConfig();
        return ResponseEntity.ok(config);
    }

    @GetMapping("/config")
    public ResponseEntity<DailyOperationsConfig> getConfigForDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now().plusDays(1);
        DailyOperationsConfig config = configService.getConfigForDate(targetDate);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/config")
    public ResponseEntity<DailyOperationsConfig> updateConfigForDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody UpdateDailyConfigRequest request
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now().plusDays(1);
        DailyOperationsConfig savedConfig = configService.updateConfigForDate(targetDate, request);
        return ResponseEntity.ok(savedConfig);
    }
}
