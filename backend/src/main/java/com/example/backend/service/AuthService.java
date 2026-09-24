package com.example.backend.service;

import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.RegisterRequest;
import com.example.backend.dto.user.UserDto;
import com.example.backend.entity.User;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public record AuthResult(UserDto user, JwtService.TokenPair tokens) {
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        String username = request.username().trim();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already in use: " + email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already taken: " + username);
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        JwtService.TokenPair tokens = jwtService.generate(user.getId(), user.getUsername());
        return new AuthResult(UserDto.from(user), tokens);
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByEmailOrUsername(identifier.toLowerCase(), identifier)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        JwtService.TokenPair tokens = jwtService.generate(user.getId(), user.getUsername());
        return new AuthResult(UserDto.from(user), tokens);
    }

    @Transactional(readOnly = true)
    public AuthResult refresh(String refreshToken) {
        if (!jwtService.isValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        Long userId = jwtService.getUserId(refreshToken);
        String username = jwtService.getUsername(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        JwtService.TokenPair tokens = jwtService.generate(user.getId(), username);
        return new AuthResult(UserDto.from(user), tokens);
    }
}