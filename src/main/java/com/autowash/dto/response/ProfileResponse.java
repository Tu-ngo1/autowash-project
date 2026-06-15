package com.autowash.dto.response;

import com.autowash.entity.CustomerProfile;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private Integer rewardPoints;
    private Integer tierPoints;

    public static ProfileResponse fromProfile(CustomerProfile profile) {
        return new ProfileResponse(
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getPhone(),
                profile.getUser().getEmail(),
                profile.getRewardPoints(),
                profile.getTierPoints()
        );
    }
}