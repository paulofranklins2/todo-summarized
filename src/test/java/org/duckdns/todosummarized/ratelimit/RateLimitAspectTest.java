package org.duckdns.todosummarized.ratelimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.Role;
import org.duckdns.todosummarized.exception.RateLimitExceededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private RateLimit rateLimit;

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    private User testUser;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("password")
                .role(Role.ROLE_USER)
                .build();

        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("enforceRateLimit")
    class EnforceRateLimitTests {

        @Test
        @DisplayName("should proceed when rate limiting is disabled")
        void shouldProceedWhenDisabled() throws Throwable {
            when(rateLimitService.isEnabled()).thenReturn(false);
            when(joinPoint.proceed()).thenReturn("result");

            Object result = rateLimitAspect.enforceRateLimit(joinPoint, rateLimit);

            assertEquals("result", result);
            verify(joinPoint).proceed();
            verify(rateLimitService, never()).tryConsume(anyString(), anyString());
        }

        @Test
        @DisplayName("should proceed when no authenticated user")
        void shouldProceedWhenNoAuthenticatedUser() throws Throwable {
            when(rateLimitService.isEnabled()).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            Object result = rateLimitAspect.enforceRateLimit(joinPoint, rateLimit);

            assertEquals("result", result);
            verify(rateLimitService, never()).tryConsume(anyString(), anyString());
        }

        @Test
        @DisplayName("should proceed when rate limit not exceeded")
        void shouldProceedWhenNotExceeded() throws Throwable {
            setAuthenticatedUser(testUser);
            RateLimitService.ConsumptionResult allowedResult = createAllowedResult();

            when(rateLimitService.isEnabled()).thenReturn(true);
            when(rateLimit.key()).thenReturn("ai-summary");
            when(rateLimitService.tryConsume(eq(testUser.getId().toString()), eq("ai-summary")))
                    .thenReturn(allowedResult);
            when(joinPoint.proceed()).thenReturn("result");

            Object result = rateLimitAspect.enforceRateLimit(joinPoint, rateLimit);

            assertEquals("result", result);
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("should throw exception when rate limit exceeded")
        void shouldThrowExceptionWhenExceeded() throws Throwable {
            setAuthenticatedUser(testUser);
            RateLimitService.ConsumptionResult rejectedResult = createRejectedResult();

            when(rateLimitService.isEnabled()).thenReturn(true);
            when(rateLimit.key()).thenReturn("ai-summary");
            when(rateLimitService.tryConsume(eq(testUser.getId().toString()), eq("ai-summary")))
                    .thenReturn(rejectedResult);

            RateLimitExceededException exception = assertThrows(
                    RateLimitExceededException.class,
                    () -> rateLimitAspect.enforceRateLimit(joinPoint, rateLimit)
            );

            assertTrue(exception.getRetryAfterSeconds() >= 0);
            verify(joinPoint, never()).proceed();
        }

        @Test
        @DisplayName("should use user ID from User principal")
        void shouldUseUserIdFromUserPrincipal() throws Throwable {
            setAuthenticatedUser(testUser);
            RateLimitService.ConsumptionResult allowedResult = createAllowedResult();

            when(rateLimitService.isEnabled()).thenReturn(true);
            when(rateLimit.key()).thenReturn("daily-summary");
            when(rateLimitService.tryConsume(anyString(), anyString())).thenReturn(allowedResult);
            when(joinPoint.proceed()).thenReturn("result");

            rateLimitAspect.enforceRateLimit(joinPoint, rateLimit);

            verify(rateLimitService).tryConsume(testUser.getId().toString(), "daily-summary");
        }
    }

    private void setAuthenticatedUser(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.emptyList()
        );
        securityContext.setAuthentication(auth);
    }

    private RateLimitService.ConsumptionResult createAllowedResult() {
        return new RateLimitService.ConsumptionResult(true, 9, 0);
    }

    private RateLimitService.ConsumptionResult createRejectedResult() {
        return new RateLimitService.ConsumptionResult(false, 0, TimeUnit.SECONDS.toNanos(30));
    }
}

