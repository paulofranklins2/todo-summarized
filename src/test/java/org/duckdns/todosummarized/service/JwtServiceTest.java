package org.duckdns.todosummarized.service;

import org.duckdns.todosummarized.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtService.
 */
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private Clock fixedClock;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("TestSecretKeyMustBeAtLeast256BitsLongForHS256Algorithm!");
        jwtProperties.setAccessTokenExpiration(900_000L); // 15 minutes
        jwtProperties.setRefreshTokenExpiration(604_800_000L); // 7 days
        jwtProperties.setIssuer("test-issuer");

        fixedClock = Clock.fixed(Instant.parse("2026-01-09T12:00:00Z"), ZoneId.of("UTC"));
        jwtService = new JwtService(jwtProperties, fixedClock);

        testUser = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Nested
    @DisplayName("Token Generation ")
    class TokenGeneration {

        @Test
        @DisplayName("generateAccessToken should create valid token")
        void generateAccessToken_ShouldCreateValidToken() {
            // Act
            String token = jwtService.generateAccessToken(testUser);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("generateRefreshToken should create valid token")
        void generateRefreshToken_ShouldCreateValidToken() {
            // Act
            String token = jwtService.generateRefreshToken(testUser);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("access and refresh tokens should be different")
        void accessAndRefreshTokens_ShouldBeDifferent() {
            // Act
            String accessToken = jwtService.generateAccessToken(testUser);
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Assert
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    @Nested
    @DisplayName("Token Extraction ")
    class TokenExtraction {

        @Test
        @DisplayName("extractUsername should return correct username")
        void extractUsername_ShouldReturnCorrectUsername() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            String username = jwtService.extractUsername(token);

            // Assert
            assertThat(username).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("extractExpiration should return future date for valid token")
        void extractExpiration_ShouldReturnFutureDate() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            var expiration = jwtService.extractExpiration(token);

            // Assert
            assertThat(expiration).isNotNull();
            assertThat(expiration.getTime()).isGreaterThan(fixedClock.millis());
        }
    }

    @Nested
    @DisplayName("Token Validation ")
    class TokenValidation {

        @Test
        @DisplayName("isTokenValid should return true for valid token and matching user")
        void isTokenValid_ShouldReturnTrue_ForValidTokenAndMatchingUser() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            boolean isValid = jwtService.isTokenValid(token, testUser);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("isTokenValid should return false for mismatched user")
        void isTokenValid_ShouldReturnFalse_ForMismatchedUser() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);
            UserDetails differentUser = User.builder()
                    .username("other@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // Act
            boolean isValid = jwtService.isTokenValid(token, differentUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("isTokenValid should return false for tampered token")
        void isTokenValid_ShouldReturnFalse_ForTamperedToken() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            // Act
            boolean isValid = jwtService.isTokenValid(tamperedToken, testUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("isTokenExpired should return false for fresh token")
        void isTokenExpired_ShouldReturnFalse_ForFreshToken() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act
            boolean isExpired = jwtService.isTokenExpired(token);

            // Assert
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("isTokenExpired should return true for expired token")
        void isTokenExpired_ShouldReturnTrue_ForExpiredToken() {
            // Arrange - Create token with immediate expiration
            JwtProperties shortLivedProps = new JwtProperties();
            shortLivedProps.setSecretKey("TestSecretKeyMustBeAtLeast256BitsLongForHS256Algorithm!");
            shortLivedProps.setAccessTokenExpiration(1L); // 1ms
            shortLivedProps.setIssuer("test-issuer");

            // Use a clock that's in the past
            Clock pastClock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.of("UTC"));
            JwtService shortLivedService = new JwtService(shortLivedProps, pastClock);
            String expiredToken = shortLivedService.generateAccessToken(testUser);

            // Act - Check expiration with current time
            boolean isExpired = jwtService.isTokenExpired(expiredToken);

            // Assert
            assertThat(isExpired).isTrue();
        }
    }

    @Nested
    @DisplayName("Role Claims ")
    class RoleClaims {

        @Test
        @DisplayName("access token should contain role claim")
        void accessToken_ShouldContainRoleClaim() {
            // Arrange
            UserDetails adminUser = User.builder()
                    .username("admin@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();

            // Act
            String token = jwtService.generateAccessToken(adminUser);
            String extractedRole = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

            // Assert
            assertThat(extractedRole).isEqualTo("ROLE_ADMIN");
        }
    }
}
