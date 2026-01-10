package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.dto.UserRegistrationDTO;
import org.duckdns.todosummarized.dto.UserResponseDTO;
import org.duckdns.todosummarized.exception.UserAlreadyExistsException;
import org.duckdns.todosummarized.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCacheService userCacheService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userCacheService, passwordEncoder);
    }

    private User createTestUser() {
        return User.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .role(Role.ROLE_USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserRegistrationDTO createRegistrationDTO(String email, String password) {
        return UserRegistrationDTO.builder()
                .email(email)
                .password(password)
                .build();
    }

    @Nested
    @DisplayName("registerUser()")
    class RegisterUserTests {

        @Test
        @DisplayName("should register user successfully with valid data")
        void shouldRegisterUserSuccessfully() {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(TEST_EMAIL, TEST_PASSWORD);
            User savedUser = createTestUser();

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponseDTO result = userService.registerUser(registrationDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(result.getRole()).isEqualTo(Role.ROLE_USER.name());
            assertThat(result.isEnabled()).isTrue();

            verify(userRepository).existsByEmail(TEST_EMAIL);
            verify(passwordEncoder).encode(TEST_PASSWORD);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should save user with correct attributes")
        void shouldSaveUserWithCorrectAttributes() {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(TEST_EMAIL, TEST_PASSWORD);
            User savedUser = createTestUser();
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

            // When
            userService.registerUser(registrationDTO);

            // Then
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(capturedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(capturedUser.getRole()).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException when email exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(TEST_EMAIL, TEST_PASSWORD);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(registrationDTO))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(TEST_EMAIL);

            verify(userRepository).existsByEmail(TEST_EMAIL);
            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            // Given
            String upperCaseEmail = "TEST@EXAMPLE.COM";
            UserRegistrationDTO registrationDTO = createRegistrationDTO(upperCaseEmail, TEST_PASSWORD);
            User savedUser = createTestUser();
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

            // When
            userService.registerUser(registrationDTO);

            // Then
            verify(userRepository).existsByEmail(TEST_EMAIL);
            assertThat(userCaptor.getValue().getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("should trim whitespace from email")
        void shouldTrimWhitespaceFromEmail() {
            // Given
            String emailWithSpaces = "  test@example.com  ";
            UserRegistrationDTO registrationDTO = createRegistrationDTO(emailWithSpaces, TEST_PASSWORD);
            User savedUser = createTestUser();
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

            // When
            userService.registerUser(registrationDTO);

            // Then
            verify(userRepository).existsByEmail(TEST_EMAIL);
            assertThat(userCaptor.getValue().getEmail()).isEqualTo(TEST_EMAIL);
        }

        @ParameterizedTest
        @DisplayName("should normalize various email formats correctly")
        @CsvSource({
                "Test@Example.Com, test@example.com",
                "USER@DOMAIN.ORG, user@domain.org",
                "  Mixed@Case.Net  , mixed@case.net"
        })
        void shouldNormalizeVariousEmailFormats(String inputEmail, String expectedEmail) {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(inputEmail, TEST_PASSWORD);
            User savedUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(expectedEmail)
                    .password(ENCODED_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            when(userRepository.existsByEmail(expectedEmail)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

            // When
            userService.registerUser(registrationDTO);

            // Then
            assertThat(userCaptor.getValue().getEmail()).isEqualTo(expectedEmail);
        }

        @Test
        @DisplayName("should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(TEST_EMAIL, TEST_PASSWORD);
            User savedUser = createTestUser();
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

            // When
            userService.registerUser(registrationDTO);

            // Then
            verify(passwordEncoder).encode(TEST_PASSWORD);
            assertThat(userCaptor.getValue().getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(userCaptor.getValue().getPassword()).isNotEqualTo(TEST_PASSWORD);
        }

        @Test
        @DisplayName("should assign ROLE_USER to new users")
        void shouldAssignRoleUserToNewUsers() {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(TEST_EMAIL, TEST_PASSWORD);
            User savedUser = createTestUser();
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

            // When
            userService.registerUser(registrationDTO);

            // Then
            assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("should check email existence with normalized email")
        void shouldCheckEmailExistenceWithNormalizedEmail() {
            // Given
            String mixedCaseEmail = "Test@Example.COM";
            UserRegistrationDTO registrationDTO = createRegistrationDTO(mixedCaseEmail, TEST_PASSWORD);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(registrationDTO))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository).existsByEmail(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("getUserProfile()")
    class GetUserProfileTests {

        @Test
        @DisplayName("should return user profile when user exists")
        void shouldReturnUserProfileWhenUserExists() {
            // Given
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserResponseDTO result = userService.getUserProfile(TEST_EMAIL);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(result.getRole()).isEqualTo(Role.ROLE_USER.name());
            assertThat(result.isEnabled()).isTrue();

            verify(userCacheService).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.getUserProfile(TEST_EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");

            verify(userCacheService).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("should normalize email when getting profile")
        void shouldNormalizeEmailWhenGettingProfile() {
            // Given
            String upperCaseEmail = "TEST@EXAMPLE.COM";
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            userService.getUserProfile(upperCaseEmail);

            // Then
            verify(userCacheService).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("should trim email whitespace when getting profile")
        void shouldTrimEmailWhitespaceWhenGettingProfile() {
            // Given
            String emailWithSpaces = "  test@example.com  ";
            User user = createTestUser();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            userService.getUserProfile(emailWithSpaces);

            // Then
            verify(userCacheService).findByEmail(TEST_EMAIL);
        }

        @ParameterizedTest
        @DisplayName("should normalize various email formats when getting profile")
        @CsvSource({
                "Test@Example.Com, test@example.com",
                "USER@DOMAIN.ORG, user@domain.org",
                "  Mixed@Case.Net  , mixed@case.net"
        })
        void shouldNormalizeVariousEmailFormatsWhenGettingProfile(String inputEmail, String expectedEmail) {
            // Given
            User user = User.builder()
                    .id(TEST_USER_ID)
                    .email(expectedEmail)
                    .password(ENCODED_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userCacheService.findByEmail(expectedEmail)).thenReturn(Optional.of(user));

            // When
            userService.getUserProfile(inputEmail);

            // Then
            verify(userCacheService).findByEmail(expectedEmail);
        }

        @Test
        @DisplayName("should include timestamps in response")
        void shouldIncludeTimestampsInResponse() {
            // Given
            LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 9, 12, 0);
            User user = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .password(ENCODED_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

            // When
            UserResponseDTO result = userService.getUserProfile(TEST_EMAIL);

            // Then
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
            assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("should return disabled status correctly")
        void shouldReturnDisabledStatusCorrectly() {
            // Given
            User disabledUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .password(ENCODED_PASSWORD)
                    .role(Role.ROLE_USER)
                    .enabled(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userCacheService.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(disabledUser));

            // When
            UserResponseDTO result = userService.getUserProfile(TEST_EMAIL);

            // Then
            assertThat(result.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Email Normalization Edge Cases")
    class EmailNormalizationEdgeCases {

        @ParameterizedTest
        @DisplayName("should handle null email in normalization")
        @NullSource
        void shouldHandleNullEmail(String nullEmail) {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(nullEmail, TEST_PASSWORD);

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(registrationDTO))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @DisplayName("should handle various whitespace patterns")
        @ValueSource(strings = {"\ttest@example.com\t", "\ntest@example.com\n", " \t test@example.com \t "})
        void shouldHandleVariousWhitespacePatterns(String emailWithWhitespace) {
            // Given
            UserRegistrationDTO registrationDTO = createRegistrationDTO(emailWithWhitespace, TEST_PASSWORD);
            User savedUser = createTestUser();

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponseDTO result = userService.registerUser(registrationDTO);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).existsByEmail(TEST_EMAIL);
        }
    }
}

