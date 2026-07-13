# Đặc tả Yêu cầu - Phân Hệ Đánh Giá Dịch Vụ (Review Feature)

Phân hệ này cho phép khách hàng đánh giá chất lượng dịch vụ (số sao và bình luận) sau khi đơn đặt lịch của họ đã hoàn thành.

---

## 1. Các thành phần cần tạo mới ở Backend

### 1.1. Repository: `ReviewRepository.java`
Tạo tại gói `com.autowash.features.booking.repository`:
```java
package com.autowash.features.booking.repository;

import com.autowash.features.booking.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Kiểm tra xem đơn đặt lịch đã được đánh giá chưa
    boolean existsByBookingId(Long bookingId);

    // Lấy danh sách đánh giá của một khách hàng cụ thể
    @Query("SELECT r FROM Review r JOIN r.booking b WHERE b.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserId(@Param("userId") Long userId);
}
```

### 1.2. DTOs: `ReviewRequest` & `ReviewResponse`
Tạo tại gói `com.autowash.features.booking.dto.request` và `response`:

**`ReviewRequest.java`**:
```java
package com.autowash.features.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull(message = "Mã đơn hàng không được để trống")
    private Long bookingId;

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
    private Integer rating;

    private String comment;
}
```

**`ReviewResponse.java`**:
```java
package com.autowash.features.booking.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long bookingId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
```

### 1.3. Service: `ReviewService.java`
Tạo tại gói `com.autowash.features.booking.service`:
```java
package com.autowash.features.booking.service;

import com.autowash.features.booking.dto.request.ReviewRequest;
import com.autowash.features.booking.dto.response.ReviewResponse;
import com.autowash.features.booking.entity.Booking;
import com.autowash.features.booking.entity.Review;
import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.repository.BookingRepository;
import com.autowash.features.booking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"));

        // Kiểm tra quyền sở hữu
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đánh giá đơn đặt lịch này");
        }

        // Kiểm tra trạng thái đơn hàng (chỉ cho phép đánh giá đơn đã hoàn thành)
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể đánh giá lịch hẹn đã hoàn thành");
        }

        // Kiểm tra xem đã có đánh giá cho booking này chưa
        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch hẹn này đã được đánh giá trước đó");
        }

        Review review = Review.builder()
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    public List<ReviewResponse> getMyReviews(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
```

### 1.4. Controller: `ReviewController.java`
Tạo tại gói `com.autowash.features.booking.controller`:
```java
package com.autowash.features.booking.controller;

import com.autowash.features.booking.dto.request.ReviewRequest;
import com.autowash.features.booking.dto.response.ReviewResponse;
import com.autowash.features.booking.service.ReviewService;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    @PostMapping
    public ReviewResponse createReview(@Valid @RequestBody ReviewRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        return reviewService.createReview(currentUser.getId(), request);
    }

    @GetMapping("/my")
    public List<ReviewResponse> getMyReviews() {
        User currentUser = userService.getCurrentUserEntity();
        return reviewService.getMyReviews(currentUser.getId());
    }
}
```
