package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.user.UserDto;
import com.example.backend.entity.User;
import com.example.backend.security.AuthenticatedUser;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userService.getById(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(UserDto.from(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> search(
            @RequestParam String username,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<UserDto> results = userService.searchByUsername(username, principal.getId())
                .stream()
                .map(UserDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}