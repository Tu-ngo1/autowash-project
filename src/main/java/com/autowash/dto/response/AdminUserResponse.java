package com.autowash.dto.response;

import com.autowash.enums.Role;
import com.autowash.enums.TierLevel;
import com.autowash.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private UserStatus status;
    private TierLevel tierLevel;
    private Integer rewardPoints;
    private Integer tierPoints;
    private Integer carCount;
    private Integer bookingCount;
    private Role role;
}
