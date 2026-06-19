package com.autowash.features.user.controller;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.user.entity.User;

import com.autowash.features.user.dto.CreateStaffRequest;
import com.autowash.features.user.dto.UserResponse;
import com.autowash.features.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.autowash.features.user.dto.AdminUserResponse;
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public List<AdminUserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/staff")
    public UserResponse createStaff(@RequestBody CreateStaffRequest request) {
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


}

