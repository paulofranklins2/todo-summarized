package org.duckdns.todosummarized.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security adapter responsible for loading users by email.
 * In this application, email is used as the authentication username.
 * Uses cached user lookup for O(1) access time on repeated requests.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "User not found with email: ";

    private final UserCacheService userCacheService;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userCacheService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND + email));
    }
}
