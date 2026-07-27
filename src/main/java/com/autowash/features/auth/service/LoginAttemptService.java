package com.autowash.features.auth.service;

import com.autowash.features.user.entity.User;
import com.autowash.features.user.enums.UserStatus;
import com.autowash.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailedAttempt(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0;

        int currentFailed = (user.getFailedAttempt() != null ? user.getFailedAttempt() : 0) + 1;
        user.setFailedAttempt(currentFailed);

        if (currentFailed >= 5) {
            user.setLockTime(LocalDateTime.now().plusMinutes(5));
            user.setStatus(UserStatus.LOCKED);
        }

        userRepository.saveAndFlush(user);
        return currentFailed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempt(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        if ((user.getFailedAttempt() != null && user.getFailedAttempt() > 0) || user.getLockTime() != null) {
            user.setFailedAttempt(0);
            user.setLockTime(null);
            if (user.getStatus() == UserStatus.LOCKED) {
                user.setStatus(UserStatus.ACTIVE);
            }
            userRepository.saveAndFlush(user);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean checkAndUnlockIfExpired(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        if (user.getLockTime() != null) {
            if (user.getLockTime().isAfter(LocalDateTime.now())) {
                return true; // Still locked
            } else {
                // Lock expired -> Unlock
                user.setLockTime(null);
                user.setFailedAttempt(0);
                if (user.getStatus() == UserStatus.LOCKED) {
                    user.setStatus(UserStatus.ACTIVE);
                }
                userRepository.saveAndFlush(user);
                return false; // Lock cleared
            }
        }
        return false;
    }
}
