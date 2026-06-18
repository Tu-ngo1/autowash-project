package com.autowash.controller;

import com.autowash.dto.request.CreateStaffRequest;
import com.autowash.dto.response.UserResponse;
import com.autowash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.autowash.dto.response.AdminCustomerResponse;
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
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


    @GetMapping("/customers")
    public List<AdminCustomerResponse> getAllCustomersForAdmin() {
        return userService.getAllCustomersForAdmin();
    }

}