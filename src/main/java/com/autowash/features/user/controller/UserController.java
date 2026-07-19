package com.autowash.features.user.controller;

import com.autowash.features.user.dto.request.CreateStaffRequest;
import com.autowash.features.user.dto.request.UpdateUserByAdminRequest;
import com.autowash.features.user.dto.request.UpdateUserPointsRequest;
import com.autowash.features.user.dto.response.AdminUserResponse;
import com.autowash.features.user.dto.response.AdminUserDetailResponse;
import com.autowash.features.user.dto.response.UserResponse;
import com.autowash.features.user.service.UserService;
import com.autowash.features.car.service.CarService;
import com.autowash.features.car.dto.request.CreateCarRequest;
import com.autowash.features.car.dto.response.CarResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CarService carService;

    @GetMapping("/users")
    public List<AdminUserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/staff")
    public UserResponse createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return userService.createStaff(request);
    }

    @PutMapping("/users/{id}/lock")
    public UserResponse lockUser(@PathVariable Long id) {
        return userService.lockUser(id);
    }

    @PutMapping("/users/{id}/unlock")
    public UserResponse unlockUser(@PathVariable Long id) {
        return userService.unlockUser(id);
    }

    @GetMapping("/users/{id}")
    public AdminUserDetailResponse getUserDetail(@PathVariable Long id) {
        return userService.getAdminUserDetail(id);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUserByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserByAdminRequest request
    ) {
        return userService.updateUserByAdmin(id, request);
    }

    @PatchMapping("/users/{id}/points")
    public UserResponse updatePointsAndRecalculateTier(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserPointsRequest request
    ) {
        return userService.updatePointsAndRecalculateTier(id, request);
    }

    @PostMapping("/users/{userId}/vehicles")
    public CarResponse addVehicle(
            @PathVariable Long userId,
            @Valid @RequestBody CreateCarRequest request
    ) {
        return carService.createCarByAdmin(userId, request);
    }

    @DeleteMapping("/users/{userId}/vehicles/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(
            @PathVariable Long userId,
            @PathVariable Long vehicleId
    ) {
        carService.deleteCarByAdmin(userId, vehicleId);
        return ResponseEntity.ok(Map.of("message", "Xóa xe thành công"));
    }
}
