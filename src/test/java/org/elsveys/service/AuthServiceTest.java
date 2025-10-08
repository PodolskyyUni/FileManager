package org.elsveys.service;

import org.elsveys.model.User;
import org.elsveys.repository.UserRepository;
import org.elsveys.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        User savedUser = new User();
        savedUser.setUserId(1L);
        savedUser.setUsername("newuser");
        savedUser.setPassword("encodedPassword");
        savedUser.setEmail("new@example.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(1L, "newuser")).thenReturn("token");

        String token = authService.register("newuser", "password123", "new@example.com");

        assertNotNull(token);
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenProvider, times(1)).generateToken(any(Long.class), anyString());
    }

    @Test
    void testRegisterUsernameAlreadyExists() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register("existinguser", "password123", "test@example.com");
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateToken(1L, "testuser")).thenReturn("jwt-token");

        String token = authService.login("testuser", "password123");

        assertNotNull(token);
        assertEquals("jwt-token", token);
        verify(tokenProvider, times(1)).generateToken(1L, "testuser");
    }

    @Test
    void testLoginInvalidUsername() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("nonexistent", "password123");
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void testLoginInvalidPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("testuser", "wrongpassword");
        });

        assertEquals("Invalid credentials", exception.getMessage());
        verify(tokenProvider, never()).generateToken(any(), any());
    }

    @Test
    void testValidateTokenSuccess() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);

        boolean result = authService.validateToken("valid-token");

        assertTrue(result);
        verify(tokenProvider, times(1)).validateToken("valid-token");
    }

    @Test
    void testValidateTokenFailure() {
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        boolean result = authService.validateToken("invalid-token");

        assertFalse(result);
    }

    @Test
    void testGetUserIdFromToken() {
        when(tokenProvider.getUserIdFromToken("test-token")).thenReturn(1L);

        Long userId = authService.getUserIdFromToken("test-token");

        assertEquals(1L, userId);
        verify(tokenProvider, times(1)).getUserIdFromToken("test-token");
    }

    @Test
    void testGetUsernameFromToken() {
        when(tokenProvider.getUsernameFromToken("test-token")).thenReturn("testuser");

        String username = authService.getUsernameFromToken("test-token");

        assertEquals("testuser", username);
        verify(tokenProvider, times(1)).getUsernameFromToken("test-token");
    }
}