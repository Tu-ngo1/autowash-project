package com.autowash;

import com.autowash.features.auth.dto.request.LoginRequest;
import com.autowash.features.auth.dto.response.AuthResponse;
import com.autowash.features.auth.service.AuthService;
import com.autowash.features.user.entity.User;
import com.autowash.features.user.enums.Role;
import com.autowash.features.user.enums.UserStatus;
import com.autowash.features.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AutowashApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_USERNAME = "testloginuser";
    private static final String TEST_PHONE = "0999999999";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    @AfterEach
    void cleanUp() {
        userRepository.findByPhone(TEST_PHONE).ifPresent(user -> userRepository.delete(user));
    }

    @Test
    void testLoginWithUsernameAndPhone() {
        // 1. Create and save test user
        User user = User.builder()
                .fullName("Test Login User")
                .username(TEST_USERNAME)
                .email("testloginuser@example.com")
                .phone(TEST_PHONE)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        // 2. Test Login using Username
        LoginRequest usernameLoginRequest = new LoginRequest();
        usernameLoginRequest.setUsernameOrPhone(TEST_USERNAME);
        usernameLoginRequest.setPassword(TEST_PASSWORD);

        AuthResponse usernameLoginResponse = authService.login(usernameLoginRequest);
        assertNotNull(usernameLoginResponse);
        assertNotNull(usernameLoginResponse.getToken());
        assertEquals("CUSTOMER", usernameLoginResponse.getRole());

        // 3. Test Login using Phone
        LoginRequest phoneLoginRequest = new LoginRequest();
        phoneLoginRequest.setUsernameOrPhone(TEST_PHONE);
        phoneLoginRequest.setPassword(TEST_PASSWORD);

        AuthResponse phoneLoginResponse = authService.login(phoneLoginRequest);
        assertNotNull(phoneLoginResponse);
        assertNotNull(phoneLoginResponse.getToken());
        assertEquals("CUSTOMER", phoneLoginResponse.getRole());
    }

}
