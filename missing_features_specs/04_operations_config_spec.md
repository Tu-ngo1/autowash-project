# Đặc tả Yêu cầu - Tải Cấu Hình Hoạt Động & Ngày Nghỉ (Operations Config)

Hỗ trợ giao diện Admin tải thông tin cấu hình hoạt động ngày mai để hiển thị trước khi thay đổi trạng thái mở/đóng cửa hoặc số ca làm việc.

---

## 1. API Endpoints cần bổ sung

### 1.1. Xem cấu hình hoạt động ngày mai (Get Tomorrow Config)
- **Method:** `GET`
- **Path:** `/api/admin/operations/config-tomorrow`
- **Mô tả:** Trả về cấu hình vận hành (giờ mở cửa, ca làm việc, số khoang) đã lưu cho ngày mai, hoặc trả về cấu hình mặc định nếu Admin chưa thiết lập tùy chỉnh.
- **Response Body:**
```json
{
  "id": 1,
  "configDate": "2026-07-11",
  "openTime": "08:00:00",
  "closeTime": "17:00:00",
  "bayCount": 3,
  "isActive": true
}
```

---

## 2. Các thay đổi cần thực hiện trong Backend

### 2.1. Cập nhật `AdminOperationsController.java`
Thêm Mapping `@GetMapping("/config-tomorrow")` vào [AdminOperationsController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/controller/AdminOperationsController.java):
```java
    @GetMapping("/config-tomorrow")
    public ResponseEntity<DailyOperationsConfig> getTomorrowConfig() {
        DailyOperationsConfig config = configService.getTomorrowConfig();
        return ResponseEntity.ok(config);
    }
```

### 2.2. Triển khai trong `OperationsConfigService.java`
Thêm hàm `getTomorrowConfig` vào [OperationsConfigService.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/booking/service/OperationsConfigService.java) để lấy cấu hình ngày mai:
```java
    public DailyOperationsConfig getTomorrowConfig() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        // Truy vấn cấu hình ngày mai trong database.
        // Nếu không có dữ liệu, trả về đối tượng cấu hình mặc định (Mở: 08:00, Đóng: 17:00, Khoang: 3, Hoạt động: true)
        return configRepository.findByConfigDate(tomorrow)
                .orElse(DailyOperationsConfig.builder()
                        .configDate(tomorrow)
                        .openTime(LocalTime.of(8, 0))
                        .closeTime(LocalTime.of(17, 0))
                        .bayCount(3)
                        .isActive(true)
                        .build());
    }
```
*Lưu ý:* Việc trả về đối tượng mặc định này giúp Frontend không bị lỗi hiển thị các trường nhập liệu trống khi ngày mai chưa được cấu hình tùy chỉnh.
