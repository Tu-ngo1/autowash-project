package com.autowash.infrastructure.security;



import com.autowash.features.user.entity.User;
import com.autowash.features.user.enums.UserStatus;
import com.autowash.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .or(() -> userRepository.findByUsernameOrPhone(username, username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail() != null ? user.getEmail() : (user.getPhone() != null ? user.getPhone() : user.getUsername()),
                user.getPassword(),
                user.getStatus() == UserStatus.ACTIVE,
                true,
                true,
                user.getStatus() != UserStatus.LOCKED,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
