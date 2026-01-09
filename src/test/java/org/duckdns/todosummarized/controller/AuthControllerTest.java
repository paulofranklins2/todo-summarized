package org.duckdns.todosummarized.controller;

import org.duckdns.todosummarized.dto.UserLoginDTO;
import org.duckdns.todosummarized.dto.UserRegistrationDTO;
import org.duckdns.todosummarized.dto.UserResponseDTO;
import org.duckdns.todosummarized.exception.UserAlreadyExistsException;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController authController;

    private UserRegistrationDTO registrationDTO;
    private UserLoginDTO loginDTO;
    private UserResponseDTO userResponseDTO;

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
    }

    /**
     * signUp returns 201 and user response when registration is successful
     */
    @Test
    void signUp_returnsCreated() {
        when(userService.registerUser(any())).thenReturn(userResponseDTO);

        ResponseEntity<UserResponseDTO> response = authController.signUp(registrationDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test@example.com", response.getBody().getEmail());
        assertEquals("ROLE_USER", response.getBody().getRole());

        verify(userService).registerUser(registrationDTO);
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
     * signIn returns 200 and user response when authentication is successful
     */
    @Test
    void signIn_returnsOk() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(userService.getUserProfile(any())).thenReturn(userResponseDTO);

        ResponseEntity<UserResponseDTO> response = authController.signIn(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test@example.com", response.getBody().getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserProfile("test@example.com");
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
        verify(userService, never()).getUserProfile(any());
    }
}

