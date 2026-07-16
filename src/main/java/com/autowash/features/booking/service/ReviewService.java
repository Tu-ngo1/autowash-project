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
