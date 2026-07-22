package com.autowash.features.promotion.service;

import com.autowash.features.promotion.dto.request.CreateVoucherRequest;
import com.autowash.features.promotion.dto.response.VoucherResponse;
import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.promotion.mapper.VoucherMapper;
import com.autowash.features.promotion.repository.PromotionRepository;
import com.autowash.features.user.entity.TierConfig;
import com.autowash.features.user.enums.TierLevel;
import com.autowash.features.user.repository.TierConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final PromotionRepository promotionRepository;
    private final TierConfigRepository tierConfigRepository;
    private final VoucherMapper voucherMapper;

    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        if (request.getVoucherCode() == null || request.getVoucherCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher không được để trống");
        }
        if (request.getCampaignName() == null || request.getCampaignName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chiến dịch không được để trống");
        }
        if (promotionRepository.existsByVoucherCode(request.getVoucherCode().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher đã tồn tại");
        }

        TierLevel tierLevel = TierLevel.MEMBER;
        if (request.getTargetTier() != null) {
            try {
                tierLevel = TierLevel.valueOf(request.getTargetTier().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to MEMBER if invalid or "all"
            }
        }

        TierConfig tierConfig = tierConfigRepository.findById(tierLevel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình hạng"));

        Boolean activeStatus = request.getIsActive();
        if (activeStatus == null) {
            activeStatus = request.getActive();
        }
        if (activeStatus == null) {
            activeStatus = true;
        }

        Promotion promotion = Promotion.builder()
                .voucherCode(request.getVoucherCode().trim())
                .campaignName(request.getCampaignName().trim())
                .pointCost(request.getPointCost() != null ? request.getPointCost() : 0)
                .targetTier(tierConfig)
                .discountAmount(request.getDiscountAmount())
                .discountPercent(request.getDiscountPercent())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .active(activeStatus)
                .build();

        Promotion saved = promotionRepository.save(promotion);
        return voucherMapper.toResponse(saved);
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, CreateVoucherRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            String code = request.getVoucherCode().trim();
            if (!code.equalsIgnoreCase(promotion.getVoucherCode())) {
                if (promotionRepository.existsByVoucherCode(code)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã voucher đã tồn tại");
                }
                promotion.setVoucherCode(code);
            }
        }

        if (request.getCampaignName() != null && !request.getCampaignName().trim().isEmpty()) {
            promotion.setCampaignName(request.getCampaignName().trim());
        }

        if (request.getPointCost() != null) {
            promotion.setPointCost(request.getPointCost());
        }

        if (request.getTargetTier() != null) {
            TierLevel tierLevel = TierLevel.MEMBER;
            try {
                tierLevel = TierLevel.valueOf(request.getTargetTier().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to MEMBER
            }
            TierConfig tierConfig = tierConfigRepository.findById(tierLevel)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình hạng"));
            promotion.setTargetTier(tierConfig);
        }

        promotion.setDiscountAmount(request.getDiscountAmount());
        promotion.setDiscountPercent(request.getDiscountPercent());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setStartAt(request.getStartAt());
        promotion.setEndAt(request.getEndAt());

        Promotion saved = promotionRepository.save(promotion);
        return voucherMapper.toResponse(saved);
    }

    @Transactional
    public void deleteVoucher(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
        promotionRepository.delete(promotion);
    }

    @Transactional
    public VoucherResponse updateStatus(Long id, boolean isActive) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
        promotion.setActive(isActive);
        Promotion saved = promotionRepository.save(promotion);
        return voucherMapper.toResponse(saved);
    }
}
