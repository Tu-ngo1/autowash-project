package com.autowash.features.user.service;

import com.autowash.features.user.dto.response.TierConfigResponse;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.enums.TierLevel;
import com.autowash.features.user.repository.CustomerProfileRepository;
import com.autowash.features.user.repository.TierConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TierConfigService {
    private final TierConfigRepository tierConfigRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public List<TierConfigResponse> getTierConfigResponseList(){
        return tierConfigRepository.findTiersWithCustomerCount().stream().toList();
    }

    @Transactional
    public TierConfigResponse updateTierConfig(String idOrLevel, Map<String, Object> body) {
        TierLevel tierLevel;
        try {
            tierLevel = TierLevel.valueOf(idOrLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            if ("1".equals(idOrLevel)) {
                tierLevel = TierLevel.MEMBER;
            } else if ("2".equals(idOrLevel)) {
                tierLevel = TierLevel.SILVER;
            } else if ("3".equals(idOrLevel)) {
                tierLevel = TierLevel.GOLD;
            } else if ("4".equals(idOrLevel)) {
                tierLevel = TierLevel.PLATINUM;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Tier ID: " + idOrLevel);
            }
        }

        TierConfig tierConfig = tierConfigRepository.findById(tierLevel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình hạng"));

        if (body.containsKey("pointsRequired")) {
            Integer pointsRequired = ((Number) body.get("pointsRequired")).intValue();
            tierConfig.setPointsToUpgrade(pointsRequired);
            tierConfig.setPointsToMaintain(pointsRequired);
        }
        if (body.containsKey("discountPercent")) {
            Double discountPercent = ((Number) body.get("discountPercent")).doubleValue();
            tierConfig.setAutoDiscountPercent(BigDecimal.valueOf(discountPercent));
        }

        TierConfig saved = tierConfigRepository.save(tierConfig);
        long count = customerProfileRepository.countByTierConfig(saved);

        return new TierConfigResponse(saved.getTierLevel(), saved.getPointsToUpgrade(), saved.getAutoDiscountPercent(), count);
    }
}


