package com.autowash.features.analytics.service;


import com.autowash.features.user.entity.User;

import com.autowash.features.booking.dto.BookingResponse;
import com.autowash.features.analytics.dto.BookingStatusResponse;
import com.autowash.features.analytics.dto.RevenueResponse;
import com.autowash.features.promotion.dto.TopUsedVoucherResponse;
import com.autowash.features.promotion.dto.VoucherResponse;
import com.autowash.features.analytics.dto.DashboardAnalyticsResponse;
import com.autowash.features.analytics.dto.ServiceRatioResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.Payment;
import com.autowash.features.promotion.entity.Promotion;
import com.autowash.features.analytics.enums.AnalyticsPeriod;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.enums.PaymentStatus;
import com.autowash.features.user.enums.Role;
import com.autowash.features.booking.mapper.BookingMapper;
import com.autowash.features.promotion.mapper.VoucherMapper;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.repository.PaymentRepository;
import com.autowash.features.promotion.repository.PromotionRepository;
import com.autowash.features.user.repository.UserRepository;
import com.autowash.features.booking.repository.BookingDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final BookingRepository bookingRepo;
    private final PromotionRepository promotionRepo;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingMapper bookingMapper;
    private final VoucherMapper voucherMapper;

    public DashboardAnalyticsResponse getDashboardAnalytics() {
        List<Payment> paidPayments = paymentRepository.findByPaymentStatus(PaymentStatus.PAID);
        long totalRevenueValue = paidPayments.stream().mapToLong(Payment::getFinalPrice).sum();

        long washCountValue = bookingRepo.countByStatusNot(BookingStatus.CANCELLED);
        long newCustomersValue = userRepository.countByRole(Role.CUSTOMER);
        long pendingBookingsValue = bookingRepo.countByStatus(BookingStatus.PENDING);

        List<ServiceRatioResponse> serviceRatios = bookingDetailRepository.getServiceRatios();

        return DashboardAnalyticsResponse.builder()
                .totalRevenue(totalRevenueValue)
                .revenue(totalRevenueValue)
                .totalSales(totalRevenueValue)
                .washCount(washCountValue)
                .totalWashes(washCountValue)
                .bookingCount(washCountValue)
                .newCustomers(newCustomersValue)
                .customerCount(newCustomersValue)
                .customers(newCustomersValue)
                .pendingBookings(pendingBookingsValue)
                .pending(pendingBookingsValue)
                .waitingBookings(pendingBookingsValue)
                .serviceRatios(serviceRatios)
                .build();
    }

    public List<TopUsedVoucherResponse> getTopVoucher() {
        return promotionRepo.findTopVoucher().stream()
                .limit(4)
                .toList();
    }
    public List<VoucherResponse> getAllVoucher() {
        List<Promotion> promotionList = promotionRepo.findAll();
        return promotionList.stream().map(voucherMapper::toResponse).toList();
    }
    //cái này gọi xún lớp repo có câu query lấy ra hoi
    public List<BookingStatusResponse> countBookingByStatus(){
        return bookingRepo.countBookingsByStatus();
    }
    //hàm lấy được danh sách các lớp booking (có thể theo bộ lọc status hoặc không)
    public List<BookingResponse> getBookingListByStatus(BookingStatus status){
        List<Booking> bookingList = bookingRepo.findBookingsByStatus(status);
        return bookingList.stream().map(bookingMapper::toResponse).toList();
    }

    public List<RevenueResponse> getRevenueAnalytics(AnalyticsPeriod analyticsPeriod) {
        List<Payment> paidPayments = paymentRepository.findByPaymentStatus(PaymentStatus.PAID);
        Function<LocalDateTime, String> labelExtractor = paidAt -> {
            switch (analyticsPeriod){
                case MONTH:
                    return "Tháng " + paidAt.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                case WEEK:
                    LocalDateTime startOfWeek = paidAt.minusDays(paidAt.getDayOfWeek().getValue() - 1);
                    return "Tuần " + startOfWeek.format(DateTimeFormatter.ofPattern("dd/MM"));
                case DAY:
                default:
                    return paidAt.format(DateTimeFormatter.ofPattern("dd/MM"));
            }
        };

        return paidPayments.stream()
                .filter(p -> p.getPaidAt() != null)
                .collect(Collectors.groupingBy(
                        p -> labelExtractor.apply(p.getPaidAt()),
                        Collectors.summingInt(Payment::getFinalPrice)
                ))
                .entrySet().stream()
                .map(entry -> new RevenueResponse(entry.getKey(), java.math.BigDecimal.valueOf(entry.getValue())))
                .toList();
    }
}


