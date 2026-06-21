package com.autowash.features.washservice.service;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.car.entity.Car;

import com.autowash.features.car.enums.VehicleSize;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.washservice.dto.request.CreateServicePriceRequest;
import com.autowash.features.washservice.dto.request.UpdateServicePriceRequest;
import com.autowash.features.washservice.dto.response.ServicePriceResponse;

import com.autowash.features.washservice.entity.ServicePrice;
import com.autowash.features.washservice.repository.ServicePriceRepository;
import com.autowash.features.washservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServicePriceService {

    private final ServicePriceRepository servicePriceRepository;
    private final ServiceRepository serviceRepository;

    // Admin tạo giá cho dịch vụ theo kích cỡ xe
    public ServicePriceResponse createServicePrice(CreateServicePriceRequest request) {

        Service service = findServiceOrThrow(request.getServiceId());

        // Một service không được có 2 giá cho cùng 1 kích cỡ xe
        if (servicePriceRepository.existsByServiceIdAndVehicleSize(
                request.getServiceId(),
                request.getVehicleSize()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dịch vụ này đã có giá cho kích cỡ xe này"
            );
        }

        ServicePrice servicePrice = ServicePrice.builder()
                .service(service)
                .vehicleSize(request.getVehicleSize())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .active(true)
                .build();

        ServicePrice saved = servicePriceRepository.save(servicePrice);

        return toServicePriceResponse(saved);
    }

    // Admin xem tất cả bảng giá
    public List<ServicePriceResponse> getAllServicePricesForAdmin() {
        return servicePriceRepository.findAll()
                .stream()
                .map(this::toServicePriceResponse)
                .toList();
    }

    // Customer xem giá đang active của một dịch vụ
    public List<ServicePriceResponse> getActivePricesByServiceId(Long serviceId) {

        findServiceOrThrow(serviceId);

        return servicePriceRepository.findByServiceIdAndActiveTrue(serviceId)
                .stream()
                .map(this::toServicePriceResponse)
                .toList();
    }

    // Xem chi tiết một service price
    public ServicePriceResponse getServicePriceById(Long id) {
        ServicePrice servicePrice = findServicePriceOrThrow(id);
        return toServicePriceResponse(servicePrice);
    }

    // Admin cập nhật giá / thời lượng / trạng thái
    public ServicePriceResponse updateServicePrice(
            Long id,
            UpdateServicePriceRequest request
    ) {
        ServicePrice servicePrice = findServicePriceOrThrow(id);

        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Giá không được âm"
                );
            }
            servicePrice.setPrice(request.getPrice());
        }

        if (request.getDurationMinutes() != null) {
            if (request.getDurationMinutes() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Thời lượng phải lớn hơn 0 phút"
                );
            }
            servicePrice.setDurationMinutes(request.getDurationMinutes());
        }

        if (request.getActive() != null) {
            servicePrice.setActive(request.getActive());
        }

        ServicePrice updated = servicePriceRepository.save(servicePrice);

        return toServicePriceResponse(updated);
    }

    // Admin xóa mềm bảng giá
    // Không xóa khỏi DB vì booking cũ vẫn cần lịch sử giá
    public void deleteServicePrice(Long id) {
        ServicePrice servicePrice = findServicePriceOrThrow(id);
        servicePrice.setActive(false);
        servicePriceRepository.save(servicePrice);
    }

    // Tìm Service, không có thì báo lỗi
    private Service findServiceOrThrow(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy dịch vụ"
                ));
    }

    // Tìm ServicePrice, không có thì báo lỗi
    private ServicePrice findServicePriceOrThrow(Long id) {
        return servicePriceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bảng giá dịch vụ"
                ));
    }

    // Convert Entity sang Response DTO
    private ServicePriceResponse toServicePriceResponse(ServicePrice servicePrice) {
        return ServicePriceResponse.builder()
                .id(servicePrice.getId())
                .serviceId(servicePrice.getService().getId())
                .serviceName(servicePrice.getService().getName())
                .vehicleSize(servicePrice.getVehicleSize())
                .price(servicePrice.getPrice())
                .durationMinutes(servicePrice.getDurationMinutes())
                .active(servicePrice.getActive())
                .build();
    }
}


