package com.autowash.features.user.service;

import com.autowash.features.user.entity.User;



import com.autowash.features.user.dto.TierConfigResponse;
import com.autowash.features.user.repository.TierConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TierConfigService {
    private final TierConfigRepository tierConfigRepository;
    public List<TierConfigResponse> getTierConfigResponseList(){
        return tierConfigRepository.findTiersWithCustomerCount().stream().toList();
    }
}


