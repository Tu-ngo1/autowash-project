package com.autowash.service;

import com.autowash.entity.VehicleModel;
import com.autowash.repository.VehicleModelRepository;
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
