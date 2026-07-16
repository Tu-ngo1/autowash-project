package com.autowash.features.booking.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long bookingId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
