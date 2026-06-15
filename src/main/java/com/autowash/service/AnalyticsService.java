package com.autowash.service;

import com.autowash.dto.response.BookingResponse;
import com.autowash.dto.response.BookingStatusResponse;
import com.autowash.dto.response.TopUsedVoucherResponse;
import com.autowash.dto.response.VoucherResponse;
import com.autowash.entity.Booking;
import com.autowash.entity.Promotion;
import com.autowash.enums.BookingStatus;
import com.autowash.mapper.BookingMapper;
import com.autowash.mapper.VoucherMapper;
import com.autowash.repository.BookingRepository;
import com.autowash.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final BookingRepository bookingRepo;
    private final PromotionRepository promotionRepo;
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
}
