package com.autowash.mapper;

import com.autowash.dto.response.UserResponse;
import com.autowash.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }
}
