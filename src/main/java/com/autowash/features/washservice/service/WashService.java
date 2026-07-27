package com.autowash.features.washservice.service;

import com.autowash.features.washservice.entity.Service;
import com.autowash.features.booking.entity.Booking;

import com.autowash.features.washservice.dto.request.CreateServiceRequest;
import com.autowash.features.washservice.dto.request.UpdateServiceRequest;
import com.autowash.features.washservice.dto.request.ServicePriceConfig;
import com.autowash.features.washservice.dto.response.ServiceResponse;
import com.autowash.features.washservice.dto.response.AdminServiceResponse;
import com.autowash.features.washservice.entity.ServicePrice;

import com.autowash.features.washservice.repository.ServiceRepository;
import com.autowash.features.washservice.repository.ServicePriceRepository;
import com.autowash.features.car.repository.CarRepository;
import com.autowash.features.car.enums.VehicleSize;
import com.autowash.features.washservice.dto.response.AvailableServiceResponse;
import com.autowash.features.car.entity.Car;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class WashService {

    private final ServiceRepository serviceRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final CarRepository carRepository;

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

    public List<AvailableServiceResponse> getServicesByVehicleSize(VehicleSize size) {
        VehicleSize targetSize = size != null ? size : VehicleSize.SMALL;
        List<ServicePrice> prices = (size != null)
                ? servicePriceRepository.findByVehicleSizeAndActiveTrueAndServiceActiveTrue(size)
                .stream()
                .filter(price -> price.getService() != null && Boolean.TRUE.equals(price.getService().getActive()))
                .toList()
                : servicePriceRepository.findAll()
                .stream()
                .filter(price -> Boolean.TRUE.equals(price.getActive()) && price.getService() != null && Boolean.TRUE.equals(price.getService().getActive()))
                .toList();

        return prices.stream()
                .map(price -> AvailableServiceResponse.builder()
                        .id(price.getService().getId())
                        .serviceId(price.getService().getId())
                        .name(price.getService().getName())
                        .serviceName(price.getService().getName())
                        .description(price.getService().getDescription())
                        .servicePriceId(price.getId())
                        .price(price.getPrice())
                        .durationMinutes(price.getDurationMinutes())
                        .vehicleSize(price.getVehicleSize() != null ? price.getVehicleSize().name() : targetSize.name())
                        .isMainService(price.getService().getIsMainService())
                        .build())
                .toList();
    }

    public List<AvailableServiceResponse> getServicesForCustomerCar(Long customerId, Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phương tiện"));

        if (!car.getUser().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không sở hữu phương tiện này");
        }

        VehicleSize size = car.getVehicleModel().getVehicleSize();
        return getServicesByVehicleSize(size);
    }

    @Transactional
    public AdminServiceResponse createServiceWithPrices(CreateServiceRequest request) {
        if (serviceRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên dịch vụ đã tồn tại");
        }

        Service service = Service.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isMainService(request.getIsMainService() != null ? request.getIsMainService() : false)
                .active(true)
                .build();
        Service saved = serviceRepository.save(service);

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

    @Transactional
    public AdminServiceResponse updateServiceWithPrices(Long id, CreateServiceRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dịch vụ không tồn tại"));

        if (!service.getName().equalsIgnoreCase(request.getName()) && serviceRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên dịch vụ đã tồn tại");
        }

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        if (request.getIsMainService() != null) {
            service.setIsMainService(request.getIsMainService());
        }
        Service saved = serviceRepository.save(service);

        if (request.getServicePrices() != null) {
            List<ServicePrice> currentPrices = servicePriceRepository.findByServiceId(saved.getId());

            for (ServicePriceConfig priceConfig : request.getServicePrices()) {
                ServicePrice existingPrice = currentPrices.stream()
                        .filter(p -> p.getVehicleSize() == priceConfig.getVehicleSize())
                        .findFirst()
                        .orElse(null);

                if (existingPrice != null) {
                    existingPrice.setPrice(priceConfig.getPrice());
                    existingPrice.setDurationMinutes(priceConfig.getDuration());
                    existingPrice.setActive(priceConfig.getActive() != null ? priceConfig.getActive() : true);
                    servicePriceRepository.save(existingPrice);
                } else {
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
        }

        return mapToAdminResponse(saved);
    }

    public List<AdminServiceResponse> getAdminServicesWithPrices() {
        return serviceRepository.findAll().stream()
                .map(this::mapToAdminResponse)
                .toList();
    }

    @Transactional
    public void deleteService(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dịch vụ không tồn tại"));
        service.setActive(false);
        serviceRepository.save(service);

        List<ServicePrice> prices = servicePriceRepository.findByServiceId(id);
        for (ServicePrice price : prices) {
            price.setActive(false);
            servicePriceRepository.save(price);
        }
    }

    @Transactional
    public AdminServiceResponse patchServiceStatus(Long id, Boolean active) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dịch vụ không tồn tại"));
        service.setActive(active);
        Service saved = serviceRepository.save(service);

        List<ServicePrice> prices = servicePriceRepository.findByServiceId(id);
        for (ServicePrice price : prices) {
            price.setActive(active);
            servicePriceRepository.save(price);
        }

        return mapToAdminResponse(saved);
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
}


