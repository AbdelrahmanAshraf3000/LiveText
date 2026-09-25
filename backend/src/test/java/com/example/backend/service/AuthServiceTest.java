package com.example.backend.service;

import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.RegisterRequest;
import com.example.backend.entity.User;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest("alice", "Alice@Example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generate(any(), anyString()))
                .thenReturn(new JwtService.TokenPair("access-tok", "refresh-tok"));

        AuthService.AuthResult result = authService.register(request);

        assertThat(result.user().username()).isEqualTo("alice");
        assertThat(result.user().email()).isEqualTo("alice@example.com");
        assertThat(result.tokens().accessToken()).isEqualTo("access-tok");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh-tok");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void login_byUsername_success() {
        LoginRequest request = new LoginRequest("alice", "password123");
        User user = userFixture();
        when(userRepository.findByEmailOrUsername("alice", "alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generate(1L, "alice"))
                .thenReturn(new JwtService.TokenPair("access-tok", "refresh-tok"));

        AuthService.AuthResult result = authService.login(request);

        assertThat(result.tokens().accessToken()).isEqualTo("access-tok");
    }

    @Test
    void login_byEmail_lowercasesAndMatches() {
        LoginRequest request = new LoginRequest("Alice@Example.com", "password123");
        User user = userFixture();
        when(userRepository.findByEmailOrUsername("alice@example.com", "Alice@Example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generate(1L, "alice"))
                .thenReturn(new JwtService.TokenPair("access-tok", "refresh-tok"));

        AuthService.AuthResult result = authService.login(request);

        assertThat(result.user().username()).isEqualTo("alice");
    }

    @Test
    void login_unknownUser_throws() {
        LoginRequest request = new LoginRequest("ghost", "password123");
        when(userRepository.findByEmailOrUsername("ghost", "ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequest request = new LoginRequest("alice", "wrong");
        User user = userFixture();
        when(userRepository.findByEmailOrUsername("alice", "alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_validToken_returnsNewTokens() {
        when(jwtService.isValid("refresh-tok")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh-tok")).thenReturn(true);
        when(jwtService.getUserId("refresh-tok")).thenReturn(1L);
        when(jwtService.getUsername("refresh-tok")).thenReturn("alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(userFixture()));
        when(jwtService.generate(1L, "alice"))
                .thenReturn(new JwtService.TokenPair("access-new", "refresh-new"));

        AuthService.AuthResult result = authService.refresh("refresh-tok");

        assertThat(result.tokens().accessToken()).isEqualTo("access-new");
    }

    @Test
    void refresh_invalidToken_throws() {
        when(jwtService.isValid("bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private User userFixture() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed");
        return user;
    }
}