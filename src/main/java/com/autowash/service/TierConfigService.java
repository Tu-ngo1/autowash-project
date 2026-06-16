package com.autowash.service;

import com.autowash.dto.response.TierConfigResponse;
import com.autowash.repository.TierConfigRepository;
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
