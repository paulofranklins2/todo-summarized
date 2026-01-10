package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserCacheService userCacheService;

    private CustomUserDetailsService customUserDetailsService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "encodedPassword123";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customUserDetailsService = new CustomUserDetailsService(userCacheService);
    }

    private User createTestUser() {
        return User.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .role(Role.ROLE_USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("should return UserDetails when user exists")
        void shouldReturnUserDetailsWhenUserExists() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(TEST_EMAIL);
            assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD);
            verify(userCacheService).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(TEST_EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining(TEST_EMAIL)
                    .hasMessageContaining("User not found with email:");

            verify(userCacheService).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("should return enabled status correctly")
        void shouldReturnEnabledStatusCorrectly() {
            // Given
            User enabledUser = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(enabledUser));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return disabled status correctly")
        void shouldReturnDisabledStatusCorrectly() {
            // Given
            User disabledUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(false)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(disabledUser));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return account non-expired status correctly")
        void shouldReturnAccountNonExpiredStatusCorrectly() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should return expired account status correctly")
        void shouldReturnExpiredAccountStatusCorrectly() {
            // Given
            User expiredUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .accountNonExpired(false)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(expiredUser));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isAccountNonExpired()).isFalse();
        }

        @Test
        @DisplayName("should return account non-locked status correctly")
        void shouldReturnAccountNonLockedStatusCorrectly() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("should return locked account status correctly")
        void shouldReturnLockedAccountStatusCorrectly() {
            // Given
            User lockedUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(false)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(lockedUser));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("should return credentials non-expired status correctly")
        void shouldReturnCredentialsNonExpiredStatusCorrectly() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should return expired credentials status correctly")
        void shouldReturnExpiredCredentialsStatusCorrectly() {
            // Given
            User credentialsExpiredUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(credentialsExpiredUser));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.isCredentialsNonExpired()).isFalse();
        }

        @Test
        @DisplayName("should return correct authorities for ROLE_USER")
        void shouldReturnCorrectAuthoritiesForRoleUser() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should return correct authorities for ROLE_ADMIN")
        void shouldReturnCorrectAuthoritiesForRoleAdmin() {
            // Given
            User adminUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(adminUser));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }

        @ParameterizedTest
        @DisplayName("should search with exact email provided")
        @ValueSource(strings = {"user@domain.com", "admin@test.org", "test.user@example.net"})
        void shouldSearchWithExactEmailProvided(String email) {
            // Given
            when(userCacheService.findByEmail(email)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining(email);

            verify(userCacheService).findByEmail(email);
        }

        @ParameterizedTest
        @DisplayName("should throw exception for null or empty email")
        @NullAndEmptySource
        void shouldThrowExceptionForNullOrEmptyEmail(String email) {
            // Given
            when(userCacheService.findByEmail(email)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                    .isInstanceOf(UsernameNotFoundException.class);

            verify(userCacheService).findByEmail(email);
        }

        @Test
        @DisplayName("should call cache service exactly once")
        void shouldCallCacheServiceExactlyOnce() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            verify(userCacheService, times(1)).findByEmail(TEST_EMAIL);
            verifyNoMoreInteractions(userCacheService);
        }

        @Test
        @DisplayName("should return User instance implementing UserDetails")
        void shouldReturnUserInstanceImplementingUserDetails() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserDetails result = customUserDetailsService.loadUserByUsername(TEST_EMAIL);

            // Then
            assertThat(result).isInstanceOf(User.class);
            assertThat(result).isSameAs(user);
        }
    }
}

