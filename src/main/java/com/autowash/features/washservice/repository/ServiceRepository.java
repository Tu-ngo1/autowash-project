package com.autowash.features.washservice.repository;

import com.autowash.features.washservice.service.WashService;

import com.autowash.features.washservice.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}


