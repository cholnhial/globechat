package dev.chol.globechat.controller;

import dev.chol.globechat.dto.UserDto;
import dev.chol.globechat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user endpoints.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        UserDto user = userService.getCurrentUserDto();
        return ResponseEntity.ok(user);
    }
}
