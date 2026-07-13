# Đặc tả Yêu cầu - Quản Lý Dịch Vụ Của Admin (Admin Service Management)

Hỗ trợ Admin quản lý danh sách các gói rửa xe (dịch vụ chính/dịch vụ phụ) và định cấu hình giá tiền, thời gian thực hiện chi tiết cho từng nhóm xe (`SMALL`, `MEDIUM`, `LARGE`).

---

## 1. API Endpoints cần bổ sung
Tạo REST Controller mới: [AdminServiceController.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/washservice/controller/AdminServiceController.java) (đường dẫn `@RequestMapping("/api/admin/services")`):

- `GET /api/admin/services` -> Lấy danh sách dịch vụ kèm ma trận giá.
- `POST /api/admin/services` -> Tạo dịch vụ mới kèm ma trận giá.
- `PUT /api/admin/services/{id}` -> Sửa thông tin dịch vụ và ma trận giá.
- `DELETE /api/admin/services/{id}` -> Xóa (hoặc tắt hoạt động) dịch vụ.
- `PATCH /api/admin/services/{id}/status` -> Bật/tắt nhanh hoạt động.

---

## 2. Thiết kế Request/Response DTO

### 2.1. Request DTO cấu hình giá (`ServicePriceConfig`)
```java
package com.autowash.features.washservice.dto.request;

import com.autowash.features.car.enums.VehicleSize;
import lombok.Data;

@Data
public class ServicePriceConfig {
    private VehicleSize vehicleSize;
    private Integer price;
    private Integer duration; // thời gian rửa (phút)
    private Boolean active;
}
```

### 2.2. Request DTO tạo mới dịch vụ (`CreateServiceRequest.java` nâng cấp)
```java
package com.autowash.features.washservice.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateServiceRequest {
    private String name;
    private String description;
    private Boolean isMainService;
    private List<ServicePriceConfig> servicePrices;
}
```

### 2.3. Response DTO lấy dịch vụ (`AdminServiceResponse.java`)
```java
package com.autowash.features.washservice.dto.response;

import com.autowash.features.car.enums.VehicleSize;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminServiceResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    private Boolean isMainService;
    private List<PriceDetail> servicePrices;

    @Data
    @Builder
    public static class PriceDetail {
        private Long id;
        private VehicleSize vehicleSize;
        private Integer price;
        private Integer duration;
        private Boolean active;
    }
}
```

---

## 3. Cập nhật logic trong `WashService.java`
Nâng cấp logic trong [WashService.java](file:///c:/Users/HP/github/SWP301/AutowashProject/backend/src/main/java/com/autowash/features/washservice/service/WashService.java):

```java
    // Import các class DTO mới
    import com.autowash.features.washservice.dto.request.ServicePriceConfig;
    import com.autowash.features.washservice.dto.response.AdminServiceResponse;
    import com.autowash.features.washservice.entity.ServicePrice;

    @Transactional
    public AdminServiceResponse createServiceWithPrices(CreateServiceRequest request) {
        if (serviceRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên dịch vụ đã tồn tại");
        }

        // 1. Tạo Service
        Service service = Service.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isMainService(request.getIsMainService() != null ? request.getIsMainService() : false)
                .active(true)
                .build();
        Service saved = serviceRepository.save(service);

        // 2. Tạo ma trận giá (ServicePrice)
        if (request.getServicePrices() != null) {
            for (ServicePriceConfig priceConfig : request.getServicePrices()) {
                ServicePrice price = ServicePrice.builder()
                        .service(saved)
                        .vehicleSize(priceConfig.getVehicleSize())
                        .price(priceConfig.getPrice())
                        .durationMinutes(priceConfig.getDuration())
                        .active(priceConfig.getActive() != null ? priceConfig.getActive() : true)
                        .build();
                servicePriceRepository.save(price);
            }
        }

        return mapToAdminResponse(saved);
    }

    // Viết tương tự cho updateService (cập nhật hoặc thêm mới các dòng ServicePrice của Service)
    
    public List<AdminServiceResponse> getAdminServicesWithPrices() {
        return serviceRepository.findAll().stream()
                .map(this::mapToAdminResponse)
                .toList();
    }

    private AdminServiceResponse mapToAdminResponse(Service s) {
        List<ServicePrice> prices = servicePriceRepository.findByServiceId(s.getId());
        List<AdminServiceResponse.PriceDetail> priceDetails = prices.stream()
                .map(p -> AdminServiceResponse.PriceDetail.builder()
                        .id(p.getId())
                        .vehicleSize(p.getVehicleSize())
                        .price(p.getPrice())
                        .duration(p.getDurationMinutes())
                        .active(p.getActive())
                        .build())
                .toList();

        return AdminServiceResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .active(s.getActive())
                .isMainService(s.getIsMainService())
                .servicePrices(priceDetails)
                .build();
    }
```
