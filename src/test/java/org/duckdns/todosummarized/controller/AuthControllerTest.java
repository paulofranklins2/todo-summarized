package org.duckdns.todosummarized.controller;

import org.duckdns.todosummarized.config.JwtProperties;
import org.duckdns.todosummarized.dto.AuthTokenResponseDTO;
import org.duckdns.todosummarized.dto.UserLoginDTO;
import org.duckdns.todosummarized.dto.UserRegistrationDTO;
import org.duckdns.todosummarized.dto.UserResponseDTO;
import org.duckdns.todosummarized.exception.UserAlreadyExistsException;
import org.duckdns.todosummarized.service.JwtService;
import org.duckdns.todosummarized.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthController authController;

    private UserRegistrationDTO registrationDTO;
    private UserLoginDTO loginDTO;
    private UserResponseDTO userResponseDTO;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        registrationDTO = UserRegistrationDTO.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        loginDTO = UserLoginDTO.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        userResponseDTO = UserResponseDTO.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role("ROLE_USER")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userDetails = new User("test@example.com", "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * signUp returns 201 and auth token response when registration is successful
     */
    @Test
    void signUp_returnsCreated() {
        when(userService.registerUser(any())).thenReturn(userResponseDTO);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);

        ResponseEntity<AuthTokenResponseDTO> response = authController.signUp(registrationDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-token", response.getBody().getAccessToken());
        assertEquals("refresh-token", response.getBody().getRefreshToken());
        assertEquals("Bearer", response.getBody().getTokenType());

        verify(userService).registerUser(registrationDTO);
        verify(userDetailsService).loadUserByUsername("test@example.com");
    }

    /**
     * signUp throws when user already exists
     */
    @Test
    void signUp_throwsWhenUserAlreadyExists() {
        when(userService.registerUser(any()))
                .thenThrow(new UserAlreadyExistsException("test@example.com"));

        assertThrows(UserAlreadyExistsException.class,
                () -> authController.signUp(registrationDTO));

        verify(userService).registerUser(registrationDTO);
    }

    /**
     * signIn returns 200 and auth token response when authentication is successful
     */
    @Test
    void signIn_returnsOk() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);

        ResponseEntity<AuthTokenResponseDTO> response = authController.signIn(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-token", response.getBody().getAccessToken());
        assertEquals("refresh-token", response.getBody().getRefreshToken());
        assertEquals("Bearer", response.getBody().getTokenType());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("test@example.com");
    }

    /**
     * signIn throws BadCredentialsException when credentials are invalid
     */
    @Test
    void signIn_throwsWhenCredentialsInvalid() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authController.signIn(loginDTO));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, never()).loadUserByUsername(any());
    }
}

