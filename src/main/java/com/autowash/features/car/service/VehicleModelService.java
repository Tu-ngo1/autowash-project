package com.autowash.features.car.service;

import com.autowash.features.car.entity.Car;



import com.autowash.features.car.entity.VehicleModel;
import com.autowash.features.car.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleModelService {

    private final VehicleModelRepository vehicleModelRepository;

    public List<VehicleModel> getAllVehicleModels() {
        return vehicleModelRepository.findAll();
    }
}


