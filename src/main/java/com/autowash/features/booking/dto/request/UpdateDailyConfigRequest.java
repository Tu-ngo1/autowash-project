package com.autowash.features.booking.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateDailyConfigRequest {
    private LocalTime openTime;     // Giờ bắt đầu mở cửa (ví dụ: 08:00)
    private Integer slotCount;      // Số lượng ca/slot muốn mở trong ngày (ví dụ: 6 ca)
    private Integer bayCount;       // Số khoang rửa xe hoạt động (ví dụ: 3 khoang)
}
