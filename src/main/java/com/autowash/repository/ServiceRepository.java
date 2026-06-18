package com.autowash.repository;

import com.autowash.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
