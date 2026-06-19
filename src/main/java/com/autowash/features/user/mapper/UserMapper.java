package com.autowash.features.user.mapper;

import com.autowash.features.user.dto.AdminUserResponse;
import com.autowash.features.user.dto.UserResponse;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.enums.TierLevel;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null
        );
    }

    public AdminUserResponse toAdminUserResponse(User user, int carCount, int bookingCount) {
        if (user == null) return null;

        CustomerProfile profile = user.getCustomerProfile();
        TierLevel tierLevel = (profile != null && profile.getTierConfig() != null)
                ? profile.getTierConfig().getTierLevel()
                : null;
        Integer rewardPoints = profile != null ? profile.getRewardPoints() : null;
        Integer tierPoints = profile != null ? profile.getTierPoints() : null;

        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus(),
                tierLevel,
                rewardPoints,
                tierPoints,
                carCount,
                bookingCount,
                user.getRole()
        );
    }
}


