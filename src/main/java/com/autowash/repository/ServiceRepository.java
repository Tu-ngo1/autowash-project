package com.autowash.repository;

import com.autowash.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Lấy danh sách dịch vụ đang hoạt động cho Customer xem
    List<Service> findByActiveTrue();

    // Kiểm tra tên dịch vụ đã tồn tại chưa
    boolean existsByNameIgnoreCase(String name);
}
