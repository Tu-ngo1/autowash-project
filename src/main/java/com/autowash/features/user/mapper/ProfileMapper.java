package com.autowash.features.user.mapper;

import com.autowash.features.user.entity.User;
import com.autowash.features.user.dto.response.ProfileResponse;
import com.autowash.features.user.entity.CustomerProfile;
import com.autowash.features.wallet.entity.Wallet;
import com.autowash.features.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileMapper {

    private final WalletRepository walletRepository;

    public ProfileResponse toResponse(CustomerProfile profile) {
        if (profile == null) return null;

        Integer walletBalance = 0;
        if (profile.getUser() != null && profile.getUser().getId() != null) {
            walletBalance = walletRepository.findWalletByUserId(profile.getUser().getId())
                    .map(Wallet::getBalance)
                    .orElse(0);
        }

        return new ProfileResponse(
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getUser() != null ? profile.getUser().getFullName() : null,
                profile.getUser() != null ? profile.getUser().getPhone() : null,
                profile.getUser() != null ? profile.getUser().getEmail() : null,
                profile.getRewardPoints(),
                profile.getTierPoints(),
                walletBalance
        );
    }
}


