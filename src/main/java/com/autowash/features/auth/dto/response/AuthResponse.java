package com.autowash.features.auth.dto.response;

import com.autowash.features.user.entity.User;

import com.autowash.features.user.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;
    private String role;
    private String dashboardUrl;
    private AuthUserResponse user;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthUserResponse {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String role;
        private String tier;
        private Integer points;
        private Integer walletBalance;
    }
}

