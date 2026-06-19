package com.autowash.features.user.mapper;

import com.autowash.features.user.entity.User;

import com.autowash.features.user.dto.ProfileResponse;
import com.autowash.features.user.entity.CustomerProfile;
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
}


