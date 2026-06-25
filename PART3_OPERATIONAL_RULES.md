# HƯỚNG DẪN PHẦN 3: ĐIỀU KHOẢN HỦY LỊCH & ĐI TRỄ (OPERATIONAL & CANCELLATION RULES)

Mục tiêu của phần này là lập trình logic bảo vệ lịch trình và doanh thu cửa hàng:
1. Cho phép hủy lịch sớm trước 60 phút và hoàn tiền 100%.
2. Tự động hủy lịch nếu khách hàng check-in muộn quá 15 phút và hoàn tiền 80% (phạt 20%).
3. Viết Scheduled Task chạy ngầm tự động quét và giải phóng các slot quá 15 phút chưa check-in.

---

## 1. Hoàn tiền 100% khi khách hàng hủy lịch sớm

Mở file: [BookingService.java](file:///C:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/service/BookingService.java)
Cập nhật phương thức `cancelBooking(Long customerId, Long bookingId)`:

```java
@Transactional
public void cancelBooking(Long customerId, Long bookingId) {
    Booking booking = findBookingOrThrow(bookingId);

    if (!booking.getUser().getId().equals(customerId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy booking này");
    }

    if (booking.getStatus() != BookingStatus.PENDING) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy lịch ở trạng thái PENDING");
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime latestCancelTime = booking.getScheduledStartTime().minusMinutes(60); // Hạn hủy 60 phút

    booking.setStatus(BookingStatus.CANCELLED);
    bookingRepository.save(booking);

    // TH 1: Khách hủy sớm trước giờ hẹn ít nhất 60 phút -> HOÀN TIỀN 100%
    if (now.isBefore(latestCancelTime) || now.isEqual(latestCancelTime)) {
        processRefund(booking, 1.0); // Hoàn tiền 100%
    } 
    // TH 2: Khách hủy trễ dưới 60 phút -> KHÔNG HOÀN TIỀN (Phạt 100% tiền cọc)
    else {
        Payment payment = booking.getPayment();
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.PAID) {
            payment.setPaymentStatus(PaymentStatus.FAILED); // Đổi trạng thái thanh toán thành thất bại
            paymentRepository.save(payment);
        }
    }
}
```

---

## 2. Đi trễ quá 15 phút khi Check-in -> Hủy lịch và hoàn tiền 80%

Cập nhật phương thức `checkInByQr(String qrContent)` trong [BookingService.java](file:///C:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/service/BookingService.java):

```java
@Transactional
public BookingResponse checkInByQr(String qrContent) {
    Booking booking = bookingRepository.findByQrContent(qrContent)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã QR không hợp lệ"));

    if (Boolean.TRUE.equals(booking.getQrUsed())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã QR đã được sử dụng");
    }

    if (booking.getStatus() != BookingStatus.PENDING) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch hẹn không ở trạng thái có thể check-in");
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime lateDeadline = booking.getScheduledStartTime().plusMinutes(15); // Hạn đi trễ 15 phút (5 phút ra vào + 10 phút ân hạn)

    // Khách đến trễ quá 15 phút -> Hủy lịch, hoàn tiền 80%
    if (now.isAfter(lateDeadline)) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setQrUsed(true);
        bookingRepository.save(booking);

        processRefund(booking, 0.8); // Hoàn tiền 80% (Phạt 20%)

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Lịch hẹn đã bị hủy tự động do bạn đến trễ quá 15 phút. Hệ thống đã hoàn lại 80% số tiền vào ví của bạn."
        );
    }

    // Khách đến đúng giờ hoặc trễ dưới 15 phút -> Cho phép check-in vào khoang
    booking.setQrUsed(true);
    booking.setStatus(BookingStatus.ARRIVED);
    booking.setArrivedAt(now);
    
    if (now.isAfter(booking.getScheduledStartTime())) {
        booking.setLate(true); // Ghi nhận đi trễ dưới 15 phút để làm dữ liệu phân tích sau này
    }

    return bookingMapper.toResponse(bookingRepository.save(booking));
}
```

### Phương thức phụ trợ `processRefund(...)` để hoàn tiền vào ví:
Thêm phương thức này vào `BookingService.java`:
```java
@Transactional
public void processRefund(Booking booking, double refundRate) {
    Payment payment = booking.getPayment();
    if (payment == null || payment.getPaymentStatus() != PaymentStatus.PAID) {
        return; // Chưa trả tiền thì không cần hoàn
    }

    if (payment.getPaymentMethod() == PaymentMethod.WALLET || payment.getPaymentMethod() == PaymentMethod.PAYOS) {
        Long userId = booking.getUser().getId();
        int originalPrice = payment.getFinalPrice();
        int refundAmount = (int) Math.round(originalPrice * refundRate);

        // 1. Tìm ví của user
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy ví người dùng"));

        // 2. Cộng lại số tiền hoàn
        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(refundAmount)));
        walletRepository.save(wallet);

        // 3. Ghi lịch sử giao dịch ví
        String note = String.format("Hoàn tiền %.0f%% lịch hẹn %s do %s", 
                refundRate * 100, 
                booking.getBookingCode(), 
                refundRate == 1.0 ? "hủy lịch sớm" : "đến trễ quá 15 phút");
                
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(BigDecimal.valueOf(refundAmount))
                .transactionType(WalletTransactionType.REFUND)
                .description(note)
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(transaction);

        // 4. Đổi trạng thái hóa đơn
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
    }
}
```

---

## 3. Scheduled Task chạy ngầm tự động quét và hủy các lịch trễ hẹn

Nếu khách đi trễ quá 15 phút nhưng không đến quét mã QR, hệ thống cần tự động quét và hủy lịch để trả slot trống cho người khác.

Tạo một class mới: `BookingCleanupScheduler.java` trong package `com.autowash.features.booking.service` hoặc `scheduler`:

```java
package com.autowash.features.booking.scheduler;

import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    // Chạy định kỳ mỗi 5 phút một lần để giải phóng các slot bị "bỏ quên"
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoCancelLateBookings() {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(15);
        
        // Tìm toàn bộ booking PENDING đã quá giờ hẹn 15 phút mà chưa check-in
        List<Booking> lateBookings = bookingRepository
                .findByStatusAndScheduledStartTimeBefore(BookingStatus.PENDING, timeLimit);

        for (Booking booking : lateBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // Hoàn lại 80% tiền cho khách hàng
            bookingService.processRefund(booking, 0.8);
        }
    }
}
```
*(Lưu ý: Đừng quên thêm `@EnableScheduling` trên class chính `AutowashApplication.java` để kích hoạt tính năng chạy ngầm).*
