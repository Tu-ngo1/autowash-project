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
