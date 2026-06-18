package com.autowash.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsResponse {
    private Long totalRevenue;
    private Long revenue;
    private Long totalSales;

    private Long washCount;
    private Long totalWashes;
    private Long bookingCount;

    private Long newCustomers;
    private Long customerCount;
    private Long customers;

    private Long pendingBookings;
    private Long pending;
    private Long waitingBookings;

    private List<ServiceRatioResponse> serviceRatios;
}
