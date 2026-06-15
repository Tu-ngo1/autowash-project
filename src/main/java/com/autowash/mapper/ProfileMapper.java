package com.autowash.mapper;

import com.autowash.dto.response.ProfileResponse;
import com.autowash.entity.CustomerProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponse toResponse(CustomerProfile profile) {
        if (profile == null) return null;
        return new ProfileResponse(
                profile.getId(),
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getTierConfig() != null ? profile.getTierConfig().getTierLevel() : null,
                profile.getRewardPoints(),
                profile.getTierPoints()
        );
    }
}
