package com.autowash.features.booking.repository;

import com.autowash.features.booking.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Kiểm tra xem đơn đặt lịch đã được đánh giá chưa
    boolean existsByBookingId(Long bookingId);

    // Lấy danh sách đánh giá của một khách hàng cụ thể
    @Query("SELECT r FROM Review r JOIN r.booking b WHERE b.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserId(@Param("userId") Long userId);
}
