package com.autowash.features.washservice.controller;

import com.autowash.features.user.entity.User;
import com.autowash.features.user.service.UserService;
import com.autowash.features.washservice.dto.response.ServiceResponse;
import com.autowash.features.washservice.dto.response.AvailableServiceResponse;
import com.autowash.features.washservice.service.WashService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final WashService washService;
    private final UserService userService;

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

    @GetMapping("/customer/cars/{carId}/services")
    public List<AvailableServiceResponse> getServicesForCar(
            @PathVariable Long carId
    ) {
        User currentUser = userService.getCurrentUserEntity();
        return washService.getServicesForCustomerCar(currentUser.getId(), carId);
    }
}
