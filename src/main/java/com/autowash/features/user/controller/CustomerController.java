package com.autowash.features.user.controller;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.user.entity.User;

import com.autowash.features.user.dto.response.ProfileResponse;
import com.autowash.features.user.dto.response.UserResponse;
import com.autowash.features.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import com.autowash.features.user.dto.request.UpdateProfileRequest;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @GetMapping("/profile")
    public ProfileResponse getCurrentUserProfile() {
        return userService.getCurrentUserProfile();
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@RequestBody UpdateProfileRequest request) {
        return userService.updateCurrentUserProfile(request);
    }

}

