package com.autowash.features.user.repository;

import com.autowash.features.user.entity.User;
import com.autowash.features.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsernameOrPhone(String username, String phone);

    List<User> findByRoleNot(Role role);

    long countByRole(Role role);

    Optional<User> findByEmailOrPhone(String email, String phone);

}

