package com.autowash.features.auth.repository;

import com.autowash.features.auth.entity.OtpToken;
import com.autowash.features.user.entity.User;
import com.autowash.features.auth.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByUserAndEmailOrderByCreatedAtDesc(User user, String email);

    Optional<OtpToken> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<OtpToken> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}


