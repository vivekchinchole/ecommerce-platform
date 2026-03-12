package com.ecommerce.user.service;

import com.ecommerce.user.dto.UserDto;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.ResourceNotFoundException;
import com.ecommerce.user.exception.UserAlreadyExistsException;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private UserService userService;

    private User user;
    private UserDto.RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("encoded_password")
                .role(User.Role.CUSTOMER)
                .build();

        registerRequest = new UserDto.RegisterRequest("John Doe", "john@example.com", "password123", User.Role.CUSTOMER);
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt_token");

        UserDto.AuthResponse response = userService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt_token");
        assertThat(response.getUser().getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ThrowsWhenEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("john@example.com");
    }

    @Test
    void login_Success() {
        UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("john@example.com", "password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt_token");

        UserDto.AuthResponse response = userService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt_token");
    }

    @Test
    void login_ThrowsOnInvalidPassword() {
        UserDto.LoginRequest loginRequest = new UserDto.LoginRequest("john@example.com", "wrongpassword");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void getUserById_ThrowsWhenNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
