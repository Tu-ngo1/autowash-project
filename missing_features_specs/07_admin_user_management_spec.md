# Đặc tả Yêu cầu - Quản Lý Người Dùng & Xe Của Admin (Admin User Management)

Hỗ trợ Admin quản lý danh sách toàn bộ khách hàng và nhân viên, sửa đổi thông tin cá nhân, cộng/trừ điểm thưởng trực tiếp, và thêm/xóa phương tiện (xe) của từng khách hàng.

---

## 1. API Endpoints cần bổ sung/chỉnh sửa
Bổ sung các endpoints sau vào [UserController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/user/controller/UserController.java) (route gốc `@RequestMapping("/api/admin")`):

### 1.1. Lấy chi tiết thông tin một User
- **Method:** `GET`
- **Path:** `/api/admin/users/{id}`
- **Response Body:**
```json
{
  "id": 201,
  "fullName": "Nguyễn Văn A",
  "email": "customer@example.com",
  "phone": "0901234567",
  "username": "customer_a",
  "role": "CUSTOMER",
  "status": "ACTIVE",
  "tier": "GOLD",
  "tierPoints": 1280,
  "rewardPoints": 420,
  "walletBalance": 150000,
  "vehicles": [
    {
      "id": 1,
      "brand": "Toyota",
      "modelName": "Vios",
      "licensePlate": "51F-123.45"
    }
  ]
}
```

### 1.2. Sửa đổi thông tin User
- **Method:** `PUT`
- **Path:** `/api/admin/users/{id}`
- **Request Body:**
```json
{
  "fullName": "Nguyễn Văn A Sửa Đổi",
  "email": "newemail@example.com",
  "phone": "0901234568"
}
```

### 1.3. Cộng/Trừ điểm thưởng trực tiếp
- **Method:** `PATCH`
- **Path:** `/api/admin/users/{id}/points`
- **Request Body:**
```json
{
  "rewardPoints": 500,  // Số điểm mới của khách
  "tierPoints": 1500    // Số điểm hạng mới của khách
}
```
- **Logic xử lý:** Cập nhật trực tiếp điểm vào `CustomerProfile` và tính toán lại thứ hạng `tier` của khách hàng nếu điểm hạng thay đổi.

### 1.4. Quản lý danh sách xe của khách hàng

#### A. Thêm xe mới cho khách hàng:
- **Method:** `POST`
- **Path:** `/api/admin/users/{userId}/vehicles`
- **Request Body:**
```json
{
  "licensePlate": "30A-999.99",
  "vehicleModelId": 3
}
```
- **Logic xử lý:** Kiểm tra xe đã tồn tại chưa. Tạo thực thể `Car` liên kết với `userId` và `vehicleModelId`.

#### B. Xóa xe của khách hàng:
- **Method:** `DELETE`
- **Path:** `/api/admin/users/{userId}/vehicles/{vehicleId}`
- **Logic xử lý:** Đổi trạng thái xe `Car.status` thành `DELETED` hoặc xóa khỏi hệ thống.

---

## 2. Thiết kế logic mã nguồn Backend

### 2.1. Cấu trúc Response chi tiết (`AdminUserDetailResponse.java`)
```java
package com.autowash.features.user.dto.response;

import com.autowash.features.car.dto.response.CarResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminUserDetailResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private String role;
    private String status;
    private String tier;
    private Integer tierPoints;
    private Integer rewardPoints;
    private Integer walletBalance;
    private List<CarResponse> vehicles;
}
```

### 2.2. Triển khai logic trong `UserService.java`
```java
    // Lấy chi tiết user cho Admin
    public AdminUserDetailResponse getAdminUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));

        CustomerProfile profile = user.getCustomerProfile();
        String tier = profile != null && profile.getTierConfig() != null ? profile.getTierConfig().getTierLevel().name() : "MEMBER";
        int tPoints = profile != null && profile.getTierPoints() != null ? profile.getTierPoints() : 0;
        int rPoints = profile != null && profile.getRewardPoints() != null ? profile.getRewardPoints() : 0;

        // Lấy số dư ví
        int walletBalance = walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(0);

        // Lấy danh sách xe
        List<CarResponse> vehicles = carRepository.findByUserId(userId).stream()
                .filter(car -> car.getStatus() == CarStatus.ACTIVE)
                .map(CarResponse::fromCar)
                .toList();

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .username(user.getUsername())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .tier(tier)
                .tierPoints(tPoints)
                .rewardPoints(rPoints)
                .walletBalance(walletBalance)
                .vehicles(vehicles)
                .build();
    }
    
    // Viết các phương thức cập nhật points, xe và thông tin tương tự...
```
