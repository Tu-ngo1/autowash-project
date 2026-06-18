package com.autowash.repository;

import com.autowash.entity.OtpToken;
import com.autowash.entity.User;
import com.autowash.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByUserAndEmailOrderByCreatedAtDesc(User user, String email);

    Optional<OtpToken> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<OtpToken> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}
