package com.autowash.mapper;

import com.autowash.dto.response.AdminCustomerResponse;
import com.autowash.dto.response.ProfileResponse;
import com.autowash.entity.CustomerProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponse toResponse(CustomerProfile profile) {
        if (profile == null) return null;
        return new ProfileResponse(
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getUser() != null ? profile.getUser().getFullName() : null,
                profile.getUser() != null ? profile.getUser().getPhone() : null,
                profile.getUser() != null ? profile.getUser().getEmail() : null,
                profile.getRewardPoints(),
                profile.getTierPoints()
        );
    }

    public AdminCustomerResponse toAdminResponse(CustomerProfile profile, int carCount, int bookingCount) {
        if (profile == null) return null;
        return new AdminCustomerResponse(
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getUser() != null ? profile.getUser().getFullName() : null,
                profile.getUser() != null ? profile.getUser().getPhone() : null,
                profile.getUser() != null ? profile.getUser().getEmail() : null,
                profile.getUser() != null ? profile.getUser().getStatus() : null,
                profile.getTierConfig() != null ? profile.getTierConfig().getTierLevel() : null,
                profile.getRewardPoints(),
                profile.getTierPoints(),
                carCount,
                bookingCount,
                profile.getUser() != null ? profile.getUser().getRole() : null
        );
    }
}
