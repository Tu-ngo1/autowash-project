package com.autowash.controller;

import com.autowash.dto.response.ServiceResponse;
import com.autowash.service.WashService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final WashService washService;

    // Customer lấy danh sách dịch vụ đang hoạt động
    @GetMapping
    public List<ServiceResponse> getActiveServices() {
        return washService.getActiveServicesForCustomer();
    }

    // Admin lấy tất cả dịch vụ
    @GetMapping("/admin")
    public List<ServiceResponse> getAllServicesForAdmin() {
        return washService.getAllServicesForAdmin();
    }

    // Lấy chi tiết 1 service
    @GetMapping("/{id}")
    public ServiceResponse getServiceById(@PathVariable Long id) {
        return washService.getServiceById(id);
    }
}