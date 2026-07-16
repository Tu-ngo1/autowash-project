package com.autowash.features.washservice.controller;

import com.autowash.features.washservice.dto.request.CreateServiceRequest;
import com.autowash.features.washservice.dto.response.AdminServiceResponse;
import com.autowash.features.washservice.service.WashService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
public class AdminServiceController {

    private final WashService washService;

    @GetMapping
    public ResponseEntity<List<AdminServiceResponse>> getAdminServices() {
        List<AdminServiceResponse> services = washService.getAdminServicesWithPrices();
        return ResponseEntity.ok(services);
    }

    @PostMapping
    public ResponseEntity<AdminServiceResponse> createService(
            @RequestBody CreateServiceRequest request
    ) {
        AdminServiceResponse saved = washService.createServiceWithPrices(request);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminServiceResponse> updateService(
            @PathVariable Long id,
            @RequestBody CreateServiceRequest request
    ) {
        AdminServiceResponse saved = washService.updateServiceWithPrices(id, request);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        washService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminServiceResponse> patchServiceStatus(
            @PathVariable Long id,
            @RequestParam Boolean active
    ) {
        AdminServiceResponse saved = washService.patchServiceStatus(id, active);
        return ResponseEntity.ok(saved);
    }
}
