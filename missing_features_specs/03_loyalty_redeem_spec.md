# Đặc tả Yêu cầu - Đổi Điểm Thưởng Lấy Voucher (Loyalty Redeem)

Tính năng này cho phép khách hàng đổi điểm thưởng tích lũy (`rewardPoints`) lấy các Voucher khuyến mại của tiệm để sử dụng khi đặt lịch.

---

## 1. API Endpoint cần bổ sung

### 1.1. Đổi Voucher (Redeem Voucher)
- **Method:** `POST`
- **Path:** `/api/customer/loyalty/redeem`
- **Request Body:**
```json
{
  "voucherId": 1
}
```
- *Lưu ý:* `voucherId` trong request body của Frontend thực tế gửi lên tương ứng với khóa chính `id` của bảng `PROMOTIONS` (thực thể `Promotion`).

---

## 2. Logic triển khai trong Backend

### 2.1. Bổ sung phương thức trong `CustomerController.java`
Thêm endpoint xử lý vào [CustomerController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/user/controller/CustomerController.java):
```java
    @PostMapping("/loyalty/redeem")
    public CustomerVoucherResponse redeemVoucher(@RequestBody RedeemVoucherRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        return userService.redeemVoucher(currentUser.getId(), request.getVoucherId());
    }

    // DTO nhận dữ liệu:
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedeemVoucherRequest {
        private Long voucherId;
    }
```

### 2.2. Triển khai logic đổi điểm trong `UserService.java`
Thêm logic kiểm tra điểm, trừ điểm và sinh voucher mới vào [UserService.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/user/service/UserService.java):
```java
    // Import các class cần thiết:
    import com.autowash.features.promotion.entity.Promotion;
    import com.autowash.features.promotion.entity.CustomerVoucher;
    import com.autowash.features.promotion.enums.VoucherStatus;
    import com.autowash.features.promotion.repository.PromotionRepository;
    import com.autowash.features.promotion.repository.CustomerVoucherRepository;
    import com.autowash.features.user.controller.CustomerController.CustomerVoucherResponse;
    import java.util.UUID;
    import java.time.LocalDateTime;

    // Inject thêm các Repository vào UserService:
    private final PromotionRepository promotionRepository;
    private final CustomerVoucherRepository customerVoucherRepository;

    @Transactional
    public CustomerVoucherResponse redeemVoucher(Long userId, Long promotionId) {
        // 1. Tìm thông tin khách hàng và profile
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
                
        CustomerProfile profile = customerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ thành viên"));

        // 2. Tìm thông tin chiến dịch khuyến mãi (Promotion)
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương trình khuyến mại"));

        // 3. Kiểm tra xem Promotion có đang hoạt động không
        if (Boolean.FALSE.equals(promotion.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương trình khuyến mại đã ngừng hoạt động");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartAt() != null && promotion.getStartAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương trình khuyến mại chưa bắt đầu");
        }
        if (promotion.getEndAt() != null && promotion.getEndAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương trình khuyến mại đã kết thúc");
        }

        // 4. Kiểm tra số dư điểm thưởng
        int pointsNeeded = promotion.getPointCost() != null ? promotion.getPointCost() : 0;
        int currentPoints = profile.getRewardPoints() != null ? profile.getRewardPoints() : 0;
        
        if (currentPoints < pointsNeeded) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điểm thưởng tích lũy không đủ để đổi voucher này");
        }

        // 5. Khấu trừ điểm thưởng
        profile.setRewardPoints(currentPoints - pointsNeeded);
        customerProfileRepository.save(profile);

        // 6. Sinh mã Voucher ngẫu nhiên duy nhất cho khách hàng
        // Định dạng: [MÃ_CHIẾN_DỊCH]-[RNG-4-CHAR] (ví dụ: GOLD10-8D2A)
        String uniqueVoucherCode = promotion.getVoucherCode() + "-" + 
                UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // 7. Tạo mới CustomerVoucher
        LocalDateTime expiredDate = promotion.getEndAt() != null ? promotion.getEndAt() : now.plusDays(30);
        CustomerVoucher customerVoucher = CustomerVoucher.builder()
                .user(user)
                .promotion(promotion)
                .voucherCode(uniqueVoucherCode)
                .status(VoucherStatus.AVAILABLE)
                .redeemedAt(now)
                .expiredAt(expiredDate)
                .build();

        CustomerVoucher savedVoucher = customerVoucherRepository.save(customerVoucher);

        // 8. Trả về Response DTO tương thích với Frontend
        return CustomerVoucherResponse.builder()
                .id(savedVoucher.getId())
                .promotionId(promotion.getId())
                .voucherCode(savedVoucher.getVoucherCode())
                .campaignName(promotion.getCampaignName())
                .discountAmount(promotion.getDiscountAmount())
                .discountPercent(promotion.getDiscountPercent())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .status(savedVoucher.getStatus().name())
                .redeemedAt(savedVoucher.getRedeemedAt())
                .expiredAt(savedVoucher.getExpiredAt())
                .build();
    }
```
