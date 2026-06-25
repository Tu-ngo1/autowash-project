package com.autowash.features.booking.service;

import com.autowash.features.booking.dto.request.UpdateDailyConfigRequest;
import com.autowash.features.booking.entity.DailyOperationsConfig;
import com.autowash.features.booking.repository.DailyOperationsConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class OperationsConfigService {

    private final DailyOperationsConfigRepository configRepository;
    private static final int SLOT_DURATION_MINUTES = 90; // Mỗi slot kéo dài 90 phút

    @Transactional
    public DailyOperationsConfig updateConfigForTomorrow(UpdateDailyConfigRequest request) {
        // 1. Validation đầu vào
        if (request.getOpenTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giờ mở cửa không được để trống");
        }
        if (request.getSlotCount() == null || request.getSlotCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng ca hoạt động phải lớn hơn 0");
        }
        if (request.getBayCount() == null || request.getBayCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng khoang rửa phải lớn hơn 0");
        }

        // 2. Xác định ngày hôm sau
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // 3. Tính toán closeTime dựa trên số slot và openTime
        int totalMinutes = request.getSlotCount() * SLOT_DURATION_MINUTES;
        LocalTime closeTime = request.getOpenTime().plusMinutes(totalMinutes);

        // Kiểm tra xem thời gian đóng cửa có vượt quá ngày hôm sau (24h) không
        if (closeTime.isBefore(request.getOpenTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng ca quá nhiều vượt quá giới hạn ngày!");
        }

        // 4. Tìm cấu hình hiện tại hoặc tạo mới bằng Pessimistic Lock để chống tranh chấp đồng thời
        DailyOperationsConfig config = configRepository.findByConfigDateWithLock(tomorrow)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(tomorrow)
                        .isActive(true)
                        .build());

        // 5. Cập nhật các thông số mới
        config.setOpenTime(request.getOpenTime());
        config.setCloseTime(closeTime);
        config.setBayCount(request.getBayCount());

        return configRepository.save(config);
    }
}
