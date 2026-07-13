# Đặc tả Yêu cầu - Khuyến Mãi & Hạng Thành Viên (Admin Promotion & Tier)

Hỗ trợ Admin cấu hình các hạng thành viên (số điểm yêu cầu, phần trăm chiết khấu tự động) và quản lý các chiến dịch Voucher khuyến mãi của hệ thống.

---

## 1. API Endpoints cần bổ sung/chỉnh sửa

### 1.1. Xem danh sách hạng Tiers (Nâng cấp)
- **Method:** `GET`
- **Path:** `/api/admin/tiers`
- **Logic xử lý:** Trả về danh sách cấu hình hạng thành viên, tính toán thêm trường `customerCount` là số lượng khách hàng thực tế đang ở hạng đó.
- **Response Body:**
```json
[
  {
    "id": 1,
    "name": "MEMBER",
    "description": "Khách hàng mới",
    "pointsRequired": 0,
    "discountPercent": 0,
    "customerCount": 18
  },
  {
    "id": 2,
    "name": "SILVER",
    "description": "Khách hàng thân thiết",
    "pointsRequired": 1000,
    "discountPercent": 5,
    "customerCount": 9
  }
]
```

### 1.2. Cập nhật cấu hình hạng
- **Method:** `PUT`
- **Path:** `/api/admin/tiers/{id}`
- **Request Body:**
```json
{
  "pointsRequired": 1200,
  "discountPercent": 6
}
```
- **Logic xử lý:** Sửa các trường tương ứng trong bảng `TIER_CONFIGS` (ví dụ: `pointsToMaintain`, `autoDiscountPercent`).

### 1.3. Thêm mới Voucher chiến dịch
- **Method:** `POST`
- **Path:** `/api/admin/vouchers`
- **Request Body:**
```json
{
  "voucherCode": "SALE20",
  "campaignName": "Giảm giá 20%",
  "pointCost": 200,
  "targetTier": "MEMBER",
  "discountAmount": null,
  "discountPercent": 20,
  "maxDiscountAmount": 50000,
  "startAt": "2026-07-01T00:00:00",
  "endAt": "2026-08-01T23:59:59"
}
```
- **Logic xử lý:** Lưu thông tin vào bảng `PROMOTIONS`. `targetTier` nhận vào Tên hạng (MEMBER, SILVER, GOLD, PLATINUM) và tìm `TierConfig` tương ứng để gán khóa ngoại.

### 1.4. Cập nhật Voucher chiến dịch
- **Method:** `PUT`
- **Path:** `/api/admin/vouchers/{id}`
- **Request Body:** (Tương tự POST)

### 1.5. Xóa Voucher chiến dịch
- **Method:** `DELETE`
- **Path:** `/api/admin/vouchers/{id}`

### 1.6. Bật/tắt nhanh trạng thái hoạt động của Voucher
- **Method:** `PATCH`
- **Path:** `/api/admin/vouchers/{id}/status`
- **Request Body:**
```json
{
  "isActive": false
}
```

---

## 2. Các thay đổi cần thực hiện trong Backend

### 2.1. Cập nhật `TierConfigResponse.java` nâng cấp
```java
package com.autowash.features.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TierConfigResponse {
    private Long id;
    private String name;
    private String description;
    private Integer pointsRequired;
    private Double discountPercent;
    private Long customerCount;
}
```

### 2.2. Triển khai trong `TierConfigService.java`
```java
    // Import các Repository cần thiết để đếm số lượng khách hàng
    private final CustomerProfileRepository customerProfileRepository;

    public List<TierConfigResponse> getTierConfigResponseList() {
        List<TierConfig> configs = tierConfigRepository.findAll();
        
        return configs.stream().map(tc -> {
            // Đếm số lượng khách hàng đang ở hạng này
            long count = customerProfileRepository.countByTierConfig(tc);
            
            // Map ID giả lập hoặc thực tế
            long id = 1;
            if (tc.getTierLevel() == TierLevel.SILVER) id = 2;
            if (tc.getTierLevel() == TierLevel.GOLD) id = 3;
            if (tc.getTierLevel() == TierLevel.PLATINUM) id = 4;

            return TierConfigResponse.builder()
                    .id(id)
                    .name(tc.getTierLevel().name())
                    .description(getDescriptionByLevel(tc.getTierLevel()))
                    .pointsRequired(tc.getPointsToMaintain() != null ? tc.getPointsToMaintain() : 0)
                    .discountPercent(tc.getAutoDiscountPercent() != null ? tc.getAutoDiscountPercent().doubleValue() : 0.0)
                    .customerCount(count)
                    .build();
        }).toList();
    }
```

### 2.3. Bổ sung các Mapping trong `AdminController.java`
Thêm các endpoints quản trị Voucher vào [AdminController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/analytics/controller/AdminController.java):
```java
    @PutMapping("/tiers/{id}")
    public TierConfigResponse updateTierConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // Thực hiện cập nhật trong TierConfigService
        return tierConfigService.updateTierConfig(id, body);
    }

    @PostMapping("/vouchers")
    public VoucherResponse createVoucher(@RequestBody CreateVoucherRequest request) {
        return voucherService.createVoucher(request);
    }

    @PutMapping("/vouchers/{id}")
    public VoucherResponse updateVoucher(@PathVariable Long id, @RequestBody CreateVoucherRequest request) {
        return voucherService.updateVoucher(id, request);
    }

    @DeleteMapping("/vouchers/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok(Map.of("message", "Xóa voucher thành công"));
    }

    @PatchMapping("/vouchers/{id}/status")
    public VoucherResponse updateVoucherStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean isActive = body.get("isActive");
        return voucherService.updateStatus(id, isActive);
    }
```
*Lưu ý:* Tạo thêm `VoucherService` nếu cần thiết để tách biệt logic xử lý của khuyến mãi khỏi phân hệ thống kê (Analytics).
