package com.autowash.features.booking.service;

import com.autowash.features.booking.dto.request.UpdateDailyConfigRequest;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.DailyOperationsConfig;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.repository.DailyOperationsConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationsConfigService {

    private final DailyOperationsConfigRepository configRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private static final int SLOT_DURATION_MINUTES = 90; // Mỗi slot kéo dài 90 phút

    @Transactional
    public DailyOperationsConfig updateConfigForTomorrow(UpdateDailyConfigRequest request) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // 1. Nếu Admin thiết lập làm ngày nghỉ (isActive = false)
        if (Boolean.FALSE.equals(request.getIsActive())) {
            // Tìm tất cả booking đang hoạt động của ngày mai để kiểm tra và hủy
            LocalDateTime startOfDay = tomorrow.atStartOfDay();
            LocalDateTime endOfDay = tomorrow.atTime(LocalTime.MAX);
            List<Booking> activeBookings = bookingRepository
                    .findByScheduledStartTimeBetweenOrderByScheduledStartTimeAsc(startOfDay, endOfDay)
                    .stream()
                    .filter(b -> b.getStatus() == BookingStatus.PENDING 
                              || b.getStatus() == BookingStatus.CONFIRM
                              || b.getStatus() == BookingStatus.ARRIVED 
                              || b.getStatus() == BookingStatus.IN_PROGRESS)
                    .toList();

            if (!activeBookings.isEmpty()) {
                if (request.getForceSave() == null || !request.getForceSave()) {
                    List<String> affectedBookingCodes = activeBookings.stream().map(Booking::getBookingCode).toList();
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "CONFLICT:" + String.join(",", affectedBookingCodes)
                    );
                } else {
                    // forceSave = true -> Tiến hành hủy các lịch hẹn này và hoàn tiền 100%
                    for (Booking booking : activeBookings) {
                        booking.setStatus(BookingStatus.CANCELLED);
                        bookingRepository.save(booking);
                        bookingService.processRefund(booking, 1.0); // Hoàn tiền 100%
                    }
                }
            }

            // Tìm cấu hình hiện tại hoặc tạo mới
            DailyOperationsConfig config = configRepository.findByConfigDateWithLock(tomorrow)
                    .orElse(DailyOperationsConfig.builder()
                            .configDate(tomorrow)
                            .openTime(LocalTime.of(8, 0))   // Giá trị mặc định vì cột nullable = false
                            .closeTime(LocalTime.of(18, 0)) // Giá trị mặc định
                            .bayCount(2)
                            .build());

            config.setIsActive(false);
            return configRepository.save(config);
        }

        // 2. Mở cửa hoạt động bình thường
        // Validation đầu vào
        if (request.getOpenTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giờ mở cửa không được để trống");
        }
        if (request.getSlotCount() == null || request.getSlotCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng ca hoạt động phải lớn hơn 0");
        }
        if (request.getBayCount() == null || request.getBayCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng khoang rửa phải lớn hơn 0");
        }

        // Tính toán closeTime dựa trên số slot và openTime
        int totalMinutes = request.getSlotCount() * SLOT_DURATION_MINUTES;
        LocalTime closeTime = request.getOpenTime().plusMinutes(totalMinutes);

        // Kiểm tra xem thời gian đóng cửa có vượt quá ngày hôm sau (24h) không
        if (closeTime.isBefore(request.getOpenTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng ca quá nhiều vượt quá giới hạn ngày!");
        }

        // Tìm các booking đang hoạt động của ngày mai và kiểm tra xem có bị xung đột không
        LocalDateTime startOfDay = tomorrow.atStartOfDay();
        LocalDateTime endOfDay = tomorrow.atTime(LocalTime.MAX);
        List<Booking> activeBookings = bookingRepository
                .findByScheduledStartTimeBetweenOrderByScheduledStartTimeAsc(startOfDay, endOfDay)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING 
                          || b.getStatus() == BookingStatus.CONFIRM
                          || b.getStatus() == BookingStatus.ARRIVED 
                          || b.getStatus() == BookingStatus.IN_PROGRESS)
                .toList();

        List<Booking> affectedBookings = new ArrayList<>();
        List<String> affectedBookingCodes = new ArrayList<>();
        for (Booking booking : activeBookings) {
            LocalTime bookingStart = booking.getScheduledStartTime().toLocalTime();
            LocalTime bookingEnd = booking.getExpectedEndTime().toLocalTime();

            if (bookingStart.isBefore(request.getOpenTime()) || bookingEnd.isAfter(closeTime)) {
                affectedBookings.add(booking);
                affectedBookingCodes.add(booking.getBookingCode());
            }
        }

        if (!affectedBookings.isEmpty()) {
            if (request.getForceSave() == null || !request.getForceSave()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "CONFLICT:" + String.join(",", affectedBookingCodes)
                );
            } else {
                // forceSave = true -> Tiến hành hủy các lịch hẹn này và hoàn tiền 100%
                for (Booking booking : affectedBookings) {
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                    bookingService.processRefund(booking, 1.0); // Hoàn tiền 100%
                }
            }
        }

        // Tìm cấu hình hiện tại hoặc tạo mới bằng Pessimistic Lock để chống tranh chấp đồng thời
        DailyOperationsConfig config = configRepository.findByConfigDateWithLock(tomorrow)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(tomorrow)
                        .build());

        // Cập nhật các thông số mới
        config.setOpenTime(request.getOpenTime());
        config.setCloseTime(closeTime);
        config.setBayCount(request.getBayCount());
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        return configRepository.save(config);
    }
}
