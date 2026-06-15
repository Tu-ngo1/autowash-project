package com.autowash.dto.response;

import com.autowash.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String username;
    private String role;
    private String status;

    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}