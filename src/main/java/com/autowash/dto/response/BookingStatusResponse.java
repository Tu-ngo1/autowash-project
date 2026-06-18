package com.autowash.dto.response;

import com.autowash.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusResponse {
    private BookingStatus status;
    private Long total;
}