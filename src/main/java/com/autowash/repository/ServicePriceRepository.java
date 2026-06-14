package com.autowash.repository;

import com.autowash.entity.ServicePrice;
import com.autowash.enums.VehicleSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicePriceRepository extends JpaRepository<ServicePrice, Long> {

    // Lấy bảng giá của 1 dịch vụ
    List<ServicePrice> findByServiceIdAndActiveTrue(Long serviceId);

    // Tìm giá theo service + kích cỡ xe
    Optional<ServicePrice> findByServiceIdAndVehicleSizeAndActiveTrue(
            Long serviceId,
            VehicleSize vehicleSize
    );

    // Chống tạo trùng giá cho cùng 1 service và size
    boolean existsByServiceIdAndVehicleSize(Long serviceId, VehicleSize vehicleSize);
}
