package com.autowash.features.washservice.service;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.washservice.dto.request.CreateServiceRequest;
import com.autowash.features.washservice.dto.request.UpdateServiceRequest;
import com.autowash.features.washservice.dto.response.ServiceResponse;

import com.autowash.features.washservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class WashService {

    private final ServiceRepository serviceRepository;

    // Admin tạo dịch vụ mới
    public ServiceResponse createService(CreateServiceRequest request) {

        // Kiểm tra tên dịch vụ có bị trùng không
        if (serviceRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên dịch vụ đã tồn tại"
            );
        }

        Service service = Service.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();

        Service savedService = serviceRepository.save(service);

        return toServiceResponse(savedService);
    }

    // Admin xem tất cả dịch vụ, bao gồm cả active và inactive
    public List<ServiceResponse> getAllServicesForAdmin() {
        return serviceRepository.findAll()
                .stream()
                .map(this::toServiceResponse)
                .toList();
    }

    // Customer chỉ xem dịch vụ đang hoạt động
    public List<ServiceResponse> getActiveServicesForCustomer() {
        return serviceRepository.findByActiveTrue()
                .stream()
                .map(this::toServiceResponse)
                .toList();
    }

    // Xem chi tiết 1 dịch vụ
    public ServiceResponse getServiceById(Long id) {
        Service service = findServiceOrThrow(id);
        return toServiceResponse(service);
    }

    // Admin cập nhật dịch vụ
    public ServiceResponse updateService(Long id, UpdateServiceRequest request) {

        Service service = findServiceOrThrow(id);

        // Nếu có truyền name mới thì mới update
        if (request.getName() != null && !request.getName().isBlank()) {

            // Nếu đổi sang tên khác thì phải kiểm tra trùng
            boolean isChangingName =
                    !service.getName().equalsIgnoreCase(request.getName());

            if (isChangingName &&
                    serviceRepository.existsByNameIgnoreCase(request.getName())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tên dịch vụ đã tồn tại"
                );
            }

            service.setName(request.getName());
        }

        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }

        if (request.getActive() != null) {
            service.setActive(request.getActive());
        }

        Service updatedService = serviceRepository.save(service);

        return toServiceResponse(updatedService);
    }

    // Admin xóa mềm dịch vụ
    // Không xóa khỏi DB để tránh mất lịch sử booking
    public void deleteService(Long id) {
        Service service = findServiceOrThrow(id);
        service.setActive(false);
        serviceRepository.save(service);
    }

    // Hàm dùng chung: tìm service, nếu không có thì báo lỗi
    private Service findServiceOrThrow(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy dịch vụ"
                ));
    }

    // Convert Entity sang DTO Response
    private ServiceResponse toServiceResponse(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .active(service.getActive())
                .build();
    }
}


