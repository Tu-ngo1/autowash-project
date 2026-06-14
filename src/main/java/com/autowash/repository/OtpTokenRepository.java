package com.autowash.repository;

import com.autowash.entity.OtpToken;
import com.autowash.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    List<OtpToken> findByUser(User user);

    List<OtpToken> findByUserId(Long userId);

    Optional<OtpToken> findTopByUserIdAndVerifiedFalseOrderByCreatedAtDesc(Long userId);

    Optional<OtpToken> findTopByEmailOrderByCreatedAtDesc(String email);
}
