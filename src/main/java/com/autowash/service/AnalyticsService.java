package com.autowash.service;

import com.autowash.dto.response.BookingResponse;
import com.autowash.dto.response.BookingStatusResponse;
import com.autowash.dto.response.RevenueResponse;
import com.autowash.dto.response.TopUsedVoucherResponse;
import com.autowash.dto.response.VoucherResponse;
import com.autowash.entity.Booking;
import com.autowash.entity.Payment;
import com.autowash.entity.Promotion;
import com.autowash.enums.AnalyticsPeriod;
import com.autowash.enums.BookingStatus;
import com.autowash.enums.PaymentStatus;
import com.autowash.mapper.BookingMapper;
import com.autowash.mapper.VoucherMapper;
import com.autowash.repository.BookingRepository;
import com.autowash.repository.PaymentRepository;
import com.autowash.repository.PromotionRepository;
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
    private final BookingMapper bookingMapper;
    private final VoucherMapper voucherMapper;

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
